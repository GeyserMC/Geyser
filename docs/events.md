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

This returns an EventResult which can be used to chain additional commands based upon the result. They include `ifNotCancelled` and 
`ifCancelled`.

!!! example
    ```java
    eventManager.triggerEvent(new MyCustomEvent())
        .ifNotCancelled((result) -> {
            // Code executed if events were not cancelled
        });
    ```


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
        public void onEnable(EventContext ctx, MyCustomEvent event) {
            System.err.println("Hello World");
        }
    }
    
    ...
    
    GeyserConnecter.getInstance().getEventManager.registerEvents(new MyClass());
    ```

!!! important
    Plugins should use the `registerEvents` method inherited from `GeyserPlugin`.

The `@Event` annotation has the following optional parameters:

* **priority** - Integer from 0 - 100. Default `50`. Event Handlers are executed in order from lowest to highest priority.
* **ignoreCancelled** - Boolean. Default `true`. If true then if an event is cancelled the handler will not be executed.

The `EventContext` will be discussed later. `MyCustomEvent` is the event defined previously.

### Anonymous Lambda

An event can be hooked through the `on` method of the EventManager provided with an anonymous function. This allows
code to be placed logically close to where it is related in the code instead of having to set up a separate class and
method listeners.

!!! example
    ```java
    GeyserConnector.getInstance().getEventManager().on(MyCustomEvent.class, (ctx, event) -> {
        System.err.println("Hello World");
    });
    ```

!!! important
    Plugins should use the `on` method inherited from `GeyserPlugin`.
    
This method takes 2 optional parameters specifying the priority of the event and if the handler should ignore cancelled events.

This returns an `EventRegisterResult` that allows one to chain a delay on, normally to used to cancel the handler. It is safe to cancel
an already cancelled handler.

!!! example
    ```java
    GeyserConnector.getInstance().getEventManager().on(MyCustomEvent.class, (ctx, event) -> {
        System.err.println("If this doesn't trigger in 10 seconds it is cancelled");
        ctx.unregister();
    }).onDelay((ctx) -> {
        ctx.unregister();
    }, 10, TimeUnit.SECONDS);
    ```

### Event Context

The event handler receives an EventContext in addition to the Event class. The EventContext holds anything related to the
EventHandler itself and presently only allows an EventHandler to `unregister` itself.


## Events

Geyser has the following predefined Events.

### DownstreamPacketReceiveEvent
*cancellable*

| Modifier and Type | Method | Description  | 
|---|---|---|
| GeyserSession | getSession() | Gets the current session | 
| Packet | getPacket() | Gets the Packet |

Triggered for each packet received from downstream. If cancelled then regular processing of the packet will not occur.

### DownstreamPacketSendEvent
*cancellable*

| Modifier and Type | Method | Description  | 
|---|---|---|
| GeyserSession | getSession() | Gets the current session | 
| Packet | getPacket() | Gets the Packet |

Triggered for each packet sent to downstream. If cancelled then the packet will not be sent.

### GeyserStopEvent

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


### UpstreamPacketReceiveEvent
*cancellable*

| Modifier and Type | Method | Description  | 
|---|---|---|
| GeyserSession | getSession() | Gets the current session | 
| BedrockPacket | getPacket() | Gets the Packet |

Triggered for each packet received from upstream. If cancelled then regular processing of the packet will not occur.

### UpstreamPacketSendEvent
*cancellable*

| Modifier and Type | Method | Description  | 
|---|---|---|
| GeyserSession | getSession() | Gets the current session | 
| BedrockPacket | getPacket() | Gets the Packet |

Triggered for each packet sent to upstream. If cancelled then the packet will not be sent.
