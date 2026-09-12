# Validation and development fixtures

## Create integration: dev.3

September 12, 2026: the full dedicated-server cycle and actual server restart /
terrain-obstruction recovery both passed on the final gate-clearance implementation.
Client startup loaded the new resources, but the preview windows closed before
the rider test completed. **The dev.3 visual and actual-rider acceptance check is
incomplete.** The shutdown-time "Boat broke" assertion from the first interrupted
preview is not evidence of a failure during normal operation; shutdown began
before that assertion. Do not substitute dev.2 screenshots or results for dev.3.

The dedicated fixture in `tools/createtest` runs with Create 6.0.8, Forge
47.4.10 and Minecraft 1.20.1. It verifies four orientations, 30-panel Create
contraptions, gradual 90-degree gate travel, loss/restoration of shaft power,
closed-gate water interlocks, the complete fill/drain cycle, canal plumbing,
broken-pipe pause and repair, and chest boats with passengers and 17 diamonds.

Prepare an isolated `run-create-checks` folder with an accepted EULA and a
fresh flat test world in `server.properties`, bound to loopback. Run
`./gradlew -PcreateChecks runServer`. Require `PASS:` in
`run-create-checks/create-checks-result.txt`; Gradle exit status alone does not
indicate that game assertions passed. Then run
`./gradlew -PcreateChecks -PcreateRestore runServer` on the same world to check
saved hinges, gate operation after reload and terrain-obstruction recovery.
Read `run-create-checks/create-restart-result.txt` separately.

The actual-rider fixture below now uses the new demo and waits for each moving
gate to finish opening. The older `LockChecks` fixture and dev.2 observations
below are retained as history; its instant-gate timings are not the dev.3 suite.

Finite canal volume, remote-client latency, other mods' boats and shaders remain
outside this prototype's acceptance checks.

## Checks performed for dev.2

A dedicated Forge 47.4.10 server exercised the actual blocks, fluids, entities,
redstone, and ticking controller in all four horizontal orientations:

- Lower-canal boat entry; a complete fill and drain.
- Ordinary boat survival, chest-boat buoyancy, 17 diamonds preserved, and a
  mounted villager retained throughout operation.
- Closed-gate requirement, unequal-level gate interlocks, continuously held
  requests, and obstruction detection before closing.
- Simultaneous fill/drain requests and interruption of valve power.
- Controller NBT serialization/reload, paused broken-wall and missing-canal
  conditions, and resumption after repair.
- Refusal to build the demo over existing blocks.

Replacement-controller recovery also passed without resetting the current water
level. An actual dedicated-server stop/start restored all four chamber levels,
chest boats, cargo, and mounted passengers.

A separate integrated Minecraft client drove an actual local player in a vanilla
boat through both gates: lower pool, chamber filling, upper pool, chamber draining,
then lower pool again. The player stayed mounted and the boat remained intact.
In-game screenshots confirmed the water renders and the rider remains afloat.

The initial still-fluid-only approach failed a chest-boat check at a whole-block
boundary. The final implementation adds a collision-aware vertical correction
once per locally controlled boat tick. A riding client's instance applies its own
correction; the server does not compete with that client's vehicle movement.

Those dev.2 checks did not establish compatibility with Create contraptions, other mods'
boats, shaders, network latency, or a second remote observer. Those remain separate
compatibility and multiplayer acceptance tests. Gate animation was not implemented in dev.2.

## Reproduce the dedicated-server checks

1. Build with a Java 17 JDK and the checked-in Gradle wrapper.
2. Prepare `run-checks/eula.txt` after reviewing Minecraft's EULA.
3. Create `run-checks/server.properties` using an unused flat test world name,
   loopback binding (`server-ip=127.0.0.1`), and an available port.
4. Run `./gradlew -PlockChecks runServer`.
5. Read `run-checks/lock-checks-result.txt` and require its leading `PASS:`.
   The server exits automatically. A successful Gradle exit alone is NOT proof
   that the game assertions passed.

Fixtures create four locks in clear space at y=200, then manipulate their levers,
entities, and structure. Use a fresh test world for each full fixture run.
They do not operate on a CurseForge instance or your normal worlds.

After a successful full run, `./gradlew -PlockChecks -PlockRestore runServer`
reopens that SAME world and checks saved chamber levels, chest boats, cargo, and
passengers against `run-checks/lock-restore-state.txt`. Require `PASS:` in
`run-checks/lock-restart-result.txt`.

## Reproduce the actual-rider checks

Copy a stopped test world's folder into
`run-preview/saves/Create Lock QA`. There must be clear space around (0,200,0).
Run `./gradlew -PlockPreview runClient`. This opt-in client fixture creates a
sample lock, mounts the local player, drives a complete round trip, records
`run-preview/client-ride-result.txt`, captures screenshots, and exits.
Require a leading `PASS:` in the result. The fixture deliberately operates only
in this isolated copy. To recapture an overview of its completed world without
repeating the ride, add `-PlockOverview`.

## Packaging

Run `./gradlew clean build` with no fixture properties. Distribute only
`build/libs/LocksAndDams-Forge-1.20.1-0.1.0-dev.3.jar`. The release must exclude
`LockChecks`, `CreateChecks` and `ClientRideChecks`. Tests and run worlds are not shipped.
