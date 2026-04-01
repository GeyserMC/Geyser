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

package org.geysermc.geyser.api.bedrock.camera;

/**
 * These are all the easing types that can be used when sending a {@link CameraPosition} instruction.
 * When using these, the client won't teleport to the new camera position, but instead transition to it.
 * <p>
 * See <a href="https://easings.net/">https://easings.net/</a> for more information.
 */
public enum CameraEaseType {
    LINEAR("linear"),
    SPRING("spring"),
    EASE_IN_SINE("in_sine"),
    EASE_OUT_SINE("out_sine"),
    EASE_IN_OUT_SINE("in_out_sine"),
    EASE_IN_QUAD("in_quad"),
    EASE_OUT_QUAD("out_quad"),
    EASE_IN_OUT_QUAD("in_out_quad"),
    EASE_IN_CUBIC("in_cubic"),
    EASE_OUT_CUBIC("out_cubic"),
    EASE_IN_OUT_CUBIC("in_out_cubic"),
    EASE_IN_QUART("in_quart"),
    EASE_OUT_QUART("out_quart"),
    EASE_IN_OUT_QUART("in_out_quart"),
    EASE_IN_QUINT("in_quint"),
    EASE_OUT_QUINT("out_quint"),
    EASE_IN_OUT_QUINT("in_out_quint"),
    EASE_IN_EXPO("in_expo"),
    EASE_OUT_EXPO("out_expo"),
    EASE_IN_OUT_EXPO("in_out_expo"),
    EASE_IN_CIRC("in_circ"),
    EASE_OUT_CIRC("out_circ"),
    EASE_IN_OUT_CIRC("in_out_circ"),
    EASE_IN_BACK("in_back"),
    EASE_OUT_BACK("out_back"),
    EASE_IN_OUT_BACK("in_out_back"),
    EASE_IN_ELASTIC("in_elastic"),
    EASE_OUT_ELASTIC("out_elastic"),
    EASE_IN_OUT_ELASTIC("in_out_elastic"),
    EASE_IN_BOUNCE("in_bounce"),
    EASE_OUT_BOUNCE("out_bounce"),
    EASE_IN_OUT_BOUNCE("in_out_bounce");

    private final String id;

    CameraEaseType(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }
}
