# Building and operating the prototype

## Quick start

In a creative test world with commands enabled, fly to an open area and run
`/locks demo`. It creates an assembled lock and two pools. Its footprint is
9 blocks wide, 23 long, and 8 high. The controller is six blocks ahead and
four above your position. The whole footprint must be loaded and empty.

Place a boat or chest boat in either pool. The lock starts at the lower level.
To enter from above, first fill the empty chamber, then open the upper gate.

## Redstone controls

Any nonzero signal requests **open**; no signal requests **closed**. Inputs are
sampled every five ticks. A full lift takes about 12 seconds at 20 TPS.

From the lower end of the demo walkway:

| Lever | Block | Action |
|---|---|---|
| 1 | Gate Drive | Lower gate |
| 2 | Fill Valve (green copper) | Raise water |
| 3 | Drain Valve (orange copper) | Lower water |
| 4 | Gate Drive | Upper gate |

A control block's top turns green when it is open. Right-click a control to read
its signal and open state. Right-click the controller for level and fault details.

**Going up:** open lower gate, row fully inside, close lower gate, open fill,
wait for upper level, open upper gate, row out. Close fill when finished.
**Going down:** enter through the upper gate at upper level, close it, open drain,
wait for lower level, then open the lower gate.

Both gates must be closed for either valve to operate. Gates cannot open at
unequal levels. Powering both valves closes both and pauses water movement.
Held gate signals are re-evaluated when levels match. A gate stays open if an
entity obstructs it. A valve may stay open at its target without moving more
water. Signal strength does not control speed.

## Build a standard chamber

The prototype has a fixed 5 by 9 interior and three-block lift, in any horizontal
orientation. The controller faces from the lower canal toward the upper canal,
in the direction you face when placing it. It occupies the lower-left gate jamb,
directly above the floor.

Coordinates are relative to the controller at (0, 0, 0): x goes right, z goes
forward toward the upper canal, and y goes up. Ranges below are inclusive.

| Part | Locations |
|---|---|
| Floor | x 0-6, z 0-10, y -1 |
| Side walls | x 0 and 6, z 0-10, y 0-5 |
| Lower gate panels | x 1-5, z 0, y 0-5 |
| Upper gate panels | x 1-5, z 10, y 0-5 |
| Lower gate drive | (0, 1, 0), replacing that wall block |
| Upper gate drive | (0, 1, 10), replacing that wall block |
| Fill valve | (0, 1, 3), replacing that wall block |
| Drain valve | (0, 1, 7), replacing that wall block |
| Lower canal sources | x 1-5, z -1, y 0 |
| Upper canal sources | x 1-5, z 11, y 3 |

Use full, dry blocks for the floor and walls. Stone bricks and full glass blocks
work; slabs, waterlogged blocks, and inventory blocks do not. Keep the interior
(x 1-5, z 1-9, y 0-5) clear. Provide canal length and headroom beyond both gates.
The upper approach floor normally ends at y 2. Leave the space above both canal
surfaces free of fluid. There is no roof.

The shell needs 60 panels, two drives, one fill valve, one drain valve, one
controller, and 204 ordinary full blocks, plus the approach canals. Right-click
the controller to assemble; it reports invalid parts. Keep boats and entities
out during initial assembly, which sets the water to the lower level.

Panels form a single opening controlled by their drive. They do not operate
individually from redstone. Open panels remain invisible, non-colliding parts
of the structure; closing restores the gate.

## Interruptions and repairs

Removing valve power holds the current level. Breaking a wall, floor, gate, or
required control, or losing a canal surface, pauses water changes. Repair the
problem and the controller resumes according to live inputs. Managed water does
not spill through a breach: this is not a flooding simulation.

The controller saves water level and gate state. Removing it leaves water and
panels in place. Replacing it at the same location and facing, then right-clicking,
can reclaim complete, uniform managed water without resetting its level. Clear
both gate openings first. Inconsistent water is refused rather than rewritten.

Managed water has no bucket, does not spread or create sources, and cannot be
harvested as ordinary water. When dismantling a prototype, first remove its
controller and use `/fill x1 y1 z1 x2 y2 z2 air replace locksanddams:chamber_water`
over its interior. Overlapping assembled locks cannot share gates or water ownership.

## Limits

Canals act as fixed supplies; their volume is not consumed. Dams, reservoirs,
spillways, hydropower, adjustable dimensions, animations, and Create integration
are not included. Other mods' boats and shader/fluid-rendering replacements need
separate compatibility tests.
