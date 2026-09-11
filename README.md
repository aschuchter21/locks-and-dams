# Locks & Dams

A standalone Minecraft Java mod for player-built navigation locks, dams, and
controlled water levels, balancing realistic operation with simple redstone controls.

## Current status

Project foundation only. This version has a Forge entry point and build/run
configuration; it does **not** yet add gates, valves, chambers, or boat support.
Create and Commercial Systems are not required.

## Development setup

- Minecraft: **1.20.1**
- Forge: **47.4.10**
- Java development kit: **17**
- Gradle: **8.8**, provided by the wrapper

Build with `./gradlew build` (Windows: `.\gradlew.bat build`). The mod and source
JARs are written to `build/libs/`. The build uses Legacy ModDevGradle, matching
the Commercial Systems development setup; the resulting mod targets Forge.

Launch an isolated development client with `./gradlew runClient`, or a dedicated
development server with `./gradlew runServer`. Server startup requires the server
operator to review and accept Minecraft's EULA. Neither command uses an existing
CurseForge instance or player world. Install a Java 17 JDK before building; if
Gradle cannot find it, configure `org.gradle.java.installations.paths` locally.

## First playable milestone

Build a rectangular lock, enter from the lower canal in a boat, close the gates,
fill the chamber, and exit into the upper canal. Repeat in reverse without
breaking the boat or ejecting its passenger.

See [the prototype design](docs/prototype.md) for agreed controls, scope, and
acceptance checks. All features in that document are planned, not implemented.

## License

All rights reserved. No open-source license has been selected.
