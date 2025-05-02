/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.fabric.gametest;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceLocation;
import org.geysermc.geyser.session.GeyserSession;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class GeyserTestInstance extends GameTestInstance {

    protected GeyserTestInstance() {
        // TODO use default vanilla test environment
        super(new TestData<>(Holder.direct(new TestEnvironmentDefinition.AllOf(List.of())),
            ResourceLocation.withDefaultNamespace("empty"), 1, 1, true));
    }

    @Override
    public void run(@NotNull GameTestHelper helper) {
        /*Map<UUID, GeyserSession> sessions = GeyserImpl.getInstance().getSessionManager().getSessions();
        while (sessions.isEmpty()) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException ignored) {}
        }
        GeyserSession session = sessions.values().stream().findAny().orElseThrow();*/
        run(helper, null);
    }

    protected abstract void run(@NotNull GameTestHelper helper, GeyserSession session);
}
