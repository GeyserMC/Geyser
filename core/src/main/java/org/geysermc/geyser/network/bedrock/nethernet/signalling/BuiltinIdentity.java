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

package org.geysermc.geyser.network.bedrock.nethernet.signalling;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.cloudburstmc.netty.util.nethernet.ServerIdentity;
import org.geysermc.geyser.GeyserImpl;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

/**
 * The persistent P-384 identity inbuilt HTTP signalling signs its SDP answers with.
 * <p>
 * Over plain HTTP, clients trust on first use: they pin the public key and only prompt the player the first time
 * they see it, so the key has to survive restarts. Neither a domain nor the server address are part of the identity;
 * the certificate's common name only surfaces as untrusted display text in that prompt.
 */
public final class BuiltinIdentity {
    public static final String FILE_NAME = "identity.p12";

    /**
     * The certificate's common name; it becomes the issuer of the identity token.
     */
    private static final String DISPLAY_NAME = GeyserImpl.NAME + "-" + GeyserImpl.getInstance().config().gameplay().serverName();

    /**
     * {@link ServerIdentity} mirrors the certificate's expiry into the token, and clients reject expired tokens.
     * Replacing the key would prompt every returning player again, so the certificate never expires (RFC 5280, 4.1.2.5).
     */
    private static final Instant NO_EXPIRY = Instant.parse("9999-12-31T23:59:59Z");

    private static final Set<PosixFilePermission> OWNER_ONLY = PosixFilePermissions.fromString("rw-------");

    private BuiltinIdentity() {
    }

    /**
     * Creates the identity on the first start, and checks that the existing one loads on every later start.
     * An existing identity is never replaced, as that would prompt every returning player again.
     *
     * @param directory the existing directory to keep the identity in
     * @return the PKCS12 keystore holding the identity, without a password
     */
    public static Path ensure(Path directory) throws Exception {
        Path file = directory.resolve(FILE_NAME);
        if (Files.isSymbolicLink(file)) {
            throw new IOException("The builtin signalling identity must not be a symbolic link");
        }

        boolean posix = Files.getFileStore(directory).supportsFileAttributeView(PosixFileAttributeView.class);
        if (!Files.exists(file)) {
            create(directory, file, posix);
        } else if (posix) {
            Files.setPosixFilePermissions(file, OWNER_ONLY);
        }

        // NetherNetHTTPSignaling only logs a broken identity and then signs with a throwaway key,
        // which would prompt every player again after each restart
        try {
            ServerIdentity.fromKeystore(file.toFile(), "");
        } catch (Exception e) {
            throw new IOException("The builtin signalling identity " + file + " is unreadable. Only delete it if it cannot be restored; " +
                "a new identity makes every player confirm the server again", e);
        }
        return file;
    }

    private static void create(Path directory, Path file, boolean posix) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        KeyPair pair = generator.generateKeyPair();

        X500Name name = new X500Name("CN=" + DISPLAY_NAME);
        var builder = new JcaX509v3CertificateBuilder(name, new BigInteger(159, new SecureRandom()).add(BigInteger.ONE),
                Date.from(Instant.now().minus(1, ChronoUnit.DAYS)), Date.from(NO_EXPIRY), name, pair.getPublic());
        var certificate = new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA384withECDSA").build(pair.getPrivate())));

        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, new char[0]);
        store.setKeyEntry("identity", pair.getPrivate(), new char[0], new Certificate[]{certificate});

        // Windows has no POSIX permissions; there the file inherits the config folder's ACL
        FileAttribute<?>[] attributes = posix ? new FileAttribute<?>[]{PosixFilePermissions.asFileAttribute(OWNER_ONLY)} : new FileAttribute<?>[0];
        Path temporary = Files.createTempFile(directory, ".identity-", ".p12", attributes);
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                store.store(output, new char[0]);
            }
            Files.move(temporary, file); // Never replace an identity created by another process
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
