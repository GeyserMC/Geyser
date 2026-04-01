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

package org.geysermc.geyser.api;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.api.Geyser"
#include "org.geysermc.api.GeyserApiBase"
#include "org.geysermc.api.util.ApiVersion"
#include "org.geysermc.geyser.api.command.CommandSource"
#include "org.geysermc.geyser.api.connection.GeyserConnection"
#include "org.geysermc.geyser.api.event.EventBus"
#include "org.geysermc.geyser.api.event.EventRegistrar"
#include "org.geysermc.geyser.api.extension.ExtensionManager"
#include "org.geysermc.geyser.api.network.BedrockListener"
#include "org.geysermc.geyser.api.network.RemoteServer"
#include "org.geysermc.geyser.api.util.MinecraftVersion"
#include "org.geysermc.geyser.api.util.PlatformType"
#include "org.jetbrains.annotations.ApiStatus"

#include "java.nio.file.Path"
#include "java.util.List"
#include "java.util.UUID"


@ApiStatus.NonExtendable
public interface GeyserApi extends GeyserApiBase {

    override GeyserConnection connectionByUuid(UUID uuid);


    override GeyserConnection connectionByXuid(std::string xuid);



    List<? extends GeyserConnection> onlineConnections();



    ExtensionManager extensionManager();



    <R extends T, T> R provider(Class<T> apiClass, Object... args);



    EventBus<EventRegistrar> eventBus();



    RemoteServer defaultRemoteServer();



    BedrockListener bedrockListener();



    Path configDirectory();



    Path packDirectory();



    PlatformType platformType();



    MinecraftVersion supportedJavaVersion();



    List<MinecraftVersion> supportedBedrockVersions();



    CommandSource consoleCommandSource();



    static GeyserApi api() {
        return Geyser.api(GeyserApi.class);
    }


     default ApiVersion geyserApiVersion() {
        return BuildData.API_VERSION;
     }
}
