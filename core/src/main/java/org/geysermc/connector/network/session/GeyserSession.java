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

package org.geysermc.connector.network.session;

import com.github.steveice10.packetlib.packet.Packet;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import org.geysermc.connector.network.session.auth.AuthData;

/**
 * Deprecated, legacy code. Serves as a wrapper around
 * the class used now.
 *
 * @deprecated legacy code
 */
@Deprecated
public class GeyserSession {
    private final org.geysermc.geyser.session.GeyserSession handle;

    public GeyserSession(org.geysermc.geyser.session.GeyserSession handle) {
        this.handle = handle;
    }

    public AuthData getAuthData() {
        return new AuthData(this.handle.getAuthData());
    }

    public boolean isMicrosoftAccount() {
        return this.handle.isMicrosoftAccount();
    }

    public boolean isClosed() {
        return this.handle.isClosed();
    }

    public String getRemoteAddress() {
        return this.handle.getRemoteAddress();
    }

    public int getRemotePort() {
        return this.handle.getRemotePort();
    }

    public int getRenderDistance() {
        return this.handle.getRenderDistance();
    }

    public boolean isSentSpawnPacket() {
        return this.handle.isSentSpawnPacket();
    }

    public boolean isLoggedIn() {
        return this.handle.isLoggedIn();
    }

    public boolean isLoggingIn() {
        return this.handle.isLoggingIn();
    }

    public boolean isSpawned() {
        return this.handle.isSpawned();
    }

    public boolean isInteracting() {
        return this.handle.isInteracting();
    }

    public boolean isCanFly() {
        return this.handle.isCanFly();
    }

    public boolean isFlying() {
        return this.handle.isFlying();
    }

    public void connect() {
        this.handle.connect();
    }

    public void login() {
        this.handle.login();
    }

    public void authenticate(String username) {
        this.handle.authenticate(username);
    }

    public void authenticate(String username, String password) {
        this.handle.authenticate(username, password);
    }

    public void authenticateWithMicrosoftCode() {
        this.handle.authenticateWithMicrosoftCode();
    }

    public void disconnect(String reason) {
        this.handle.disconnect(reason);
    }

    public void close() {
        this.handle.close();
    }

    public void executeInEventLoop(Runnable runnable) {
        this.handle.executeInEventLoop(runnable);
    }

    public String getName() {
        return this.handle.name();
    }

    public boolean isConsole() {
        return this.handle.isConsole();
    }

    public String getLocale() {
        return this.handle.getLocale();
    }

    public void sendUpstreamPacket(BedrockPacket packet) {
        this.handle.sendUpstreamPacket(packet);
    }

    public void sendUpstreamPacketImmediately(BedrockPacket packet) {
        this.handle.sendUpstreamPacketImmediately(packet);
    }

    public void sendDownstreamPacket(Packet packet) {
        this.handle.sendDownstreamPacket(packet);
    }

    public boolean hasPermission(String permission) {
        return this.handle.hasPermission(permission);
    }

    public void sendAdventureSettings() {
        this.handle.sendAdventureSettings();
    }
}
