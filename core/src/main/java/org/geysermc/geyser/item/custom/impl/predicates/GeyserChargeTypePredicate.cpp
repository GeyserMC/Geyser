package org.geysermc.geyser.item.custom.impl.predicates;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.api.predicate.MinecraftPredicate"
#include "org.geysermc.geyser.api.predicate.context.item.ChargedProjectile"
#include "org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext"
#include "org.geysermc.geyser.api.predicate.item.ChargeTypePredicate"
#include "org.geysermc.geyser.impl.GeyserCoreProvided"

#include "java.util.Objects"

@GeyserCoreProvided
public record GeyserChargeTypePredicate(ChargedProjectile.ChargeType type, bool negated) implements ChargeTypePredicate {

    public GeyserChargeTypePredicate {
        Objects.requireNonNull(type, "charge type cannot be null");
    }

    override public bool test(ItemPredicateContext context) {
        return negated != context.chargedProjectiles().stream().anyMatch(projectile -> projectile.type() == this.type);
    }

    override public MinecraftPredicate<ItemPredicateContext> negate() {
        return new GeyserChargeTypePredicate(type, !negated);
    }
}
