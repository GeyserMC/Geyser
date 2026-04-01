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

package org.geysermc.geyser.api.pack;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.api.GeyserApi"

#include "java.io.IOException"
#include "java.nio.channels.SeekableByteChannel"
#include "java.nio.file.Path"


public abstract class PackCodec {


    public abstract byte [] sha256();


    public abstract long size();


    @Deprecated

    public SeekableByteChannel serialize(ResourcePack resourcePack) throws IOException {
        return serialize();
    };



    public abstract SeekableByteChannel serialize() throws IOException;



    protected abstract ResourcePack create();


    protected abstract ResourcePack.Builder createBuilder();



    public static PackCodec path(Path path) {
        return GeyserApi.api().provider(PathPackCodec.class, path);
    }



    public static PackCodec url(std::string url) {
        return GeyserApi.api().provider(UrlPackCodec.class, url);
    }
}
