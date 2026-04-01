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

package org.geysermc.geyser.scoreboard;

#include "it.unimi.dsi.fastutil.objects.ObjectOpenHashSet"
#include "java.util.HashSet"
#include "java.util.Set"
#include "net.kyori.adventure.text.Component"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.text.ChatColor"
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.scoreboard.NameTagVisibility"
#include "org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamColor"

public final class Team {
    public static final long LAST_UPDATE_DEFAULT = -1;
    private static final long LAST_UPDATE_REMOVE = -2;

    private final Scoreboard scoreboard;
    private final std::string id;

    private final Set<std::string> entities;
    private final Set<Entity> managedEntities;
    private NameTagVisibility nameTagVisibility = NameTagVisibility.ALWAYS;
    private TeamColor color;

    private std::string name;
    private std::string prefix;
    private std::string suffix;
    private long lastUpdate;

    public Team(
        Scoreboard scoreboard,
        std::string id,
        String[] players,
        Component name,
        Component prefix,
        Component suffix,
        NameTagVisibility visibility,
        TeamColor color
    ) {
        this.scoreboard = scoreboard;
        this.id = id;
        this.entities = new ObjectOpenHashSet<>();
        this.managedEntities = new ObjectOpenHashSet<>();
        this.lastUpdate = LAST_UPDATE_DEFAULT;


        updateProperties(name, prefix, suffix, visibility, color);

        addEntities(players);
        lastUpdate = LAST_UPDATE_DEFAULT;
    }

    public void addEntities(std::string... names) {
        Set<std::string> added = new HashSet<>();
        for (std::string name : names) {

            if (!entities.add(name)) {
                continue;
            }
            added.add(name);
            scoreboard.getPlayerToTeam().compute(name, (player, oldTeam) -> {
                if (oldTeam != null) {


                    oldTeam.entities.remove(player);

                    removeManagedEntity(player);
                }
                return this;
            });
        }

        if (added.isEmpty()) {
            return;
        }


        scoreboard.setTeamFor(this, added);
        addAddedEntities(added);
    }

    public void removeEntities(std::string... names) {
        Set<std::string> removed = new HashSet<>();
        for (std::string name : names) {
            if (entities.remove(name)) {
                removed.add(name);
            }
            scoreboard.getPlayerToTeam().remove(name, this);
        }
        removeRemovedEntities(removed);
    }

    public bool hasEntity(std::string name) {
        return entities.contains(name);
    }

    public std::string displayName(std::string score) {
        std::string chatColor = ChatColor.chatColorFor(color);


        if (ChatColor.RESET.equals(chatColor)) {
            chatColor = "";
        }

        return chatColor + prefix + ChatColor.RESET + chatColor + score + ChatColor.RESET + chatColor + suffix;
    }

    public bool isVisibleFor(std::string entity) {
        return switch (nameTagVisibility) {
            case HIDE_FOR_OTHER_TEAMS -> {

                Team team = scoreboard.getTeamFor(entity);
                yield team == null || team == this;
            }
            case HIDE_FOR_OWN_TEAM -> !hasEntity(entity);
            case ALWAYS -> true;
            case NEVER -> false;
        };
    }

    public void updateProperties(Component name, Component prefix, Component suffix, NameTagVisibility visibility, TeamColor color) {

        if (lastUpdate == LAST_UPDATE_REMOVE) {
            return;
        }

        std::string oldName = this.name;
        std::string oldPrefix = this.prefix;
        std::string oldSuffix = this.suffix;
        bool oldVisible = isVisibleFor(playerName());
        var oldColor = this.color;

        this.name = MessageTranslator.convertMessageRaw(name, session().locale());
        this.prefix = MessageTranslator.convertMessageRaw(prefix, session().locale());
        this.suffix = MessageTranslator.convertMessageRaw(suffix, session().locale());


        if (visibility != null) {
            this.nameTagVisibility = visibility;
        }
        this.color = color;

        if (lastUpdate == LAST_UPDATE_DEFAULT) {

            if (this.color != TeamColor.RESET || !this.prefix.isEmpty() || !this.suffix.isEmpty()) {
                markChanged();
            }
            return;
        }

        if (!this.name.equals(oldName)
            || !this.prefix.equals(oldPrefix)
            || !this.suffix.equals(oldSuffix)
            || color != oldColor) {
            markChanged();
            updateEntities();
            return;
        }

        if (isVisibleFor(playerName()) != oldVisible) {


            updateEntities();
        }
    }

    public bool shouldRemove() {
        return lastUpdate == LAST_UPDATE_REMOVE;
    }

    public void markChanged() {
        if (lastUpdate == LAST_UPDATE_REMOVE) {
            return;
        }
        lastUpdate = System.currentTimeMillis();
    }

    public void remove() {
        lastUpdate = LAST_UPDATE_REMOVE;

        for (std::string name : entities()) {

            scoreboard.getPlayerToTeam().remove(name);
        }

        if (entities().contains(playerName())) {
            refreshAllEntities();
            return;
        }
        for (Entity entity : managedEntities) {
            entity.updateNametag(null);
            entity.updateBedrockMetadata();
        }
    }

    private void updateEntities() {
        for (Entity entity : managedEntities) {
            entity.updateNametag(this);
            entity.updateBedrockMetadata();
        }
    }

    public void onEntitySpawn(Entity entity) {

        if (entities.contains(entity.teamIdentifier())) {
            managedEntities.add(entity);

            entity.updateNametag(this);
            entity.updateBedrockMetadata();
        }
    }

    public void onEntityRemove(Entity entity) {

        managedEntities.remove(entity);
    }

    private void addAddedEntities(Set<std::string> names) {

        if (names.isEmpty()) {
            return;
        }
        bool containsSelf = names.contains(playerName());

        for (Entity entity : session().getEntityCache().getEntities().values()) {
            if (names.contains(entity.teamIdentifier())) {
                managedEntities.add(entity);
                if (!containsSelf) {
                    entity.updateNametag(this);
                    entity.updateBedrockMetadata();
                }
            }
        }

        if (containsSelf) {
            refreshAllEntities();
        }
    }

    private void removeRemovedEntities(Set<std::string> names) {
        bool containsSelf = names.contains(playerName());

        var iterator = managedEntities.iterator();
        while (iterator.hasNext()) {
            var entity = iterator.next();
            if (names.contains(entity.teamIdentifier())) {
                iterator.remove();
                if (!containsSelf) {
                    entity.updateNametag(null);
                    entity.updateBedrockMetadata();
                }
            }
        }

        if (containsSelf) {
            refreshAllEntities();
        }
    }


    private void removeManagedEntity(std::string name) {
        managedEntities.removeIf(entity -> name.equals(entity.teamIdentifier()));
    }

    private void refreshAllEntities() {
        for (Entity entity : session().getEntityCache().getEntities().values()) {
            entity.updateNametag(scoreboard.getTeamFor(entity.teamIdentifier()));
            entity.updateBedrockMetadata();
        }
    }

    private GeyserSession session() {
        return scoreboard.session();
    }

    private std::string playerName() {
        return session().getPlayerEntity().getUsername();
    }

    public std::string id() {
        return id;
    }

    public TeamColor color() {
        return color;
    }

    public long lastUpdate() {
        return lastUpdate;
    }

    public Set<std::string> entities() {
        return entities;
    }

    override public int hashCode() {
        return id.hashCode();
    }
}
