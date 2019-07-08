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

package org.geysermc.connector.console;

import com.google.common.base.Charsets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingOutputStream extends ByteArrayOutputStream {

    private Logger logger;
    private Level level;

    public LoggingOutputStream(Logger logger, Level level) {
        this.logger = logger;
        this.level = level;
    }

    @Override
    public synchronized void write(int b) {
        super.write(b);

        try {
            String contents = toString(Charsets.UTF_8.name());
            if (!contents.isEmpty() && !contents.equals(System.getProperty("line.separator")))
                logger.logp(level, "", "", contents);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(LoggingOutputStream.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public synchronized void flush() throws IOException {
        String contents = toString(Charsets.UTF_8.name());
        super.reset();

        if (!contents.isEmpty() && !contents.equals(System.getProperty("line.separator")))
            logger.logp(level, "", "", contents);
    }
}
