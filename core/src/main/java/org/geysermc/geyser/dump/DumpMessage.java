/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.dump;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.text.AsteriskSerializer;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * A problem Geyser logged that keeps a part of it from working, kept for the dump so support does not need the
 * logs to see it.
 *
 * @param source the part of Geyser that logged it, e.g. "nethernet"
 * @param message what was logged
 * @param cause the type and message of the exception behind it, and of its root cause, if any
 */
public record DumpMessage(String source, Level level, String message, @Nullable String cause) {

    public enum Level {
        WARNING,
        ERROR
    }

    public static DumpMessage warning(String source, String message) {
        return new DumpMessage(source, Level.WARNING, message, null);
    }

    public static DumpMessage error(String source, String message, @Nullable Throwable cause) {
        return new DumpMessage(source, Level.ERROR, message, cause == null ? null : describe(cause));
    }

    /**
     * @return this message with public addresses censored, unless the dump is full
     */
    DumpMessage censored() {
        return new DumpMessage(source, level, AsteriskSerializer.censorText(message),
            cause == null ? null : AsteriskSerializer.censorText(cause));
    }

    /**
     * The exception's type and message, and those of its root cause when that is another exception, e.g.
     * "IllegalStateException: could not bind (caused by BindException: Address already in use)". No stack trace,
     * which the logs already have.
     */
    static String describe(Throwable failure) {
        while (failure.getCause() != null && (failure instanceof CompletionException || failure instanceof ExecutionException)) {
            failure = failure.getCause();
        }
        Throwable root = failure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root == failure ? summary(failure) : summary(failure) + " (caused by " + summary(root) + ")";
    }

    private static String summary(Throwable failure) {
        String type = failure.getClass().getSimpleName();
        return failure.getMessage() == null ? type : type + ": " + failure.getMessage();
    }
}
