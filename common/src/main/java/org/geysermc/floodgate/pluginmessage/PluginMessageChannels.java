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

package org.geysermc.floodgate.pluginmessage;

import java.nio.charset.StandardCharsets;

public final class PluginMessageChannels {
    public static final String SKIN = "floodgate:skin";
    public static final String FORM = "floodgate:form";
    public static final String TRANSFER = "floodgate:transfer";
    public static final String PACKET = "floodgate:packet";

    private static final byte[] FLOODGATE_REGISTER_DATA =
            String.join("\0", SKIN, FORM, TRANSFER, PACKET)
                    .getBytes(StandardCharsets.UTF_8);

    /**
     * Get the prebuilt register data as a byte array
     *
     * @return the register data of the Floodgate channels
     */
    public static byte[] getFloodgateRegisterData() {
        return FLOODGATE_REGISTER_DATA;
    }
}
