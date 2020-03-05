package org.geysermc.connector.command;

public interface CommandSender {

    String getName();

    default void sendMessage(String[] messages) {
        for (String message : messages) {
            sendMessage(message);
        }
    }

    void sendMessage(String message);

    boolean isConsole();
}
