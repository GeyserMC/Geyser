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

package org.geysermc.geyser.entity.type.player;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.SkinManager;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.ResolvableProfile;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MannequinEntity extends AvatarEntity {

    private int profileVersion;

    public MannequinEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw,
                EntityUtils.translatedEntityName(definition.entityType(), session));
    }

    @Override
    protected boolean handlesDisplayName() {
        return true;
    }

    public void setProfile(EntityMetadata<ResolvableProfile, ?> entityMetadata) {
        ResolvableProfile resolvableProfile = entityMetadata.getValue();
        if (resolvableProfile == null) {
            resolvableProfile = SkinManager.EMPTY_RESOLVABLE_PROFILE;
        }

        int version = ++this.profileVersion;
        ResolvableProfile finalResolvableProfile = resolvableProfile;
        SkinManager.resolveProfile(resolvableProfile).whenCompleteAsync((resolvedProfile, throwable) -> {
            GameProfile profile = resolvedProfile;
            if (throwable != null || profile == null) {
                profile = finalResolvableProfile.getProfile();
            }
            if (profile == null) {
                profile = SkinManager.EMPTY_PROFILE;
            }

            GameProfile finalProfile = profile;
            session.ensureInEventLoop(() -> {
                if (version != this.profileVersion) {
                    return;
                }
                applyProfile(finalResolvableProfile, finalProfile);
            });
        });
    }

    public void setDescription(EntityMetadata<Optional<Component>, ?> entityMetadata) {
        Optional<Component> description = entityMetadata.getValue();
        if (description != null && description.isPresent()) {
            setBelowNameText(MessageTranslator.convertMessage(description.get(), session.locale()));
        } else {
            setBelowNameText(null);
        }
    }

    private void applyProfile(ResolvableProfile resolvableProfile, GameProfile profile) {
        setSkin(profile, true, () -> {});
        updateUsername(resolvableProfile, profile);
    }

    private void updateUsername(ResolvableProfile resolvableProfile, GameProfile profile) {
        String previousName = this.username;
        String newName = resolveName(resolvableProfile, profile);
        if (Objects.equals(previousName, newName)) {
            return;
        }

        this.username = newName;
        boolean updatedNametag = false;
        if (Objects.equals(this.nametag, previousName) || this.nametag.isEmpty()) {
            setNametag(newName, false);
            updatedNametag = true;
        }

        if (updatedNametag) {
            var team = session.getWorldCache().getScoreboard().getTeamFor(teamIdentifier());
            if (team != null) {
                updateNametag(team);
            }
        }
    }

    private String resolveName(ResolvableProfile resolvableProfile, GameProfile profile) {
        if (profile != null) {
            String name = profile.getName();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }

        if (resolvableProfile != null) {
            GameProfile partial = resolvableProfile.getProfile();
            if (partial != null) {
                String name = partial.getName();
                if (name != null && !name.isBlank()) {
                    return name;
                }
            }
        }

        return EntityUtils.translatedEntityName(definition.entityType(), session);
    }
}
