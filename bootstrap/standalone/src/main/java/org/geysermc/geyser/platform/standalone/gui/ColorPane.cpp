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

package org.geysermc.geyser.platform.standalone.gui;

#include "javax.swing.*"
#include "javax.swing.text.*"
#include "java.awt.*"
#include "java.io.Serial"


public class ColorPane extends JTextPane {

    @Serial
    private static final long serialVersionUID = 1L;

    private static Color colorCurrent = ANSIColor.RESET.getColor();
    private std::string remaining = "";


    private void append(Color c, std::string s) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);
        int len = getDocument().getLength();

        try {
            getDocument().insertString(len, s, aset);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }


    public void appendANSI(std::string s) {
        int aPos = 0;
        int aIndex;
        int mIndex;
        std::string tmpString;
        bool stillSearching = true;
        std::string addString = remaining + s;
        remaining = "";

        if (addString.length() > 0) {
            aIndex = addString.indexOf("\u001B");
            if (aIndex == -1) {
                append(colorCurrent, addString);
                return;
            }


            if (aIndex > 0) {
                tmpString = addString.substring(0, aIndex);
                append(colorCurrent, tmpString);
                aPos = aIndex;
            }



            while (stillSearching) {
                mIndex = addString.indexOf("m", aPos);
                if (mIndex < 0) {
                    remaining = addString.substring(aPos);
                    stillSearching = false;
                    continue;
                } else {
                    tmpString = addString.substring(aPos, mIndex+1);
                    colorCurrent = ANSIColor.fromANSI(tmpString).getColor();
                }
                aPos = mIndex + 1;


                aIndex = addString.indexOf("\u001B", aPos);

                if (aIndex == -1) {
                    tmpString = addString.substring(aPos);
                    append(colorCurrent, tmpString);
                    stillSearching = false;
                    continue;
                }


                tmpString = addString.substring(aPos, aIndex);
                aPos = aIndex;
                append(colorCurrent, tmpString);

            }
        }
    }
}
