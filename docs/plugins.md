# Plugins

Geyser provides support for third party plugins which can be placed into a `plugins` folder under the Geyser data folder.

Plugins provide a way to extend the features of Geyser without needing to deal with the Geyser code.  It is hoped that
developers will be able to create plugins that would be of use to others.

This page describes how to write a plugin.

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
        <version>1.0-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

!!! info
    Plugin support is presently only available through a feature branch and thus there will be references to a third party
    maven repository that holds a build of this branch.  This will change in the future.


## Example Plugin

```java
    @Plugin(
        name = "ExamplePlugin",
        version = "1.1.0-dev",
        authors = {"bundabrg"},
        description = "Provides an example plugin"
    )
    public class MyPlugin extends GeyserPlugin {
        public MyPlugin(PluginManager pluginManager, PluginClassLoader pluginClassLoader) {
            super(pluginManager, pluginClassLoader);
        }

        @Event
        public void onEnable(EventContext ctx, EnableEvent event) {
            System.err.println("I'm alive");

            // Register another class with event handlers
            registerEvents(new MyAdditionalClass());

            // Example of lambda event hook
            on(DisableEvent.class, (ctx, event) -> {
                System.err.println("I'm also dead");
            }, PRIORITY.HIGH);      
        }
        
        @Event
        public void onDisable(EventContext ctx, DisableEvent event) {
            System.err.println("I'm dead");
        }

    }
```

## Plugin EntryPoint

A plugin must at a minimum define a class that extends `GeyserPlugin` and be annotated with `@Plugin`. The annotation 
provides details about the plugin such as its version and author(s).

The following fields are available for `@Plugin`:

* **name** - Name of the plugin. Used in the logs.
* **version** - Version of the plugin.
* **authors** - A list of authors
* **description** - A short description of the plugin
* **global** - Should the plugin make its classes available to other plugins (default: true)

## Plugin Events

A plugin will generally hook into several events and provides its own event registration inherited from `GeyserPlugin`.

A plugin class will look for any methods annotated with `@Event` and will treat them as Event Handlers, using reflection
to determine which event is being trapped. In the previous example the plugin has trapped both the `EnableEvent` and `DisableEvent`.

Please refer to [events](events.md) for more information about the event system. 

!!! note
    There is no need to register the plugin class for events as it will be registered by default.

!!! note
    The plugin class itself provides many of the registration methods found in the Event Manager to track which events belong to the plugin. You
    should use the plugins own registration methods in preference to those in the Event Manger. This includes
    `registerEvents` and `on`.
