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

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.PlayerListUtils;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.NameTagVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamColor;

public final class Team {
    public static final long LAST_UPDATE_DEFAULT = -1;
    private static final long LAST_UPDATE_REMOVE = -2;

    private final Scoreboard scoreboard;
    private final String id;

    private final Set<String> entities;
    private final Set<Entity> managedEntities;
    @NonNull private NameTagVisibility nameTagVisibility = NameTagVisibility.ALWAYS;
    private TeamColor color;

    private String name;
    private String prefix;
    private String suffix;
    private long lastUpdate;

    public Team(
        Scoreboard scoreboard,
        String id,
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

        // doesn't call entity update
        updateProperties(name, prefix, suffix, visibility, color);
        // calls entity update
        addEntities(players);
        lastUpdate = LAST_UPDATE_DEFAULT;
    }

    public void addEntities(String... names) {
        Set<String> added = new HashSet<>();
        for (String name : names) {
            // go to next score if score is already present
            if (!entities.add(name)) {
                continue;
            }
            added.add(name);
            scoreboard.getPlayerToTeam().compute(name, (player, oldTeam) -> {
                if (oldTeam != null) {
                    // Remove old team from this map, and from the set of players of the old team.
                    // Java 1.19.3 Mojmap: Scoreboard#addPlayerToTeam calls #removePlayerFromTeam
                    oldTeam.entities.remove(player);
                    // also remove the managed entity if there is one
                    removeManagedEntity(player);
                }
                return this;
            });
        }

        if (added.isEmpty()) {
            return;
        }
        // we don't have to change our updateType,
        // because the scores themselves need updating, not the team
        scoreboard.setTeamFor(this, added);
        addAddedEntities(added);
    }

    public void removeEntities(String... names) {
        Set<String> removed = new HashSet<>();
        for (String name : names) {
            if (entities.remove(name)) {
                removed.add(name);
            }
            scoreboard.getPlayerToTeam().remove(name, this);
        }
        removeRemovedEntities(removed);
    }

    public boolean hasEntity(String name) {
        return entities.contains(name);
    }

    public String displayName(String score) {
        String chatColor = ChatColor.chatColorFor(color);
        // most sidebar plugins will use the reset color, because they don't want color
        // skip the unneeded double reset color in that case
        if (ChatColor.RESET.equals(chatColor)) {
            chatColor = "";
        }
        // also add reset because setting the color does not reset the formatting, unlike Java
        return chatColor + prefix + ChatColor.RESET + chatColor + score + ChatColor.RESET + chatColor + suffix;
    }

    public String formatTabDisplay(String username, boolean spectator) {
        String chatColor = ChatColor.chatColorFor(color);
        if (ChatColor.RESET.equals(chatColor)) {
            chatColor = "";
        }

        if (spectator && !ChatColor.ITALIC.equals(chatColor)) {
            chatColor += ChatColor.ITALIC;
        }
        // also add reset because setting the color does not reset the formatting, unlike Java
        return chatColor + prefix + ChatColor.RESET + chatColor + username + ChatColor.RESET + chatColor + suffix;
    }

    public boolean isVisibleFor(String entity) {
        return switch (nameTagVisibility) {
            case HIDE_FOR_OTHER_TEAMS -> {
                // Player must be in a team in order for HIDE_FOR_OTHER_TEAMS to be triggered
                Team team = scoreboard.getTeamFor(entity);
                yield team == null || team == this;
            }
            case HIDE_FOR_OWN_TEAM -> !hasEntity(entity);
            case ALWAYS -> true;
            case NEVER -> false;
        };
    }

    public void updateProperties(Component name, Component prefix, Component suffix, NameTagVisibility visibility, TeamColor color) {
        // this shouldn't happen but hey!
        if (lastUpdate == LAST_UPDATE_REMOVE) {
            return;
        }

        String oldName = this.name;
        String oldPrefix = this.prefix;
        String oldSuffix = this.suffix;
        boolean oldVisible = isVisibleFor(playerName());
        var oldColor = this.color;

        this.name = MessageTranslator.convertMessageRaw(name, session().locale());
        this.prefix = MessageTranslator.convertMessageRaw(prefix, session().locale());
        this.suffix = MessageTranslator.convertMessageRaw(suffix, session().locale());
        // matches vanilla behaviour, the visibility is not reset (to ALWAYS) if it is null.
        // instead the visibility is not altered
        if (visibility != null) {
            this.nameTagVisibility = visibility;
        }
        this.color = color;

        if (lastUpdate == LAST_UPDATE_DEFAULT) {
            // addEntities is called after the initial updateProperties, so no need to do any entity updates here
            if (this.color != TeamColor.RESET || !this.prefix.isEmpty() || !this.suffix.isEmpty()) {
                markChanged();
            }
            return;
        }

        // Avoid updating player list entries if just the name changed
        boolean stylingChange = !this.prefix.equals(oldPrefix)
            || !this.suffix.equals(oldSuffix)
            || color != oldColor;
        if (!this.name.equals(oldName) || stylingChange) {
            markChanged();
            updateEntities(managedEntities, this, stylingChange);
            return;
        }

        if (isVisibleFor(playerName()) != oldVisible) {
            // if just the visibility changed, we only have to update the entities.
            // We don't have to mark it as changed
            updateEntities(managedEntities, this, false);
        }
    }

    public boolean shouldRemove() {
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

        for (String name : entities()) {
            // 1.19.3 Mojmap Scoreboard#removePlayerTeam(PlayerTeam)
            scoreboard.getPlayerToTeam().remove(name);
        }

        updateEntities(managedEntities, null, true);
    }

    /**
     * Iterates through the provided entity collection to perform nametag, metadata and tablist updates.
     */
    public void updateEntities(Collection<Entity> entities, Team team, boolean updatePlayerList) {
        if (entities().contains(playerName())) {
            refreshAllEntities();
            return;
        }

        iterateAndUpdate(entities.iterator(), updatePlayerList, ($, $$) -> true, $ -> team);
    }

    /**
     * Iterates through the provided entity iterator, and, if the predicate matches, updates the nametag based on the
     * team function. It further updates the player list entries.
     */
    public void iterateAndUpdate(Iterator<Entity> iterator, boolean updatePlayerList, BiPredicate<Entity, Iterator<Entity>> predicate, Function<Entity, Team> function) {
        Set<PlayerEntity> entries = updatePlayerList ? new HashSet<>() : null;
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (predicate.test(entity, iterator)) {
                entity.updateNametag(function.apply(entity));
                entity.updateBedrockMetadata();
                if (entries != null && entity instanceof PlayerEntity player) {
                    entries.add(player);
                }
            }
        }

        if (entries != null && !entries.isEmpty()) {
            PlayerListUtils.updateEntries(session(), entries);
        }
    }

    public void onEntitySpawn(Entity entity) {
        // I've basically ported addAddedEntities
        if (entities.contains(entity.teamIdentifier())) {
            managedEntities.add(entity);
            // onEntitySpawn includes all entities but players, so it cannot contain self
            entity.updateNametag(this);
            entity.updateBedrockMetadata();
        }
    }

    public void onEntityRemove(Entity entity) {
        // we don't have to update anything, since the player is removed.
        managedEntities.remove(entity);
    }

    private void addAddedEntities(Set<String> names) {
        // can't contain self if none are added
        if (names.isEmpty()) {
            return;
        }

        boolean containsSelf = names.contains(playerName());
        iterateAndUpdate(session().getEntityCache().getEntities().values().iterator(),
            requiresPlayerListUpdate(),
            (entity, $) -> {
            if (names.contains(entity.teamIdentifier())) {
                managedEntities.add(entity);
                return !containsSelf;
            }
            return false;
            },
            $ -> this);

        if (containsSelf) {
            refreshAllEntities();
        }
    }

    private void removeRemovedEntities(Set<String> names) {
        boolean containsSelf = names.contains(playerName());
        iterateAndUpdate(managedEntities.iterator(), requiresPlayerListUpdate(), (entity, iterator) -> {
            if (names.contains(entity.teamIdentifier())) {
                iterator.remove();
                return !containsSelf;
            }
            return false;
        }, $ -> null);

        if (containsSelf) {
            refreshAllEntities();
        }
    }

    /**
     * Used internally to remove a managed entity without causing an update.
     * This is fine because its only used when the entity is added to another team,
     * which will fire the correct nametag updates etc.
     */
    private void removeManagedEntity(String name) {
        managedEntities.removeIf(entity -> name.equals(entity.teamIdentifier()));
    }

    private void refreshAllEntities() {
        iterateAndUpdate(
            session().getEntityCache().getEntities().values().iterator(),
            requiresPlayerListUpdate(),
            ($, $$) -> true,
            entity -> scoreboard.getTeamFor(entity.teamIdentifier()));
    }

    private GeyserSession session() {
        return scoreboard.session();
    }

    private String playerName() {
        return session().getPlayerEntity().getUsername();
    }

    public String id() {
        return id;
    }

    public TeamColor color() {
        return color;
    }

    public long lastUpdate() {
        return lastUpdate;
    }

    public Set<String> entities() {
        return entities;
    }

    private boolean requiresPlayerListUpdate() {
        return !prefix.isEmpty() || !suffix.isEmpty() || color() != TeamColor.RESET;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
