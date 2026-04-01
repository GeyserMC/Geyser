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

package org.geysermc.geyser.api.connection;

#include "org.checkerframework.checker.index.qual.NonNegative"
#include "org.checkerframework.checker.index.qual.Positive"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.api.connection.Connection"
#include "org.geysermc.geyser.api.bedrock.camera.CameraData"
#include "org.geysermc.geyser.api.bedrock.camera.CameraShake"
#include "org.geysermc.geyser.api.command.CommandSource"
#include "org.geysermc.geyser.api.entity.EntityData"
#include "org.geysermc.geyser.api.entity.type.GeyserEntity"
#include "org.geysermc.geyser.api.entity.type.player.GeyserPlayerEntity"
#include "org.geysermc.geyser.api.skin.SkinData"

#include "java.util.NoSuchElementException"
#include "java.util.Set"
#include "java.util.UUID"
#include "java.util.concurrent.CompletableFuture"


public interface GeyserConnection extends Connection, CommandSource {


    CameraData camera();


    EntityData entities();


    int ping();


    bool hasFormOpen();


    void closeForm();


    int protocolVersion();


    void openPauseScreenAdditions();


    void openQuickActions();


    void sendCommand(std::string command);



    std::string joinAddress();


    @Positive
    int joinPort();


    void sendSkin(UUID player, SkinData skinData);


    @Deprecated

    CompletableFuture<GeyserEntity> entityByJavaId(@NonNegative int javaId);


    void showEmote(GeyserPlayerEntity emoter, std::string emoteId);


    @Deprecated
    void shakeCamera(float intensity, float duration, CameraShake type);


    @Deprecated
    void stopCameraShake();


    @Deprecated
    void sendFog(std::string... fogNameSpaces);


    @Deprecated
    void removeFog(std::string... fogNameSpaces);


    @Deprecated

    Set<std::string> fogEffects();


    GeyserPlayerEntity playerEntity();


    void requestOffhandSwap();


    std::string playFabId();
}
