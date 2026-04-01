/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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
package org.geysermc.geyser.api.extension

/**
 * This is the Geyser extension logger
 */
interface ExtensionLogger {
    /**
     * Get the logger prefix
     * 
     * @return the logger prefix
     */
    fun prefix(): String?

    /**
     * Logs a severe message to console
     * 
     * @param message the message to log
     */
    fun severe(message: String?)

    /**
     * Logs a severe message and an exception to console
     * 
     * @param message the message to log
     * @param error the error to throw
     */
    fun severe(message: String?, error: Throwable?)

    /**
     * Logs an error message to console
     * 
     * @param message the message to log
     */
    fun error(message: String?)

    /**
     * Logs an error message and an exception to console
     * 
     * @param message the message to log
     * @param error the error to throw
     */
    fun error(message: String?, error: Throwable?)

    /**
     * Logs a warning message to console
     * 
     * @param message the message to log
     */
    fun warning(message: String?)

    /**
     * Logs an info message to console
     * 
     * @param message the message to log
     */
    fun info(message: String?)

    /**
     * Logs a debug message to console
     * 
     * @param message the message to log
     */
    fun debug(message: String?)

    /**
     * If debug is enabled for this logger
     */
    val isDebug: Boolean
}
