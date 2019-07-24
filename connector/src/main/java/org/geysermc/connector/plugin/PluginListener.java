package org.geysermc.connector.plugin;

import org.geysermc.api.events.EventHandler;
import org.geysermc.api.events.Listener;
import org.geysermc.api.plugin.Plugin;
import org.geysermc.connector.console.GeyserLogger;

import java.lang.reflect.Method;

public class PluginListener {
    Method run;
    Plugin plugin;
    Listener listener;
    Class clazz;
    EventHandler.EventPriority priority;

    void runIfNeeded(EventHandler.EventPriority p, Object o) {
        if(p.equals(priority) && clazz.isInstance(o)) {
            try {
                run.invoke(listener, o);
            } catch (ReflectiveOperationException ex) {
                GeyserLogger.DEFAULT.severe("Exception while trying to run event! Contact the maintainer of " + plugin.getName());

                ex.printStackTrace();
            }
        }
    }
}
