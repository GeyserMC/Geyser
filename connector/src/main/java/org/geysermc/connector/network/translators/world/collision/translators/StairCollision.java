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
import org.geysermc.connector.network.translators.world.collision.translators.BlockCollision;
import org.geysermc.connector.utils.BoundingBox;

@CollisionRemapper(regex = "_stairs$")
public class StairCollision extends BlockCollision {
    public StairCollision(String params) {
        super();
        if (params.contains("north")) {
            boundingBoxes = new BoundingBox[]{
                    new BoundingBox(0.5, 0.25, 0.75, 1, 0.5, 0.5),
                    new BoundingBox(0.5, 0.5, 0.25, 1, 1, 0.5)
            };
        } else if (params.contains("east")) {
            boundingBoxes = new BoundingBox[]{
                    new BoundingBox(0.25, 0.25, 0.5, 0.5, 0.5, 1),
                    new BoundingBox(0.75, 0.5, 0.5, 0.5, 1, 1)
            };
        } else if (params.contains("south")) {
            boundingBoxes = new BoundingBox[]{
                    new BoundingBox(0.5, 0.25, 0.25, 1, 0.5, 0.5),
                    new BoundingBox(0.5, 0.5, 0.75, 1, 1, 0.5)
            };
        } else if (params.contains("west")) {
            boundingBoxes = new BoundingBox[]{
                    new BoundingBox(0.75, 0.25, 0.5, 0.5, 0.5, 1),
                    new BoundingBox(0.25, 0.5, 0.5, 0.5, 1, 1),
            };
        }
    }
}