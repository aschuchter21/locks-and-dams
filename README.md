# Locks & Dams

A navigation-lock mod for **Minecraft 1.20.1 / Forge 47.4.10 / Create 6.0.8**.

## Prototype: 0.1.0-dev.5

Controller replacement now detects the lock direction automatically, including
the older demo. Replacing it while facing the side wall no longer loses the chamber.

New: [player-built rectangular chambers](docs/custom-chambers.md). Four hinges
define the opening width and chamber length; the raised upper pair has its own
height. Two canal surface ports establish the water limits automatically.
Try `/locks demo_custom` or `/locks demo_custom 7 15 5`.

- Automatically detected rectangular chambers, plus the legacy 5 by 9 example.
- Two gate drives, a fill valve, and a drain valve with independent redstone inputs.
- Non-flowing managed water and boat support across changing water levels.
- Level and obstruction interlocks, fault pauses, and saved chamber state.
- Creative tab, survival recipes, and a non-destructive `/locks demo` builder.
- Create-powered lock hinges that swing real panel contraptions through 90 degrees.
- Physical culvert routes from the upper canal to the fill valve and from the drain valve to the lower canal.

Create 6.0.8 is required on client and server. Commercial Systems is not required.
Canals remain fixed water supplies; finite reservoir simulation and dams are later milestones.

## Try it

Install the normal mod JAR and [Create 6.0.8 for 1.20.1](https://github.com/Creators-of-Create/Create/releases/tag/mc1.20.1-6.0.8) in a Forge 47.4.10 / Minecraft 1.20.1 instance. Both
client and server need the mod. In a creative test world with commands enabled,
fly into clear space and run `/locks demo`. It refuses to replace blocks or
entities and creates two pools, the chamber, four levers, culvert plumbing and
two hinges powered by 8 RPM creative motors. Replace those motors with your own
Create shaft network for a survival build.

From the lower end, the levers are **lower gate, fill, drain, upper gate**.
Open the lower gate and row in. Turn that gate off, turn fill on, wait for the
upper level, then open the upper gate and row out. Reverse the sequence to descend.
Right-click the controller to read the level and any interlock message.

See [the building and operating guide](docs/prototype.md) and
[validation notes](docs/validation.md).

## Development

Install a Java 17 JDK, then run `./gradlew build` (Windows: `.\gradlew.bat build`).
The wrapper supplies Gradle 8.8. Legacy ModDevGradle matches the Commercial Systems
build setup and produces a **Forge** mod. Normal and source JARs are in `build/libs/`.
If Java 17 is not detected, configure `org.gradle.java.installations.paths` locally.

`./gradlew runClient` and `./gradlew runServer` use isolated development folders.
The server operator must review and accept Minecraft's EULA. These commands do
not use an existing CurseForge instance or player world.

Integration fixtures are opt-in; see the validation notes. Always run a normal
`build` after tests so the distributed JAR excludes those fixtures.

## License

All rights reserved. No open-source license has been selected.
