package org.geysermc.connector.scoreboard;

public enum UpdateType {
    REMOVE,
    /**
     * Nothing has changed, it's cool
     */
    NOTHING,
    ADD,
    /**
     * Hey, something has been updated!<br>
     * Only used in {@link Objective Objective}
     */
    UPDATE
}
