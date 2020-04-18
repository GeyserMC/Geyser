package org.geysermc.connector.network.session.cache;

import com.github.steveice10.mc.protocol.data.message.Message;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import com.nukkitx.protocol.bedrock.packet.BossEventPacket;
import com.nukkitx.protocol.bedrock.packet.RemoveEntityPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.MessageUtils;

public class BossBar {

    private GeyserSession session;

    private long entityId;
    private Message title;
    private float health;
    private int color;
    private int overlay;
    private int darkenSky;

    public BossBar(GeyserSession session, Message title, float health, int color, int overlay, int darkenSky) {
        this.session = session;
        this.entityId = session.getEntityCache().getNextEntityId().incrementAndGet();
        this.title = title;
        this.health = health;
        this.color = color;
        this.overlay = overlay;
        this.darkenSky = darkenSky;
    }

    public void addBossBar() {
        addBossEntity();
        updateBossBar();
    }

    public void updateBossBar() {
        BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(entityId);
        bossEventPacket.setAction(BossEventPacket.Action.SHOW);
        bossEventPacket.setTitle(MessageUtils.getTranslatedBedrockMessage(title, session.getClientData().getLanguageCode()));
        bossEventPacket.setHealthPercentage(health);
        bossEventPacket.setColor(color); //ignored by client
        bossEventPacket.setOverlay(overlay);
        bossEventPacket.setDarkenSky(darkenSky);

        session.getUpstream().sendPacket(bossEventPacket);
    }

    public void updateTitle(Message title) {
        this.title = title;
        BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(entityId);
        bossEventPacket.setAction(BossEventPacket.Action.TITLE);
        bossEventPacket.setTitle(MessageUtils.getTranslatedBedrockMessage(title, session.getClientData().getLanguageCode()));

        session.getUpstream().sendPacket(bossEventPacket);
    }

    public void updateHealth(float health) {
        this.health = health;
        BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(entityId);
        bossEventPacket.setAction(BossEventPacket.Action.HEALTH_PERCENTAGE);
        bossEventPacket.setHealthPercentage(health);

        session.getUpstream().sendPacket(bossEventPacket);
    }

    public void removeBossBar() {
        BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(entityId);
        bossEventPacket.setAction(BossEventPacket.Action.HIDE);

        session.getUpstream().sendPacket(bossEventPacket);
        removeBossEntity();
    }

    /**
     * Bedrock still needs an entity to display the BossBar.<br>
     * Just like 1.8 but it doesn't care about which entity
     */
    private void addBossEntity() {
        AddEntityPacket addEntityPacket = new AddEntityPacket();
        addEntityPacket.setUniqueEntityId(entityId);
        addEntityPacket.setRuntimeEntityId(entityId);
        addEntityPacket.setIdentifier("minecraft:creeper");
        addEntityPacket.setEntityType(33);
        addEntityPacket.setPosition(session.getPlayerEntity().getPosition());
        addEntityPacket.setRotation(Vector3f.ZERO);
        addEntityPacket.setMotion(Vector3f.ZERO);
        addEntityPacket.getMetadata().put(EntityData.SCALE, 0.01F); // scale = 0 doesn't work?

        session.getUpstream().sendPacket(addEntityPacket);
    }

    private void removeBossEntity() {
        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.setUniqueEntityId(entityId);

        session.getUpstream().sendPacket(removeEntityPacket);
    }
}
