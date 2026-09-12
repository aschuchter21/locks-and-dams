# Gates and control-desk fixes — dev.9

Replace the previous Locks & Dams JAR with dev.9 on Minecraft 1.20.1 / Forge
47.4.10 / Create 6.0.8. Existing desks, hinges and gate models update in place.

## Placement and repairs

Gate panels take their direction from the panel you attach them to. The first
panel faces your horizontal viewing direction. Adjacent sections now meet
edge-to-edge, and hinges have a solid full-block housing.

To edit an assembled gate, close it first. Mining a panel stops the lock and
returns that leaf to ordinary blocks; the selected piece is removed. Replace
the missing panel, then right-click the controller to assemble again. Reset
the desk's emergency stop and operate a selector to resume. Other commands
do not automatically restart. Removing a hinge also returns its leaf as visible,
closed panels instead of preserving an invisible OPEN state.

The desk now has a sloped selection shape. Aim at the center rotary switch for
FILL/DRAIN, or the red button in front of it for emergency stop. The switch and
button have separate click areas.

## Recessed gates

New `/locks demo_custom` and `/locks demo_desk` builds put the hinge columns
inside the two side walls. When open, the leaves and catwalks sit inside those
wall columns, clear of the navigable channel. The demos include the recesses,
backing walls, floors and relocated canal ports.

For a new player-built recessed lock:

1. Choose the clear channel width (4–16 blocks). Put the two hinge columns in
   the side walls, one block outside each side of that clear channel. For a
   six-block channel, the complete closed gate is eight panels across, including
   its two wall columns.
2. Build rectangular leaves directly above the hinges, reaching the center.
   Split the complete gate width in half, rounding the left leaf down for an
   odd width. The upper hinges may sit above the lower hinges as before.
3. Build the chamber side walls along those same hinge columns. Place the
   controller and valves in either wall between the gate planes.
4. On the canal side of each gate, leave a one-block-wide pocket in each wall
   column. Each pocket must be at least as long as its leaf's width and clear
   through the leaf's full height, including the catwalk. Lower pockets extend
   into the lower approach; upper pockets extend into the upper approach.
5. Build a solid backing wall one block outward from each pocket, including
   behind the hinge column, and a solid floor below it. Keep the gate's sweep
   clear. Put water ports and other hardware beyond the pocket ends.
6. Right-click the controller to discover and assemble the structure. Connect
   the gate rotation and desk as described in the control-desk guide.

Existing locks retain their saved wall positions and water volume. Updating the
JAR does not move their walls or carve recesses. To adopt the recessed layout,
rebuild the structure using the arrangement above or generate a new demo.
The previous layout, with hinges just inside the walls, remains supported.
