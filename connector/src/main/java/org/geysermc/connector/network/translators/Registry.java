package org.geysermc.connector.network.translators;

import com.github.steveice10.packetlib.packet.Packet;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class Registry<T> {

    private final Map<Class<? extends T>, BiConsumer<? extends T, GeyserSession>> MAP = new HashMap<>();

    public static final Registry<Packet> JAVA = new Registry<>();

    public static <T extends Packet> void add(Class<T> clazz, BiConsumer<T, GeyserSession> translator) {
        JAVA.MAP.put(clazz, translator);
    }

    public <P extends T> void translate(Class<P> clazz, P p, GeyserSession s) {
        try {
            ((BiConsumer<P, GeyserSession>) JAVA.MAP.get(clazz)).accept(p, s);
        } catch (NullPointerException e) {
            System.err.println("could not translate packet" + p.getClass().getSimpleName());
        }
    }
}
