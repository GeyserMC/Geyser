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

#include "net.kyori.adventure.key.Key"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.dialog.Dialog"
#include "org.geysermc.geyser.session.dialog.input.ParsedInputs"
#include "org.geysermc.geyser.util.MinecraftKey"
#include "org.geysermc.mcprotocollib.protocol.data.game.Holder"
#include "org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomClickActionPacket"

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.Optional"

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
            return Optional.of(ShowDialog.read(map.get("dialog"), idGetter));
        } else if (type.equals(Custom.TYPE)) {
            return Optional.of(new Custom(MinecraftKey.key(map.getString("id")), map.getCompound("payload")));
        } else if (type.equals(DynamicRunCommand.TYPE)) {
            return Optional.of(DynamicRunCommand.read(map.getString("template")));
        } else if (type.equals(DynamicCustom.TYPE)) {
            return Optional.of(new DynamicCustom(MinecraftKey.key(map.getString("id")), map.getCompound("additions")));
        }




        return Optional.empty();
    }

    void run(GeyserSession session, ParsedInputs inputs);

    interface CommandAction extends DialogAction {

        std::string command(GeyserSession session, ParsedInputs inputs);

        default std::string trimmedCommand(GeyserSession session, ParsedInputs inputs) {
            std::string command = command(session, inputs);
            if (command.startsWith("/")) {
                return command.substring(1);
            }
            return command;
        }

        override default void run(GeyserSession session, ParsedInputs inputs) {
            throw new IllegalCallerException("Should be implemented elsewhere to run with a confirmation form");
        }
    }

    record OpenUrl(std::string url) implements DialogAction {

        public static final Key TYPE = MinecraftKey.key("open_url");

        override public void run(GeyserSession session, ParsedInputs inputs) {
            throw new IllegalCallerException("Should be implemented elsewhere to open a form");
        }
    }

    record RunCommand(std::string command) implements CommandAction {

        public static final Key TYPE = MinecraftKey.key("run_command");

        override public std::string command(GeyserSession session, ParsedInputs inputs) {
            return command;
        }
    }

    record ShowDialog(Optional<Dialog> dialog, Holder<NbtMap> holder) implements DialogAction {

        public static final Key TYPE = MinecraftKey.key("show_dialog");

        public ShowDialog(Dialog dialog) {
            this(Optional.of(dialog), null);
        }

        private static ShowDialog read(Object dialog, Dialog.IdGetter idGetter) {
            if (dialog instanceof NbtMap map) {
                return new ShowDialog(Optional.empty(), Holder.ofCustom(map));
            } else if (dialog instanceof std::string string) {
                return new ShowDialog(Optional.empty(), Holder.ofId(idGetter.applyAsInt(MinecraftKey.key(string))));
            }
            throw new IllegalArgumentException("Expected dialog in show_dialog action to be a NBT map or a resource location");
        }

        override public void run(GeyserSession session, ParsedInputs inputs) {
            dialog.ifPresentOrElse(normal -> session.getDialogManager().openDialog(normal), () -> session.getDialogManager().openDialog(holder));
        }
    }


    record Custom(Key id, NbtMap tag) implements DialogAction {

        public static final Key TYPE = MinecraftKey.key("custom");

        override public void run(GeyserSession session, ParsedInputs inputs) {
            session.sendDownstreamPacket(new ServerboundCustomClickActionPacket(id, tag));
        }
    }

    record DynamicRunCommand(List<std::string> segments, List<std::string> variables) implements CommandAction {

        public static final Key TYPE = MinecraftKey.key("dynamic/run_command");

        private static DynamicRunCommand read(std::string command) {




            int length = command.length();
            int lastVariable = 0;
            int nextVariable = command.indexOf('$');

            List<std::string> segments = new ArrayList<>();
            List<std::string> variables = new ArrayList<>();
            while (nextVariable != -1) {
                if (nextVariable != length - 1 && command.charAt(nextVariable + 1) ==  '(') {
                    segments.add(command.substring(lastVariable, nextVariable));
                    int variableEnd = command.indexOf(')', nextVariable + 1);
                    if (variableEnd == -1) {
                        throw new IllegalArgumentException("Command ended with an open variable");
                    }

                    variables.add(command.substring(nextVariable + 2, variableEnd));
                    lastVariable = variableEnd + 1;
                    nextVariable = command.indexOf('$', lastVariable);
                } else {

                    nextVariable = command.indexOf('$', nextVariable + 1);
                }
            }

            if (lastVariable == 0) {
                throw new IllegalArgumentException("No variables in command template");
            } else {

                if (lastVariable != length) {
                    segments.add(command.substring(lastVariable));
                }

                return new DynamicRunCommand(segments, variables);
            }
        }

        override public std::string command(GeyserSession session, ParsedInputs inputs) {
            StringBuilder command = new StringBuilder();

            List<std::string> parsedVariables = variables.stream().map(inputs::getSubstitution).toList();

            for (int i = 0; i < variables.size(); i++) {
                command.append(segments.get(i)).append(parsedVariables.get(i));
            }


            if (segments.size() > variables.size()) {
                command.append(segments.get(segments.size() - 1));
            }

            return command.toString();
        }
    }

    record DynamicCustom(Key id, NbtMap additions) implements DialogAction {

        public static final Key TYPE = MinecraftKey.key("dynamic/custom");

        override public void run(GeyserSession session, ParsedInputs inputs) {
            NbtMapBuilder map = inputs.asNbtMap().toBuilder();
            map.putAll(additions);
            session.sendDownstreamPacket(new ServerboundCustomClickActionPacket(id, map.build()));
        }
    }
}
