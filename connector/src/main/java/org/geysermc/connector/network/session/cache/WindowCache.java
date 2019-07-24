package org.geysermc.connector.network.session.cache;

import com.nukkitx.protocol.bedrock.packet.ModalFormRequestPacket;
import lombok.Getter;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.api.window.FormWindow;

import java.util.HashMap;
import java.util.Map;

public class WindowCache {

    private GeyserSession session;

    public WindowCache(GeyserSession session) {
        this.session = session;
    }

    @Getter
    private Map<Integer, FormWindow> windows = new HashMap<Integer, FormWindow>();

    public void addWindow(FormWindow window) {
        windows.put(windows.size() + 1, window);
    }

    public void addWindow(FormWindow window, int id) {
        windows.put(id, window);
    }

    public void showWindow(FormWindow window) {
        showWindow(window, windows.size() + 1);
    }

    public void showWindow(int id) {
        if (!windows.containsKey(id))
            return;

        ModalFormRequestPacket formRequestPacket = new ModalFormRequestPacket();
        formRequestPacket.setFormId(id);
        formRequestPacket.setFormData(windows.get(id).getJSONData());

        session.getUpstream().sendPacket(formRequestPacket);
    }

    public void showWindow(FormWindow window, int id) {
        ModalFormRequestPacket formRequestPacket = new ModalFormRequestPacket();
        formRequestPacket.setFormId(id);
        formRequestPacket.setFormData(window.getJSONData());

        session.getUpstream().sendPacket(formRequestPacket);

        addWindow(window, id);
    }
}
