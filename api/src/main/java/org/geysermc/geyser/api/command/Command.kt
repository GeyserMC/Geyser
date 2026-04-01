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
package org.geysermc.geyser.api.command

import org.geysermc.geyser.api.GeyserApi
import org.geysermc.geyser.api.connection.GeyserConnection
import org.geysermc.geyser.api.extension.Extension
import org.geysermc.geyser.api.util.TriState

/**
 * Represents a command.
 */
interface Command {
    /**
     * Gets the command name.
     * 
     * @return the command name
     */
    fun name(): String

    /**
     * Gets the command description.
     * 
     * @return the command description
     */
    fun description(): String

    /**
     * Gets the permission node associated with
     * this command.
     * 
     * @return the permission node for this command if defined, otherwise an empty string
     */
    fun permission(): String

    /**
     * Gets the aliases for this command, as an unmodifiable list
     * 
     * @return the aliases for this command as an unmodifiable list
     */
    fun aliases(): MutableList<String?>

    @get:Deprecated("this method is not guaranteed to provide meaningful or expected results.")
    val isSuggestedOpOnly: Boolean
        /**
         * Gets if this command is designed to be used only by server operators.
         * 
         * @return if this command is designated to be used only by server operators.
         */
        get() = false

    @get:Deprecated("use {@link #isPlayerOnly()} instead (inverted)")
    val isExecutableOnConsole: Boolean
        /**
         * @return true if this command is executable on console
         */
        get() = !this.isPlayerOnly

    /**
     * @return true if this command can only be used by players
     */
    val isPlayerOnly: Boolean

    /**
     * @return true if this command can only be used by Bedrock players
     */
    val isBedrockOnly: Boolean

    @Deprecated("this method will always return an empty immutable list")
    fun subCommands(): MutableList<String?> {
        return mutableListOf<String?>()
    }

    interface Builder<T : CommandSource?> {
        /**
         * Defines the source type to use for this command.
         * 
         * 
         * Command source types can be anything that extend
         * [CommandSource], such as [GeyserConnection].
         * This will guarantee that the source used in the executor
         * is an instance of this source.
         * 
         * @param sourceType the source type
         * @return this builder
         */
        fun source(sourceType: Class<out T?>): Builder<T?>?

        /**
         * Sets the command name.
         * 
         * @param name the command name
         * @return this builder
         */
        fun name(name: String): Builder<T?>?

        /**
         * Sets the command description.
         * 
         * @param description the command description
         * @return this builder
         */
        fun description(description: String): Builder<T?>?

        /**
         * Sets the permission node required to run this command. <br></br>
         * It will not be registered with any permission registries, such as an underlying server,
         * or a permissions Extension (unlike [.permission]).
         * 
         * @param permission the permission node
         * @return this builder
         */
        fun permission(permission: String): Builder<T?>?

        /**
         * Sets the permission node and its default value. The usage of the default value is platform dependant
         * and may or may not be used. For example, it may be registered to an underlying server.
         * 
         * 
         * Extensions may instead listen for [GeyserRegisterPermissionsEvent] to register permissions,
         * especially if the same permission is required by multiple commands. Also see this event for TriState meanings.
         * 
         * @param permission the permission node
         * @param defaultValue the node's default value
         * @return this builder
         */
        @Deprecated("this method is experimental and may be removed in the future")
        fun permission(permission: String, defaultValue: TriState): Builder<T?>?

        /**
         * Sets the aliases.
         * 
         * @param aliases the aliases
         * @return this builder
         */
        fun aliases(aliases: MutableList<String?>): Builder<T?>?

        /**
         * Sets if this command is designed to be used only by server operators.
         * 
         * @param suggestedOpOnly if this command is designed to be used only by server operators
         * @return this builder
         */
        @Deprecated("this method is not guaranteed to produce meaningful or expected results")
        fun suggestedOpOnly(suggestedOpOnly: Boolean): Builder<T?>?

        /**
         * Sets if this command is executable on console.
         * 
         * @param executableOnConsole if this command is executable on console
         * @return this builder
         */
        @Deprecated("use {@link #isPlayerOnly()} instead (inverted)")
        fun executableOnConsole(executableOnConsole: Boolean): Builder<T?>?

        /**
         * Sets if this command can only be executed by players.
         * 
         * @param playerOnly if this command is player only
         * @return this builder
         */
        fun playerOnly(playerOnly: Boolean): Builder<T?>?

        /**
         * Sets if this command can only be executed by bedrock players.
         * 
         * @param bedrockOnly if this command is bedrock only
         * @return this builder
         */
        fun bedrockOnly(bedrockOnly: Boolean): Builder<T?>?

        /**
         * Sets the subcommands.
         * 
         * @param subCommands the subcommands
         * @return this builder
         */
        @Deprecated("this method has no effect")
        fun subCommands(subCommands: MutableList<String?>): Builder<T?> {
            return this
        }

        /**
         * Sets the [CommandExecutor] for this command.
         * 
         * @param executor the command executor
         * @return this builder
         */
        fun executor(executor: CommandExecutor<T?>): Builder<T?>?

        /**
         * Builds the command.
         * 
         * @return a new command from this builder
         */
        fun build(): Command
    }

    companion object {
        /**
         * Creates a new [Command.Builder] used to construct commands.
         * 
         * @param extension the extension
         * @param <T> the source type
         * @return a new command builder used to construct commands
        </T> */
        fun <T : CommandSource?> builder(extension: Extension): Builder<T?> {
            return GeyserApi.Companion.api().provider<Builder<T?>, Builder<*>?>(Builder::class.java, extension)
        }
    }
}
