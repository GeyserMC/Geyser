///*
// * Copyright (c) 2025 GeyserMC. http://geysermc.org
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy
// * of this software and associated documentation files (the "Software"), to deal
// * in the Software without restriction, including without limitation the rights
// * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// * copies of the Software, and to permit persons to whom the Software is
// * furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in
// * all copies or substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// * THE SOFTWARE.
// *
// * @author GeyserMC
// * @link https://github.com/GeyserMC/Geyser
// */
//
//package org.geysermc.geyser.extension;
//
//import org.cloudburstmc.math.vector.Vector3f;
//import org.geysermc.event.subscribe.Subscribe;
//import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
//import org.geysermc.geyser.api.entity.data.GeyserEntityDataTypes;
//import org.geysermc.geyser.api.entity.data.types.Hitbox;
//import org.geysermc.geyser.api.entity.property.type.GeyserFloatEntityProperty;
//import org.geysermc.geyser.api.event.bedrock.ClientEmoteEvent;
//import org.geysermc.geyser.api.event.java.ServerAttachParrotsEvent;
//import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
//import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
//import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
//import org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent;
//import org.geysermc.geyser.api.pack.PathPackCodec;
//import org.geysermc.geyser.api.pack.ResourcePack;
//import org.geysermc.geyser.api.util.Identifier;
//
//import java.util.List;
//
//public class TestExtension {
//
//    public static CustomEntityDefinition ROBOT = CustomEntityDefinition.of("sample:robot");
//    public static Identifier PARROT = Identifier.of("parrot");
//    // optional; only if you want properties
//    public static GeyserFloatEntityProperty MY_FLOAT_PROPERTY;
//
//    @Subscribe
//    public void onDefineEntities(GeyserDefineEntitiesEvent event) {
//        event.register(ROBOT);
//    }
//
//    // Optional; only if you want them!
//    @Subscribe
//    public void onDefineEntityProperties(GeyserDefineEntityPropertiesEvent event) {
//        MY_FLOAT_PROPERTY = event.registerFloatProperty(ROBOT.identifier(), Identifier.of("sample", "my_float_property"), 0, 100);
//    }
//
//    @Subscribe
//    public void onSpawnEntities(ServerSpawnEntityEvent event) {
//        // Chain whatever checks you want
//        if (event.entityType().is(PARROT)) {
//            event.definition(ROBOT);
//
//            // Make the entity smaller
//            event.preSpawnConsumer(entity -> {
//                entity.update(GeyserEntityDataTypes.SCALE, 2f);
//                entity.update(GeyserEntityDataTypes.VARIANT, 0);
//                entity.update(GeyserEntityDataTypes.HITBOXES, List.of(Hitbox.builder()
//                        .min(Vector3f.from(0, 0, 0))
//                        .max(Vector3f.from(10, 10, 10))
//                        .pivot(Vector3f.ZERO)
//                    .build()));
//
//                List<Hitbox> test = entity.value(GeyserEntityDataTypes.HITBOXES);
//                System.out.println(test);
//            });
//        }
//    }
//
//    @Subscribe
//    public void onParrotEvent(ServerAttachParrotsEvent event) {
//        // We also want to make the robot sit on our shoulder!
//        // here, for some reason, only variants that aren't one
//        event.definition(ROBOT);
//        event.preSpawnConsumer(entity -> {
//            entity.update(GeyserEntityDataTypes.SCALE, 0.5f);
//            entity.updateProperty(MY_FLOAT_PROPERTY, 10f);
//        });
//    }
//
//    @Subscribe
//    public void onGeyserDefineResourcePacksEvent(GeyserDefineResourcePacksEvent event) {
//
//        ResourcePack pack = event.resourcePacks().stream().filter(
//            resourcePack -> resourcePack.manifest().header().name().equals("Geyser Player Skull Resource Pack"))
//            .findFirst().orElse(null);
//
//        if (pack == null) {
//            logger.warn("Could not find skull pack!");
//            return;
//        }
//
//        // Unregister skull pack
//        event.unregister(pack.uuid());
//    }
//
//    @Subscribe
//    public void onEmote(ClientEmoteEvent event) {
//        event.connection().playerEntity().update(GeyserEntityDataTypes.SCALE, 0.5f);
//    }
//}
