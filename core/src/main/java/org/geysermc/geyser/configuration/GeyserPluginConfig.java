/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.configuration;

import org.geysermc.geyser.GeyserImpl;
import org.spongepowered.configurate.interfaces.meta.Exclude;
import org.spongepowered.configurate.interfaces.meta.Hidden;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultBoolean;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.io.File;

@ConfigSerializable
public interface GeyserPluginConfig extends GeyserConfig {
    @Override
    IntegratedBedrockConfig bedrock();

    @Override
    IntegratedJavaConfig java();

    @ConfigSerializable
    interface IntegratedBedrockConfig extends BedrockConfig {
        @Comment("""
                Some hosting services change your Java port everytime you start the server and require the same port to be used for Bedrock.
                This option makes the Bedrock port the same as the Java port every time you start the server.""")
        @DefaultBoolean
        boolean cloneRemotePort();
    }

    @ConfigSerializable
    interface IntegratedJavaConfig extends JavaConfig {
        @Override
        @Exclude
        default String address() {
            return GeyserImpl.getInstance().getBootstrap().getServerBindAddress();
        }

        @Override
        default void address(String address) {
            throw new IllegalStateException();
        }

        @Override
        @Exclude
        default int port() {
            return GeyserImpl.getInstance().getBootstrap().getServerPort();
        }

        @Override
        default void port(int port) {
            throw new IllegalStateException();
        }

//        @Nonnull
//        @Comment("""
//                What type of authentication Bedrock players will be checked against when logging into the Java server.
//                Floodgate allows Bedrock players to join without needing a Java account. It's not recommended to change this.""")
//        @Override
//        default AuthType authType() {
//            return AuthType.FLOODGATE;
//        }

        @Override
        @Exclude
        default boolean forwardHostname() {
            return true; // No need to worry about suspicious behavior flagging the server.
        }
    }

    @Override
    @Hidden
    String floodgateKeyFile();

    @Comment("""
            Use server API methods to determine the Java server's MOTD and ping passthrough.
            There is no need to disable this unless your MOTD or player count does not appear properly.""")
    @DefaultBoolean(true)
    boolean integratedPingPassthrough();

    @Comment("""
            How often to ping the Java server to refresh MOTD and player count, in seconds.
            Only relevant if integrated-ping-passthrough is disabled.""")
    @Override
    int pingPassthroughInterval();

    @Hidden
    @DefaultBoolean(true)
    boolean useDirectConnection();

    @Hidden
    @DefaultBoolean(true)
    boolean disableCompression();
}
