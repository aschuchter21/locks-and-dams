# Locks & Dams

A navigation-lock mod for **Minecraft 1.20.1 / Forge 47.4.10 / Create 6.0.8**.

## Timber miter gates: 1.1.0-beta.1

The new custom-lock gate design assembles individual panels into continuous timber-and-steel leaves with an upstream V closure. See [building, clearance, and upgrade instructions](docs/timber-miter-gates.md). The lower gates now open into chamber-side pockets. Existing assembled gates stay unchanged until rebuilt.

## Release 1.0.0

Build rectangular canal locks with smooth water levels, Create-powered swing gates,
grated catwalks and handrails, rounded flanged piping, and inline culvert valves.
A linked industrial control desk provides gate selectors, FILL/DRAIN control,
status indicators, a latched emergency stop, and redstone alarm/horn outputs.

See [release notes](CHANGELOG.md), [plumbing](docs/inline-plumbing.md),
and [construction and desk wiring](docs/control-desk.md).

Create 6.0.8 is required on client and server. Commercial Systems is not required.
Canals are fixed water supplies; finite reservoirs and dam simulation are not included.

## Try it

Install the normal mod JAR and [Create 6.0.8 for 1.20.1](https://github.com/Creators-of-Create/Create/releases/tag/mc1.20.1-6.0.8) in a Forge 47.4.10 / Minecraft 1.20.1 instance. Both
client and server need the mod. In a creative test world with commands enabled,
fly into clear space and run `/locks demo_desk`. It creates a custom lock with a
linked three-block control desk and real Create clutches and reversing gearshifts.
See [the control-desk and wiring guide](docs/control-desk.md) for placement,
linking, selectors, warning/horn terminals, and emergency-stop reset.

Positive hinge RPM opens, negative closes, and zero holds. The Gate Drive is
retired. The desk provides a 10-second warning before filling/draining, a latched
emergency stop, and a 2-second horn pulse 4 seconds after a gate fully opens.
Existing assembled locks load stopped on upgrade; link a desk or shift-right-click
an unlinked controller after preparing the new rotation controls.

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
