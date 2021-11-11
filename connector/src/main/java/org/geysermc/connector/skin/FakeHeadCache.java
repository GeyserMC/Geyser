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

package org.geysermc.connector.skin;

import lombok.Getter;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author Jan / qlow (11.11.21)
 */
public class FakeHeadCache {

    @Getter
    private final Set<UUID> playersWithCustomSkin = new HashSet<>();

    public void addCustomSkin( UUID uuid ) {
        this.playersWithCustomSkin.add( uuid );
    }

    public void removeEntity( Entity entity ) {
        if ( entity instanceof PlayerEntity ) {
            this.playersWithCustomSkin.remove( ( ( PlayerEntity ) entity ).getUuid() );
        }
    }

    public void clear() {
        this.playersWithCustomSkin.clear();
    }


}
