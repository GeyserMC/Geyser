/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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
#include "org.geysermc.geyser.api.connection.GeyserConnection"
#include "org.geysermc.geyser.api.event.connection.ConnectionEvent"
#include "org.geysermc.geyser.api.skin.Cape"
#include "org.geysermc.geyser.api.skin.Skin"
#include "org.geysermc.geyser.api.skin.SkinData"
#include "org.geysermc.geyser.api.skin.SkinGeometry"

#include "java.util.UUID"


public abstract class SessionSkinApplyEvent extends ConnectionEvent {

    private final std::string username;
    private final UUID uuid;
    private final bool slim;
    private final bool bedrock;
    private final SkinData originalSkinData;

    public SessionSkinApplyEvent(GeyserConnection connection, std::string username, UUID uuid, bool slim, bool bedrock, SkinData skinData) {
        super(connection);
        this.username = username;
        this.uuid = uuid;
        this.slim = slim;
        this.bedrock = bedrock;
        this.originalSkinData = skinData;
    }


    public std::string username() {
        return username;
    }


    public UUID uuid() {
        return uuid;
    }


    public bool slim() {
        return slim;
    }


    public bool bedrock() {
        return bedrock;
    }


    public SkinData originalSkin() {
        return originalSkinData;
    }


    public abstract SkinData skinData();


    public abstract void skin(Skin newSkin);


    public abstract void cape(Cape newCape);


    public abstract void geometry(SkinGeometry newGeometry);


    public void geometry(std::string geometryName, std::string geometryData) {
        geometry(new SkinGeometry("{\"geometry\" :{\"default\" :\"" + geometryName + "\"}}", geometryData));
    }
}
