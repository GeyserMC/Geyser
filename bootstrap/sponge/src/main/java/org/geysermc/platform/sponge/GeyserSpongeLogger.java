package org.geysermc.platform.sponge;

import lombok.AllArgsConstructor;

import org.geysermc.common.logger.IGeyserLogger;
import org.slf4j.Logger;

@AllArgsConstructor
public class GeyserSpongeLogger implements IGeyserLogger {

    private Logger logger;
    private boolean debugMode;

    @Override
    public void severe(String message) {
        logger.error(message);
    }

    @Override
    public void severe(String message, Throwable error) {
        logger.error(message, error);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void error(String message, Throwable error) {
        logger.error(message, error);
    }

    @Override
    public void warning(String message) {
        logger.warn(message);
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void debug(String message) {
        if (debugMode)
            info(message);
    }

    @Override
    public void setDebug(boolean debugMode) {
        this.debugMode = debugMode;
    }
}
