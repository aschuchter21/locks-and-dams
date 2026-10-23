# Timber miter gates — 1.1.0-beta.1

Build each gate leaf from the existing Gate Panel blocks. Assemble the lock with its controller (linking a desk to an unassembled valid controller also assembles it). Each rectangular leaf becomes one continuous timber-and-steel structure with a whole-leaf frame, diagonal braces, brass fasteners, grated catwalk, and handrails. The geometry is generated for the measured leaf dimensions; hardware and rail thickness remain fixed.

Both gate pairs form a shallow V when closed. The lower V points into the chamber; the upper V points into the upper canal. The meeting point is one block toward high water from the hinge line. Unequal leaf widths on odd-width chambers meet at the same point.

Positive hinge rotation still opens; negative closes. Both new gate pairs swing toward high water: the lower pair into chamber-side pockets, the upper pair into upper-canal pockets. The existing desk controls, warning/horn timing, and emergency stop are unchanged.

## Try the new layout

Use `/locks demo_desk` in a large clear area of a creative test world. The new demo includes the appropriate pockets and places the controller and chamber ports beyond the lower gate sweep. `/locks demo_custom` also uses the new gates.

The custom demo's minimum interior length is half the total hinge-to-hinge block width, rounded up, plus two blocks, to provide room for its controller and ports. Player-built chambers need clearance for the leaf length and must keep controllers and chamber ports out of the lower gate pockets.

## Existing builds

Existing saved flat gate assemblies retain their old appearance and motion until rebuilt. To convert a custom four-hinge lock:

1. Stop the structure and clear boats/players from its gates.
2. Break a Gate Panel at the reserved gate opening. Entering gate edit mode restores all four leaves to individual building blocks. Replace the one piece you removed.
3. Prepare the lower gate recesses on the chamber side. Each pocket needs a solid floor and backing wall, with space for its leaf and handrails. Move any controller or chamber ports in that sweep beyond the pockets, and repair/reconnect the structure as needed.
4. Keep the upper pockets on the upper-canal side. Leave one water/air row immediately on the high-water side of each gate for its angled water boundary.
5. Use the controller to reassemble and relink a moved controller/desk if applicable.

Editing a gate restores its original building pieces and the water cells reserved around the angled boundary. Old two-hinge legacy demo structures keep the old gate system; use a four-hinge custom lock for the new design.

## Validation

The mod was compiled and packaged without opening Minecraft. An offline mathematical audit covered 120 leaf configurations across widths 4–18 and all four headings, checking the meeting point, opening clearance, and catwalk bounds. In-game rendering, boat passage, collision, save/reload, and upgrade behavior have not been exercised for this beta. Keep the completed 1.0.0 release kit separate until you have checked this build.
