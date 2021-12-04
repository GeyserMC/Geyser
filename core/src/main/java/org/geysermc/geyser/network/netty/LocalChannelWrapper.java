/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network.netty;

import io.netty.channel.DefaultChannelPipeline;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;

import java.net.InetSocketAddress;

public class LocalChannelWrapper extends LocalChannel {
    private final ChannelWrapper wrapper;
    /**
     * {@link #newChannelPipeline()} is called during super, so this exists until the wrapper can be initialized.
     */
    private volatile ChannelWrapper tempWrapper;

    public LocalChannelWrapper() {
        wrapper = new ChannelWrapper(this);
    }

    public LocalChannelWrapper(LocalServerChannel parent, LocalChannel peer) {
        super(parent, peer);
        if (tempWrapper == null) {
            this.wrapper = new ChannelWrapper(this);
        } else {
            this.wrapper = tempWrapper;
        }
        wrapper.remoteAddress(new InetSocketAddress(0));
    }

    public ChannelWrapper wrapper() {
        return wrapper;
    }

    @Override
    protected DefaultChannelPipeline newChannelPipeline() {
        if (wrapper != null) {
            return new DefaultChannelPipelinePublic(wrapper);
        } else if (tempWrapper != null) {
            return new DefaultChannelPipelinePublic(tempWrapper);
        } else {
            tempWrapper = new ChannelWrapper(this);
            return new DefaultChannelPipelinePublic(tempWrapper);
        }
    }
}
