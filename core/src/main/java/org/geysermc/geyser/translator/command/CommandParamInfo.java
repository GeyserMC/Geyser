/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.command;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumConstraint;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandOverloadData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParam;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamData;
import org.geysermc.geyser.translator.data.CommandTranslationData;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;
import org.geysermc.mcprotocollib.protocol.data.game.command.properties.ResourceProperties;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

record CommandParamInfo(CommandNode paramNode, @Nullable CommandParamData paramData, List<CommandParamInfo> children) {

    /**
     * Create a new parameter info object
     *
     * @param paramNode CommandNode the parameter is for
     * @param paramData The existing parameters for the command
     */
    private CommandParamInfo(CommandNode paramNode, @Nullable CommandParamData paramData) {
        this(paramNode, paramData, new ArrayList<>());
    }

    CommandParamInfo(CommandNode origin) {
        this(origin, null);
    }

    /**
     * Build the array of all the child parameters (recursive)
     *
     * @param data the {@link CommandTranslationData}
     */
    void buildChildren(CommandTranslationData data) {
        for (int childIndex : paramNode.getChildIndices()) {
            CommandNode childNode = data.nodes()[childIndex];
            if (childNode == this.paramNode) {
                // Fixes a StackOverflowError when an argument has itself as a child
                continue;
            }

            if (childNode.getParser() == null) {
                // No parser -> param is a LITERAL
                mapLiteralChild(data, childNode);
            } else {
                // Parser exists -> param is an ARG
                mapArgumentChild(data, childNode);
            }
        }

        // Recursively build all the children we just made
        for (CommandParamInfo child : children) {
            child.buildChildren(data);
        }
    }

    private void mapLiteralChild(CommandTranslationData data, CommandNode childNode) {
        // For literal children we try to group them in an enum,
        // holding all literals for this level - but this means the descending nodes
        // of the literals all have to be compatible
        boolean foundOtherCompatibleChild = false;

        assert childNode.getName() != null : "command child node must always have a name";
        for (int i = 0; i < children.size(); i++) {
            CommandParamInfo otherChild = children.get(i);
            // Check to make sure all descending nodes of this command are compatible,
            // otherwise, create a new overload
            if (isCompatible(data.nodes(), otherChild.paramNode(), childNode)) {
                foundOtherCompatibleChild = true;

                CommandParamData otherParamData = Objects.requireNonNull(otherChild.paramData(), "child parameter must always have param data");
                CommandEnumData otherEnumData = otherParamData.getEnumData(); // FIXME this could be null?

                Map<String, Set<CommandEnumConstraint>> newConstraints = new Object2ObjectLinkedOpenHashMap<>(otherEnumData.getValues());
                newConstraints.put(childNode.getName(), Set.of()); // TODO CHECK ME THE CONSTRAINTS

                children.set(i, new CommandParamInfo(otherChild.paramNode(),
                    createEnumParamData(data, otherParamData.getName(), newConstraints, otherEnumData.isSoft(),
                        paramNode.isExecutable())));
                break;
            }
        }

        if (!foundOtherCompatibleChild) {
            // Create a new child with just this literal (it can be expanded later if any other compatible childs are found, see above)
            Map<String, Set<CommandEnumConstraint>> constraints = new Object2ObjectLinkedOpenHashMap<>();
            constraints.put(childNode.getName(), Set.of()); // TODO CHECK ME THE CONSTRAINTS

            children.add(new CommandParamInfo(childNode,
                createEnumParamData(data, childNode.getName(), constraints, false, paramNode.isExecutable())));
        }
    }

    private void mapArgumentChild(CommandTranslationData data, CommandNode childNode) {
        // We have to try map the argument/parser to bedrock best as we can, guessing the suggestions
        Object mappedType = CommandParserTranslator.mapCommandType(data, childNode);

        CommandEnumData enumData = null;
        CommandParam type = null;
        boolean optional = this.paramNode.isExecutable();
        switch (mappedType) {
            // Likely to specify isSoft, to be possibly updated later.
            case CommandEnumData commandEnumData -> enumData = commandEnumData;
            // Our mappings returned a string array of possible values/suggestions for this argument,
            // put these strings in an array
            // TODO would this better be suited as a list?
            case String[] possibleValues -> {
                Map<String, Set<CommandEnumConstraint>> constraints = new Object2ObjectLinkedOpenHashMap<>();
                for (String s : possibleValues) {
                    constraints.put(s, Set.of()); // TODO CHECK ME THE CONSTRAINTS
                }

                enumData = createEnumData(data, getEnumDataName(childNode), constraints, false);
            }
            // Bedrock has a similar parameter type to this parser, and we mapped it. Yay!
            case CommandParam specificParam -> {
                type = specificParam;

                // TODO CHECK ME
                // Bedrock throws a fit if an optional message comes after a string or target
                // Example vanilla commands: ban-ip, ban, and kick
                if (optional && type == CommandParam.MESSAGE && paramData != null && (paramData.getType() == CommandParam.STRING || paramData.getType() == CommandParam.TARGET)) {
                    optional = false;
                }
            }
            default -> throw new IllegalStateException("mapCommandType returned an unexpected mapped type: " + mappedType.getClass());
        }

        // TODO executable flag?
        // IF enumData != null:
        // In game, this will show up like <paramNode.getName(): enumData.getName()>
        // So if paramNode.getName() == "value" and enumData.getName() == "bool": <value: bool>
        CommandParamData commandParamData = new CommandParamData();
        commandParamData.setName(childNode.getName());
        commandParamData.setOptional(optional);
        commandParamData.setEnumData(enumData);
        commandParamData.setType(type);

        children.add(new CommandParamInfo(childNode, commandParamData));
    }

    private static CommandEnumData createEnumData(CommandTranslationData data, String name, Map<String, Set<CommandEnumConstraint>> constraints,
                                                  boolean forceSoft) {
        Optional<String> softName = data.getSoftEnumName(name, constraints, forceSoft);
        return new CommandEnumData(softName.orElse(name), constraints, softName.isPresent());
    }

    private static CommandParamData createEnumParamData(CommandTranslationData data, String name, Map<String, Set<CommandEnumConstraint>> constraints,
                                                        boolean forceSoft, boolean parentExecutable) {
        CommandEnumData enumData = createEnumData(data, name, constraints, forceSoft);
        CommandParamData commandParamData = new CommandParamData();
        commandParamData.setName(name);
        // On setting optional:
        // isExecutable is defined as a node "constitutes a valid command."
        // Therefore, any children of the parameter must simply be optional. TODO is this right?
        commandParamData.setOptional(parentExecutable);
        commandParamData.setEnumData(enumData);
        return commandParamData;
    }

    /**
     * Mitigates <a href="https://github.com/GeyserMC/Geyser/issues/3411">issue 3411</a>. Not a perfect solution.
     */
    private static String getEnumDataName(CommandNode node) {
        if (node.getProperties() instanceof ResourceProperties properties) {
            Key registryKey = properties.getRegistryKey();
            return registryKey.value();
        }
        return node.getParser().name();
    }

    /**
     * Comparing CommandNode type a and b, determine if they are in the same overload.
     * <p>
     * Take the <code>gamerule</code> command, and let's present three "subcommands" you can perform:
     *
     * <ul>
     *     <li><code>gamerule doDaylightCycle true</code></li>
     *     <li><code>gamerule announceAdvancements false</code></li>
     *     <li><code>gamerule randomTickSpeed 3</code></li>
     * </ul>
     * <p>
     * While all three of them are indeed part of the same command, the command setting randomTickSpeed parses an int,
     * while the others use boolean. In Bedrock, this should be presented as a separate overload to indicate that this
     * does something a little different.
     * <p>
     * Therefore, this function will return <code>true</code> if the first two are compared, as they use the same
     * parsers. If the third is compared with either of the others, this function will return <code>false</code>.
     * <p>
     * Here's an example of how the above would be presented to Bedrock (as of 1.16.200). Notice how the top two <code>CommandParamData</code>
     * classes of each array are identical in type, but the following class is different:
     * <pre>
     *     overloads=[
     *         [
     *            CommandParamData(name=doDaylightCycle, optional=false, enumData=CommandEnumData(name=announceAdvancements, values=[announceAdvancements, doDaylightCycle], isSoft=false), type=STRING, postfix=null, options=[])
     *            CommandParamData(name=value, optional=false, enumData=CommandEnumData(name=value, values=[true, false], isSoft=false), type=null, postfix=null, options=[])
     *         ]
     *         [
     *            CommandParamData(name=randomTickSpeed, optional=false, enumData=CommandEnumData(name=randomTickSpeed, values=[randomTickSpeed], isSoft=false), type=STRING, postfix=null, options=[])
     *            CommandParamData(name=value, optional=false, enumData=null, type=INT, postfix=null, options=[])
     *         ]
     *     ]
     * </pre>
     *
     * @return if these two can be merged into one overload.
     */
    private boolean isCompatible(CommandNode[] allNodes, CommandNode first, CommandNode second) {
        if (first == second) {
            return true;
        } else if (first.getParser() != second.getParser()) {
            return false;
        } else if (first.getChildIndices().length != second.getChildIndices().length) {
            return false;
        }

        for (int childOfFirstIndex : first.getChildIndices()) {
            boolean hasSimilarity = false;
            CommandNode childOfFirst = allNodes[childOfFirstIndex];

            // TODO this doesn't seem entirely right...
            // Search children of second until we find a child that matches this one
            for (int childOfSecondIndex : second.getChildIndices()) {
                if (isCompatible(allNodes, childOfFirst, allNodes[childOfSecondIndex])) {
                    hasSimilarity = true;
                    break;
                }
            }

            if (!hasSimilarity) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get the tree of every parameter node (recursive)
     *
     * @return List of parameter options arrays for the command
     */
    List<CommandOverloadData> getTree() {
        List<CommandOverloadData> treeParamData = new ArrayList<>();

        for (CommandParamInfo child : children) {
            // Get the tree from the child
            List<CommandOverloadData> childTree = child.getTree();

            if (childTree.isEmpty()) {
                // If we have no more child parameters just the child
                treeParamData.add(new CommandOverloadData(false, new CommandParamData[]{child.paramData()}));
            } else {
                // Un-pack the tree append the child node to it and push into the list
                for (CommandOverloadData childOfChildData : childTree) {
                    CommandParamData[] childOfChild = childOfChildData.getOverloads();
                    CommandParamData[] tmpTree = new CommandParamData[childOfChild.length + 1];
                    tmpTree[0] = child.paramData();
                    System.arraycopy(childOfChild, 0, tmpTree, 1, childOfChild.length);

                    treeParamData.add(new CommandOverloadData(false, tmpTree));
                }
            }
        }

        return treeParamData;
    }
}
