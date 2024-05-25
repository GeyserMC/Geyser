/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level.block.type;

import com.github.steveice10.mc.auth.data.GameProfile;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.SkullCache;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.Collections;
import java.util.UUID;

public class SkullBlock extends Block {
    private final Type type;

    public SkullBlock(String javaIdentifier, Type type, Builder builder) {
        super(javaIdentifier, builder);
        this.type = type;
    }

    @Override
    protected void sendBlockUpdatePacket(GeyserSession session, BlockState state, BlockDefinition definition, Vector3i position) {
        if (this.type == Type.PLAYER) {
            // The changed block was a player skull so check if a custom block was defined for this skull
            SkullCache.Skull skull = session.getSkullCache().updateSkull(position, state);
            if (skull != null && skull.getBlockDefinition() != null) {
                definition = skull.getBlockDefinition();
            }
        }
        super.sendBlockUpdatePacket(session, state, definition, position);
    }

    @Override
    protected void checkForEmptySkull(GeyserSession session, BlockState state, Vector3i position) {
        // It's not an empty skull.
    }

    public ItemStack pickItem(GeyserSession session, BlockState state, Vector3i position) {
        SkullCache.Skull skull = session.getSkullCache().getSkulls().get(position);
        if (skull == null) {
            return new ItemStack(pickItem(state).getId());
        }

        GeyserItemStack itemStack = GeyserItemStack.of(pickItem(state).getId(), 1);
        // This is a universal block entity behavior, but hardcode how it works for now.
        NbtMapBuilder builder = NbtMap.builder()
                .putString("id", "minecraft:skull")
                .putInt("x", position.getX())
                .putInt("y", position.getY())
                .putInt("z", position.getZ());
        DataComponents components = itemStack.getOrCreateComponents();
        components.put(DataComponentType.BLOCK_ENTITY_DATA, builder.build());

        UUID uuid = skull.getUuid();
        String texturesProperty = skull.getTexturesProperty();
        GameProfile profile = new GameProfile(uuid, null);
        if (texturesProperty != null) {
            profile.setProperties(Collections.singletonList(new GameProfile.Property("textures", texturesProperty)));
        }
        components.put(DataComponentType.PROFILE, profile);
        return itemStack.getItemStack();
    }

    public Type skullType() {
        return type;
    }

    /**
     * Enum order matches Java.
     */
    public enum Type {
        SKELETON(0),
        WITHER_SKELETON(1),
        PLAYER(3),
        ZOMBIE(2),
        CREEPER(4),
        PIGLIN(6),
        DRAGON(5);

        private final int bedrockId;

        Type(int bedrockId) {
            this.bedrockId = bedrockId;
        }

        public int bedrockId() {
            return bedrockId;
        }
    }
}
