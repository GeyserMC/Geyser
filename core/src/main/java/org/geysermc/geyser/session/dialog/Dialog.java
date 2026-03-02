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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.response.FormResponse;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.geyser.session.dialog.input.DialogInput;
import org.geysermc.geyser.session.dialog.input.ParsedInputs;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.ToIntFunction;

@Accessors(fluent = true)
public abstract class Dialog {

    private static final Key PLAIN_MESSAGE_BODY = MinecraftKey.key("plain_message");

    @Getter
    private final String title;
    @Getter
    private final Optional<String> externalTitle;
    @Getter
    private final boolean canCloseWithEscape;
    @Getter
    private final AfterAction afterAction;
    private final List<String> labels;
    private final List<DialogInput<?>> inputs = new ArrayList<>();
    @Getter
    private final ParsedInputs defaultInputs;

    protected Dialog(Optional<GeyserSession> session, NbtMap map) {
        title = MessageTranslator.convertFromNullableNbtTag(session, map.get("title"));
        externalTitle = Optional.ofNullable(MessageTranslator.convertFromNullableNbtTag(session, map.get("external_title")));
        canCloseWithEscape = map.getBoolean("can_close_with_escape", true);
        afterAction = AfterAction.fromString(map.getString("after_action"));

        Object bodyTag = map.get("body");
        if (bodyTag == null) {
            labels = List.of();
        } else if (bodyTag instanceof NbtMap bodyMap) {
            labels = readBody(session, bodyMap).map(List::of).orElse(List.of());
        } else if (bodyTag instanceof List<?> bodyList) {
            labels = new ArrayList<>();
            for (Object tag : bodyList) {
                if (tag instanceof NbtMap bodyMap) {
                    readBody(session, bodyMap).ifPresent(labels::add);
                } else {
                    throw new IllegalStateException("Found non-NBT map in list of bodies, was: " + tag);
                }
            }
        } else {
            throw new IllegalStateException("Expected body tag to either be a NBT map or list thereof, was: " + bodyTag);
        }

        List<NbtMap> inputTag = map.getList("inputs", NbtType.COMPOUND);
        for (NbtMap input : inputTag) {
            inputs.add(DialogInput.read(session, input));
        }
        defaultInputs = inputs.isEmpty() ? ParsedInputs.EMPTY : new ParsedInputs(inputs);
    }

    private static Optional<String> readBody(Optional<GeyserSession> session, NbtMap tag) {
        Key type = MinecraftKey.key(tag.getString("type"));
        if (type.equals(PLAIN_MESSAGE_BODY)) {
            return Optional.ofNullable(MessageTranslator.convertFromNullableNbtTag(session, tag.get("contents")));
        }
        // Other type is item, can't display that in forms
        return Optional.empty();
    }

    protected abstract Optional<DialogButton> onCancel();

    protected FormBuilder<? extends FormBuilder<?,?,?>, ? extends Form, ? extends FormResponse> createForm(DialogHolder holder, Optional<ParsedInputs> restored) {
        if (inputs.isEmpty()) {
            SimpleForm.Builder builder = SimpleForm.builder()
                .translator(MinecraftLocale::getLocaleString, holder.session().locale())
                .title(title);
            builder.content(String.join("\n\n", labels));

            builder.closedOrInvalidResultHandler(() -> holder.closeDialog(onCancel()));
            addCustomComponents(holder, builder);
            return builder;
        } else {
            CustomForm.Builder builder = CustomForm.builder()
                .translator(MinecraftLocale::getLocaleString, holder.session().locale())
                .title(title);
            for (String label : labels) {
                builder.label(label);
            }

            restored.ifPresentOrElse(last -> last.restore(holder, builder), () -> inputs.forEach(input -> input.addComponent(builder)));
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

    public static Dialog readDialog(RegistryEntryContext context) {
        return readDialogFromNbt(context.session(), context.data(), context::getNetworkId);
    }

    public static Dialog readDialogFromNbt(Optional<GeyserSession> session, NbtMap map, IdGetter idGetter) {
        Key type = MinecraftKey.key(map.getString("type"));
        if (type.equals(NoticeDialog.TYPE)) {
            return new NoticeDialog(session, map, idGetter);
        } else if (type.equals(ServerLinksDialog.TYPE)) {
            return new ServerLinksDialog(session, map, idGetter);
        } else if (type.equals(DialogListDialog.TYPE)) {
            return new DialogListDialog(session, map, idGetter);
        } else if (type.equals(MultiActionDialog.TYPE)) {
            return new MultiActionDialog(session, map, idGetter);
        } else if (type.equals(ConfirmationDialog.TYPE)) {
            return new ConfirmationDialog(session, map, idGetter);
        }

        throw new UnsupportedOperationException("Unable to read unknown dialog type " + type + "!");
    }

    public static Dialog getDialogFromHolder(GeyserSession session, Holder<NbtMap> holder) {
        if (holder.isId()) {
            return Objects.requireNonNull(JavaRegistries.DIALOG.value(session, holder.id()));
        } else {
            return Dialog.readDialogFromNbt(Optional.of(session), holder.custom(), key -> JavaRegistries.DIALOG.networkId(session, key));
        }
    }

    public static Dialog getDialogFromKey(GeyserSession session, Key key) {
        return Objects.requireNonNull(JavaRegistries.DIALOG.value(session, key));
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

    @FunctionalInterface
    public interface IdGetter extends ToIntFunction<Key> {}
}
