# Events

Geyser has an Event Manager that allows one to listen to or trigger an event easily. Events can be easily defined
either by defining a method to be an event handler or by providing a lambda.  Each event handler will be executed
in turn based upon their priority.

## Triggering an Event

An event is derived from either `GeyserEvent` or `CancellableGeyserEvent`.

!!! example
    ```java
    public class MyCustomEvent extends GeyserEvent {
    }
    ```

The event is triggered through the `triggerEvent` method of the Event Manager.  

!!! example
    ```java
    eventManager.triggerEvent(new MyCustomEvent());
    ```

This returns an EventResult which can be used to chain additional commands based upon the result. They include `onNotCancelled()`, 
`onCancelled()` and `orElse()` to more easily execute based upon the result and they can chain together.

!!! example
    ```java
    eventManager.triggerEvent(new MyCustomEvent())
        .onNotCancelled((result) -> {
            // Code executed if events were not cancelled
        })
        .orElse((result) -> {
            // Code executed if the above condition was not satisfied
        });
    ```

`triggerEvent()` can have an optional extra parameter passing in a `Class<?>`. If set then the handler will only execute if it 
has a filter list containing this filter.

## Listening to an Event

There are two ways to listen for an event. One can either create an event handler method or one can create an anonymous
lamda to be executed when the event is triggered.


### Class Event Handler

An event handler method is a method that is annotated with `@Event`. The class it belongs to must also be registered
with the event manager.

!!! example
    ```java
    public class MyClass {

        @Event
        public void onEnable(MyCustomEvent event) {
            System.err.println("Hello World");
        }
    }
    
    ...
    
    GeyserConnecter.getInstance().getEventManager.registerEvents(new MyClass());
    ```

!!! important
    Plugins should use the `registerEvents` method inherited from `GeyserPlugin`.

The `@Event` annotation has the following optional parameters:

* **priority** - `Integer` from 0 - 100. Default `50`. Event Handlers are executed in order from lowest to highest priority.
* **ignoreCancelled** - `Boolean`. Default `true`. If true then if an event is cancelled the handler will not be executed.
* **filter** - `List<Class<?>>`. Default {}. If set will only execute the event handler if the passed in filter to `triggerEvent` 
is null or matches any on this list.

`MyCustomEvent` is the event defined previously.

### Anonymous Lambda

An event can be hooked through the `on` method of the EventManager provided with an anonymous function. This allows
code to be placed logically close to where it is related in the code instead of having to set up a separate class and
method listeners.

!!! example
    ```java
    GeyserConnector.getInstance().getEventManager().on(MyCustomEvent.class, (handler, event) -> {
        System.err.println("Hello World");
    }).build();
    ```

!!! important
    Plugins should use the `on` method inherited from `GeyserPlugin`.
    
You'll note the `build()` on the end. `on()` returns a Builder that can add optional parameters. This must be finalized with a
`build()` that generates the EventHandler and registers it with the EventManager. 

The following additional parameters are available:

* `priority(int)` - Set the event priority. Default `EventHandler.PRIORITY.NORMAL`
* `ignoreCancelled(boolean)` - If true the handler will not execute if cancelled. Default `true`.
* `filter(Class<?>[])` - List of filters the handler will accept. Default `{}`

!!! example
    ```java
    GeyserConnector.getInstance().getEventManager().on(MyCustomEvent.class, (handler, event) -> {
        System.err.println("Hello World");
    })
        .priority(30)
        .ignoreCancelled(false)
        .build();
    ```

## Events

Geyser has the following predefined Events.

### DownstreamPacketReceiveEvent`<T>`
*cancellable*

| Modifier and Type | Method | Description  | 
|---|---|---|
| GeyserSession | getSession() | Gets the current session | 
| T | getPacket() | Gets the Packet |

Triggered for each packet received from downstream. If cancelled then regular processing of the packet will not occur.

The type of packet should be passed in as a Type. The filter should also be set to limit what packets you want otherwise
every Downstream packet will trigger this handler.

!!!example
    ```java
    @Event(filter = ClientChatPacket.class)
    public void onTextPacket(DownstreamPacketReceiveEvent<ClientChatPacket> event) {
        getLogger().warning("Got packet: " + event.getPacket());
    }
    ```

!!!example
    ```java
    @Event
    public void onGenericPacket(DownstreamPacketReceiveEvent<Packet> event) {
        getLogger().warning("Got generic packet: " + event.getPacket());
    }
    ```

### DownstreamPacketSendEvent`<T>`
*cancellable*

| Modifier and Type | Method | Description  | 
|---|---|---|
| GeyserSession | getSession() | Gets the current session | 
| T | getPacket() | Gets the Packet |

Triggered for each packet sent to downstream. If cancelled then the packet will not be sent.

The type of packet should be passed in as a Type. The filter should also be set to limit what packets you want otherwise
every Downstream packet will trigger this handler.

!!!example
    ```java
    @Event(filter = ServerChatPacket.class)
    public void onTextPacket(DownstreamPacketSendEvent<ServerChatPacket> event) {
        getLogger().warning("Sending packet: " + event.getPacket());
    }
    ```

!!!example
    ```java
    @Event
    public void onGenericPacket(DownstreamPacketSendEvent<Packet> event) {
        getLogger().warning("Sending generic packet: " + event.getPacket());
    }
    ```

### GeyserStartEvent

Triggered after Geyser has finished starting.

### GeyserStopEvent

Triggered when Geyser is about to stop.

### PluginDisableEvent

| Modifier and Type | Method | Description  | 
|---|---|---|
| GeyserPlugin | getPlugin() | Gets the Plugin |

Triggered each time a plugin is disabled.

### PluginEnableEvent
*cancellable*

| Modifier and Type | Method | Description  | 
|---|---|---|
| GeyserPlugin | getPlugin() | Gets the Plugin |

Triggered each time a plugin is enabled.

### PluginMessageEvent
*cancellable*

| Modifier and Type | Method | Description  | 
|---|---|---|
| String | getChannel() | Gets the message channel |
| byte[] | getData() | Gets the message data |
| GeyserSession | getSession() | Gets the current session |


### UpstreamPacketReceiveEvent`<T>`
*cancellable*

| Modifier and Type | Method | Description  | 
|---|---|---|
| GeyserSession | getSession() | Gets the current session | 
| T | getPacket() | Gets the Packet |

Triggered for each packet received from upstream. If cancelled then regular processing of the packet will not occur.

The type of packet should be passed in as a Type. The filter should also be set to limit what packets you want otherwise
every Upstream packet will trigger this handler.

!!!example
    ```java
    @Event(filter = TextPacket.class)
    public void onTextPacket(UpstreamPacketReceiveEvent<TextPacket> event) {
        getLogger().warning("Got packet: " + event.getPacket());
    }
    ```

!!!example
    ```java
    @Event
    public void onGenericPacket(UpstreamPacketReceiveEvent<BedrockPacket> event) {
        getLogger().warning("Got generic packet: " + event.getPacket());
    }
    ```

### UpstreamPacketSendEvent`<T>`
*cancellable*

| Modifier and Type | Method | Description  | 
|---|---|---|
| GeyserSession | getSession() | Gets the current session | 
| T | getPacket() | Gets the Packet |

Triggered for each packet sent upstream. If cancelled then the packet will not be sent.

The type of packet should be passed in as a Type. The filter should also be set to limit what packets you want otherwise
every Upstream packet will trigger this handler.

!!!example
    ```java
    @Event(filter = TextPacket.class)
    public void onTextPacket(UpstreamPacketSendEvent<TextPacket> event) {
        getLogger().warning("Sending packet: " + event.getPacket());
    }
    ```

!!!example
    ```java
    @Event
    public void onGenericPacket(UpstreamPacketSendEvent<BedrockPacket> event) {
        getLogger().warning("Sending generic packet: " + event.getPacket());
    }
    ```