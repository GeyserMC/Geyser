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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
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
import java.util.concurrent.atomic.AtomicBoolean;
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
    /**
     * Adoption attempts for one persisted certificate before it is replaced
     * once rather than retried. Three spans the first two backoff steps, so
     * on an otherwise healthy server a transient rejection heals well inside
     * 45 minutes; the backoff is shared with every other maintenance
     * failure, so on a server already retrying at the maximum the same three
     * attempts span considerably longer.
     */
    private static final int MAX_ADOPTION_ATTEMPTS = 3;
    /**
     * Returned when the trust store could not be consulted at all, which is
     * a statement about the environment rather than about a certificate.
     * Compared by identity, so it can never collide with a real rejection.
     */
    private static final String EVALUATION_UNAVAILABLE = "the platform trust store could not be consulted";
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

    /**
     * The served certificate, published as one reference so a reader can
     * never pair one adoption's context with another's expiry (two separate
     * references allowed serving an already expired context under a freshly
     * written expiry during a renewal). Carries the whole chain so adoption
     * can both recognize whether the certificate on disk is the one being
     * served and re check what it is about to keep, plus its own expiry
     * warn flag so a reader racing a renewal cannot consume the fresh
     * certificate's warning.
     */
    private record Published(SslContext context, List<X509Certificate> chain, AtomicBoolean expiryWarned) {
        X509Certificate leaf() {
            return chain.get(0);
        }

        Instant expiry() {
            return leaf().getNotAfter().toInstant();
        }
    }

    private final AtomicReference<Published> published = new AtomicReference<>();
    // Warn once per distinct rejection cause rather than per streak: a cause
    // that changes (clock skew healing into a path building failure) is news
    // and must not be swallowed. Only the ACME thread touches these.
    private String warnedRejection;
    // Adoption of a persisted certificate is retried on the failure backoff,
    // which heals transient rejections, but permanently rejected material (a
    // chain torn inside the intermediate parses clean and then fails path
    // building) must eventually be replaced rather than retried forever.
    private X509Certificate rejectedLeaf;
    private int rejectionAttempts;
    // The leaf this manager last issued as a replacement for rejected
    // material. Reissuing cannot fix a rejection the material is not the
    // cause of (a clock hours behind, a trust store without the ISRG root),
    // and a fresh certificate changes the leaf and so resets the attempt
    // counter, which would order a certificate every few passes forever and
    // exhaust the Let's Encrypt duplicate allowance. Holding the leaf rather
    // than a flag distinguishes the two cases that matter: our own
    // replacement being rejected again buys nothing and falls back to free
    // adoption retries, while material that arrives from somewhere else
    // (an operator dropping in a file) is worth one attempt of its own.
    private X509Certificate replacementLeaf;
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

    /**
     * The TLS context to serve, or null while no valid certificate exists.
     * An expired certificate is withdrawn rather than served: clients treat
     * an invalid certificate as terminal for signaling and go to RakNet,
     * while a server that refuses TLS outright sends 26.40 and later
     * clients to plain HTTP and their TOFU flow, keeping NetherNet alive.
     * Consulted per connection, so withdrawal needs no rebind and reverses
     * itself the moment a renewal succeeds.
     */
    public SslContext currentContext() {
        Published current = published.get();
        if (current == null) {
            return null;
        }
        if (!Instant.now().isBefore(current.expiry())) {
            if (current.expiryWarned().compareAndSet(false, true)) {
                logger.warning(LOG_PREFIX + "HTTP signaling certificate expired " + current.expiry()
                        + " and renewal has not succeeded; withdrawing TLS so updated clients"
                        + " fall back to plain HTTP with their trust prompt");
            }
            return null;
        }
        return current.context();
    }

    /**
     * Publishes a freshly adopted or issued certificate atomically with its
     * expiry, unless a stock client would reject it, in which case TLS is
     * withdrawn instead: serving rejected material sends clients to RakNet,
     * refusing TLS sends 26.40 and later to plain HTTP and their TOFU flow.
     * For a certificate this manager just obtained from Let's Encrypt a
     * rejection is pathological (a JVM trust store without the ISRG root),
     * but the same gate applies to every certificate regardless of origin.
     */
    private boolean adoptContext(SslContext context, List<X509Certificate> chain) {
        String rejection = clientRejectionReason(chain);
        if (rejection != null) {
            // A previously published certificate may be kept rather than
            // dropping working TLS because a replacement failed validation,
            // but only if it would still pass the gate today. Passing once
            // is not enough: the anchor it chains to can be removed mid
            // life, and serving material clients now reject is the one
            // outcome withdrawal exists to prevent, so re check it here.
            Published current = published.get();
            // Keep only what still passes today, but do not drop a working
            // certificate because the trust store was momentarily
            // unreadable: that says nothing about the material, and the
            // dropped context exists only in memory.
            boolean keeping = current != null
                    && currentContext() != null
                    && stillAcceptable(current.chain());
            if (current != null && !keeping) {
                published.set(null);
            }
            String message = LOG_PREFIX + "Certificate not served: a stock client rejects it ("
                    + rejection + "). "
                    + (keeping
                            ? "Keeping the previously adopted certificate"
                            : "TLS is withdrawn; updated clients fall back to plain HTTP with"
                                    + " their trust prompt");
            // Once per distinct cause: the backoff retries adoption every 15
            // minutes to 6 hours, so a permanently rejecting trust store must
            // not repeat this forever, but a cause that changed is news.
            if (rejection.equals(warnedRejection)) {
                logger.debug(message);
            } else {
                warnedRejection = rejection;
                logger.warning(message);
            }
            return false;
        }
        published.set(new Published(context, chain, new AtomicBoolean()));
        warnedRejection = null;
        rejectedLeaf = null;
        rejectionAttempts = 0;
        replacementLeaf = null;
        return true;
    }

    /**
     * Why a stock client would reject this chain, or null if it would not:
     * temporal validity and a path to the platform trust anchors, checked
     * the way a client checks them. Deliberately unchecked: SAN matching
     * (the server cannot know which address clients dial) and privately
     * trusted certificates (a client with a hand loaded trust store is
     * rejected here). Source agnostic; the manual certificate path applies
     * the same gate.
     */
    static String clientRejectionReason(List<X509Certificate> chain) {
        if (chain.isEmpty()) {
            return "no certificates in the file";
        }
        try {
            TrustManagerFactory factory;
            try {
                factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                factory.init((KeyStore) null);
            } catch (Exception e) {
                // The trust store itself could not be consulted, which says
                // nothing about this certificate. Adoption still refuses,
                // since unvalidated material must not be served, but the
                // caller deciding whether to KEEP a working certificate can
                // tell this apart from a real rejection.
                return EVALUATION_UNAVAILABLE;
            }
            for (TrustManager manager : factory.getTrustManagers()) {
                if (manager instanceof X509TrustManager trustManager) {
                    // The authType parameter takes TLS authentication types,
                    // not key algorithms; UNKNOWN is what JSSE itself passes
                    // on every TLS 1.3 connection (suites stopped encoding
                    // the auth type) and the JDK accepts it, while a key
                    // algorithm like EC is rejected as an unknown authType.
                    trustManager.checkServerTrusted(chain.toArray(new X509Certificate[0]), "UNKNOWN");
                    return null;
                }
            }
            return EVALUATION_UNAVAILABLE;
        } catch (Exception e) {
            // Never null: a null here would read as acceptance at the call
            // sites and serve an unvalidated certificate.
            return e.getMessage() != null ? e.getMessage() : e.toString();
        }
    }

    /**
     * Whether an already served chain may go on being served. Unlike
     * adoption, which refuses anything it cannot validate, this keeps a
     * certificate whose verdict is merely unavailable: dropping a working
     * context because the trust store was unreadable for a moment would
     * withdraw TLS for something that is not the certificate's fault.
     */
    private static boolean stillAcceptable(List<X509Certificate> chain) {
        String rejection = clientRejectionReason(chain);
        return rejection == null || rejection == EVALUATION_UNAVAILABLE;
    }

    /**
     * Why this private key is not the certificate's counterpart, or null if
     * it is. Both certificate sources check this themselves because netty's
     * context builders accept a mismatched pair and fail only at handshake
     * time, which the client reads as an invalid certificate and answers by
     * abandoning NetherNet. A certificate key algorithm that cannot be
     * signed with here skips the check rather than rejecting material netty
     * would accept.
     */
    static String keyMismatchReason(PrivateKey key, PublicKey certKey) {
        String signatureAlgorithm = switch (certKey.getAlgorithm()) {
            case "EC" -> "SHA256withECDSA";
            case "RSA" -> "SHA256withRSA";
            // EdDSA rather than Ed25519: the JDK reports both Ed25519 and
            // Ed448 keys as EdDSA, so naming the curve here would hand an
            // Ed448 key to an Ed25519 signature and read a matching pair as
            // a mismatch. The generic name binds to whatever the key is.
            case "EdDSA", "Ed25519", "Ed448" -> "EdDSA";
            default -> null;
        };
        if (signatureAlgorithm == null) {
            return null;
        }
        if (!normalizeKeyAlgorithm(key.getAlgorithm()).equals(normalizeKeyAlgorithm(certKey.getAlgorithm()))) {
            return "the key is " + key.getAlgorithm() + " but the certificate holds " + certKey.getAlgorithm();
        }
        // Both halves parsed with matching algorithms, so a failed sign and
        // verify round trip means mismatch, a thrown verification error
        // included: well formed material that cannot verify is exactly what
        // this check exists to catch.
        try {
            byte[] payload = new byte[32];
            Signature signer = Signature.getInstance(signatureAlgorithm);
            signer.initSign(key);
            signer.update(payload);
            byte[] signature = signer.sign();
            Signature verifier = Signature.getInstance(signatureAlgorithm);
            verifier.initVerify(certKey);
            verifier.update(payload);
            return verifier.verify(signature) ? null : "the key does not match the certificate";
        } catch (Exception e) {
            return "the key does not match the certificate ("
                    + (e.getMessage() != null ? e.getMessage() : e.toString()) + ")";
        }
    }

    static String normalizeKeyAlgorithm(String algorithm) {
        return algorithm.startsWith("Ed") ? "EdDSA" : algorithm;
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
            String detail = e.getMessage() != null ? e.getMessage() : e.toString();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                detail += ": " + e.getCause().getMessage();
            }
            String message = LOG_PREFIX + "Certificate maintenance failed (" + detail
                    + "); retrying in " + backoffMinutes + " minutes. HTTP signaling stays "
                    + (currentContext() != null ? "on the previous certificate" : "plaintext")
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
            // Every way the persisted material can be unusable converges on
            // one reissue with one accurate reason. A torn write is the
            // motivating case and takes three shapes, measured by truncating
            // a real chain at every offset: only a zero byte file yields an
            // empty collection, a tear inside the leaf throws (about 46% of
            // offsets), and a tear past the leaf parses clean as a short
            // chain that then fails path building (about 54%).
            String unusable = null;
            List<X509Certificate> chain = List.of();
            try {
                chain = readChain(certFile);
                if (chain.isEmpty()) {
                    unusable = "the certificate file is empty";
                }
            } catch (Exception e) {
                unusable = "the certificate file is unreadable ("
                        + (e.getMessage() != null ? e.getMessage() : e.toString()) + ")";
            }
            if (unusable == null) {
                X509Certificate leaf = chain.get(0);
                if (!needsRenewal(leaf.getNotBefore().toInstant(), leaf.getNotAfter().toInstant(), Instant.now())) {
                    // Adopt whenever the certificate on disk is not the one
                    // being served: that covers boot (nothing published), a
                    // kept previous certificate whose rejected replacement
                    // has since become acceptable (clock skew healed), and
                    // the expired kept case. Comparing leaves rather than
                    // asking whether anything is serving is what lets the
                    // backoff genuinely retry a rejected replacement.
                    Published current = published.get();
                    // Re check what is being served, not just whether it is
                    // the same file: the anchor a certificate chains to can
                    // be removed mid life, and this steady state pass is the
                    // only place that would ever notice. A rejection here
                    // falls through to the adoption path, which withdraws.
                    if (current != null && current.leaf().equals(leaf)
                            && stillAcceptable(current.chain())) {
                        return;
                    }
                    SslContext context = null;
                    try {
                        context = buildContext(certFile, domainKeyFile);
                    } catch (Exception e) {
                        // Includes the key not being the certificate's
                        // counterpart, which no retry can heal: only fresh
                        // material can.
                        unusable = "the certificate and key are unusable together ("
                                + (e.getMessage() != null ? e.getMessage() : e.toString()) + ")";
                    }
                    if (context != null) {
                        if (adoptContext(context, chain)) {
                            logger.info(LOG_PREFIX + "HTTP signaling certificate loaded (valid until "
                                    + leaf.getNotAfter().toInstant() + ")");
                            return;
                        }
                        if (recordRejection(leaf) < MAX_ADOPTION_ATTEMPTS || leaf.equals(replacementLeaf)) {
                            // Fail the pass so the backoff retries adoption,
                            // which heals temporal rejections for free
                            // without spending rate limits on a certificate
                            // that is fresh by age. Once a replacement has
                            // already been tried this is also the permanent
                            // resting state: the cause is not the material,
                            // so ordering more certificates cannot help.
                            throw new IllegalStateException("persisted certificate rejected; retrying adoption");
                        }
                        // Retries exhausted and no replacement tried yet:
                        // the material itself may be the problem, so replace
                        // it once and judge the result.
                        unusable = "the certificate stayed rejected across " + MAX_ADOPTION_ATTEMPTS
                                + " adoption attempts";
                    }
                }
            }
            if (unusable != null) {
                logger.warning(LOG_PREFIX + "Replacing the persisted certificate: " + unusable);
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

    /**
     * Counts consecutive rejections of the same persisted leaf, restarting
     * the count whenever the material on disk changes.
     */
    private int recordRejection(X509Certificate leaf) {
        if (!leaf.equals(rejectedLeaf)) {
            rejectedLeaf = leaf;
            rejectionAttempts = 0;
        }
        return ++rejectionAttempts;
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
        writeAtomically(certFile, pem.toString());

        List<X509Certificate> chain = readChain(certFile);
        // Remember what was just issued, whatever the gate makes of it: a
        // certificate this fresh being rejected means the cause is not the
        // material, so ordering another cannot help and the manager falls
        // back to free adoption retries for exactly this leaf.
        replacementLeaf = chain.isEmpty() ? null : chain.get(0);
        if (!adoptContext(buildContext(certFile, domainKeyFile), chain)) {
            // Surface the rejection as a maintenance failure so the backoff
            // retries adoption of the persisted certificate; a temporal
            // rejection of a just issued certificate heals within it.
            throw new IllegalStateException("issued certificate rejected by the stock client gate");
        }
        logger.info(LOG_PREFIX + "HTTP signaling certificate issued for " + ip.getHostAddress()
                + " (valid until " + chain.get(0).getNotAfter().toInstant() + ")");
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
    private KeyPair loadOrCreateKeyPair(Path file) throws Exception {
        if (Files.exists(file)) {
            try {
                String pem = Files.readString(file, StandardCharsets.UTF_8);
                KeyFactory keyFactory = KeyFactory.getInstance("EC");
                return new KeyPair(
                        keyFactory.generatePublic(new X509EncodedKeySpec(readPemBlock(pem, "PUBLIC KEY"))),
                        keyFactory.generatePrivate(new PKCS8EncodedKeySpec(readPemBlock(pem, "PRIVATE KEY"))));
            } catch (Exception e) {
                // An unreadable key file used to wedge issuance forever, and
                // a torn one can even leave the private block loadable while
                // the public block is gone, so the certificate keeps serving
                // while renewal never succeeds again. Regenerating recovers.
                // For the domain key any certificate issued against the old
                // one is invalidated by the caller, which reissues with this
                // one; for the account key the old ACME registration is
                // abandoned rather than reused, so say which is happening.
                logger.warning(LOG_PREFIX + "Replacing " + file.getFileName() + ", which is unreadable ("
                        + (e.getMessage() != null ? e.getMessage() : e.toString()) + "). "
                        + (file.getFileName().toString().startsWith("account")
                                ? "A new Let's Encrypt account is registered and the previous one abandoned"
                                : "A replacement certificate is requested for the new key"));
            }
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair pair = generator.generateKeyPair();
        writeAtomically(file,
                writePemBlock(pair.getPrivate().getEncoded(), "PRIVATE KEY")
                        + writePemBlock(pair.getPublic().getEncoded(), "PUBLIC KEY"));
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
        if (chain.isEmpty()) {
            throw new IllegalStateException("the certificate file holds no certificate");
        }
        // netty builds a context from a mismatched pair without complaint,
        // producing handshakes no client can verify. The pair can drift
        // apart here the same way it can on the manual path: an interrupted
        // issuance leaves a new key beside the old certificate.
        String mismatch = keyMismatchReason(key, chain.get(0).getPublicKey());
        if (mismatch != null) {
            throw new IllegalStateException(mismatch);
        }
        return SslContextBuilder.forServer(key, chain.toArray(new X509Certificate[0])).build();
    }

    /**
     * Replaces a file in one step. A crash or full disk mid write must not
     * leave truncated material behind: a torn certificate wedges adoption
     * and a torn key file can silently split into a state where the served
     * certificate still works but renewal can never succeed again.
     */
    private static void writeAtomically(Path file, String content) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.writeString(tempFile, content, StandardCharsets.UTF_8);
            try {
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            // A failed write or move must not strand the temporary file; the
            // name is fixed, so a leftover would only be overwritten, but
            // leaving debris in the operator's config folder is worse. A
            // successful move already removed it, so this is normally a no
            // op, and its own failure must not replace the real cause.
            try {
                Files.deleteIfExists(tempFile);
            } catch (Exception ignored) {
            }
        }
    }

    /** The certificate chain from a PEM file, leaf first. */
    private static List<X509Certificate> readChain(Path certFile) throws Exception {
        try (var in = Files.newInputStream(certFile)) {
            return CertificateFactory.getInstance("X.509").generateCertificates(in).stream()
                    .map(X509Certificate.class::cast).toList();
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
