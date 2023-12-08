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

package org.geysermc.geyser.bedrock.camera;

import org.cloudburstmc.protocol.bedrock.data.camera.CameraAudioListener;
import org.cloudburstmc.protocol.bedrock.data.camera.CameraPreset;
import org.cloudburstmc.protocol.common.DefinitionRegistry;
import org.cloudburstmc.protocol.common.NamedDefinition;
import org.cloudburstmc.protocol.common.SimpleDefinitionRegistry;
import org.cloudburstmc.protocol.common.util.OptionalBoolean;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CameraUtil {

    public static final DefinitionRegistry<NamedDefinition> CAMERA_DEFINITIONS;

    public static final List<CameraPreset> CAMERA_PRESETS;

    static {
        CAMERA_PRESETS = List.of(
                new CameraPreset("minecraft:first_person", "", null, null, null, null, OptionalBoolean.empty()),
                new CameraPreset("minecraft:free", "", null, null, null, null, OptionalBoolean.empty()),
                new CameraPreset("minecraft:third_person", "", null, null, null, null, OptionalBoolean.empty()),
                new CameraPreset("minecraft:third_person_front", "", null, null, null, null, OptionalBoolean.empty()),
                new CameraPreset("geyser:free_audio", "minecraft:free", null, null, null, CameraAudioListener.CAMERA, OptionalBoolean.of(false)),
                new CameraPreset("geyser:free_effects", "minecraft:free", null, null, null, CameraAudioListener.PLAYER, OptionalBoolean.of(true)),
                new CameraPreset("geyser:free_audio_effects", "minecraft:free", null, null, null, CameraAudioListener.CAMERA, OptionalBoolean.of(true)));

        CAMERA_DEFINITIONS = SimpleDefinitionRegistry.<NamedDefinition>builder()
                .addAll(IntStream.range(0, CAMERA_PRESETS.size())
                        .mapToObj(i -> CameraDefinition.of(CAMERA_PRESETS.get(i).getIdentifier(), i))
                        .collect(Collectors.toList()))
                .build();
    }

    public static NamedDefinition getById(int id) {
        return CAMERA_DEFINITIONS.getDefinition(id);
    }

    public static NamedDefinition getByFunctionality(boolean audio, boolean effects) {
        if (!audio && !effects) {
            return getById(1); // FREE
        }
        if (audio) {
            if (effects) {
                return getById(6); // FREE_AUDIO_EFFECTS
            } else {
                return getById(4); // FREE_AUDIO
            }
        } else {
            return getById(5); // FREE_EFFECTS
        }
    }
}
