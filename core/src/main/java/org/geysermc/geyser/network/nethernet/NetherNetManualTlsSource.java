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

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.geysermc.geyser.GeyserLogger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Serves the operator supplied TLS material for HTTP signaling. Consulted per
 * connection: cert.pem and key.pem are re-read whenever their modification
 * times change, so certificates can be swapped while the server is live, with
 * no rebind and no restart.
 *
 * Every certificate passes {@link NetherNetCertificateManager#clientRejectionReason}
 * before being served, and expiry is re-checked per connection, because a
 * certificate a stock client rejects is strictly worse than none: the 26.40
 * client treats an invalid certificate as terminal for signaling and goes to
 * RakNet, while a server that refuses TLS outright sends it to plain HTTP and
 * its TOFU flow. Returning null here is that refusal.
 *
 * Removing both files while live withdraws TLS but cannot hand over to
 * automatic certificate management, which only starts when no manual files
 * exist at bind; that handover needs a restart.
 */
final class NetherNetManualTlsSource implements Supplier<SslContext> {

    private static final String LOG_PREFIX = "[Nethernet] ";

    private final Path certPath;
    private final Path keyPath;
    private final GeyserLogger logger;

    /**
     * Change detection state: modification times and sizes of both files,
     * re-checked at most once per second so probe floods cannot turn the
     * per connection consultation into a syscall storm.
     */
    private static final long CHECK_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(1);
    private boolean checkedOnce;
    private long lastCheckNanos;
    private FileTime certTime;
    private FileTime keyTime;
    private long certSize;
    private long keySize;
    private SslContext context;
    private Instant expiry;
    private String problem;
    private boolean expiryWarned;
    private boolean missingWarned;

    NetherNetManualTlsSource(Path certPath, Path keyPath, GeyserLogger logger) {
        this.certPath = certPath;
        this.keyPath = keyPath;
        this.logger = logger;
    }

    @Override
    public synchronized SslContext get() {
        long now = System.nanoTime();
        if (!checkedOnce || now - lastCheckNanos >= CHECK_INTERVAL_NANOS) {
            checkedOnce = true;
            lastCheckNanos = now;
            checkFiles();
        }
        if (context == null) {
            return null;
        }
        if (expiry != null && !Instant.now().isBefore(expiry)) {
            if (!expiryWarned) {
                expiryWarned = true;
                logger.warning(LOG_PREFIX + "Manual certificate expired " + expiry
                        + "; withdrawing TLS so updated clients fall back to plain HTTP with"
                        + " their trust prompt. Replace or remove nethernet/cert.pem and key.pem");
            }
            return null;
        }
        return context;
    }

    /**
     * Why TLS is currently withheld, or null while a servable certificate is
     * loaded. Meant for the bind time mode log.
     */
    synchronized String currentProblem() {
        get();
        if (context == null) {
            return problem == null ? "certificate files unreadable" : problem;
        }
        if (expiry != null && !Instant.now().isBefore(expiry)) {
            return "certificate expired " + expiry;
        }
        return null;
    }

    private void checkFiles() {
        try {
            if (!Files.exists(certPath) || !Files.exists(keyPath)) {
                if (context != null && !missingWarned) {
                    missingWarned = true;
                    logger.warning(LOG_PREFIX + "Manual certificate files removed; withdrawing TLS."
                            + " Automatic certificate management takes over on the next restart");
                }
                context = null;
                certTime = null;
                keyTime = null;
                return;
            }
            missingWarned = false;
            FileTime certModified = Files.getLastModifiedTime(certPath);
            FileTime keyModified = Files.getLastModifiedTime(keyPath);
            long newCertSize = Files.size(certPath);
            long newKeySize = Files.size(keyPath);
            // Sizes participate so same tick swaps on coarse mtime
            // filesystems are still noticed when the content length moved.
            if (!certModified.equals(certTime) || !keyModified.equals(keyTime)
                    || newCertSize != certSize || newKeySize != keySize) {
                certTime = certModified;
                keyTime = keyModified;
                certSize = newCertSize;
                keySize = newKeySize;
                reload();
            }
        } catch (Exception e) {
            logger.debug(LOG_PREFIX + "Manual certificate check failed: " + e.getMessage());
        }
    }

    /**
     * Parses, validates, and builds the current files. Any failure leaves TLS
     * withdrawn rather than serving material a client would reject; the file
     * times stay recorded either way so a broken pair is not re-parsed on
     * every connection.
     */
    private void reload() {
        boolean replacing = context != null || problem != null;
        context = null;
        expiry = null;
        expiryWarned = false;
        try {
            List<X509Certificate> chain;
            try (var in = Files.newInputStream(certPath)) {
                chain = CertificateFactory.getInstance("X.509").generateCertificates(in).stream()
                        .map(X509Certificate.class::cast).toList();
            }
            String rejection = NetherNetCertificateManager.clientRejectionReason(chain);
            if (rejection != null) {
                problem = "a stock client rejects the certificate (" + rejection + ")";
                logger.warning(LOG_PREFIX + "Manual certificate not served: " + problem
                        + ". TLS is withdrawn; updated clients fall back to plain HTTP with"
                        + " their trust prompt");
                return;
            }
            String mismatch = keyMismatchReason(chain.get(0));
            if (mismatch != null) {
                problem = mismatch;
                logger.warning(LOG_PREFIX + "Manual certificate not served: " + problem
                        + ". TLS is withdrawn until the pair matches; a half finished swap"
                        + " heals when the second file lands");
                return;
            }
            context = SslContextBuilder.forServer(certPath.toFile(), keyPath.toFile()).build();
            expiry = chain.get(0).getNotAfter().toInstant();
            problem = null;
            if (replacing) {
                logger.info(LOG_PREFIX + "Manual certificate reloaded (valid until " + expiry + ")");
            }
        } catch (Exception e) {
            problem = "failed to load (" + (e.getMessage() != null ? e.getMessage() : e.toString()) + ")";
            logger.warning(LOG_PREFIX + "Manual certificate " + problem + "; TLS is withdrawn."
                    + " Fix or remove nethernet/cert.pem and key.pem");
        }
    }

    /**
     * Whether key.pem holds the private half of the certificate, so a half
     * finished swap (new cert copied, old key still in place) withdraws TLS
     * instead of serving handshakes no client can verify while we log
     * success. Netty's SslContextBuilder does not check correspondence. A
     * key this cannot inspect (PKCS#1 armor, encrypted, exotic algorithm)
     * skips the check rather than rejecting material netty may accept.
     */
    private String keyMismatchReason(X509Certificate leaf) {
        PrivateKey key;
        try {
            String pem = Files.readString(keyPath, StandardCharsets.UTF_8);
            int begin = pem.indexOf("-----BEGIN PRIVATE KEY-----");
            int end = pem.indexOf("-----END PRIVATE KEY-----");
            if (begin < 0 || end < 0) {
                return null;
            }
            byte[] der = Base64.getMimeDecoder().decode(
                    pem.substring(begin + "-----BEGIN PRIVATE KEY-----".length(), end).replaceAll("\\s", ""));
            key = parsePrivateKey(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            return null;
        }
        if (key == null) {
            return null;
        }
        // The comparison itself is shared with the automatic path, which can
        // drift its pair apart the same way.
        String mismatch = NetherNetCertificateManager.keyMismatchReason(key, leaf.getPublicKey());
        return mismatch == null ? null : "key.pem: " + mismatch;
    }

    private static PrivateKey parsePrivateKey(PKCS8EncodedKeySpec spec) {
        for (String algorithm : new String[]{"EC", "RSA", "EdDSA"}) {
            try {
                return KeyFactory.getInstance(algorithm).generatePrivate(spec);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
