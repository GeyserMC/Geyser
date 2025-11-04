/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.input;

import lombok.RequiredArgsConstructor;
import lombok.Getter;

// This is taken from (https://gist.github.com/wan-adrian/e919b46be3889d865801eb8883407587) or (https://github.com/PowerNukkitX/PowerNukkitX/blob/master/src/main/java/cn/nukkit/network/protocol/types/ClientInputLocksFlag.java)
@RequiredArgsConstructor
@Getter
public enum InputLocksFlag {
    RESET(0),
    CAMERA(2),
    MOVEMENT(4),
    LATERAL_MOVEMENT(16),
    SNEAK(32),
    JUMP(64),
    MOUNT(128),
    DISMOUNT(256),
    MOVE_FORWARD(512),
    MOVE_BACKWARD(1024),
    MOVE_LEFT(2048),
    MOVE_RIGHT(4096);

    private final int offset;
}
