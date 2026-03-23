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

package org.geysermc.geyser.network;

/**
 * Determines how the education server registers with MESS and verifies clients.
 *
 * <ul>
 *   <li>{@link #OFF} — Education support is disabled. No education code runs.
 *       Education clients are rejected on connect.</li>
 *   <li>{@link #OFFICIAL} — MESS-registered. All clients must be verified via nonce.</li>
 *   <li>{@link #HYBRID} — MESS-registered, but additional tenants from server-tokens
 *       are allowed without nonce verification (config trust).</li>
 *   <li>{@link #STANDALONE} — No MESS registration. All tokens come from server-tokens.
 *       No client verification possible.</li>
 * </ul>
 */
public enum EducationTenancyMode {
    OFF,
    OFFICIAL,
    HYBRID,
    STANDALONE
}
