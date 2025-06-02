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

import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.response.FormResponse;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.geyser.session.dialog.action.DialogAction;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

public abstract class Dialog {

    private static final Key PLAIN_MESSAGE_BODY = MinecraftKey.key("plain_message");

    private final String title;
    private final String externalTitle;
    protected final AfterAction afterAction;
    private final List<String> labels;
    private final List<Object> inputs; // TODO

    protected Dialog(GeyserSession session, NbtMap map) {
        title = MessageTranslator.convertFromNullableNbtTag(session, map.get("title"));
        externalTitle = MessageTranslator.convertFromNullableNbtTag(session, map.get("title"));
        afterAction = AfterAction.fromString(map.getString("after_action"));

        Object bodyTag = map.get("body");
        if (bodyTag == null) {
            labels = List.of();
        } else if (bodyTag instanceof NbtMap bodyMap) {
            labels = readBody(session, bodyMap).map(List::of).orElse(List.of());
        } else if (bodyTag instanceof List<?> bodyList) {
            List<String> bodies = new ArrayList<>();
            for (Object tag : bodyList) {
                if (tag instanceof NbtMap bodyMap) {
                    readBody(session, bodyMap).ifPresent(bodies::add);
                } else {
                    throw new IllegalStateException("Found non-NBT map in list of bodies, was: " + tag);
                }
            }
            labels = List.copyOf(bodies);
        } else {
            throw new IllegalStateException("Expected body tag to either be a NBT map or list thereof, was: " + bodyTag);
        }

        inputs = List.of();
    }

    private static Optional<String> readBody(GeyserSession session, NbtMap tag) {
        Key type = MinecraftKey.key(tag.getString("type"));
        if (type.equals(PLAIN_MESSAGE_BODY)) {
            return Optional.of(MessageTranslator.convertFromNullableNbtTag(session, tag.get("contents")));
        }
        // Other type is item, can't display that in forms
        return Optional.empty();
    }

    protected abstract Optional<DialogAction> onCancel();

    protected FormBuilder<? extends FormBuilder<?,?,?>, ? extends Form, ? extends FormResponse> createForm(GeyserSession session) {
        if (inputs.isEmpty()) {
            SimpleForm.Builder builder = SimpleForm.builder()
                .title(title);
            builder.content(String.join("\n", labels));

            builder.closedOrInvalidResultHandler(actionResult(session, onCancel()));
            addCustomComponents(session, builder);
            return builder;
        } else {
            CustomForm.Builder builder = CustomForm.builder()
                .title(title);

            builder.closedOrInvalidResultHandler(actionResult(session, onCancel())); // TODO parse input
            addCustomComponents(session, builder);
            return builder;
        }
    }

    protected abstract void addCustomComponents(GeyserSession session, CustomForm.Builder builder);

    protected abstract void addCustomComponents(GeyserSession session, SimpleForm.Builder builder);

    public Form buildForm(GeyserSession session) {
        return createForm(session).build();
    }

    protected Object parseInput(CustomFormResponse response) {
        return 0; // TODO
    }

    protected Runnable actionResult(GeyserSession session, Optional<DialogAction> action) {
        return () -> action.ifPresent(present -> present.run(session, afterAction));
    }

    protected Consumer<CustomFormResponse> validResultAction(GeyserSession session, Optional<DialogAction> action) {
        Runnable runnable = actionResult(session, action);
        return response -> runnable.run();
    }

    public static Dialog readDialog(RegistryEntryContext context) {
        return readDialogFromNbt(context.session(), context.data(), context::getNetworkId);
    }

    public static Dialog readDialogFromNbt(GeyserSession session, NbtMap map, IdGetter idGetter) {
        // TYPES: notice, server_links, dialog_list, multi_action, confirmation
        Key type = MinecraftKey.key(map.getString("type"));
        if (type.equals(NoticeDialog.TYPE)) {
            return new NoticeDialog(session, map, idGetter);
        } else if (type.equals(ConfirmationDialog.TYPE)) {
            return new ConfirmationDialog(session, map, idGetter);
        } else if (type.equals(MultiActionDialog.TYPE)) {
            return new MultiActionDialog(session, map, idGetter);
        }

        return new Dialog(session, map) {

            @Override
            protected void addCustomComponents(GeyserSession session, CustomForm.Builder builder) {}

            @Override
            protected void addCustomComponents(GeyserSession session, SimpleForm.Builder builder) {}

            @Override
            protected Optional<DialogAction> onCancel() {
                return Optional.empty();
            }
        };
        // throw new UnsupportedOperationException("Unable to read unknown dialog type " + type + "!"); // TODO put this here once all types are implemented
    }

    public static void showDialog(GeyserSession session, Holder<NbtMap> holder) {
        Dialog dialog;
        if (holder.isId()) {
            dialog = JavaRegistries.DIALOG.fromNetworkId(session, holder.id());
        } else {
            dialog = Dialog.readDialogFromNbt(session, holder.custom(), key -> JavaRegistries.DIALOG.keyToNetworkId(session, key));
        }
        session.sendForm(Objects.requireNonNull(dialog).buildForm(session));
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
            return null;
        }
    }

    @FunctionalInterface
    public interface IdGetter extends ToIntFunction<Key> {}
}
