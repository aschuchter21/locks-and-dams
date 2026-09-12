# Industrial lock control desk — dev.8

For Minecraft 1.20.1, Forge 47.4.10 and Create 6.0.8.

## Try the complete wired example

Install the dev.9 JAR in place of the older Locks & Dams JAR, then run
`/locks demo_desk` in clear air in a creative test world. The builder refuses
occupied space. It creates a custom lock, linked three-block desk, and real
Create motors, clutches and gearshifts. The supplied motors run at +8 RPM.

## Build and link a desk

1. Assemble your lock by right-clicking its controller.
2. Place a Lock Control Desk; it occupies the selected block and one block on
   either side. Its sloped face points toward you. Leave all three spaces clear.
3. Use a Control Link Tool on the controller, then on any desk section.
   Both must be loaded, in the same dimension, within 128 blocks by Manhattan
   distance. Each controller supports one desk.
4. Place Gate Control Nodes where the wiring is accessible. Shift-right-click
   a node to choose UPPER or LOWER. Use the same link tool on each node.
5. Supply Create rotation to every hinge, through a clutch and gearshift.
   Wire the node's **front CLUTCH** contact to the clutch; wire its **top
   REVERSE** contact independently to the gearshift. A node can control both
   hinges of its gate pair. The demo repeats a node at each hinge for short wiring.

These contacts output ordinary redstone. Use redstone dust, repeaters, or other
mods' redstone connectors. Direct attachment of Immersive Engineering wire coils
to these blocks is not implemented; use that mod's redstone connectors.

| Contact | Signal 0 | Signal 15 |
| --- | --- | --- |
| CLUTCH (the RUN control) | Rotation connected: run | Rotation disconnected: stop |
| REVERSE (DIRECTION) | Positive hinge RPM: open | Negative hinge RPM: close |

Create clutches stop when powered, so the RUN control has inverted electrical
polarity. Feed positive rotation into each gearshift when its reverse signal is
off. Check the actual RPM at each hinge: **positive opens, negative closes, zero
holds**. The two leaves swing opposite ways using the same RPM sign convention.
The old Gate Drive is no longer needed; its registration remains for saved worlds.

The controller also exposes these four outputs directly on its labeled horizontal
faces. Relative to its facing direction: upper CLUTCH on the facing side, upper
REVERSE clockwise, lower CLUTCH opposite, lower REVERSE counterclockwise.
Mountable nodes mirror those channels and make them accessible outside a wall.

## Operate the desk

Viewed from the front, the left selector controls the upper gate, the right
selector controls the lower gate, and the center selector chooses **FILL / DRAIN**.
Right-click a gate section to turn its selector. On the center section, click
the rotary switch for water selection and the front red button for emergency stop.
There is no OFF position on the water selector.

Every water selection starts a fresh **10-second warning** (200 simulation ticks).
Switching again restarts that warning. Both gates must be closed before water
can move. Once selected, filling or draining stops automatically at the target
water level. A gate opens only when the chamber matches its canal level.

| Indicator | Meaning |
| --- | --- |
| Gate green | Fully closed |
| Gate red | Fully open |
| Gate flashing yellow | Moving |
| Gate steady yellow | Stopped between endpoints |
| Water green | Valves closed |
| Water red + flashing yellow | Water moving |
| Water flashing yellow | Pre-start warning |
| E-stop flashing red | Emergency stop latched |

The **ALARM end** is at the upper-gate end of the desk; it outputs signal 15
throughout the warning. The controller's **bottom** mirrors that warning signal.
The opposite **HORN end** produces a **2-second pulse**, starting **4 seconds
after a commanded gate fully opens**. Closing that gate or pressing E-stop
cancels its pending horn. Attach your preferred alarm or horn from another mod.
The desk itself does not play an alarm sound.

Pressing the red button immediately holds gates and water and cancels timers.
**Shift-right-click the red button to reset.** Resetting does not restart old
commands: operate a selector again. Its physical position is retained, so you
may need to turn it away and back to repeat the same operation.

Breaking any desk section removes the whole desk, unlinks it, and stops the
controller. Shift-right-click a linked desk with the link tool to unlink it.
Relinking starts idle. Reloading a world also starts with idle commands and
preserves a latched E-stop. Timings use game ticks and stretch when the server lags.

## Existing locks

Water, dimensions, gates and catwalks are retained. Previously assembled locks
load stopped on their first dev.8 upgrade because rotation now selects direction.
Link a desk, or shift-right-click an unlinked controller to acknowledge the stop
after arranging the new rotation controls. Positive RPM opens; negative closes.
Without a desk, fill/drain valves retain their direct redstone operation and do
not use the desk's warning timer. `/locks demo_custom` provides this manual setup.

Keep the controller, desk, nodes and machinery loaded together. Missing desk or
structure faults cancel commands; nodes default to powered STOP when their
controller is unavailable. Catwalk and obstruction interlocks remain active.
