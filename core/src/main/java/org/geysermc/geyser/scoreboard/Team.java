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
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.NameTagVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamColor;

public final class Team {
    public static final long LAST_UPDATE_DEFAULT = -1;
    private static final long LAST_UPDATE_REMOVE = -2;

    private final Scoreboard scoreboard;
    private final String id;

    private final Set<String> entities;
    private final Set<LivingEntity> managedEntities;
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

        addEntitiesNoUpdate(players);
        // this calls the update
        updateProperties(name, prefix, suffix, visibility, color);
        lastUpdate = LAST_UPDATE_DEFAULT;
    }

    public void addEntities(String... names) {
        addAddedEntities(addEntitiesNoUpdate(names));
    }

    private Set<String> addEntitiesNoUpdate(String... names) {
        Set<String> added = new HashSet<>();
        for (String name : names) {
            if (entities.add(name)) {
                added.add(name);
            }
            scoreboard.getPlayerToTeam().compute(name, (player, oldTeam) -> {
                if (oldTeam != null) {
                    // Remove old team from this map, and from the set of players of the old team.
                    // Java 1.19.3 Mojmap: Scoreboard#addPlayerToTeam calls #removePlayerFromTeam
                    oldTeam.entities.remove(player);
                }
                return this;
            });
        }

        if (added.isEmpty()) {
            return added;
        }
        // we don't have to change our updateType,
        // because the scores itself need updating, not the team
        scoreboard.setTeamFor(this, added);
        return added;
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
        return prefix + score + suffix;
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

        var oldName = this.name;
        var oldPrefix = this.prefix;
        var oldSuffix = this.suffix;
        var oldVisible = isVisibleFor(playerName());
        var oldColor = this.color;

        this.name = MessageTranslator.convertMessage(name, session().locale());
        this.prefix = MessageTranslator.convertMessage(prefix, session().locale());
        this.suffix = MessageTranslator.convertMessage(suffix, session().locale());
        // matches vanilla behaviour, the visibility is not reset (to ALWAYS) if it is null.
        // instead the visibility is not altered
        if (visibility != null) {
            this.nameTagVisibility = visibility;
        }
        this.color = color;

        if (lastUpdate == LAST_UPDATE_DEFAULT) {
            if (entities.isEmpty()) {
                return;
            }

            var hidden = false;
            if (nameTagVisibility != NameTagVisibility.ALWAYS && !isVisibleFor(playerName())) {
                // while the team has technically changed, we don't mark it as changed because the visibility
                // doesn't influence any of the display slots
                hideEntities();
                hidden = true;
            }

            if (this.color != TeamColor.RESET || !this.prefix.isEmpty() || !this.suffix.isEmpty()) {
                markChanged();
                // we've already hidden the entities, so we don't have to update them again
                if (!hidden) {
                    updateEntities();
                }
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
            // if just the visibility changed, we only have to update the entities.
            // We don't have to mark it as changed
            updateEntities();
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

        if (entities().contains(playerName())) {
            refreshAllEntities();
            return;
        }
        for (LivingEntity entity : managedEntities) {
            entity.updateNametag(null);
            entity.updateBedrockMetadata();
        }
    }

    private void hideEntities() {
        for (LivingEntity entity : managedEntities) {
            entity.hideNametag();
        }
    }

    private void updateEntities() {
        for (LivingEntity entity : managedEntities) {
            entity.updateNametag(this);
        }
    }

    private void addAddedEntities(Set<String> names) {
        var containsSelf = names.contains(playerName());

        for (Entity entity : session().getEntityCache().getEntities().values()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (names.contains(living.teamIdentifier())) {
                managedEntities.add(living);
                if (!containsSelf) {
                    living.updateNametag(this);
                    living.updateBedrockMetadata();
                }
            }
        }

        if (containsSelf) {
            refreshAllEntities();
        }
    }

    private void removeRemovedEntities(Set<String> names) {
        var containsSelf = names.contains(playerName());

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

    private void refreshAllEntities() {
        for (Entity entity : session().getEntityCache().getEntities().values()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            living.updateNametag(scoreboard.getTeamFor(living.teamIdentifier()));
            living.updateBedrockMetadata();
        }
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

    public String prefix() {
        return prefix;
    }

    public String suffix() {
        return suffix;
    }

    public long lastUpdate() {
        return lastUpdate;
    }

    public Set<String> entities() {
        return entities;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
