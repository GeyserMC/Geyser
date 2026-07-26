import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.ResourceTransformer.Companion.transform

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

plugins {
    id("geyser.base-conventions")
    id("geyser.publish-conventions")
    id("io.freefair.lombok")
}

// These dependencies are always provided by the isolated loaded and should never be in the "base-platform" jars

// Provided by base api (and legacy floodgate api)
provided(libs.base.api)
provided(libs.cumulus)
provided(libs.gson.asProvider())
provided(libs.events)
provided(libs.jspecify)

tasks {
    shadowJar {
        // Shadow defaults to EXCLUDE, which makes it drop duplicate files before
        // ShadowCopyAction (and therefore the transformer below) ever sees them.
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        // Merges the interface mappings needed for config loading
        // let's please merge both configs into one!
        transform(PropertiesFileTransformer::class.java) {
            paths.add("org/spongepowered/configurate/interfaces/interface_mappings.properties")
        }
    }
}

