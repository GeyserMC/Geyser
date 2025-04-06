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
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.NBTComponent;
import net.kyori.adventure.text.ScoreComponent;
import net.kyori.adventure.text.SelectorComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.function.Supplier;

/**
 * This interface contains various {@link MinecraftHasher}s used to encode properties of {@link Component}s. Usually, you'll only need {@link ComponentHasher#COMPONENT}.
 */
public interface ComponentHasher {

    MinecraftHasher<Component> COMPONENT = MinecraftHasher.lazyInitialize(new Supplier<>() {
        @Override
        public MinecraftHasher<Component> get() {
            return ACTUAL_COMPONENT;
        }
    });

    MinecraftHasher<NamedTextColor> NAMED_COLOR = MinecraftHasher.STRING.cast(NamedTextColor::toString);

    MinecraftHasher<TextColor> DIRECT_COLOR = MinecraftHasher.STRING.cast(TextColor::asHexString);

    MinecraftHasher<TextColor> COLOR = (value, encoder) -> {
        if (value instanceof NamedTextColor named) {
            return NAMED_COLOR.hash(named, encoder);
        }
        return DIRECT_COLOR.hash(value, encoder);
    };

    MinecraftHasher<TextDecoration.State> DECORATION_STATE = MinecraftHasher.BOOL.cast(state -> switch (state) {
        case NOT_SET -> null; // Should never happen since we're using .optional() with NOT_SET as default value below
        case FALSE -> false;
        case TRUE -> true;
    });

    MinecraftHasher<ClickEvent.Action> CLICK_EVENT_ACTION = MinecraftHasher.STRING.cast(ClickEvent.Action::toString);

    MinecraftHasher<ClickEvent> CLICK_EVENT = CLICK_EVENT_ACTION.dispatch("action", ClickEvent::action, action -> switch (action) {
        case OPEN_URL -> builder -> builder.accept("url", MinecraftHasher.STRING, ClickEvent::value);
        case OPEN_FILE -> builder -> builder.accept("path", MinecraftHasher.STRING, ClickEvent::value);
        case RUN_COMMAND, SUGGEST_COMMAND -> builder -> builder.accept("command", MinecraftHasher.STRING, ClickEvent::value);
        case CHANGE_PAGE -> builder -> builder.accept("page", MinecraftHasher.STRING, ClickEvent::value);
        case COPY_TO_CLIPBOARD -> builder -> builder.accept("value", MinecraftHasher.STRING, ClickEvent::value);
    });

    MinecraftHasher<HoverEvent.Action<?>> HOVER_EVENT_ACTION = MinecraftHasher.STRING.cast(HoverEvent.Action::toString);

    MinecraftHasher<HoverEvent<?>> HOVER_EVENT = HOVER_EVENT_ACTION.dispatch("action", HoverEvent::action, action -> {
        if (action == HoverEvent.Action.SHOW_TEXT) {
            return builder -> builder.accept("value", COMPONENT, event -> (Component) event.value());
        } else if (action == HoverEvent.Action.SHOW_ITEM) {
            return builder -> builder
                .accept("id", MinecraftHasher.KEY, event -> ((HoverEvent.ShowItem) event.value()).item())
                .accept("count", MinecraftHasher.INT, event -> ((HoverEvent.ShowItem) event.value()).count()); // Data components are probably not possible
        }
        return builder -> builder
            .accept("id", MinecraftHasher.KEY, event -> ((HoverEvent.ShowEntity) event.value()).type())
            .accept("uuid", MinecraftHasher.UUID, event -> ((HoverEvent.ShowEntity) event.value()).id())
            .optionalNullable("name", COMPONENT, event -> ((HoverEvent.ShowEntity) event.value()).name());
    });

    // TODO shadow colours - needs kyori bump
    MapBuilder<Style> STYLE = builder ->  builder
        .optionalNullable("color", COLOR, Style::color)
        .optional("bold", DECORATION_STATE, style -> style.decoration(TextDecoration.BOLD), TextDecoration.State.NOT_SET)
        .optional("italic", DECORATION_STATE, style -> style.decoration(TextDecoration.ITALIC), TextDecoration.State.NOT_SET)
        .optional("underlined", DECORATION_STATE, style -> style.decoration(TextDecoration.UNDERLINED), TextDecoration.State.NOT_SET)
        .optional("strikethrough", DECORATION_STATE, style -> style.decoration(TextDecoration.STRIKETHROUGH), TextDecoration.State.NOT_SET)
        .optional("obfuscated", DECORATION_STATE, style -> style.decoration(TextDecoration.OBFUSCATED), TextDecoration.State.NOT_SET)
        .optionalNullable("click_event", CLICK_EVENT, Style::clickEvent)
        .optionalNullable("hover_event", HOVER_EVENT, Style::hoverEvent)
        .optionalNullable("insertion", MinecraftHasher.STRING, Style::insertion)
        .optionalNullable("font", MinecraftHasher.KEY, Style::font);

    MinecraftHasher<TextComponent> SIMPLE_TEXT_COMPONENT = MinecraftHasher.STRING.cast(TextComponent::content);

    MinecraftHasher<TextComponent> FULL_TEXT_COMPONENT = component(builder -> builder
        .accept("text", MinecraftHasher.STRING, TextComponent::content));

    MinecraftHasher<TextComponent> TEXT_COMPONENT = MinecraftHasher.dispatch(component -> {
        if (component.children().isEmpty() && component.style().isEmpty()) {
            return SIMPLE_TEXT_COMPONENT;
        }
        return FULL_TEXT_COMPONENT;
    });

    MinecraftHasher<TranslatableComponent> TRANSLATABLE_COMPONENT = component(builder -> builder
        .accept("translate", MinecraftHasher.STRING, TranslatableComponent::key)
        .optionalNullable("fallback", MinecraftHasher.STRING, TranslatableComponent::fallback)); // Arguments are probably not possible

    MinecraftHasher<KeybindComponent> KEYBIND_COMPONENT = component(builder -> builder
        .accept("keybind", MinecraftHasher.STRING, component -> component.keybind()));

    MinecraftHasher<ScoreComponent> SCORE_COMPONENT = component(builder -> builder
        .accept("name", MinecraftHasher.STRING, ScoreComponent::name)
        .accept("objective", MinecraftHasher.STRING, ScoreComponent::objective));

    MinecraftHasher<SelectorComponent> SELECTOR_COMPONENT = component(builder -> builder
        .accept("selector", MinecraftHasher.STRING, SelectorComponent::pattern)
        .optionalNullable("separator", COMPONENT, SelectorComponent::separator));

    MinecraftHasher<NBTComponent<?, ?>> NBT_COMPONENT = component(builder -> builder
        .accept("nbt", MinecraftHasher.STRING, NBTComponent::nbtPath)
        .optional("interpret", MinecraftHasher.BOOL, NBTComponent::interpret, false)
        .optionalNullable("separator", COMPONENT, NBTComponent::separator)); // TODO source key, needs kyori update?

    MinecraftHasher<Component> ACTUAL_COMPONENT = (component, encoder) -> {
        if (component instanceof TextComponent text) {
            return TEXT_COMPONENT.hash(text, encoder);
        } else if (component instanceof TranslatableComponent translatable) {
            return TRANSLATABLE_COMPONENT.hash(translatable, encoder);
        } else if (component instanceof KeybindComponent keybind) {
            return KEYBIND_COMPONENT.hash(keybind, encoder);
        } else if (component instanceof ScoreComponent score) {
            return SCORE_COMPONENT.hash(score, encoder);
        } else if (component instanceof SelectorComponent selector) {
            return SELECTOR_COMPONENT.hash(selector, encoder);
        } else if (component instanceof NBTComponent<?,?> nbt) {
            return NBT_COMPONENT.hash(nbt, encoder);
        }
        throw new IllegalStateException("Unimplemented component hasher: " + component);
    };

    private static <T extends Component> MinecraftHasher<T> component(MapBuilder<T> componentBuilder) {
        return MinecraftHasher.mapBuilder(builder -> builder
            .accept(componentBuilder)
            .accept(STYLE, Component::style)
            .optionalList("extra", COMPONENT, Component::children));
    }
}
