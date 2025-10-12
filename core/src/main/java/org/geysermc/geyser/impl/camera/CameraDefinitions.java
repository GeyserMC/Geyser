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

package org.geysermc.geyser.impl.camera;

import org.cloudburstmc.protocol.bedrock.data.camera.CameraAudioListener;
import org.cloudburstmc.protocol.bedrock.data.camera.CameraPreset;
import org.cloudburstmc.protocol.common.DefinitionRegistry;
import org.cloudburstmc.protocol.common.NamedDefinition;
import org.cloudburstmc.protocol.common.SimpleDefinitionRegistry;
import org.cloudburstmc.protocol.common.util.OptionalBoolean;
import org.geysermc.geyser.api.bedrock.camera.CameraPerspective;

import java.util.List;

public class CameraDefinitions {

    public static final DefinitionRegistry<NamedDefinition> CAMERA_DEFINITIONS;

    public static final List<CameraPreset> CAMERA_PRESETS;

    static {
        CAMERA_PRESETS = List.of(
            CameraPreset.builder().identifier(CameraPerspective.FIRST_PERSON.id()).build(),
            CameraPreset.builder().identifier(CameraPerspective.FREE.id()).build(),
            CameraPreset.builder().identifier(CameraPerspective.THIRD_PERSON.id()).build(),
            CameraPreset.builder().identifier(CameraPerspective.THIRD_PERSON_FRONT.id()).build(),
            CameraPreset.builder().identifier("geyser:free_audio").parentPreset(CameraPerspective.FREE.id()).listener(CameraAudioListener.PLAYER).playEffect(OptionalBoolean.of(false)).build(),
            CameraPreset.builder().identifier("geyser:free_effects").parentPreset(CameraPerspective.FREE.id()).listener(CameraAudioListener.CAMERA).playEffect(OptionalBoolean.of(true)).build(),
            CameraPreset.builder().identifier("geyser:free_audio_effects").parentPreset(CameraPerspective.FREE.id()).listener(CameraAudioListener.PLAYER).playEffect(OptionalBoolean.of(true)).build()
        );

        SimpleDefinitionRegistry.Builder<NamedDefinition> builder = SimpleDefinitionRegistry.builder();
        for (int i = 0; i < CAMERA_PRESETS.size(); i++) {
            builder.add(CameraDefinition.of(CAMERA_PRESETS.get(i).getIdentifier(), i));
        }
        CAMERA_DEFINITIONS = builder.build();
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

    public record CameraDefinition(String identifier, int runtimeId) implements NamedDefinition {

        @Override
        public String getIdentifier() {
            return identifier;
        }

        @Override
        public int getRuntimeId() {
            return runtimeId;
        }

        public static CameraDefinition of(String identifier, int runtimeId) {
            return new CameraDefinition(identifier, runtimeId);
        }
    }
}
