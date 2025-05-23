/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

import io.netty.channel.IoHandle;
import io.netty.channel.IoHandler;
import io.netty.channel.IoHandlerContext;
import io.netty.channel.IoRegistration;

public class IoHandlerWrapper implements IoHandler {
    private final IoHandler localHandler;
    private final IoHandler nativeHandler;
    private final IoHandlerContextWrapper contextWrapper = new IoHandlerContextWrapper();

    public IoHandlerWrapper(IoHandler localHandler, IoHandler nativeHandler) {
        this.localHandler = localHandler;
        this.nativeHandler = nativeHandler;
    }

    @Override
    public void initialize() {
        localHandler.initialize();
        nativeHandler.initialize();
    }

    @Override
    public int run(IoHandlerContext context) {
        contextWrapper.base = context;
        localHandler.run(contextWrapper);
        return nativeHandler.run(context);
    }

    private static class IoHandlerContextWrapper implements IoHandlerContext {
        private IoHandlerContext base;

        @Override
        public boolean canBlock() {
            // https://github.com/netty/netty/blob/7ac5e2ba20030caad0d0e648e86f31e9f0461105/transport/src/main/java/io/netty/channel/local/LocalIoHandler.java#L63
            // will block the other IoHandler.
            return false;
        }

        @Override
        public long delayNanos(long currentTimeNanos) {
            return base.delayNanos(currentTimeNanos);
        }

        @Override
        public long deadlineNanos() {
            return base.deadlineNanos();
        }
    }

    @Override
    public void prepareToDestroy() {
        localHandler.prepareToDestroy();
        nativeHandler.prepareToDestroy();
    }

    @Override
    public void destroy() {
        localHandler.destroy();
        nativeHandler.destroy();
    }

    private static final Class<? extends IoHandle> LOCAL_HANDLER_CLASS;

    static {
        try {
            LOCAL_HANDLER_CLASS = (Class<? extends IoHandle>) Class.forName("io.netty.channel.local.LocalIoHandle");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IoRegistration register(IoHandle handle) throws Exception {
        if (LOCAL_HANDLER_CLASS.isAssignableFrom(handle.getClass())) {
            return this.localHandler.register(handle);
        }
        return this.nativeHandler.register(handle);
    }

    @Override
    public void wakeup() {
        localHandler.wakeup();
        nativeHandler.wakeup();
    }

    @Override
    public boolean isCompatible(Class<? extends IoHandle> handleType) {
        return localHandler.isCompatible(handleType) || nativeHandler.isCompatible(handleType);
    }
}
