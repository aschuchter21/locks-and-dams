# Locks & Dams

A standalone navigation-lock mod for **Minecraft 1.20.1 / Forge 47.4.10**.

## Prototype: 0.1.0-dev.2

- A 5 by 9 interior chamber with a three-block lift.
- Two gate drives, a fill valve, and a drain valve with independent redstone inputs.
- Non-flowing managed water and boat support across changing water levels.
- Level and obstruction interlocks, fault pauses, and saved chamber state.
- Creative tab, survival recipes, and a non-destructive `/locks demo` builder.

Create and Commercial Systems are not required. Gates currently open and close
instantly. Dams, finite reservoirs, adjustable sizes, animated machinery, and
Create integration are later milestones.

## Try it

Install the normal mod JAR in a Forge 47.4.10 / Minecraft 1.20.1 instance. Both
client and server need the mod. In a creative test world with commands enabled,
fly into clear space and run `/locks demo`. It refuses to replace blocks or
entities and creates two pools, the chamber, and four levers.

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
