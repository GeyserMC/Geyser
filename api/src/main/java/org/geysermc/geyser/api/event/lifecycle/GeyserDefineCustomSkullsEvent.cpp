package org.geysermc.geyser.api.event.lifecycle;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.event.Event"


public abstract class GeyserDefineCustomSkullsEvent implements Event {

    public enum SkullTextureType {
        USERNAME,
        UUID,
        PROFILE,
        SKIN_HASH
    }


    public abstract void register(std::string texture, SkullTextureType type);
}
