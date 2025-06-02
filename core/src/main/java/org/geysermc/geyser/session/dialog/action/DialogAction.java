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

package org.geysermc.geyser.session.dialog.action;

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.dialog.Dialog;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomClickActionPacket;

import java.util.Optional;

public interface DialogAction {

    static Optional<DialogAction> read(Object tag, Dialog.IdGetter idGetter) {
        if (!(tag instanceof NbtMap map)) {
            return Optional.empty();
        }

        Key type = MinecraftKey.key(map.getString("type"));
        if (type.equals(OpenUrl.TYPE)) {
            return Optional.of(new OpenUrl(map.getString("url")));
        } else if (type.equals(RunCommand.TYPE)) {
            return Optional.of(new RunCommand(map.getString("command")));
        } else if (type.equals(ShowDialog.TYPE)) {
            return Optional.of(ShowDialog.readDialog(map.get("dialog"), idGetter));
        } else if (type.equals(Custom.TYPE)) {
            return Optional.of(new Custom(MinecraftKey.key(map.getString("id")), map.getCompound("payload")));
        }
        // TODO the dynamic types
        return Optional.empty();
    }

    void run(GeyserSession session, Dialog.AfterAction after);

    record OpenUrl(String url) implements DialogAction {

        public static final Key TYPE = MinecraftKey.key("open_url");

        @Override
        public void run(GeyserSession session, Dialog.AfterAction after) {
            session.sendForm(SimpleForm.builder().title("Open URL").content(url));
        }
    }

    record RunCommand(String command) implements DialogAction {

        public static final Key TYPE = MinecraftKey.key("run_command");

        @Override
        public void run(GeyserSession session, Dialog.AfterAction after) {
            session.sendCommand(command);
        }
    }

    record ShowDialog(Holder<NbtMap> dialog) implements DialogAction {

        public static final Key TYPE = MinecraftKey.key("show_dialog");

        private static ShowDialog readDialog(Object dialog, Dialog.IdGetter idGetter) {
            if (dialog instanceof NbtMap map) {
                return new ShowDialog(Holder.ofCustom(map));
            } else if (dialog instanceof String string) {
                return new ShowDialog(Holder.ofId(idGetter.applyAsInt(MinecraftKey.key(string))));
            }
            throw new IllegalArgumentException("Expected dialog in show_dialog action to be a NBT map or a resource location");
        }

        @Override
        public void run(GeyserSession session, Dialog.AfterAction after) {
            // TODO figure out parent dialog
            Dialog.showDialog(session, dialog);
        }
    }

    // TODO tag can be any kind of NBT tag, not just a map
    record Custom(Key id, @Nullable NbtMap tag) implements DialogAction {

        public static final Key TYPE = MinecraftKey.key("custom");

        @Override
        public void run(GeyserSession session, Dialog.AfterAction after) {
            session.sendDownstreamPacket(new ServerboundCustomClickActionPacket(id, tag));
        }
    }
}
