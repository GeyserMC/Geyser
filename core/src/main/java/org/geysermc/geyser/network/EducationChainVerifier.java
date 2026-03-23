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

package org.geysermc.geyser.network;

import org.cloudburstmc.protocol.bedrock.data.auth.AuthPayload;
import org.cloudburstmc.protocol.bedrock.data.auth.CertificateChainPayload;
import org.cloudburstmc.protocol.bedrock.data.auth.TokenPayload;
import org.geysermc.geyser.GeyserLogger;

import java.util.Base64;
import java.util.List;

/**
 * Diagnostic utilities for Education Edition JWT chains.
 * Provides chain dumping for development and debugging.
 */
public final class EducationChainVerifier {

    private EducationChainVerifier() {
    }

    /**
     * Pads a Base64URL string to the correct length for decoding.
     */
    static String padBase64(String base64) {
        int padding = 4 - (base64.length() % 4);
        if (padding != 4) {
            base64 += "=".repeat(padding);
        }
        return base64;
    }

    /**
     * Dumps the full JWT chain and client data JWT for an education client.
     * This logs every JWT's header and payload (base64-decoded) for debugging
     * the token structure used by Education Edition clients.
     *
     * @param logger the logger instance
     * @param authPayload the authentication payload from the login packet
     * @param clientDataJwt the client data JWT string
     */
    public static void dumpEduChain(GeyserLogger logger, AuthPayload authPayload, String clientDataJwt) {
        try {
            logger.debug("[EduChainDump] ========== EDUCATION CLIENT JWT CHAIN DUMP ==========");

            if (authPayload instanceof CertificateChainPayload certChain) {
                List<String> chain = certChain.getChain();
                logger.debug("[EduChainDump] Chain length: %s", chain.size());

                for (int i = 0; i < chain.size(); i++) {
                    String jwtToken = chain.get(i);
                    String[] parts = jwtToken.split("\\.");
                    logger.debug("[EduChainDump] --- Chain JWT #%s (parts: %s) ---", i, parts.length);

                    if (parts.length >= 2) {
                        String header = new String(Base64.getUrlDecoder().decode(padBase64(parts[0])));
                        String payload = new String(Base64.getUrlDecoder().decode(padBase64(parts[1])));
                        logger.debug("[EduChainDump]   Header:  %s", header);
                        logger.debug("[EduChainDump]   Payload: %s", payload);
                    } else {
                        logger.debug("[EduChainDump]   Raw: %s", jwtToken);
                    }
                }
            } else if (authPayload instanceof TokenPayload tokenPayload) {
                String token = tokenPayload.getToken();
                logger.debug("[EduChainDump] Auth payload is TokenPayload (single token).");
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    String header = new String(Base64.getUrlDecoder().decode(padBase64(parts[0])));
                    String payload = new String(Base64.getUrlDecoder().decode(padBase64(parts[1])));
                    logger.debug("[EduChainDump]   Header:  %s", header);
                    logger.debug("[EduChainDump]   Payload: %s", payload);
                }
            } else {
                logger.debug("[EduChainDump] Unknown auth payload type: %s", authPayload.getClass().getName());
            }

            // Also dump the client data JWT
            if (clientDataJwt != null) {
                String[] parts = clientDataJwt.split("\\.");
                logger.debug("[EduChainDump] --- Client Data JWT (parts: %s) ---", parts.length);
                if (parts.length >= 2) {
                    String header = new String(Base64.getUrlDecoder().decode(padBase64(parts[0])));
                    String payload = new String(Base64.getUrlDecoder().decode(padBase64(parts[1])));
                    logger.debug("[EduChainDump]   Header:  %s", header);
                    // Client data payloads can exceed 10KB (skin data); truncate to keep logs readable
                    if (payload.length() > 2000) {
                        logger.debug("[EduChainDump]   Payload (truncated): %s...", payload.substring(0, 2000));
                    } else {
                        logger.debug("[EduChainDump]   Payload: %s", payload);
                    }
                }
            }

            logger.debug("[EduChainDump] ========== END CHAIN DUMP ==========");
        } catch (Exception e) {
            logger.warning("[EduChainDump] Failed to dump education chain: " + e.getMessage());
        }
    }
}
