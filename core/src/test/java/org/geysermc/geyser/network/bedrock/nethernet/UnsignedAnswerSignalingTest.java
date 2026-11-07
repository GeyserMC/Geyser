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


package org.geysermc.geyser.network.bedrock.nethernet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnsignedAnswerSignalingTest {
    private static final String ANSWER_START = "CONNECTRESPONSE 42 v=0\r\no=rtc 1 0 IN IP4 127.0.0.1\r\n";
    private static final String ANSWER_END = "m=application 46330 UDP/DTLS/SCTP webrtc-datachannel\r\na=setup:active\r\n"
        + "a=candidate:1 1 UDP 2114977791 192.168.1.150 46330 typ host\r\n";

    @Test
    void answerLosesOnlyItsIdentityLine() {
        String signed = ANSWER_START + "a=identity:eyJpZHAiOnsiZG9tYWluIjoic2VsZiJ9\r\n" + ANSWER_END;
        assertEquals(ANSWER_START + ANSWER_END, UnsignedAnswerSignaling.withoutIdentity(signed));
    }

    @Test
    void otherSignalsPassUnchanged() {
        String candidate = "CANDIDATEADD 42 candidate:1 1 udp 1 192.168.1.150 46330 typ host a=identity:x\r\n";
        assertEquals(candidate, UnsignedAnswerSignaling.withoutIdentity(candidate));
    }
}
