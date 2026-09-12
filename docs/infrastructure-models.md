# Industrial infrastructure models — dev.7

Install the dev.7 mod JAR on client and server, replacing the older Locks & Dams
JAR. Minecraft 1.20.1, Forge 47.4.10 and Create 6.0.8 remain the dependencies.
The models and textures are included in the mod; no extra resource pack is needed.

## Blocks

- Gate Panels: timber infill, dark steel frames and brass fasteners.
- Gate top row: a grated deck with brass edge strips and support brackets.
- Lock Hinges: bolted steel bases, brass bearing collars and Create's animated
  bearing plate and shaft connection.
- Fill Valves: teal cabinets, front-mounted brass wheels, gauges and status lamps.
- Drain Valves: amber cabinets with matching wheels, gauges and status lamps.
- Gate Drives: steel control cabinets in the same visual style.
- Culvert Pipes: copper modules with flanged branches that follow adjacent pipes,
  the underside of a valve, and the back of a port.
- Upper intake: a screened mouth with a teal inward-flow marker.
- Lower outlet: horizontal louvres and an amber outward-flow marker.

Inputs and outputs use the existing Culvert Port item. In a custom lock, a valid
connection to the fill valve marks the intake; a valid drain route marks the
outlet. An unconnected port retains a neutral brass marking. Valve lamps indicate
the actual open/closed state after the lock's interlocks, rather than just the
presence of a redstone signal. Pipe modules retain their existing full-block
sealed collision envelope, so existing walls containing culverts remain valid.

## Walking across the gates

Build both leaves of an opening to the same top height, then assemble normally.
The top row becomes the catwalk automatically. Its walking surface is 10/16 of
a block wide and scales with the gate width; small end clearances let it swing
past the walls and meet the other leaf. The deck travels with the Create gate.
Connect your side-wall landings at the top of the gate and leave headroom.
The upper and lower gates can have different overall elevations.
New custom demos include stepped wall landings at both ends of each crossing.

Cross when both leaves are fully closed. A player or mob on either catwalk
pauses both leaves until the crossing is clear. This is a walkway with edge
strips, not a fenced bridge: its sides have no full-height guardrails.

Existing saved gate entities receive their top-row model and collision states
when their hinges and contraptions load. Their angle, ownership, water level
and entity identity are retained. No gate reconstruction is needed for the art
update. Loose, unassembled Gate Panels acquire the catwalk on assembly.

## Editing the models

The native Minecraft Java block-model JSON files are in
`src/main/resources/assets/locksanddams/models/block`, with original 32-pixel
materials in the adjacent `textures/block` directory. They can be imported as
Java block/item models in a model editor such as Blockbench. Models are ordinary
cuboid geometry, so the mod does not need a custom model loader.

`python tools/model_infrastructure.py` regenerates the current models, materials,
item models and blockstates. Run it after the older `create_resources.py` if
regenerating the prototype's historical resources. The optional contact-sheet
renderer is `tools/preview_infrastructure.py` (Pillow and NumPy). Screenshots
from the actual game remain separate from the geometry contact sheet.
