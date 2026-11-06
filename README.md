# TradeHelper

A client-side Fabric mod for **Minecraft Java 26.2**.

TradeHelper is designed to make villager trading easier to evaluate while a trading screen is open.

## Features in this starter release

- Displays the villager's available offers in a compact HUD panel.
- Shows each offer's input items, output items, and current uses / maximum uses.
- Calculates a simple emerald net when an offer directly spends emeralds and returns emeralds.
- Starts a local timer when an offer is observed as exhausted and displays an **estimated** restock countdown.
- Toggle the overlay with **H** (customizable in Options → Controls → Key Binds).

## Important limitations

- Restock timing is an estimate, not a server-authoritative prediction. Villagers restock according to their workstation access and work schedule; servers can also change villager behavior.
- The timer begins only while the villager's trade menu is open and the client can observe the exhausted offer. It is local to your current game session and is not persisted between launches.
- The simple emerald net calculation only applies when emeralds are part of the input and the output is also emeralds. It does not assign market values to items such as crops, paper, or sticks yet.
- Client-side only. No server installation is required.

## Requirements

- Minecraft Java **26.2**
- Java **25**
- Fabric Loader **0.19.3 or newer**
- Fabric API **0.161.0+26.2**

## Build

1. Install a JDK 25.
2. Open this folder in IntelliJ IDEA or another Gradle-compatible IDE.
3. Run `gradle build` (or use `./gradlew build` after adding the standard Gradle wrapper JAR).
4. The mod JAR will be in `build/libs/`.

> This source ZIP includes Gradle wrapper configuration and launcher scripts. The Gradle wrapper JAR is intentionally not bundled; run `gradle wrapper` once on a machine with Gradle installed, or use an IDE's Gradle integration.

## Install

1. Install Fabric Loader for Minecraft 26.2.
2. Install the matching Fabric API.
3. Build the project.
4. Put `build/libs/tradehelper-1.0.0.jar` into your Minecraft `mods` folder.
5. Launch the Fabric 26.2 profile and open a villager trading screen.

## GitHub

Create a new GitHub repository, extract this ZIP, then upload/commit the contents of the `TradeHelper` folder. The included `.gitignore` keeps generated Gradle/build files out of version control.

## License

MIT
