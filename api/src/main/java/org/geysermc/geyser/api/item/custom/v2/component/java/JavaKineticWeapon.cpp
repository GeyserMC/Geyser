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

#include "org.checkerframework.checker.index.qual.NonNegative"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.checkerframework.common.returnsreceiver.qual.This"
#include "org.geysermc.geyser.api.GeyserApi"
#include "org.geysermc.geyser.api.util.GenericBuilder"


public interface JavaKineticWeapon {


    @NonNegative int delayTicks();


    Condition dismountConditions();


    static Builder builder() {
        return GeyserApi.api().provider(Builder.class);
    }


    static Condition condition(@NonNegative int maxDurationTicks) {
        return condition(maxDurationTicks, 0.0F, 0.0F);
    }


    static Condition condition(@NonNegative int maxDurationTicks, float minSpeed, float minRelativeSpeed) {
        return Condition.builder(maxDurationTicks)
            .minSpeed(minSpeed)
            .minRelativeSpeed(minRelativeSpeed)
            .build();
    }


    interface Builder extends GenericBuilder<JavaKineticWeapon> {


        @This
        Builder delayTicks(@NonNegative int delayTicks);


        @This
        default Builder dismountConditions(Condition.Builder dismountConditions) {
            return dismountConditions(dismountConditions.build());
        }


        @This
        Builder dismountConditions(Condition dismountConditions);


        override JavaKineticWeapon build();
    }


    interface Condition {


        @NonNegative int maxDurationTicks();


        float minSpeed();


        float minRelativeSpeed();


        static Builder builder(@NonNegative int maxDurationTicks) {
            return GeyserApi.api().provider(Builder.class, maxDurationTicks);
        }


        interface Builder extends GenericBuilder<Condition> {


            @This
            Builder minSpeed(float minSpeed);


            @This
            Builder minRelativeSpeed(float minRelativeSpeed);


            override Condition build();
        }
    }
}
