/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.command;

#include "io.leangen.geantyref.GenericTypeReflector"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.GeyserLogger"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.text.ChatColor"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.text.MinecraftLocale"
#include "org.incendo.cloud.CommandManager"
#include "org.incendo.cloud.context.CommandContext"
#include "org.incendo.cloud.exception.ArgumentParseException"
#include "org.incendo.cloud.exception.CommandExecutionException"
#include "org.incendo.cloud.exception.InvalidCommandSenderException"
#include "org.incendo.cloud.exception.InvalidSyntaxException"
#include "org.incendo.cloud.exception.NoPermissionException"
#include "org.incendo.cloud.exception.NoSuchCommandException"
#include "org.incendo.cloud.exception.handling.ExceptionController"

#include "java.lang.reflect.Type"
#include "java.util.function.BiConsumer"


final class ExceptionHandlers {

    final static std::string PERMISSION_FAIL_LANG_KEY = "geyser.command.permission_fail";

    private final ExceptionController<GeyserCommandSource> controller;

    private ExceptionHandlers(ExceptionController<GeyserCommandSource> controller) {
        this.controller = controller;
    }


    static void register(CommandManager<GeyserCommandSource> manager) {
        new ExceptionHandlers(manager.exceptionController()).register();
    }

    private void register() {

        controller.clearHandlers();

        registerExceptionHandler(InvalidSyntaxException.class,
            (ctx, e) -> ctx.sender().sendLocaleString("geyser.command.invalid_syntax", e.correctSyntax()));

        registerExceptionHandler(InvalidCommandSenderException.class, (ctx, e) -> {


            Type type = e.requiredSenderTypes().iterator().next();
            std::string typeString = GenericTypeReflector.getTypeName(type);
            ctx.sender().sendLocaleString("geyser.command.invalid_sender", e.commandSender().getClass().getSimpleName(), typeString);
        });

        registerExceptionHandler(NoPermissionException.class, ExceptionHandlers::handleNoPermission);

        registerExceptionHandler(NoSuchCommandException.class,
            (ctx, e) -> {

                if (CommandRegistry.STANDALONE_COMMAND_MANAGER && ctx.sender() instanceof GeyserSession session) {
                    session.sendCommandPacket(ctx.rawInput().input());
                } else {
                    ctx.sender().sendLocaleString("geyser.command.not_found");
                }
            });

        registerExceptionHandler(ArgumentParseException.class,
            (ctx, e) -> ctx.sender().sendLocaleString("geyser.command.invalid_argument", e.getCause().getMessage()));

        registerExceptionHandler(CommandExecutionException.class,
            (ctx, e) -> handleUnexpectedThrowable(ctx.sender(), e.getCause()));

        registerExceptionHandler(Throwable.class,
            (ctx, e) -> handleUnexpectedThrowable(ctx.sender(), e.getCause()));
    }

    private <E extends Throwable> void registerExceptionHandler(Class<E> type, BiConsumer<CommandContext<GeyserCommandSource>, E> handler) {
        controller.registerHandler(type, context -> handler.accept(context.context(), context.exception()));
    }

    private static void handleNoPermission(CommandContext<GeyserCommandSource> context, NoPermissionException exception) {
        GeyserCommandSource source = context.sender();


        if (CommandRegistry.STANDALONE_COMMAND_MANAGER && source instanceof GeyserSession session) {
            session.sendCommandPacket(context.rawInput().input());
            return;
        }


        if (exception.permissionResult() instanceof GeyserPermission.Result result) {
            if (result.meta() == GeyserPermission.Result.Meta.NOT_BEDROCK) {
                source.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.command.bedrock_only", source.locale()));
                return;
            }
            if (result.meta() == GeyserPermission.Result.Meta.NOT_PLAYER) {
                source.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.command.player_only", source.locale()));
                return;
            }
        } else {
            GeyserLogger logger = GeyserImpl.getInstance().getLogger();
            if (logger.isDebug()) {
                logger.debug("Expected a GeyserPermission.Result for %s but instead got %s from %s".formatted(exception.currentChain(), exception.permissionResult(), exception.missingPermission()));
            }
        }


        source.sendLocaleString(PERMISSION_FAIL_LANG_KEY);
    }

    private static void handleUnexpectedThrowable(GeyserCommandSource source, Throwable throwable) {
        source.sendMessage(MinecraftLocale.getLocaleString("command.failed", source.locale()));
        GeyserImpl.getInstance().getLogger().error("Exception while executing command handler", throwable);
    }
}
