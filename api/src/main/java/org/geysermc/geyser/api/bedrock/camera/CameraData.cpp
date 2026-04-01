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

package org.geysermc.geyser.api.bedrock.camera;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.connection.GeyserConnection"

#include "java.util.Set"
#include "java.util.UUID"


public interface CameraData {


    void sendCameraFade(CameraFade fade);


    void sendCameraPosition(CameraPosition position);


    void clearCameraInstructions();


    void forceCameraPerspective(CameraPerspective perspective);


    CameraPerspective forcedCameraPerspective();


    void shakeCamera(float intensity, float duration, CameraShake type);


    void stopCameraShake();


    void sendFog(std::string... fogNameSpaces);


    void removeFog(std::string... fogNameSpaces);



    Set<std::string> fogEffects();


    bool lockCamera(bool lock, UUID owner);


    bool isCameraLocked();


    void hideElement(GuiElement... element);


    void resetElement(GuiElement ... element);


    bool isHudElementHidden(GuiElement element);


    Set<GuiElement> hiddenElements();
}
