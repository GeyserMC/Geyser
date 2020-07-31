# Coding

This section will contain a list of coding hints for when working with Geyser either as a contributer or as a plugin developer.

## Async Tasks

You can obtain a [ScheduledExectorService](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ScheduledExecutorService.html?is-external=true)
by using [GeyserConnector#getGeneralThreadPool](https://bundabrg.github.io/Geyser/apidocs//org/geysermc/connector/GeyserConnector.html#getGeneralThreadPool()).

!!! example
    ```java
     GeyserConnector.getInstance().getGeneralThreadPool().schedule(() -> {
        ...
     }, 5, TimeUnit.SECONDS);
    ```
