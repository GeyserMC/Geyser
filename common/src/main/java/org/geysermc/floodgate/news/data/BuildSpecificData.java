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

package org.geysermc.floodgate.news.data;

import com.google.gson.JsonObject;

public final class BuildSpecificData implements ItemData {
    private String branch;

    private boolean allAffected;
    private int affectedGreaterThan;
    private int affectedLessThan;

    private BuildSpecificData() {}

    public static BuildSpecificData read(JsonObject data) {
        BuildSpecificData updateData = new BuildSpecificData();
        updateData.branch = data.get("branch").getAsString();

        JsonObject affectedBuilds = data.getAsJsonObject("affected_builds");
        if (affectedBuilds.has("all")) {
            updateData.allAffected = affectedBuilds.get("all").getAsBoolean();
        }
        if (!updateData.allAffected) {
            updateData.affectedGreaterThan = affectedBuilds.get("gt").getAsInt();
            updateData.affectedLessThan = affectedBuilds.get("lt").getAsInt();
        }
        return updateData;
    }

    public boolean isAffected(String branch, int buildId) {
        return this.branch.equals(branch) &&
                (allAffected || buildId > affectedGreaterThan && buildId < affectedLessThan);
    }

    @SuppressWarnings("unused")
    public String getBranch() {
        return branch;
    }
}
