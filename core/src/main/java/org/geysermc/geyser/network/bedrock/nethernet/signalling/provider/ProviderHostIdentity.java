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

package org.geysermc.geyser.network.bedrock.nethernet.signalling.provider;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.cloudburstmc.netty.signalling.admission.NativeHostIdentity;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Set;

/**
 * Host-owned DTLS identity; first start creates it, all later starts preserve it.
 */
public final class ProviderHostIdentity {
    private static final Set<PosixFilePermission> PRIVATE_DIRECTORY = PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> PRIVATE_FILE = PosixFilePermissions.fromString("rw-------");

    private ProviderHostIdentity() {
    }

    public static NativeHostIdentity ensure(Path directory) throws Exception {
        // Windows has no POSIX permissions; there the files inherit the config folder's ACL
        boolean posix = directory.getFileSystem().supportedFileAttributeViews().contains("posix");
        Files.createDirectories(directory, ownerOnly(posix, PRIVATE_DIRECTORY));
        if (Files.isSymbolicLink(directory))
            throw new IOException("Provider identity directory must not be a symbolic link");
        if (posix) Files.setPosixFilePermissions(directory, PRIVATE_DIRECTORY);
        Path certificate = directory.resolve("host-cert.pem"), key = directory.resolve("host-key.pem");
        Path lockPath = directory.resolve("host-identity.lock");
        try (FileChannel channel = FileChannel.open(lockPath,
                Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS),
                ownerOnly(posix, PRIVATE_FILE))) {
            try (var lock = channel.tryLock()) {
                if (lock == null) throw new IOException("Provider DTLS identity is already being initialized");
                if (Files.isSymbolicLink(certificate) || Files.isSymbolicLink(key))
                    throw new IOException("Provider PEM identity files must not be symbolic links");
                boolean hasCertificate = Files.exists(certificate), hasKey = Files.exists(key);
                if (hasCertificate != hasKey) throw new IOException(
                        "Incomplete provider DTLS identity: restore the matching host-cert.pem and host-key.pem pair; refusing to replace existing identity");
                if (hasCertificate) {
                    if (posix) Files.setPosixFilePermissions(key, PRIVATE_FILE);
                    return NativeHostIdentity.load(certificate, key);
                }

                KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
                generator.initialize(new ECGenParameterSpec("secp256r1"));
                var pair = generator.generateKeyPair();
                Instant now = Instant.now();
                X500Name name = new X500Name("CN=NetherNet Host");
                var builder = new JcaX509v3CertificateBuilder(name,
                        new BigInteger(159, new SecureRandom()).add(BigInteger.ONE),
                        Date.from(now.minus(1, ChronoUnit.DAYS)), Date.from(now.plus(3650, ChronoUnit.DAYS)),
                        name, pair.getPublic());
                var signer = new JcaContentSignerBuilder("SHA256withECDSA").build(pair.getPrivate());
                var cert = new JcaX509CertificateConverter().getCertificate(builder.build(signer));
                cert.verify(pair.getPublic());
                byte[] encodedKey = pair.getPrivate().getEncoded();
                boolean createdKey = false, createdCertificate = false;
                try {
                    writePem(key, "PRIVATE KEY", encodedKey, posix);
                    createdKey = true;
                    writePem(certificate, "CERTIFICATE", cert.getEncoded(), posix);
                    createdCertificate = true;
                    NativeHostIdentity identity = NativeHostIdentity.load(certificate, key);
                    if (posix) {
                        // Windows cannot open a directory as a channel to sync it
                        try (FileChannel parent = FileChannel.open(directory, StandardOpenOption.READ)) {
                            parent.force(true);
                        }
                    }
                    return identity;
                } catch (Exception failure) {
                    // Only remove this attempt's new files; existing identities are never replaced.
                    if (createdCertificate) Files.deleteIfExists(certificate);
                    if (createdKey) Files.deleteIfExists(key);
                    throw failure;
                } finally {
                    Arrays.fill(encodedKey, (byte) 0);
                }
            }
        } catch (OverlappingFileLockException busy) {
            throw new IOException("Provider DTLS identity is already being initialized", busy);
        }
    }

    private static FileAttribute<?>[] ownerOnly(boolean posix, Set<PosixFilePermission> permissions) {
        return posix ? new FileAttribute<?>[]{PosixFilePermissions.asFileAttribute(permissions)} : new FileAttribute<?>[0];
    }

    private static void writePem(Path path, String label, byte[] der, boolean posix) throws IOException {
        byte[] pem = ("-----BEGIN " + label + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der)
                + "\n-----END " + label + "-----\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        try (FileChannel file = FileChannel.open(path, Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE),
                ownerOnly(posix, PRIVATE_FILE))) {
            ByteBuffer buffer = ByteBuffer.wrap(pem);
            while (buffer.hasRemaining()) file.write(buffer);
            file.force(true);
        } finally {
            Arrays.fill(pem, (byte) 0);
        }
    }
}
