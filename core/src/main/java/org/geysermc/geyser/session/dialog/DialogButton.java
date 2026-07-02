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

package org.geysermc.geyser.session.dialog;

import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.dialog.action.DialogAction;
import org.geysermc.geyser.translator.text.MessageTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record DialogButton(String label, Optional<FormImage> icon, Optional<DialogAction> action) {

    public DialogButton(String label, Optional<DialogAction> action) {
        this(label, Optional.empty(), action);
    }

    public static List<DialogButton> readList(Optional<GeyserSession> session, List<NbtMap> tag, Dialog.IdGetter idGetter) {
        if (tag == null) {
            return List.of();
        }
        List<DialogButton> buttons = new ArrayList<>();
        for (NbtMap map : tag) {
            buttons.add(read(session, map, idGetter).orElseThrow()); // Should never throw because we know map is a NbtMap
        }
        return buttons;
    }

    public static Optional<DialogButton> read(Optional<GeyserSession> session, Object tag, Dialog.IdGetter idGetter) {
        if (!(tag instanceof NbtMap map)) {
            return Optional.empty();
        }
        Object label = map.get("label");
        return Optional.of(new DialogButton(
            MessageTranslator.convertFromNullableNbtTag(session, label),
            iconFromLabel(label),
            DialogAction.read(map.get("action"), idGetter)));
    }

    /**
     * Java allows a button label to embed an atlas sprite (a {@code type: "object"} component). Bedrock cannot render
     * a sprite inside text, but a {@link org.geysermc.cumulus.form.SimpleForm} button can show an image next to its
     * text, so we surface the first sprite in the label as the button's icon.
     */
    private static Optional<FormImage> iconFromLabel(Object label) {
        NbtMap sprite = findSprite(label);
        if (sprite == null) {
            return Optional.empty();
        }
        return spriteTexturePath(sprite.getString("atlas", ""), sprite.getString("sprite", ""))
            .map(path -> FormImage.of(FormImage.Type.PATH, path));
    }

    /**
     * Recursively searches a label component for the first atlas sprite ({@code type: "object"} with a {@code sprite}).
     */
    private static NbtMap findSprite(Object tag) {
        if (tag instanceof NbtMap map) {
            if ("object".equals(map.getString("type", null)) && map.containsKey("sprite")) {
                return map;
            }
            Object extra = map.get("extra");
            if (extra != null) {
                return findSprite(extra);
            }
        } else if (tag instanceof List<?> list) {
            for (Object entry : list) {
                NbtMap found = findSprite(entry);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Best-effort translation of a Java atlas sprite to the equivalent vanilla Bedrock texture path. Java lays item
     * and block textures out under {@code textures/item} and {@code textures/block}, while Bedrock uses the pluralized
     * {@code textures/items} and {@code textures/blocks}. Leaf names do not always match between the two editions, so
     * an unrecognized sprite resolves to no icon (Bedrock simply renders no image) rather than a wrong one.
     */
    public static Optional<String> spriteTexturePath(String atlas, String sprite) {
        if (!atlas.equals("minecraft:items") && !atlas.equals("minecraft:blocks")) {
            return Optional.empty();
        }
        // Sprites may be namespaced (e.g. "minecraft:item/barrier"); we only care about the path.
        int colon = sprite.indexOf(':');
        if (colon != -1) {
            sprite = sprite.substring(colon + 1);
        }
        int slash = sprite.indexOf('/');
        if (slash == -1) {
            return Optional.empty();
        }
        String bedrockCategory = switch (sprite.substring(0, slash)) {
            case "item" -> "items";
            case "block" -> "blocks";
            default -> null;
        };
        if (bedrockCategory == null) {
            return Optional.empty();
        }
        return Optional.of("textures/" + bedrockCategory + "/" + sprite.substring(slash + 1));
    }
}
