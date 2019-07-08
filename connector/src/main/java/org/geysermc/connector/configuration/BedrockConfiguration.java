/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 3, 29 June 2007
 *
 * Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 *
 * You can view the LICENCE file for details.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.configuration;

import lombok.Getter;

@Getter
public class BedrockConfiguration {

    private String address;
    private int port;

    private String motd1;
    private String motd2;

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getMotd1() {
        return motd1;
    }

    public String getMotd2() {
        return motd2;
    }
}