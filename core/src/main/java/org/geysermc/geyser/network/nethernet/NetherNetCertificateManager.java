/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.network.nethernet;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.geysermc.geyser.GeyserLogger;
import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Identifier;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Session;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.challenge.TlsAlpn01Challenge;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.CertificateUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Automatic HTTPS for the NetherNet HTTP signaling listener: obtains and
 * renews a Let's Encrypt certificate for the server's public IP address
 * (current clients validate against the IP, so only IP certificates work).
 *
 * Active only when the operator has not supplied a manual
 * nethernet/cert.pem + key.pem pair. IP certificates come from the
 * "shortlived" profile (about 6.5 days of validity), so renewal is a
 * first class loop: re issue at two thirds of the lifetime, roughly every
 * 4.5 days, and atomically swap the TLS context; signaling connections are
 * one shot, so every new connection picks the fresh certificate up.
 *
 * Failure semantics are forgiving throughout: issuance needs port 80
 * (HTTP-01) or port 443 (TLS-ALPN-01) reachable from the internet during
 * the challenge. A port that cannot be bound falls through to the other
 * challenge type within the same attempt; a triggered challenge that fails
 * validation kills its authorization (RFC 8555), so the preferred type also
 * alternates across retries for redundancy against one blocked port. Any
 * failure (ports busy, NAT, rate limits, no public IPv4) logs a warning,
 * backs off, and leaves the listener serving plaintext; clients silently
 * fall back to RakNet and nobody is locked out.
 *
 * Deliberately configuration free: manual certificate files are the opt
 * out (they take precedence and this manager is never created), the
 * certified IP is the auto detected public address, and the Let's Encrypt
 * staging environment is reachable for testing via the
 * geyser.nethernet.acme.staging system property.
 */
public final class NetherNetCertificateManager implements AutoCloseable {

    private static final String LOG_PREFIX = "[Nethernet] ";
    private static final Duration CHECK_INTERVAL = Duration.ofHours(6);
    private static final Duration RETRY_BASE = Duration.ofMinutes(15);
    private static final Duration RETRY_MAX = Duration.ofHours(6);
    private static final Duration CHALLENGE_TIMEOUT = Duration.ofSeconds(90);
    private static final List<String> IP_SERVICES = List.of(
            "https://checkip.amazonaws.com", "https://api.ipify.org", "https://icanhazip.com");

    static {
        // acme4j's CSR and TLS-ALPN certificate builders address BouncyCastle
        // through the JCA by name; having it on the classpath is not enough.
        if (java.security.Security.getProvider(org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME) == null) {
            java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }
    }

    private final GeyserLogger logger;
    private final Path acmeDir;
    private final boolean staging = Boolean.getBoolean("geyser.nethernet.acme.staging");

    private final AtomicReference<SslContext> currentContext = new AtomicReference<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Nethernet ACME");
        thread.setDaemon(true);
        // acme4j discovers its providers via ServiceLoader on the thread
        // context classloader. On plugin platforms (Velocity) the inherited
        // context loader cannot see the plugin jar, yielding "No ACME
        // provider found"; pin the loader that holds our shaded classes.
        thread.setContextClassLoader(NetherNetCertificateManager.class.getClassLoader());
        return thread;
    });
    private int consecutiveFailures;
    // Environments where issuance can never succeed (LAN only, no public
    // reachability) should not warn every backoff period forever: the first
    // failure warns, repeats log at debug until something changes.
    private boolean failureWarned;
    private volatile boolean closed;

    public NetherNetCertificateManager(Path nethernetDir, GeyserLogger logger) {
        this.logger = logger;
        this.acmeDir = nethernetDir.resolve("acme");
    }

    /** The TLS context to serve, or null while no valid certificate exists. */
    public SslContext currentContext() {
        return currentContext.get();
    }

    /**
     * Starts the maintenance loop: adopt a persisted certificate immediately
     * if still fresh, otherwise issue, then keep renewing at two thirds of
     * each certificate's lifetime.
     */
    public void start() {
        scheduler.execute(this::runMaintenance);
    }

    private void runMaintenance() {
        if (closed) {
            return;
        }
        Duration nextCheck = CHECK_INTERVAL;
        try {
            ensureCertificate();
            consecutiveFailures = 0;
            failureWarned = false;
        } catch (Exception e) {
            consecutiveFailures++;
            long backoffMinutes = Math.min(RETRY_MAX.toMinutes(),
                    RETRY_BASE.toMinutes() << Math.min(consecutiveFailures - 1, 5));
            nextCheck = Duration.ofMinutes(backoffMinutes);
            String detail = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                detail += ": " + e.getCause().getMessage();
            }
            String message = LOG_PREFIX + "Certificate maintenance failed (" + detail
                    + "); retrying in " + backoffMinutes + " minutes. HTTP signaling stays "
                    + (currentContext.get() != null ? "on the previous certificate" : "plaintext")
                    + " and clients fall back to RakNet as needed";
            // Warn once per failure streak; permanently unreachable servers
            // (LAN only) otherwise repeat this every backoff period forever.
            if (failureWarned) {
                logger.debug(message);
            } else {
                failureWarned = true;
                logger.warning(message);
            }
        }
        if (!closed) {
            scheduler.schedule(this::runMaintenance, nextCheck.toSeconds(), TimeUnit.SECONDS);
        }
    }

    private void ensureCertificate() throws Exception {
        Path certFile = acmeDir.resolve("cert.pem");
        Path domainKeyFile = acmeDir.resolve("domain-key.pem");

        if (Files.exists(certFile) && Files.exists(domainKeyFile)) {
            X509Certificate leaf = readLeafCertificate(certFile);
            if (!needsRenewal(leaf.getNotBefore().toInstant(), leaf.getNotAfter().toInstant(), Instant.now())) {
                if (currentContext.get() == null) {
                    currentContext.set(buildContext(certFile, domainKeyFile));
                    logger.info(LOG_PREFIX + "HTTP signaling certificate loaded (valid until "
                            + leaf.getNotAfter().toInstant() + ")");
                }
                return;
            }
        }
        issueCertificate(certFile, domainKeyFile);
    }

    /**
     * Renew once two thirds of the certificate lifetime has elapsed. With
     * the roughly 6.5 day shortlived profile that means a fresh certificate
     * about every 4.5 days, leaving 2 days of margin for outages.
     */
    static boolean needsRenewal(Instant notBefore, Instant notAfter, Instant now) {
        Instant renewAt = notBefore.plus(Duration.between(notBefore, notAfter).dividedBy(3).multipliedBy(2));
        return !now.isBefore(renewAt);
    }

    private void issueCertificate(Path certFile, Path domainKeyFile) throws Exception {
        InetAddress ip = resolvePublicIp();
        logger.info(LOG_PREFIX + "Requesting a Let's Encrypt certificate for " + ip.getHostAddress()
                + (staging ? " (staging)" : "") + "; this accepts the Let's Encrypt subscriber agreement");

        Files.createDirectories(acmeDir);
        Session session = new Session(staging ? "acme://letsencrypt.org/staging" : "acme://letsencrypt.org");
        Account account = new AccountBuilder()
                .agreeToTermsOfService()
                .useKeyPair(loadOrCreateKeyPair(acmeDir.resolve("account-key.pem")))
                .create(session);

        // IP identifiers are only issued under the short lived profile.
        Order order = account.newOrder()
                .identifier(Identifier.ip(ip))
                .profile("shortlived")
                .create();

        // Alternate the preferred challenge type across failed attempts: a
        // host with one blocked port still converges on the working one.
        boolean preferTlsAlpn = (consecutiveFailures % 2) == 1;
        for (Authorization auth : order.getAuthorizations()) {
            if (auth.getStatus() != Status.VALID) {
                completeChallenge(auth, preferTlsAlpn);
            }
        }

        KeyPair domainKeyPair = loadOrCreateKeyPair(domainKeyFile);
        CSRBuilder csr = new CSRBuilder();
        csr.addIP(ip);
        csr.sign(domainKeyPair);
        order.execute(csr.getEncoded());

        awaitStatus(CHALLENGE_TIMEOUT, "order", () -> {
            order.fetch();
            return order.getStatus();
        });

        Certificate certificate = order.getCertificate();
        StringWriter pem = new StringWriter();
        certificate.writeCertificate(pem);
        Files.writeString(certFile, pem.toString(), StandardCharsets.UTF_8);

        currentContext.set(buildContext(certFile, domainKeyFile));
        X509Certificate leaf = readLeafCertificate(certFile);
        logger.info(LOG_PREFIX + "HTTP signaling certificate issued for " + ip.getHostAddress()
                + " (valid until " + leaf.getNotAfter().toInstant() + ")");
    }

    /**
     * Completes one authorization via HTTP-01 (port 80) or TLS-ALPN-01
     * (port 443). Ports are bound on demand and released right after the
     * validation; below 1024 they need privileges on Linux (setcap,
     * authbind, or net.ipv4.ip_unprivileged_port_start). A port that cannot
     * be bound falls through to the other challenge type; a triggered
     * challenge that fails validation invalidates the whole authorization,
     * so no second type is attempted then (the retry alternates instead).
     */
    private void completeChallenge(Authorization auth, boolean preferTlsAlpn) throws Exception {
        StringBuilder bindFailures = new StringBuilder();
        boolean[] tryAlpnFirst = preferTlsAlpn ? new boolean[]{true, false} : new boolean[]{false, true};
        for (boolean alpn : tryAlpnFirst) {
            if (alpn ? tryTlsAlpnChallenge(auth, bindFailures) : tryHttpChallenge(auth, bindFailures)) {
                return;
            }
        }
        throw new IllegalStateException("no usable ACME challenge (" + bindFailures
                + "); grant the process port 80 or 443, or provide a manual certificate");
    }

    /** @return true when validated; false when the challenge could not even start. */
    private boolean tryHttpChallenge(Authorization auth, StringBuilder bindFailures) throws Exception {
        Http01Challenge challenge = auth.findChallenge(Http01Challenge.class).orElse(null);
        if (challenge == null) {
            appendFailure(bindFailures, "HTTP-01 not offered");
            return false;
        }
        try (HttpChallengeServer server = new HttpChallengeServer()) {
            server.tokens.put(challenge.getToken(), challenge.getAuthorization());
            try {
                server.bind();
            } catch (Exception e) {
                appendFailure(bindFailures, "port 80: " + e.getMessage());
                return false;
            }
            awaitValidation(auth, challenge::trigger);
            return true;
        }
    }

    /** @return true when validated; false when the challenge could not even start. */
    private boolean tryTlsAlpnChallenge(Authorization auth, StringBuilder bindFailures) throws Exception {
        TlsAlpn01Challenge challenge = auth.findChallenge(TlsAlpn01Challenge.class).orElse(null);
        if (challenge == null) {
            appendFailure(bindFailures, "TLS-ALPN-01 not offered");
            return false;
        }
        // Self signed throwaway certificate carrying the acmeValidation
        // extension; presenting it in an acme-tls/1 handshake IS the
        // validation, no bytes are served. RSA because acme4j's builder
        // signs the challenge certificate with SHA256withRSA.
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair challengeKey = generator.generateKeyPair();
        X509Certificate challengeCert = CertificateUtils.createTlsAlpn01Certificate(
                challengeKey, auth.getIdentifier(), challenge.getAcmeValidation());
        SslContext challengeContext = SslContextBuilder.forServer(challengeKey.getPrivate(), challengeCert)
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                        ApplicationProtocolConfig.Protocol.ALPN,
                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                        TlsAlpn01Challenge.ACME_TLS_1_PROTOCOL))
                .build();

        try (TlsAlpnChallengeServer server = new TlsAlpnChallengeServer()) {
            try {
                server.bind(challengeContext);
            } catch (Exception e) {
                appendFailure(bindFailures, "port 443: " + e.getMessage());
                return false;
            }
            awaitValidation(auth, challenge::trigger);
            return true;
        }
    }

    private static void appendFailure(StringBuilder failures, String message) {
        if (failures.length() > 0) {
            failures.append("; ");
        }
        failures.append(message);
    }

    private static void awaitValidation(Authorization auth, ThrowingRunnable trigger) throws Exception {
        trigger.run();
        awaitStatus(CHALLENGE_TIMEOUT, "challenge", () -> {
            auth.fetch();
            return auth.getStatus();
        });
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    /** Polls an ACME resource until it settles, failing on INVALID or timeout. */
    private static void awaitStatus(Duration timeout, String what, StatusPoller poller) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        while (true) {
            Status status = poller.poll();
            if (status == Status.VALID) {
                return;
            }
            if (status == Status.INVALID) {
                throw new IllegalStateException("ACME " + what + " failed validation");
            }
            if (Instant.now().isAfter(deadline)) {
                throw new IllegalStateException("ACME " + what + " timed out in status " + status);
            }
            Thread.sleep(3000);
        }
    }

    @FunctionalInterface
    private interface StatusPoller {
        Status poll() throws Exception;
    }

    private InetAddress resolvePublicIp() throws IOException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        for (String service : IP_SERVICES) {
            try {
                HttpResponse<String> response = client.send(
                        HttpRequest.newBuilder(URI.create(service)).timeout(Duration.ofSeconds(5)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                String body = response.body().trim();
                if (response.statusCode() == 200 && body.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
                    InetAddress address = InetAddress.getByName(body);
                    if (!address.isSiteLocalAddress() && !address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
                        return address;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("interrupted while detecting the public IP");
            } catch (Exception ignored) {
                // Try the next service.
            }
        }
        throw new IOException("could not determine a public IPv4 address");
    }

    /**
     * Loads a persisted P-256 keypair (private PKCS#8 and public SPKI PEM
     * blocks in one file), generating and persisting one on first use.
     */
    private static KeyPair loadOrCreateKeyPair(Path file) throws Exception {
        if (Files.exists(file)) {
            String pem = Files.readString(file, StandardCharsets.UTF_8);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return new KeyPair(
                    keyFactory.generatePublic(new X509EncodedKeySpec(readPemBlock(pem, "PUBLIC KEY"))),
                    keyFactory.generatePrivate(new PKCS8EncodedKeySpec(readPemBlock(pem, "PRIVATE KEY"))));
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair pair = generator.generateKeyPair();
        Files.createDirectories(file.getParent());
        Files.writeString(file,
                writePemBlock(pair.getPrivate().getEncoded(), "PRIVATE KEY")
                        + writePemBlock(pair.getPublic().getEncoded(), "PUBLIC KEY"),
                StandardCharsets.UTF_8);
        return pair;
    }

    private static SslContext buildContext(Path certFile, Path domainKeyFile) throws Exception {
        // The domain key file carries both PEM blocks; netty wants only the
        // PKCS#8 private key, so hand it the isolated block.
        String pem = Files.readString(domainKeyFile, StandardCharsets.UTF_8);
        byte[] keyDer = readPemBlock(pem, "PRIVATE KEY");
        java.security.PrivateKey key = KeyFactory.getInstance("EC")
                .generatePrivate(new PKCS8EncodedKeySpec(keyDer));
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        List<X509Certificate> chain;
        try (var in = Files.newInputStream(certFile)) {
            chain = factory.generateCertificates(in).stream()
                    .map(X509Certificate.class::cast).toList();
        }
        return SslContextBuilder.forServer(key, chain.toArray(new X509Certificate[0])).build();
    }

    private static X509Certificate readLeafCertificate(Path certFile) throws Exception {
        try (var in = Files.newInputStream(certFile)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
    }

    private static byte[] readPemBlock(String pem, String label) {
        String begin = "-----BEGIN " + label + "-----";
        String end = "-----END " + label + "-----";
        int start = pem.indexOf(begin);
        int stop = pem.indexOf(end);
        if (start < 0 || stop < 0) {
            throw new IllegalArgumentException("missing " + label + " PEM block");
        }
        return Base64.getDecoder().decode(pem.substring(start + begin.length(), stop).replaceAll("\\s", ""));
    }

    private static String writePemBlock(byte[] der, String label) {
        return "-----BEGIN " + label + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(der)
                + "\n-----END " + label + "-----\n";
    }

    @Override
    public void close() {
        closed = true;
        scheduler.shutdownNow();
    }

    /** Presents the TLS-ALPN-01 challenge certificate on port 443; the handshake is the validation. */
    private static final class TlsAlpnChallengeServer implements AutoCloseable {
        private NioEventLoopGroup group;
        private Channel channel;

        void bind(SslContext challengeContext) throws Exception {
            group = new NioEventLoopGroup(1);
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(group)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(challengeContext.newHandler(ch.alloc()));
                            ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                                    if (evt instanceof SslHandshakeCompletionEvent) {
                                        ctx.close();
                                    }
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    ctx.close();
                                }
                            });
                        }
                    });
            channel = bootstrap.bind(new InetSocketAddress(443)).sync().channel();
        }

        @Override
        public void close() {
            if (channel != null) {
                channel.close().syncUninterruptibly();
            }
            if (group != null) {
                group.shutdownGracefully(0, 2, TimeUnit.SECONDS).syncUninterruptibly();
            }
        }
    }

    /** Minimal one purpose HTTP server answering ACME HTTP-01 lookups on port 80. */
    private static final class HttpChallengeServer implements AutoCloseable {
        private static final String CHALLENGE_PATH = "/.well-known/acme-challenge/";

        final Map<String, String> tokens = new ConcurrentHashMap<>();
        private NioEventLoopGroup group;
        private Channel channel;

        void bind() throws Exception {
            group = new NioEventLoopGroup(1);
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(group)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(8192));
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
                                    String content = null;
                                    if (HttpMethod.GET.equals(request.method()) && request.uri().startsWith(CHALLENGE_PATH)) {
                                        content = tokens.get(request.uri().substring(CHALLENGE_PATH.length()));
                                    }
                                    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                            content != null ? HttpResponseStatus.OK : HttpResponseStatus.NOT_FOUND,
                                            Unpooled.copiedBuffer(content != null ? content : "Not found", StandardCharsets.UTF_8));
                                    response.headers()
                                            .set(HttpHeaderNames.CONTENT_TYPE, "text/plain")
                                            .setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    ctx.close();
                                }
                            });
                        }
                    });
            channel = bootstrap.bind(new InetSocketAddress(80)).sync().channel();
        }

        @Override
        public void close() {
            if (channel != null) {
                channel.close().syncUninterruptibly();
            }
            if (group != null) {
                group.shutdownGracefully(0, 2, TimeUnit.SECONDS).syncUninterruptibly();
            }
        }
    }
}
