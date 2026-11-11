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

package org.geysermc.geyser.platform.spigot;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Geyser compiles against an older Paper API so it keeps running on older servers. That hides new abstract methods
 * in Paper interfaces Geyser implements, which then only fail at runtime on newer servers, such as
 * NetworkClient#getSocketAddress breaking ping passthrough on Paper 26.3.
 * <p>
 * This checks Geyser's implementations against the paper-api of the runServer version instead.
 */
public class PaperApiCompatibilityTest {
    private static final List<String> API_PACKAGES = List.of("org.bukkit.", "com.destroystokyo.paper.", "io.papermc.paper.");

    private static URLClassLoader runServerApi;

    @BeforeAll
    static void loadRunServerApi() {
        String path = System.getProperty("geyser.runServerPaperApi");
        if (path == null || path.isBlank()) {
            fail("geyser.runServerPaperApi is not set; run this test through Gradle");
        }
        URL[] urls = Arrays.stream(path.split(File.pathSeparator))
            .map(PaperApiCompatibilityTest::toUrl)
            .toArray(URL[]::new);
        // Platform loader as parent, so Paper classes only come from the runServer API, not the one Geyser compiles against
        runServerApi = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader());
    }

    @AfterAll
    static void close() throws IOException {
        if (runServerApi != null) {
            runServerApi.close();
        }
    }

    /**
     * Geyser classes implementing Paper or Bukkit interfaces. Add new ones here.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "org.geysermc.geyser.platform.spigot.GeyserPaperPingPassthrough$GeyserStatusClient"
    })
    void implementsRunServerApi(String className) throws ClassNotFoundException {
        Class<?> implementation = Class.forName(className, false, getClass().getClassLoader());

        List<Class<?>> checked = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (Class<?> compiledInterface : implementation.getInterfaces()) {
            if (API_PACKAGES.stream().noneMatch(compiledInterface.getName()::startsWith)) {
                continue;
            }
            Class<?> runServerInterface = Class.forName(compiledInterface.getName(), false, runServerApi);
            checked.add(runServerInterface);

            for (Method method : runServerInterface.getMethods()) {
                if (Modifier.isAbstract(method.getModifiers()) && !isImplemented(implementation, method)) {
                    missing.add(runServerInterface.getSimpleName() + "#" + method.getName()
                        + Arrays.stream(method.getParameterTypes()).map(Class::getName).toList());
                }
            }
        }

        assertTrue(!checked.isEmpty(), className + " implements no Paper or Bukkit interface; remove it from this test");
        assertTrue(missing.isEmpty(), className + " does not implement " + missing
            + " from the runServer paper-api, so calls to them fail with an AbstractMethodError on that version");
    }

    /**
     * Compares by type names, as the runServer API's classes come from another class loader.
     */
    private static boolean isImplemented(Class<?> implementation, Method required) {
        List<String> parameters = Arrays.stream(required.getParameterTypes()).map(Class::getName).toList();
        for (Method method : implementation.getMethods()) {
            if (!Modifier.isAbstract(method.getModifiers()) && method.getName().equals(required.getName())
                && Arrays.stream(method.getParameterTypes()).map(Class::getName).toList().equals(parameters)) {
                return true;
            }
        }
        return false;
    }

    private static URL toUrl(String file) {
        try {
            return new File(file).toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(file, e);
        }
    }
}
