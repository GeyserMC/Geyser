# Extensions

Geyser provides support for third party extensions which can be placed into a `extensions` folder under the Geyser data folder.

Extensions provide a way to extend the features of Geyser without needing to deal with the Geyser code. 

This page describes how to write a extension.

!!! alert
    Please keep in mind that Geyser itself may run as a plugin to a server or standalone. If you wish to have your extension
    supported by a wide audience try to write it as if it runs on the standalone version of Geyser and thus does
    not have access to the server code. For example don't assume you can access the Spigot API unless you wish to limit
    your extension to run only on Geyser running as a Spigot plugin.

## Maven

Add the following to the relevant section of your `pom.xml`

```xml
<repositories>
    <!-- Bundabrg's Repo -->
    <repository>
        <id>bundabrg-repo</id>
        <url>https://repo.worldguard.com.au/repository/maven-public</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

<dependencies>
    <!-- Geyser -->
    <dependency>
        <groupId>org.geysermc</groupId>
        <artifactId>connector</artifactId>
        <version>1.2.0-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

!!! info
    Extension support is presently only available through a feature branch and thus there will be references to a third party
    maven repository that holds a build of this branch.  This will change in the future.


## Example Extension

```java
    @Extension(
        name = "ExampleExtension",
        version = "1.1.0-dev",
        authors = {"bundabrg"},
        description = "Provides an example extension"
    )
    public class MyExtension extends GeyserExtension {
        public MyExtension(ExtensionManager extensionManager, ExtensionClassLoader extensionClassLoader) {
            super(extensionManager, extensionClassLoader);
        }

        @GeyserEventHandler
        public void onStartup(GeyserStartEvent event) {
            System.err.println("I'm alive");

            // Register another class with event handlers
            registerEvents(new MyAdditionalClass());

            // Example of lambda event hook
            EventHandler<GeyserStopEvent> handler = on(GeyserStopEvent.class, event -> {
                System.err.println("I'm also dead");
            })
                .priority(EventHandler.PRIORITY.HIGH);
        }
        
        @GeyserEventHandler
        public void onDisable(GeyserStopEvent event) {
            System.err.println("I'm dead");
        }
    }
```

## Extension EntryPoint

A extension must at a minimum define a class that extends [GeyserExtension](https://bundabrg.github.io/Geyser/apidocs/org/geysermc/connector/extension/GeyserExtension.html) 
and be annotated with [@Extension](https://bundabrg.github.io/Geyser/apidocs/org/geysermc/connector/extension/annotations/Extension.html). The annotation 
provides details about the extension such as its version and author(s).

The following fields are available for `@Extension`:

* **name** - Name of the extension. Used in the logs.
* **version** - Version of the extension.
* **authors** - A list of authors
* **description** - A short description of the extension
* **global** - Should the extension make its classes available to other extensions (default: true)

## Extension Events

A extension will generally hook into several events and provides its own event registration inherited from 
[GeyserExtension](https://bundabrg.github.io/Geyser/apidocs/org/geysermc/connector/extension/GeyserExtension.html).

A extension class will look for any methods annotated with [@GeyserEventHandler](https://bundabrg.github.io/Geyser/apidocs/org/geysermc/connector/event/annotations/GeyserEventHandler.html) 
and will treat them as Event Handlers, using reflection to determine which event is trapped. 
In the previous example the extension has trapped both the [GeyserStartEvent](https://bundabrg.github.io/Geyser/apidocs/org/geysermc/connector/event/events/geyser/GeyserStartEvent.html)
and [GeyserStopEvent](https://bundabrg.github.io/Geyser/apidocs/org/geysermc/connector/event/events/geyser/GeyserStopEvent.html).

Please refer to [events](events.md) for more information about the event system. 

!!! note
    There is no need to register the extension class for events as it will be registered by default.

!!! note
    The extension class itself provides many of the registration methods found in the Event Manager to track which events belong to the extension. You
    should use the extensions own registration methods in preference to those in the Event Manger. This includes
    `registerEvents` and `on`.

## Plugin Messages

A extension can communicate with a extension on the downstream server through the use of plugin message channels. More information about
this can be found [here](https://www.spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel). 

### Sending a Plugin Message

To send a plugin message use `GeyserSession#sendPluginMessage.`

!!! example
    ```java
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeUTF("Data1");
    out.writeUTF("Data2");
    session.sendPluginMessage("myextension:channel", out.toByteArray());
    ```
    
### Receiving Plugin Messages

To receive plugin messages you need to first register to receive the message then listen for the `PluginMessageEvent`.

!!! example
    ```java
    GeyserConnector.getInstance().registerPluginChannel("myextension:channelname");
    
    '''
    
    @Event
    void public onPluginMessageEvent(PluginMessageEvent event) {
        if (!event.getChannel().equals("myextension:channelname")) {
            return;
        }
        
        ...
    }
    
    ```

## Logging

Extension classes use their `getLogger()` to retrieve a logging interface. This will log messages to the regular log
with the extension name prefixed in the messages.

!!! example
    ```java
    @Extension(...)
    public class MyExtension extends GeyserExtension {
        public void myMethod() {
            ...
            getLogger().info("This is an informative message!");
        }
    }
    ``` 
