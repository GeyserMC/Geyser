package org.geysermc.connector.utils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
public class BiValue<F, S> {
    private F f;
    private S s;
}
