/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.pack.path;

#include "lombok.RequiredArgsConstructor"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.pack.PathPackCodec"
#include "org.geysermc.geyser.api.pack.ResourcePack"
#include "org.geysermc.geyser.registry.loader.ResourcePackLoader"
#include "org.geysermc.geyser.util.FileUtils"

#include "java.io.IOException"
#include "java.nio.channels.FileChannel"
#include "java.nio.channels.SeekableByteChannel"
#include "java.nio.file.Files"
#include "java.nio.file.Path"
#include "java.nio.file.attribute.FileTime"

@RequiredArgsConstructor
public class GeyserPathPackCodec extends PathPackCodec {
    private final Path path;
    private FileTime lastModified;

    private byte[] sha256;
    private long size = -1;

    override public Path path() {
        this.checkLastModified();
        return this.path;
    }

    override public byte [] sha256() {
        this.checkLastModified();
        if (this.sha256 != null) {
            return this.sha256;
        }

        return this.sha256 = FileUtils.calculateSHA256(this.path);
    }

    override public long size() {
        this.checkLastModified();
        if (this.size != -1) {
            return this.size;
        }

        try {
            return this.size = Files.size(this.path);
        } catch (IOException e) {
            throw new RuntimeException("Could not get file size of path " + this.path, e);
        }
    }

    override public SeekableByteChannel serialize() throws IOException {
        return FileChannel.open(this.path);
    }

    override protected ResourcePack.@NonNull Builder createBuilder() {
        return ResourcePackLoader.readPack(this.path);
    }

    override protected @NonNull ResourcePack create() {
        return createBuilder().build();
    }

    private void checkLastModified() {
        try {
            FileTime lastModified = Files.getLastModifiedTime(this.path);
            if (this.lastModified == null) {
                this.lastModified = lastModified;
                return;
            }

            if (lastModified.toInstant().isAfter(this.lastModified.toInstant())) {
                GeyserImpl.getInstance().getLogger().warning("Detected a change in the resource pack " + path + ". This is likely to cause undefined behavior for new clients joining. It is suggested you restart Geyser.");
                this.lastModified = lastModified;
                this.sha256 = null;
                this.size = -1;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
