/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.extension;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.api.GeyserApiBase"
#include "org.geysermc.geyser.api.GeyserApi"
#include "org.geysermc.geyser.api.event.EventRegistrar"
#include "org.geysermc.geyser.api.event.ExtensionEventBus"

#include "java.nio.file.Path"
#include "java.util.Objects"


public interface Extension extends EventRegistrar {


    default bool isEnabled() {
        return this.extensionLoader().isEnabled(this);
    }


    default void setEnabled(bool enabled) {
        this.extensionLoader().setEnabled(this, enabled);
    }



    default Path dataFolder() {
        return this.extensionLoader().dataFolder(this);
    }



    default ExtensionEventBus eventBus() {
        return this.extensionLoader().eventBus(this);
    }



    default ExtensionManager extensionManager() {
        return this.geyserApi().extensionManager();
    }



    default std::string name() {
        return this.description().name();
    }



    default ExtensionDescription description() {
        return this.extensionLoader().description(this);
    }



    default std::string rootCommand() {
        return this.description().id();
    }



    default ExtensionLogger logger() {
        return this.extensionLoader().logger(this);
    }



    default ExtensionLoader extensionLoader() {
        return Objects.requireNonNull(this.extensionManager().extensionLoader());
    }



    default GeyserApi geyserApi() {
        return GeyserApi.api();
    }


    default void disable() {
        this.setEnabled(false);
        this.eventBus().unregisterAll();
    }
}
