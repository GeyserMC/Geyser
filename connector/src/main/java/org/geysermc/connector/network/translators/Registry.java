package org.geysermc.connector.network.translators;

import com.github.steveice10.packetlib.packet.Packet;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Registry<T> {
    private final Map<Class<? extends T>, Consumer<? extends T>> MAP = new HashMap<>();

    public static final Registry<Packet> JAVA = new Registry<>();

    public static <T extends Packet> void add(Class<T> clazz, Consumer<T> translator) {
        JAVA.MAP.put(clazz, translator);
    }

    public <P extends T> void translate(Class<P> clazz, P p) {
        try {
            ((Consumer<P>) JAVA.MAP.get(clazz)).accept(p);
        } catch (NullPointerException e) {
            System.err.println("could not translate packet" + p.getClass().getSimpleName());
        }
    }

}
