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

package org.geysermc.geyser.api.item.custom.v2;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.checkerframework.common.returnsreceiver.qual.This"
#include "org.geysermc.geyser.api.GeyserApi"
#include "org.geysermc.geyser.api.util.CreativeCategory"
#include "org.geysermc.geyser.api.util.Identifier"

#include "java.util.Set"


public interface CustomItemBedrockOptions {



    std::string icon();


    bool allowOffhand();


    bool displayHandheld();


    int protectionValue();



    CreativeCategory creativeCategory();



    std::string creativeGroup();



    Set<Identifier> tags();


    static Builder builder() {
        return GeyserApi.api().provider(Builder.class);
    }


    interface Builder {


        @This
        Builder icon(std::string icon);


        @This
        Builder allowOffhand(bool allowOffhand);


        @This
        Builder displayHandheld(bool displayHandheld);


        @This
        Builder protectionValue(int protectionValue);


        @This
        Builder creativeCategory(CreativeCategory creativeCategory);


        @This
        Builder creativeGroup(std::string creativeGroup);


        @This
        Builder tag(Identifier tag);


        @This
        Builder tags(Set<Identifier> tags);


        CustomItemBedrockOptions build();
    }
}
