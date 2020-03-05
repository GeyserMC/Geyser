package org.geysermc.connector.network.session.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class AuthData {

    private String name;
    private UUID UUID;
    private String xboxUUID;
}
