package org.geysermc.connector.network.session.cache;

import com.nukkitx.math.vector.Vector3f;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TeleportCache {

    private static final double ERROR = 0.1;

    private double x,y,z;
    private int teleportConfirmId;

    public boolean canConfirm(Vector3f postion){
        return (Math.abs(this.x - postion.getX()) < ERROR &&
                Math.abs(this.y - postion.getY()) < ERROR &&
                Math.abs(this.z - postion.getZ()) < ERROR);
    }
}
