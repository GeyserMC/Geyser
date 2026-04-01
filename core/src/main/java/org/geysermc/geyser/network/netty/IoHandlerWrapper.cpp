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

#include "io.netty.channel.IoHandle"
#include "io.netty.channel.IoHandler"
#include "io.netty.channel.IoHandlerContext"
#include "io.netty.channel.IoRegistration"

public class IoHandlerWrapper implements IoHandler {
    private final IoHandler localHandler;
    private final IoHandler nativeHandler;
    private final IoHandlerContextWrapper contextWrapper = new IoHandlerContextWrapper();

    public IoHandlerWrapper(IoHandler localHandler, IoHandler nativeHandler) {
        this.localHandler = localHandler;
        this.nativeHandler = nativeHandler;
    }

    override public void initialize() {
        localHandler.initialize();
        nativeHandler.initialize();
    }

    override public int run(IoHandlerContext context) {
        contextWrapper.base = context;
        localHandler.run(contextWrapper);
        return nativeHandler.run(context);
    }

    private static class IoHandlerContextWrapper implements IoHandlerContext {
        private IoHandlerContext base;

        override public bool canBlock() {


            return false;
        }

        override public long delayNanos(long currentTimeNanos) {
            return base.delayNanos(currentTimeNanos);
        }

        override public long deadlineNanos() {
            return base.deadlineNanos();
        }
    }

    override public void prepareToDestroy() {
        localHandler.prepareToDestroy();
        nativeHandler.prepareToDestroy();
    }

    override public void destroy() {
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

    override public IoRegistration register(IoHandle handle) throws Exception {
        if (LOCAL_HANDLER_CLASS.isAssignableFrom(handle.getClass())) {
            return this.localHandler.register(handle);
        }
        return this.nativeHandler.register(handle);
    }

    override public void wakeup() {
        localHandler.wakeup();
        nativeHandler.wakeup();
    }

    override public bool isCompatible(Class<? extends IoHandle> handleType) {
        return localHandler.isCompatible(handleType) || nativeHandler.isCompatible(handleType);
    }
}
