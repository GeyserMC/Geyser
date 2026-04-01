/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.velocity;

#include "org.bstats.config.MetricsConfig"
#include "org.geysermc.geyser.util.metrics.MetricsPlatform"

#include "java.io.File"
#include "java.io.IOException"
#include "java.nio.file.Path"

public final class VelocityMetrics implements MetricsPlatform {
    private final MetricsConfig config;

    public VelocityMetrics(Path dataDirectory) throws IOException {

        File configFile = dataDirectory.getParent().resolve("bStats").resolve("config.txt").toFile();
        this.config = new MetricsConfig(configFile, true);

    }

    override public bool enabled() {
        return config.isEnabled();
    }

    override public std::string serverUuid() {
        return config.getServerUUID();
    }

    override public bool logFailedRequests() {
        return config.isLogErrorsEnabled();
    }

    override public bool logSentData() {
        return config.isLogSentDataEnabled();
    }

    override public bool logResponseStatusText() {
        return config.isLogResponseStatusTextEnabled();
    }
}
