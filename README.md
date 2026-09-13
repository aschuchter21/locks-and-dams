# Locks & Dams

A navigation-lock mod for **Minecraft 1.20.1 / Forge 47.4.10 / Create 6.0.8**.

## Prototype: 0.1.0-dev.13

New plumbing uses a chamber port and canal port with one flanged **Inline Culvert Valve**
anywhere along each circuit. The controller assigns intake/outlet roles automatically.
[Build and upgrade guide](docs/inline-plumbing.md). Gate panels place their smooth canal face toward the player.

New desk demos start with DRAIN selected, so the first water-selector click requests FILL.
Controller status reports the warning countdown, water limits, idle state and gate interlocks.
See [fill/drain troubleshooting](docs/fill-drain-troubleshooting.md).

Approved Blender gate and pipe designs are integrated: modular steel gates with
grated catwalks and collidable handrails, and rounded flanged culverts that follow
their connections. See [installation and clearance](docs/approved-gates-and-pipes.md).

Gate Control Nodes now have a shift-right-click settings window. Closed gates
show water against both faces at their respective levels, and the desk alarm
and horn also power the solid blocks underneath their end sections.

New: [gate repairs and recessed-wall layout](docs/gate-repairs-and-recesses.md).
Gate sections align on placement and join edge-to-edge; the desk uses a sloped
click shape. New demos put hinges and open leaves inside the wall footprint.

The three-block industrial desk links to a lock controller and supplies labeled
gate selectors, a FILL/DRAIN selector, status lamps and a latched emergency stop.
Separate contacts drive a pre-start alarm and departure horn. Linked gate nodes
operate real Create clutches and reversing gearshifts. Start with
`/locks demo_desk` and [the desk wiring guide](docs/control-desk.md).

Industrial gate, hinge, valve and culvert models now replace the placeholder
art. Gate top rows automatically carry grated catwalks, including existing
saved gate contraptions. A closed pair forms a crossing when both leaves have
matching top heights. Both leaves pause while a player or mob occupies the
catwalk. See [the model and catwalk guide](docs/infrastructure-models.md).

Custom controllers can now move to another valid side-wall position. Remove the
old controller and repair its wall opening before assembling the new one. Existing
gate contraptions and uniform water are reclaimed; live controllers retain ownership.

Controller replacement now detects the lock direction automatically, including
the older demo. Replacing it while facing the side wall no longer loses the chamber.

New: [player-built rectangular chambers](docs/custom-chambers.md). Four hinges
define the opening width and chamber length; the raised upper pair has its own
height. Two canal surface ports establish the water limits automatically.
Try `/locks demo_custom` or `/locks demo_custom 7 15 5`.

- Automatically detected rectangular chambers, plus the legacy 5 by 9 example.
- Linked industrial control desk, gate terminals, and physical fill/drain valves.
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
