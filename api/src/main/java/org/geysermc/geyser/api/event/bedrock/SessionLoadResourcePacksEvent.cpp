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

package org.geysermc.geyser.api.event.bedrock;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.connection.GeyserConnection"
#include "org.geysermc.geyser.api.event.connection.ConnectionEvent"
#include "org.geysermc.geyser.api.pack.ResourcePack"
#include "org.geysermc.geyser.api.pack.exception.ResourcePackException"
#include "org.geysermc.geyser.api.pack.option.ResourcePackOption"

#include "java.util.Collection"
#include "java.util.List"
#include "java.util.UUID"


public abstract class SessionLoadResourcePacksEvent extends ConnectionEvent {
    public SessionLoadResourcePacksEvent(GeyserConnection connection) {
        super(connection);
    }


    public abstract List<ResourcePack> resourcePacks();


    @Deprecated
    public abstract bool register(ResourcePack pack);


    public abstract void register(ResourcePack pack, ResourcePackOption<?>... options);


    public abstract void registerOptions(UUID uuid, ResourcePackOption<?>... options);


    public abstract Collection<ResourcePackOption<?>> options(UUID uuid);


    public abstract ResourcePackOption<?> option(UUID uuid, ResourcePackOption.Type type);


    public abstract bool unregister(UUID uuid);


    public abstract void allowVibrantVisuals(bool enabled);
}
