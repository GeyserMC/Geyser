/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.world.collision.translators;

import org.geysermc.connector.network.translators.world.collision.CollisionRemapper;
import org.geysermc.connector.utils.BoundingBox;

@CollisionRemapper(regex = "air$|_sapling$|^water$|^lava$|_rail$|^cobweb$|^grass$|fern$|^dead_bush$|^dandelion$|^poppy$|^blue_orchid$|^allium$|^azure_bluet$|_tulip$|^oxeye_daisy$|^cornflower$|^wither_rose$|^lily_of_the_valley$|_mushroom$|^fire$|^redstone_wire$|^wheat$|^rail$|_sign$|^lever$|torch$|^sugar_cane$|^cake$|^repeater$|_stem$|^vine$|^nether_wart$|_portal$|^tripwire|^potted_dandelion$|^carrots$|^potatoes$|_button$|_pressure_plate$|^comparator$|^sunflower$|^lilac$|^rose_bush$|^tall_grass$|_banner$|^beetroots$|^end_gateway$")
public class EmptyCollision extends BlockCollision {
    public EmptyCollision(String params) {
        super();
        boundingBoxes = new BoundingBox[0];
    }
}
