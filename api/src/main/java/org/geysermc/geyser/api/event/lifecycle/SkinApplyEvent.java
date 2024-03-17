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

package org.geysermc.geyser.api.event.lifecycle;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.event.Event;
import org.geysermc.geyser.api.skin.Cape;
import org.geysermc.geyser.api.skin.Skin;
import org.geysermc.geyser.api.skin.SkinData;
import org.geysermc.geyser.api.skin.SkinGeometry;

import java.util.UUID;

public abstract class SkinApplyEvent implements Event {

    private final String username;
    private final UUID uuid;
    private final boolean slim;
    private final boolean isBedrock;
    private final SkinData skinData;

    public SkinApplyEvent(String username, UUID uuid, boolean slim, boolean isBedrock, SkinData skinData) {
        this.username = username;
        this.uuid = uuid;
        this.slim = slim;
        this.isBedrock = isBedrock;
        this.skinData = skinData;
    }

    public String username() {
        return username;
    }

    public UUID uuid() {
        return uuid;
    }

    public boolean slim() {
        return slim;
    }

    public boolean isBedrock() {
        return isBedrock;
    }

    public SkinData skinData() {
        return skinData;
    }

    public abstract void skin(@NonNull Skin newSkin);
    public abstract void cape(@NonNull Cape newCape);
    public abstract void geometry(@NonNull SkinGeometry newGeometry);

    public void geometry(@NonNull String geometryName, @NonNull String geometryData) {
        geometry(new SkinGeometry("{\"geometry\" :{\"default\" :\"" + geometryName + "\"}}", geometryData));
    }
}
