# Events

Geyser has an Event Manager that allows one to listen to or trigger an event easily. Events can be easily defined
either by defining a method to be an event handler or by providing a lambda.  Each event handler will be executed
in turn based upon their priority.

## Triggering an Event

An event derives from [GeyserEvent](https://bundabrg.github.io/Geyser/apidocs/org/geysermc/connector/event/GeyserEvent.html) 
and can implement [Cancellable](https://bundabrg.github.io/Geyser/apidocs/org/geysermc/connector/event/Cancellable.html) 
if it should support being cancelled, and [Session](https://bundabrg.github.io/Geyser/apidocs/org/geysermc/connector/event/Session.html) 
if it should support a [GeyserSession](https://bundabrg.github.io/Geyser/apidocs/org/geysermc/connector/network/session/GeyserSession.html).

!!! example
    ```java
    public class MyCustomEvent extends GeyserEvent implements Cancellable {
    }
    ```

The event can be triggered through the `triggerEvent` method of the [EventManager](https://bundabrg.github.io/Geyser/apidocs/org/geysermc/connector/event/EventManager.html).

!!! example
    ```java
    EventManager.getInstance().triggerEvent(new MyCustomEvent());
    ```

This returns an [EventResult](https://bundabrg.github.io/Geyser/apidocs/org/geysermc/connector/event/EventResult.html) 
which can be used to chain additional commands based upon the result. They include `onNotCancelled()`, 
`onCancelled()` and `orElse()` to more easily execute based upon the result. All methods can be chained together.

!!! example
    ```java
    EventManager.getInstance().triggerEvent(new MyCustomEvent())
        .onNotCancelled(result -> {
            // Code executed if events were not cancelled
        })
        .orElse((result) -> {
            // Code executed if the above condition was not satisfied
        });
    ```
    
!!! example
    ```java
    EventResult<MyCustomEvent> result = EventManager.getInstance().triggerEvent(new MyCustomEvent());
    if (!result.isCancelled()) {
        // do something
    }
    ```

## Listening to an Event

There are two ways to listen for an event. One can either create an event handler method or one can create an anonymous
lamda to be executed when the event is triggered.


### Class Event Handler

An event handler method is a method that is annotated with [@GeyserEventHandler](https://bundabrg.github.io/Geyser/apidocs/org/geysermc/connector/event/annotations/GeyserEventHandler.html). 
The class it belongs to must also be registered with the event manager.

!!! example
    ```java
    public class MyClass {

        @Event
        public void onMyEvent(MyCustomEvent event) {
            GeyserConnector.getInstance().getLogger().info("Hello World");
        }
    }
    
    ...
    
    EventManager().getInstance().registerEvents(new MyClass());
    ```

!!! important
    Plugins should use the `registerEvents` method inherited from `GeyserPlugin`.

`MyCustomEvent` is the event defined previously.

### Anonymous Lambda

An event can be hooked through the `on` method of the EventManager provided with an anonymous function. This allows
code to be placed logically close to where it is related in the code instead of having to set up a separate class and
method listeners. You will need to remember to unregister an event handler when needed.

!!! example
    ```java
    EventHandler<MyCustomEvent> handler = EventManager.getInstance().on(MyCustomEvent.class, event -> {
        System.err.println("Hello World");
    })
        .build();
    ```

!!! important
    Plugins should use the `on` method inherited from [GeyserPlugin](https://bundabrg.github.io/Geyser/apidocs/org/geysermc/connector/plugin/GeyserPlugin.html).
    
You'll note the `build()` on the end. `on()` returns a Builder that can add optional parameters. This must be finalized with a
`build()` that generates the EventHandler and registers it with the EventManager. 

The following additional parameters are available:

* `priority(int)` - Set the event priority. Default `EventHandler.PRIORITY.NORMAL`
* `ignoreCancelled(boolean)` - If true the handler will not execute if cancelled. Default `true`.

!!! example
    ```java
    EventHandler<MyCustomEvent> handler = EventManager.getInstance().on(MyCustomEvent.class, event -> {
        System.err.println("Hello World");
    })
        .priority(30)
        .ignoreCancelled(false)
        .build();
    ```

## Events

Please refer to the [API Docs](https://bundabrg.github.io/Geyser/apidocs/) for more information. Geyser events are
defined under `org.geysermc.connector.event.events`.

Geyser has the following predefined Events.

