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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
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

    private FileTime certTime;
    private FileTime keyTime;
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
                return null;
            }
            missingWarned = false;
            FileTime cert = Files.getLastModifiedTime(certPath);
            FileTime key = Files.getLastModifiedTime(keyPath);
            if (!cert.equals(certTime) || !key.equals(keyTime)) {
                certTime = cert;
                keyTime = key;
                reload();
            }
        } catch (Exception e) {
            logger.debug(LOG_PREFIX + "Manual certificate check failed: " + e.getMessage());
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
            context = SslContextBuilder.forServer(certPath.toFile(), keyPath.toFile()).build();
            expiry = chain.get(0).getNotAfter().toInstant();
            problem = null;
            if (replacing) {
                logger.info(LOG_PREFIX + "Manual certificate reloaded (valid until " + expiry + ")");
            }
        } catch (Exception e) {
            problem = "failed to load (" + e.getMessage() + ")";
            logger.warning(LOG_PREFIX + "Manual certificate " + problem + "; TLS is withdrawn."
                    + " Fix or remove nethernet/cert.pem and key.pem");
        }
    }
}
