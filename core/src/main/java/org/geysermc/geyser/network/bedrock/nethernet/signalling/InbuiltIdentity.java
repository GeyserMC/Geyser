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

import dev.kastle.netty.util.nethernet.ServerIdentity;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;
import java.security.cert.Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Automatic, persistent ES384 identity for inbuilt HTTP signalling.
 */
public final class InbuiltIdentity {
    private InbuiltIdentity() {
    }

    public static void ensure(Path directory) throws Exception {
        Path file = directory.resolve("identity.p12");
        if (Files.isSymbolicLink(file)) {
            throw new java.io.IOException("Inbuilt identity must not be a symbolic link");
        }

        if (!Files.exists(file)) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp384r1"));
            KeyPair pair = generator.generateKeyPair();
            Instant now = Instant.now();
            X500Name name = new X500Name("CN=Geyser");
            var builder = new JcaX509v3CertificateBuilder(name, new BigInteger(159, new SecureRandom()).add(BigInteger.ONE),
                    Date.from(now.minus(1, ChronoUnit.DAYS)), Date.from(now.plus(3650, ChronoUnit.DAYS)), name, pair.getPublic());
            var certificate = new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA384withECDSA").build(pair.getPrivate())));
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(null, new char[0]);
            store.setKeyEntry("identity", pair.getPrivate(), new char[0], new Certificate[]{certificate});
            Path temporary = Files.createTempFile(directory, ".identity-", ".p12", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            try {
                try (var output = Files.newOutputStream(temporary)) {
                    store.store(output, new char[0]);
                }
                Files.move(temporary, file); // Never replace an identity created by another process.
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        ServerIdentity.fromKeystore(file.toFile(), "");
    }
}
