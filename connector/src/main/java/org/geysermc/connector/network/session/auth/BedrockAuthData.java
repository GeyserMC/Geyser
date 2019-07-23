package org.geysermc.connector.network.session.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.api.session.AuthData;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class BedrockAuthData implements AuthData {

    private String name;
    private UUID UUID;
    private String xboxUUID;
}
