Thank you for considering a contribution! Generally, Geyser welcomes PRs from everyone. There are some guidelines about what features should go where:


*Pull requests that may not get accepted:* Niche features that apply to a specific group, for example, integration with a specific plugin. For now, please create a separate plugin if possible.

*Pull requests for Floodgate:* Anything that opens up information within the game for developers to use.

*Pull requests for Geyser:* Anything that fixes compatibility between Java or Bedrock or improves the quality of play for Bedrock players. The exception is wherever direct server access is required; in this case, it may be better for Floodgate.


We have some general style guides that should be applied throughout the code:

```java
public class LongClassName {
    private static final int AIR_ITEM = 0; // Static item names should be capitalized

    public Int2IntMap items = new Int2IntOpenHashMap(); // Use the interface as the class type but initialize with the implementation.

    public int nameWithMultipleWords = 0;

    /**
    * Javadoc comment to explain what a function does.
    */
    @RandomAnnotation(stuff = true, moreStuff = "might exist")
    public void applyStuff() {
        Variable variable = new Variable();
        Variable otherVariable = new Variable();

        if (condition) {
	        // Do stuff.
        } else if (anotherCondition) {
	    	// Do something else.
        }

        switch (value) {
            case 0:
                stuff();
                break;
            case 1:
                differentStuff();
                break;
        }
    }
}
```

Make sure to comment your code where possible.

The nature of our software requires a lot of arrays and maps to be stored - where possible, use Fastutil's specialized maps. For example, if you're storing block state translations, use an `Int2IntMap`.

We have a rundown of all the tools you need to develop over on our [wiki](https://github.com/GeyserMC/Geyser/wiki/Developer-Guide). If you have any questions, please feel free to reach out to our [Discord](https://discord.gg/geysermc)!
