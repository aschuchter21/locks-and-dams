# Player-built chambers: dev.7

Minecraft 1.20.1, Forge 47.4.10 and Create 6.0.8. Replace the older Locks & Dams
JAR with dev.7 on both client and server. Existing fixed-size locks remain supported.

## Try the example

Fly into a large area of clear, loaded air and run `/locks demo_custom`.
It builds a 6-wide, 11-long chamber with a four-block lift, paired gates at
different heights, separate culverts, and powered Create shafts. For another
example, use `/locks demo_custom 7 15 5` (width, interior length, lift).
The command refuses to overwrite blocks or entities. `/locks demo` still builds
the older fixed-size example.

These arguments are only for generating examples. A player-built lock gets its
dimensions from the hinges and its water limits from the ports.

## Build your own

1. Build a rectangular chamber floor and side walls. Leave gate openings at both
   ends. The lower gate sits at the lower canal floor; the upper gate sits at
   the raised upper canal floor. Build a solid wall below that raised opening.
2. Put two upward-facing **Lock Hinges** at each gate opening, beneath its first
   and last panel columns. Their tops meet the bottom of their gate panels.
   The two hinges at an end must share a height. The upper pair may be higher
   than the lower pair. Viewed from above, all four hinge positions form a rectangle.
3. Build each gate from two rectangular **Gate Panel** leaves, starting directly
   above the hinges and extending toward the center. No glue is needed. The
   left leaf covers half the width, rounded down; the right gets the remainder.
   For a seven-wide opening, use a three-wide left leaf and four-wide right leaf.
   Build both leaves at an opening to the same top height for a level catwalk.
   Assembly automatically adds the walking deck to their top rows; no separate
   catwalk item is needed. Connect side-wall landings at that deck height.
4. Make both lower leaves tall enough to contain the upper water level. The
   upper leaves need to contain the upper canal above its raised sill. Give
   boats adequate clearance above both canal surfaces. Keep both outward gate
   sweeps clear of blocks, boats and mobs.
5. Put one **Fill Valve** and one **Drain Valve** in either side wall. Connect a
   **Culvert Pipe** directly below each valve, then route separate pipes to one
   **Culvert Port** each. The pipe enters the back of its port.
6. Put the fill port's marked mouth beside source water at the upper pool's
   surface, beyond the upper gate. Put the drain port at the lower pool's
   surface, beyond the lower gate. The space above that water must be free of
   fluid. Submerged or dry mouths are refused. The water across each approach
   must match its port's surface.
7. Place a **Lock Controller** in either side wall, below the upper water limit.
   Your placement direction does not matter; the controller detects the canal axis.
   Keep the valves below that limit too.
   Clear boats and other entities from the chamber, then right-click to assemble.
   The controller reports the detected size and lift, or the part needing repair.

Width includes the first and last gate-panel columns above the hinges. Interior
length is the space between the two gate planes. Side walls sit immediately
outside those columns. The chamber floor lies beneath the first row of lower
gate panels. No corner-selection tool, dimension entry, or manual port linking
is required.

## Power and operate it

Supply Create rotation below **all four hinges**. Fully enclosed Create shafts
can run through the wall below the upper gates; the demo uses Andesite Encased
Shafts with motors beneath the chamber floor. Bare shafts do not count as a
solid wall. Gates move at up to two degrees per tick; 8 RPM gives approximately
5.6 seconds for a quarter turn.

Apply redstone to either hinge in a pair to request that both leaves open.
For accessible controls, put a **Gate Drive** directly against the outward side
of that hinge, then attach a lever to the drive. Removing the signal requests
closed. Gate Drives elsewhere are not automatically linked.

Players can walk across the gate-top catwalk when both leaves are fully closed.
Both leaves pause while a player or mob stands on either leaf's catwalk, then
resume the redstone request once it is clear. Keep the wider top sweep clear
as well as the gate panels below it. Upper and lower gate catwalks may be at
different elevations.

Power the fill valve to raise the water, or the drain valve to lower it. Both
gate pairs must finish closing first. Powering both valves pauses the water.
The port surfaces supply the targets automatically. A full block of lift takes
about four seconds at 20 TPS. Canals remain fixed supplies: finite reservoir
volume is not simulated.

## Limits and repairs

- Opening width: 4-16 blocks; interior length: 3-47 blocks.
- Upper hinge rise and water lift: up to 16 blocks each.
- Chamber water depth and individual leaf height: up to 32 blocks.
- Managed interior volume: at most 16,384 blocks.
- Culvert: at most 256 connected blocks, within 96 Manhattan blocks of its valve.

The controller refuses ambiguous hinge rectangles and overlapping assembled
locks. Keep unrelated hinges away from its detection area. No chunks are forced
to load; missing chamber, gate or culvert chunks pause operation.

Broken walls, missing pipes, dry/moved ports and missing gates pause the lock.
Restore the original structure and connections to resume. Port positions and
water limits are saved at assembly, so relocating a port does not silently
change a running chamber's target.

To move only a custom lock's controller, clear the chamber, remove the old
controller, and fill its hole with a solid, dry wall block. Replace a wall block
at the new valid location with the controller and right-click. It reclaims the
existing gate contraptions and uniform water without resetting their state.
This is refused if the old controller still exists or its chunk is unloaded.
Legacy two-hinge locks still require their original controller location.

To reconfigure the chamber itself, clear the chamber, remove the controller and all four hinges,
then rebuild the shell and panel leaves before replacing them. Removing hinges
returns their panels to the original opening. A replacement controller in the
same location can reclaim uniform managed water if it still fits the
detected limits. Inconsistent or out-of-range water is refused.

See the validation notes for client and server checks. Remote multiplayer
testing remains a separate acceptance check.
