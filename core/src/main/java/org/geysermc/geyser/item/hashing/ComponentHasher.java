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

package org.geysermc.geyser.item.hashing;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public interface ComponentHasher {

    MinecraftHasher<Component> COMPONENT = (value, encoder) -> encoder.empty(); // TODO

    MinecraftHasher<NamedTextColor> NAMED_COLOR = MinecraftHasher.STRING.convert(NamedTextColor::toString);

    MinecraftHasher<TextColor> DIRECT_COLOR = MinecraftHasher.STRING.convert(TextColor::asHexString);

    MinecraftHasher<TextColor> COLOR = (value, encoder) -> {
        if (value instanceof NamedTextColor named) {
            return NAMED_COLOR.hash(named, encoder);
        }
        return DIRECT_COLOR.hash(value, encoder);
    };

    MinecraftHasher<TextDecoration.State> DECORATION_STATE = MinecraftHasher.BOOL.convert(state -> switch (state) {
        case NOT_SET -> null; // Should never happen since we're using .optional() with NOT_SET as default value below
        case FALSE -> false;
        case TRUE -> true;
    });

    MinecraftHasher<ClickEvent.Action> CLICK_EVENT_ACTION = MinecraftHasher.STRING.convert(ClickEvent.Action::toString);

    MinecraftHasher<ClickEvent> CLICK_EVENT = CLICK_EVENT_ACTION.dispatch("action", ClickEvent::action, action -> switch (action) {
        case OPEN_URL -> builder -> builder.accept("url", MinecraftHasher.STRING, ClickEvent::value);
        case OPEN_FILE -> builder -> builder.accept("path", MinecraftHasher.STRING, ClickEvent::value);
        case RUN_COMMAND, SUGGEST_COMMAND -> builder -> builder.accept("command", MinecraftHasher.STRING, ClickEvent::value);
        case CHANGE_PAGE -> builder -> builder.accept("page", MinecraftHasher.STRING, ClickEvent::value);
        case COPY_TO_CLIPBOARD -> builder -> builder.accept("value", MinecraftHasher.STRING, ClickEvent::value);
    });

    MinecraftHasher<HoverEvent.Action<?>> HOVER_EVENT_ACTION = MinecraftHasher.STRING.convert(HoverEvent.Action::toString);

    MinecraftHasher<HoverEvent<?>> HOVER_EVENT = HOVER_EVENT_ACTION.dispatch("action", HoverEvent::action, action -> {
        if (action == HoverEvent.Action.SHOW_TEXT) {

        } else if (action == HoverEvent.Action.SHOW_ITEM) {

        }
        return builder -> builder;
    });

    // TODO shadow colours - needs kyori bump
    MinecraftHasher<Style> STYLE = MinecraftHasher.mapBuilder(builder ->  builder
        .optionalNullable("color", COLOR, Style::color)
        .optional("bold", DECORATION_STATE, style -> style.decoration(TextDecoration.BOLD), TextDecoration.State.NOT_SET)
        .optional("italic", DECORATION_STATE, style -> style.decoration(TextDecoration.ITALIC), TextDecoration.State.NOT_SET)
        .optional("underlined", DECORATION_STATE, style -> style.decoration(TextDecoration.UNDERLINED), TextDecoration.State.NOT_SET)
        .optional("strikethrough", DECORATION_STATE, style -> style.decoration(TextDecoration.STRIKETHROUGH), TextDecoration.State.NOT_SET)
        .optional("obfuscated", DECORATION_STATE, style -> style.decoration(TextDecoration.OBFUSCATED), TextDecoration.State.NOT_SET)
        .optionalNullable("click_event", CLICK_EVENT, Style::clickEvent)
        /*.optional() // hover event*/
        .optionalNullable("insertion", MinecraftHasher.STRING, Style::insertion)
        .optionalNullable("font", MinecraftHasher.KEY, Style::font));

    MinecraftHasher<TextComponent> SIMPLE_TEXT_COMPONENT = MinecraftHasher.STRING.convert(TextComponent::content);

    MinecraftHasher<TextComponent> TEXT_COMPONENT = (value, encoder) -> {
        if (value.children().isEmpty() && value.style().isEmpty()) {
            return SIMPLE_TEXT_COMPONENT.hash(value, encoder);
        }
        return encoder.empty(); // TODO
    };

    MinecraftHasher<Component> ACTUAL_COMPONENT = (value, encoder) -> encoder.empty(); // TODO
}
