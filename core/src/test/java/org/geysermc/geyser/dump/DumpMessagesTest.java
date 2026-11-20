/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.dump;

import com.google.gson.Gson;
import org.geysermc.geyser.text.AsteriskSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.BindException;
import java.util.List;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The "messages" section of a dump: what each part of Geyser logged as keeping it from working.
 */
class DumpMessagesTest {

    @AfterEach
    void resetSensitivity() {
        AsteriskSerializer.showSensitive = false;
    }

    @Test
    void namesTheFailureAndItsRootCause() {
        IOException bind = new IOException("could not bind", new BindException("Address already in use"));
        assertEquals("IOException: could not bind (caused by BindException: Address already in use)",
            DumpMessage.describe(new CompletionException(bind)));
    }

    @Test
    void namesAMissingNativeLibraryOrClass() {
        assertEquals("UnsatisfiedLinkError: no datachannel-java in java.library.path",
            DumpMessage.describe(new UnsatisfiedLinkError("no datachannel-java in java.library.path")));
        assertEquals("NoClassDefFoundError: io/netty/handler/codec/http/HttpObjectAggregator",
            DumpMessage.describe(new NoClassDefFoundError("io/netty/handler/codec/http/HttpObjectAggregator")));
    }

    @Test
    void warningsHaveNoCause() {
        assertNull(DumpMessage.warning("nethernet", "loopback").cause());
    }

    @Test
    void censorsPublicAddressesButKeepsLocalOnesAndPorts() {
        assertEquals("Could not reach ***:443, [***]:19132 or ***, but 192.168.1.5:19132, 127.0.0.1 and :: are fine",
            AsteriskSerializer.censorText("Could not reach 198.51.100.1:443, [2001:db8::1]:19132 or 2001:db8::2, "
                + "but 192.168.1.5:19132, 127.0.0.1 and :: are fine"));
    }

    @Test
    void leavesVersionsTimesAndHostNamesAlone() {
        String text = "Netty 4.2.20.Final at 20:44:51 failed to reach nxs.geysermc.org";
        assertEquals(text, AsteriskSerializer.censorText(text));
    }

    @Test
    void showsAddressesInAFullDump() {
        AsteriskSerializer.showSensitive = true;
        assertEquals("Could not reach 198.51.100.1:443", AsteriskSerializer.censorText("Could not reach 198.51.100.1:443"));
    }

    @Test
    void collectsAndCensorsEverySource() {
        DumpMessageSource nethernet = () -> List.of(
            DumpMessage.warning("nethernet", "The Bedrock \"address\" 127.0.0.1 is a loopback address"),
            DumpMessage.error("nethernet", "Could not start the external signaling service",
                new IOException("Connection refused: /198.51.100.7:443")));

        assertEquals("[{\"source\":\"nethernet\",\"level\":\"WARNING\",\"message\":\"The Bedrock \\\"address\\\" 127.0.0.1 is a loopback address\"},"
                + "{\"source\":\"nethernet\",\"level\":\"ERROR\",\"message\":\"Could not start the external signaling service\","
                + "\"cause\":\"IOException: Connection refused: /***:443\"}]",
            new Gson().toJson(DumpInfo.messages(List.of(nethernet))));
    }
}
