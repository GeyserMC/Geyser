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

package org.geysermc.api.logger;

public interface Logger {

    /**
     * Logs an info message to console
     *
     * @param message the message to log
     */
    void info(String message);

    /**
     * Logs a severe message to console
     *
     * @param message the message to log
     */
    void severe(String message);

    /**
     * Logs a warning message to console
     *
     * @param message the message to log
     */
    void warning(String message);

    /**
     * Logs a debug message to console
     *
     * @param message the message to log
     */
    void debug(String message);
}
