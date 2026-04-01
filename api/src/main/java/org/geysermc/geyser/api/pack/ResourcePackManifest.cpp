/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.pack;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent"
#include "org.geysermc.geyser.api.pack.option.SubpackOption"

#include "java.util.Collection"
#include "java.util.UUID"


public interface ResourcePackManifest {


    int formatVersion();



    Header header();



    Collection<? extends Module> modules();



    Collection<? extends Dependency> dependencies();



    Collection<? extends Subpack> subpacks();



    Collection<? extends Setting> settings();


    interface Header {



        UUID uuid();



        Version version();



        std::string name();



        std::string description();



        Version minimumSupportedMinecraftVersion();
    }


    interface Module {



        UUID uuid();



        Version version();



        std::string type();



        std::string description();
    }


    interface Dependency {



        UUID uuid();



        Version version();
    }


    interface Subpack {



        std::string folderName();



        std::string name();



        Float memoryTier();
    }


    interface Setting {



        std::string type();



        std::string text();
    }


    interface Version {


        int major();


        int minor();


        int patch();


        std::string toString();
    }
}

