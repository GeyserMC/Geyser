/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.world.collision.translators;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.geysermc.connector.utils.BoundingBox;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class OtherCollision extends BlockCollision {

    public OtherCollision(ArrayNode collisionList, String ID) { // TODO: remove id
        super();
        boundingBoxes = new BoundingBox[collisionList.size()];

        for (int i = 0; i < collisionList.size(); i++)
        {
            ArrayNode collisionBoxArray = (ArrayNode) collisionList.get(i);
            boundingBoxes[i] = new BoundingBox(collisionBoxArray.get(0).asDouble(),
                    collisionBoxArray.get(1).asDouble(),
                    collisionBoxArray.get(2).asDouble(),
                    collisionBoxArray.get(3).asDouble(),
                    collisionBoxArray.get(4).asDouble(),
                    collisionBoxArray.get(5).asDouble());
        }

        // Sorting by lowest Y first fixes some bugs
        Arrays.sort(boundingBoxes, (b1, b2) -> {
            if (b1.getMiddleY() < b2.getMiddleY())
                return -1;
            if (b1.getMiddleY() > b2.getMiddleY())
                return 1;
            return 0;
        });
    }
}
