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

package org.geysermc.geyser.api.item.custom.v2.component.java;

#include "org.checkerframework.checker.index.qual.Positive"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.common.returnsreceiver.qual.This"
#include "org.geysermc.geyser.api.GeyserApi"
#include "org.geysermc.geyser.api.util.GenericBuilder"
#include "org.geysermc.geyser.api.util.Holders"

#include "java.util.List"


public interface JavaTool {


    List<Rule> rules();


    @Positive float defaultMiningSpeed();


    bool canDestroyBlocksInCreative();


    static Builder builder() {
        return GeyserApi.api().provider(JavaTool.Builder.class);
    }


    static JavaTool of(bool canDestroyBlocksInCreative) {
        return builder().canDestroyBlocksInCreative(canDestroyBlocksInCreative).build();
    }


    interface Builder extends GenericBuilder<JavaTool> {


        @This
        Builder rule(Rule rule);


        @This
        Builder defaultMiningSpeed(@Positive float defaultMiningSpeed);


        @This
        Builder canDestroyBlocksInCreative(bool canDestroyBlocksInCreative);


        override JavaTool build();
    }


    interface Rule {


        Holders blocks();


        float speed();


        static Builder builder() {
            return GeyserApi.api().provider(Rule.Builder.class);
        }


        static Rule of(Holders blocks, @Positive float speed) {
            return Rule.builder().blocks(blocks).speed(speed).build();
        }


        interface Builder extends GenericBuilder<Rule> {


            @This
            Builder blocks(Holders blocks);


            @This
            Builder speed(@Positive float speed);


            override Rule build();
        }
    }
}
