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

package org.geysermc.geyser.api.entity;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.downstream.entity.ServerSpawnEntityEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;

/**
 * Holds information about an entity that remains constant no matter
 * its properties. This is typically data such as its identifier,
 * its height/width, offset, etc.
 * <p>
 * This class can be used to register custom entities through the
 * {@link GeyserDefineEntitiesEvent}. Custom entities can be created
 * using the builder like so:
 * <pre>
 * {@code
 *     public static final EntityDefinition CUSTOM_MOB = EntityDefinition.builder()
 *                 .identifier(EntityIdentifier.builder()
 *                         .identifier("geysermc:custom_mob")
 *                         .summonable(false)
 *                         .spawnEgg(false)
 *                         .build())
 *                 .width(1.0f)
 *                 .height(1.0f)
 *                 .offset(1.0f)
 *                 .build();
 * }
 * </pre>
 *
 * <p>
 * Within the {@link GeyserDefineEntitiesEvent}, you can then add the
 * custom entity definition to the {@link GeyserDefineEntitiesEvent#definitions()}
 * list. A resource pack utilizing this entity identifier will need to be
 * provided for this to fully work.
 * <p>
 * Summoning custom entities on their own is not supported through the
 * API exclusively. Third party extensions or plugins on supported platforms
 * may provide an interface for this, but the current suggestion using
 * the Geyser API exclusively is to listen for the {@link ServerSpawnEntityEvent}
 * and modify the entity definition based on the data provided there.
 * <p>
 * An example of doing so is as follows:
 * <pre>
 * {@code
 *     @Subscribe
 *     public void onSpawn(ServerSpawnEntityEvent event) {
 *         if (event.entityDefinition().identifier().equals("mob_to_replace")) {
 *             event.setEntityDefinition(CUSTOM_MOB);
 *         }
 *     }
 * }
 * </pre>
 */
public interface EntityDefinition {

    /**
     * Gets the identifier of this entity.
     *
     * @return the identifier of this entity
     */
    @NonNull
    EntityIdentifier entityIdentifier();

    /**
     * Gets the width of this entity.
     *
     * @return the width of this entity
     */
    float width();

    /**
     * Gets the height of this entity.
     *
     * @return the height of this entity
     */
    float height();

    /**
     * Gets the offset of this entity.
     *
     * @return the offset of this entity
     */
    float offset();

    static Builder builder() {
        return GeyserApi.api().provider(EntityDefinition.Builder.class);
    }

    interface Builder {

        /**
         * Sets the identifier of this entity.
         *
         * @param identifier the identifier of this entity
         * @return the builder
         */
        Builder identifier(EntityIdentifier identifier);

        /**
         * Sets the width of this entity.
         *
         * @param width the width of this entity
         * @return the builder
         */
        Builder width(float width);

        /**
         * Sets the height of this entity.
         *
         * @param height the height of this entity
         * @return the builder
         */
        Builder height(float height);

        /**
         * Sets the offset of this entity.
         *
         * @param offset the offset of this entity
         * @return the builder
         */
        Builder offset(float offset);

        /**
         * Builds the entity definition.
         *
         * @return the entity definition
         */
        EntityDefinition build();
    }
}
