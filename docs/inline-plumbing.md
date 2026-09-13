# Two-port plumbing - dev.13

Each circuit uses two Culvert Ports, Culvert Pipe, and one Inline Culvert Valve.

- **Fill:** upper canal intake -> pipe -> inline valve -> pipe -> chamber outlet.
- **Drain:** chamber intake -> pipe -> inline valve -> pipe -> lower canal outlet.

Place both chamber ports in the long walls between the gates, with their mouths
facing into the chamber. Keep the drain intake below the lowest water surface;
the fill outlet must be below the upper water surface. Place the canal ports at
the source-water surfaces beyond their respective gates. Their heights still set
the automatic upper and lower water limits. Pipe connects behind each port.

Insert the valve in any straight horizontal or vertical section of its circuit.
Place it against the end of an existing pipe so its flange axis lines up. Elbows
and vertical runs can go before or after it. Both valve flanges must connect.
Keep the circuits separate, use one valve in each, and do not create a bypass
around a valve. Each circuit supports up to 256 connected blocks and a 96-block
Manhattan reach from its chamber port.

Right-click the controller to assemble. It identifies the fill and drain circuits
from their canal endpoints, assigns intake/outlet port appearances, and links the
inline valves to the desk. The existing 10-second warning, gate interlocks,
emergency stop and horn timing still apply. Without a desk, power the inline
valves with redstone. Reassemble if you relocate a linked valve or port.

`/locks demo_desk` and `/locks demo_custom` now build this layout. Existing wall-valve
builds remain supported. To convert an old build, replace its wall valves with
chamber ports, connect pipe to their backs, and insert one Inline Culvert Valve in
each circuit. Keep the drain chamber intake low enough to remain submerged at the
lower target. Restore any old pipe openings through the wall with solid blocks,
then reassemble the controller. The update does not rebuild existing structures.

## Gate placement

Stand on the intended canal side when placing Gate Panels. The smooth watertight
face points toward you; the braces face away. Place all panels of a leaf from the
same side so their skins line up. Orientation is retained when the gate becomes a
Create contraption and when it is taken apart for editing. Gate swing direction
continues to be determined by the hinge geometry.
