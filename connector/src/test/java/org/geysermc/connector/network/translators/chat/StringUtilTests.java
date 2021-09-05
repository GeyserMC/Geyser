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

package org.geysermc.connector.network.translators.chat;

import org.geysermc.connector.utils.StringByteUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class StringUtilTests {

    final byte[] byteMsg1 = new byte[]{72, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100, 33, 0}; // Hello world!
    final String stringMsg1 = "Hello world!";

    //a minecraft:register message content, last string is NOT null-terminated
    final byte[] byteMsg2 = new byte[]{102, 97, 98, 114, 105, 99, 58, 99, 111, 110, 116, 97, 105, 110, 101, 114, 47, 111, 112, 101, 110, 0, 102, 97, 98, 114, 105, 99, 58, 114, 101, 103, 105, 115, 116, 114, 121, 47, 115, 121, 110, 99, 0, 102, 97, 98, 114, 105, 99, 45, 115, 99, 114, 101, 101, 110, 45, 104, 97, 110, 100, 108, 101, 114, 45, 97, 112, 105, 45, 118, 49, 58, 111, 112, 101, 110, 95, 115, 99, 114, 101, 101, 110, 0, 101, 109, 111, 116, 101, 99, 114, 97, 102, 116, 58, 101, 109, 111, 116, 101};

    // fabric:container/open
    // fabric:registry/sync
    // fabric-screen-handler-api-v1:open_screen
    // emotecraft:emote
    final List<String> msg2 = new ArrayList<>();

    byte[] byteMsg2Terminated;

    @Before
    public void prepMsg2(){
        msg2.clear();
        msg2.add("fabric:container/open");
        msg2.add("fabric:registry/sync");
        msg2.add("fabric-screen-handler-api-v1:open_screen");
        msg2.add("emotecraft:emote");

        byteMsg2Terminated = new byte[byteMsg2.length+1];
        System.arraycopy(byteMsg2, 0, byteMsg2Terminated, 0, byteMsg2.length);
        byteMsg2Terminated[byteMsg2Terminated.length-1] = 0;
    }

    @Test
    public void testStrings2bytes(){
        Assert.assertArrayEquals(byteMsg1, StringByteUtil.string2bytes(stringMsg1));
        Assert.assertArrayEquals(byteMsg2Terminated, StringByteUtil.string2bytes(msg2));
    }

    @Test
    public void testBytes2String(){
        Assert.assertEquals(stringMsg1, StringByteUtil.bytes2strings(byteMsg1).get(0));
        Assert.assertEquals(msg2, StringByteUtil.bytes2strings(byteMsg2Terminated));
        Assert.assertEquals(msg2, StringByteUtil.bytes2strings(byteMsg2));
    }
}
