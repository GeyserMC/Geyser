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

import lombok.Getter;
import lombok.experimental.Accessors;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.response.FormResponse;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistryProvider;
import org.geysermc.geyser.registry.java.reader.JavaRegistryReader;
import org.geysermc.geyser.session.dialog.input.DialogInput;
import org.geysermc.geyser.session.dialog.input.ParsedInputs;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Accessors(fluent = true)
public abstract class Dialog {

    private static final Key PLAIN_MESSAGE_BODY = MinecraftKey.key("plain_message");

    @Getter
    private final Component title;
    @Getter
    private final Optional<Component> externalTitle;
    @Getter
    private final boolean canCloseWithEscape;
    @Getter
    private final AfterAction afterAction;
    private final List<Component> labels;
    private final List<DialogInput<?>> inputs = new ArrayList<>();
    @Getter
    private final ParsedInputs defaultInputs;

    protected Dialog(NbtMap map) {
        title = MessageTranslator.componentFromNbtTag(map.get("title"));
        externalTitle = Optional.ofNullable(map.get("external_title")).map(MessageTranslator::componentFromNbtTag);
        canCloseWithEscape = map.getBoolean("can_close_with_escape", true);
        afterAction = AfterAction.fromString(map.getString("after_action"));

        Object bodyTag = map.get("body");
        switch (bodyTag) {
            case null -> labels = List.of();
            case NbtMap bodyMap -> labels = readBody(bodyMap).map(List::of).orElse(List.of());
            case List<?> bodyList -> {
                labels = new ArrayList<>();
                for (Object tag : bodyList) {
                    if (tag instanceof NbtMap bodyMap) {
                        readBody(bodyMap).ifPresent(labels::add);
                    } else {
                        throw new IllegalStateException("Found non-NBT map in list of bodies, was: " + tag);
                    }
                }
            }
            default -> throw new IllegalStateException("Expected body tag to either be a NBT map or list thereof, was: " + bodyTag);
        }

        List<NbtMap> inputTag = map.getList("inputs", NbtType.COMPOUND);
        for (NbtMap input : inputTag) {
            inputs.add(DialogInput.read(input));
        }
        defaultInputs = inputs.isEmpty() ? ParsedInputs.EMPTY : new ParsedInputs(inputs);
    }

    private static Optional<Component> readBody(NbtMap tag) {
        Key type = MinecraftKey.key(tag.getString("type"));
        if (type.equals(PLAIN_MESSAGE_BODY)) {
            return Optional.of(MessageTranslator.componentFromNbtTag(tag.get("contents")));
        }
        // Other type is item, can't display that in forms
        return Optional.empty();
    }

    protected abstract Optional<DialogButton> onCancel();

    protected FormBuilder<? extends FormBuilder<?,?,?>, ? extends Form, ? extends FormResponse> createForm(DialogHolder holder, Optional<ParsedInputs> restored) {
        if (inputs.isEmpty()) {
            SimpleForm.Builder builder = SimpleForm.builder()
                .title(holder.convertText(title));
            builder.content(labels.stream().map(holder::convertText).collect(Collectors.joining("\n\n")));

            builder.closedOrInvalidResultHandler(() -> holder.closeDialog(onCancel()));
            addCustomComponents(holder, builder);
            return builder;
        } else {
            CustomForm.Builder builder = CustomForm.builder()
                .title(holder.convertText(title));
            for (Component label : labels) {
                builder.label(holder.convertText(label));
            }

            restored.ifPresentOrElse(last -> last.restore(holder, builder), () -> inputs.forEach(input -> input.addComponent(builder, holder)));
            builder.closedOrInvalidResultHandler(response -> holder.closeDialog(onCancel()));
            addCustomComponents(holder, builder);
            return builder;
        }
    }

    protected abstract void addCustomComponents(DialogHolder holder, CustomForm.Builder builder);

    protected abstract void addCustomComponents(DialogHolder holder, SimpleForm.Builder builder);

    public void sendForm(DialogHolder holder) {
        holder.session().sendDialogForm(createForm(holder, Optional.empty()).build());
    }

    public void restoreForm(DialogHolder holder, @NonNull ParsedInputs inputs) {
        holder.session().sendDialogForm(createForm(holder, Optional.of(inputs)).build());
    }

    protected Optional<ParsedInputs> parseInput(DialogHolder holder, CustomFormResponse response) {
        ParsedInputs parsed = new ParsedInputs(inputs, response);
        if (parsed.hasErrors()) {
            restoreForm(holder, parsed);
            return Optional.empty();
        }
        return Optional.of(parsed);
    }

    public static Dialog readDialog(JavaRegistryReader.Context context) {
        return readDialogFromNbt(context.javaRegistries(), context.dataAsMap());
    }

    public static Dialog readDialogFromNbt(JavaRegistryProvider registries, NbtMap map) {
        Key type = MinecraftKey.key(map.getString("type"));
        if (type.equals(NoticeDialog.TYPE)) {
            return new NoticeDialog(registries, map);
        } else if (type.equals(ServerLinksDialog.TYPE)) {
            return new ServerLinksDialog(registries, map);
        } else if (type.equals(DialogListDialog.TYPE)) {
            return new DialogListDialog(registries, map);
        } else if (type.equals(MultiActionDialog.TYPE)) {
            return new MultiActionDialog(registries, map);
        } else if (type.equals(ConfirmationDialog.TYPE)) {
            return new ConfirmationDialog(registries, map);
        }

        throw new UnsupportedOperationException("Unable to read unknown dialog type " + type + "!");
    }

    public static Dialog getDialogFromHolder(JavaRegistryProvider registries, Holder<NbtMap> holder) {
        if (holder.isId()) {
            return JavaRegistries.DIALOG.getOrThrow(registries, holder.id());
        } else {
            return Dialog.readDialogFromNbt(registries, holder.custom());
        }
    }

    public static Dialog getDialogFromKey(JavaRegistryProvider registries, Key key) {
        return JavaRegistries.DIALOG.getOrThrow(registries, key);
    }

    public enum AfterAction {
        CLOSE,
        NONE,
        WAIT_FOR_RESPONSE;

        public static AfterAction fromString(String string) {
            for (AfterAction action : values()) {
                if (action.name().toLowerCase(Locale.ROOT).equals(string)) {
                    return action;
                }
            }
            return CLOSE;
        }
    }
}
