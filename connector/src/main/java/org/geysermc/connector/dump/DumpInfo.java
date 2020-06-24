/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.dump;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import lombok.Getter;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.geysermc.connector.utils.DockerCheck;
import org.geysermc.connector.utils.FileUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

@Getter
public class DumpInfo {

    private DumpInfo.VersionInfo versionInfo;
    private Properties gitInfo;
    private GeyserConfiguration config;
    private BoostrapDumpInfo bootstrapInfo;

    public DumpInfo() {
        try {
            this.gitInfo = new Properties();
            this.gitInfo.load(FileUtils.getResource("git.properties"));
        } catch (IOException e) { }

        this.config = GeyserConnector.getInstance().getConfig();

        this.versionInfo = new DumpInfo.VersionInfo();
        this.bootstrapInfo = GeyserConnector.getInstance().getBootstrap().getDumpInfo();
    }

    @Getter
    public class VersionInfo {

        private String name;
        private String version;
        private String javaVersion;
        private String operatingSystem;

        private NetworkInfo network;
        private MCInfo mcInfo;

        VersionInfo() {
            this.name = GeyserConnector.NAME;
            this.version = GeyserConnector.VERSION;
            this.javaVersion = System.getProperty("java.version");
            this.operatingSystem = System.getProperty("os.name");

            this.network = new NetworkInfo();
            this.mcInfo = new MCInfo();
        }
    }

    @Getter
    public class NetworkInfo {

        private String internalIP;
        private boolean dockerCheck;

        NetworkInfo() {
            try {
                // This is the most reliable for getting the main local ip
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress("geysermc.org", 80));
                this.internalIP = socket.getLocalAddress().getHostAddress();
            } catch (IOException e1) {
                try {
                    // Fallback to the normal way of getting the local ip
                    this.internalIP = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e2) { }
            }

            this.dockerCheck = DockerCheck.checkBasic();
        }
    }

    @Getter
    public class MCInfo {

        private String bedrockVersion;
        private int bedrockProtocol;
        private String javaVersion;
        private int javaProtocol;

        MCInfo() {
            this.bedrockVersion = GeyserConnector.BEDROCK_PACKET_CODEC.getMinecraftVersion();
            this.bedrockProtocol = GeyserConnector.BEDROCK_PACKET_CODEC.getProtocolVersion();
            this.javaVersion = MinecraftConstants.GAME_VERSION;
            this.javaProtocol = MinecraftConstants.PROTOCOL_VERSION;
        }
    }
}
