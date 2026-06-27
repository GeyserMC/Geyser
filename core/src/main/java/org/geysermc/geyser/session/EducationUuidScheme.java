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

package org.geysermc.geyser.session;

/**
 * Selects how an education player's Java UUID is derived. This is the player's identity,
 * so the chosen scheme must match on EduGeyser and every EduFloodgate instance on a network.
 * Loaded once at startup and read explicitly at the UUID derivation site, rather than via
 * ambient global state, so the dependency is visible and cannot be silently bypassed.
 */
public enum EducationUuidScheme {
    /**
     * Derive from the MESS-verified Entra OID. The current, recommended scheme.
     */
    MODERN,
    /**
     * Derive from {@code SHA-256(tenantId:username)}. Preserved only for deployments with
     * existing player data keyed by this scheme.
     */
    LEGACY;

    public boolean legacy() {
        return this == LEGACY;
    }
}
