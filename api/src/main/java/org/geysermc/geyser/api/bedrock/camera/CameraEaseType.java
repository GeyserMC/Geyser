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

public enum CameraEaseType {
    IN_BACK("in_back"),
    IN_BOUNCE("in_bounce"),
    IN_CIRC("in_circ"),
    IN_CUBIC("in_cubic"),
    IN_ELASTIC("in_elastic"),
    IN_EXPO("in_expo"),
    IN_OUT_BACK("in_out_back"),
    IN_OUT_BOUNCE("in_out_bounce"),
    IN_OUT_CIRC("in_out_circ"),
    IN_OUT_CUBIC("in_out_cubic"),
    IN_OUT_ELASTIC("in_out_elastic"),
    IN_OUT_EXPO("in_out_expo"),
    IN_OUT_QUAD("in_out_quad"),
    IN_OUT_QUART("in_out_quart"),
    IN_OUT_QUINT("in_out_quint"),
    IN_OUT_SINE("in_out_sine"),
    IN_QUAD("in_quad"),
    IN_QUART("in_quart"),
    IN_QUINT("in_quint"),
    IN_SINE("in_sine"),
    LINEAR("linear"),
    OUT_BACK("out_back"),
    OUT_BOUNCE("out_bounce"),
    OUT_CIRC("out_circ"),
    OUT_CUBIC("out_cubic"),
    OUT_ELASTIC("out_elastic"),
    OUT_EXPO("out_expo"),
    OUT_QUAD("out_quad"),
    OUT_QUART("out_quart"),
    OUT_QUINT("out_quint"),
    OUT_SINE("out_sine"),
    SPRING("spring");
    CameraEaseType(String name) {
    }
}
