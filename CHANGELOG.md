# 1.1.0-beta.15

- Allow Sneak + Use on the E-stop button with an item in either hand, including the linker.
- Show configured Sneak and Use keys in the E-stop hint.
- Keep ordinary desk/linker interactions outside the E-stop button unchanged.

# 1.1.0-beta.14

- Preserve fractional miter-gate angles through Create speed changes, preventing whole-degree snapping from repeatedly restarting the clutch at the closed position.
- Includes the hidden hinge movement-mode control from beta.13.

# 1.1.0-beta.13

- Hide the inherited bearing movement-mode control on lock hinges.
- Keep the internal Never Place mode required for assembled gates.

# 1.1.0-beta.12

- Replace the missing linker appearance with a compact wrench model.
- Reduce gate item sizes in the inventory and both hands.
- Add a craftable Minecraft written-book manual covering construction, power, node linking, operation, and troubleshooting.
- Clarify that the linker must be used on the desk and every gate node.

# 1.1.0-beta.11

- Cache gate vertex lighting for five ticks rather than recomputing it every rendered frame.
- Skip gate obstruction scans when desk interlocks already require the drive to stop.
- Active movement obstruction checks remain enabled.
- No Minecraft session or performance benchmark was run.

# 1.1.0-beta.10

- Protect players and boats in gate paths; catwalk checks remain player-only.
- Ignore other entities and allow players/boats elsewhere in the chamber.

# 1.1.0-beta.9

- Entity obstruction checks now apply only to non-spectator players in gate or catwalk space.
- Remove the whole-chamber entity check during assembly; players elsewhere in the chamber are allowed.
- Fish, all other mobs, boats, minecarts, and dropped items no longer trigger mod gate stops.
- Structural block obstructions and water-level interlocks remain in place.

# 1.1.0-beta.8

- Ignore bats, dropped items, and other non-occupant entities during assembly and gate obstruction checks.
- Continue protecting players, other living mobs, boats, and minecarts.
- Apply the same rule to legacy locks, custom locks, and moving hinges.

# 1.1.0-beta.7

- Expand custom locks to 32 clear width, 127 interior length, 48 lift, and 64 total depth, capped at 131,072 water cells.
- Expand inline plumbing to 1,024 connected blocks and 256 reach.
- Support wider assembled gate tiles and larger demo parameters.
- Update only changed water layers and avoid duplicate validation within a tick.
- No Minecraft session or large-world performance test was run.

# 1.1.0-beta.6

- Fix the gate boundary property that disabled water-face occlusion despite a custom occlusion shape.
- Retain empty physical collision while allowing shared fluid faces to be hidden.
- Preserve empty selection shape, skylight transmission, and non-suffocating behavior.
- Built without launching Minecraft; visual verification remains pending.

# 1.1.0-beta.5

- Match clipped gate water to the Forge water tint and alpha instead of applying an additional hard-coded transparency multiplier.
- Match vanilla top-surface UV shrink, directional shading, normals, and combined current/above-cell light sampling.
- Preserve the fixed translucent buffer ordering from beta.4.
- Visual polish candidate; no Minecraft session was launched.

# 1.1.0-beta.4

- Batch gate water in the fixed translucent buffer so opaque gate leaves render before water, independent of block-entity iteration order.
- Sort custom water patches together instead of flushing separate cells between gate leaves.
- Built without opening Minecraft; the visual correction awaits in-game confirmation.

# 1.1.0-beta.3

- Keep every plank color inside the supported range, fixing repeated black strips from color overflow.
- Suppress ordinary water faces at custom boundary joins without hiding adjoining masonry.
- Match uncovered canal source-water height and extend clipped surfaces to the timber skin.
- Preserve skylight through invisible gate boundary cells.
- Compiled without launching Minecraft; visual behavior still needs confirmation.

# 1.1.0-beta.2

- Sample gate lighting across the leaf instead of using the embedded hinge light for the entire model.
- Use position-based UVs for clipped water surfaces so adjacent triangles share a continuous texture.
- Slightly inset the custom water surface to avoid coplanar seams.
- Built without launching Minecraft; visual result requires in-game confirmation.

# Locks & Dams 1.1.0-beta.1

- Continuous timber-and-steel assembled gate leaves, scaled by measured dimensions.
- Upstream miter closure with a shared one-block inset, including odd chamber widths.
- Create collision tiles and clipped water surfaces aligned with the new geometry.
- Lower gate pockets now face into the chamber; upper pockets face into the upper canal.
- Gate editing restores all four leaves and reserved adjacent water cells.
- Updated custom/desk demos and migration documentation.
- Existing assembled and two-hinge legacy gates retain their previous geometry until rebuilt.

Build and offline geometry checks only; Minecraft was not launched for this beta.

# Locks & Dams 1.0.0

Initial public release for Minecraft 1.20.1, Forge 47.4.10, and Create 6.0.8.

- Player-built rectangular lock chambers with automatically detected water limits.
- Smooth chamber filling/draining and boat support.
- Create-powered swing gates, grated catwalks, and handrails.
- Flanged culvert piping, two-port fill/drain circuits, and inline valves.
- Linked industrial control desk, gate control nodes, status indicators, and latched emergency stop.
- Ten-second pre-start warning output and delayed departure-horn output.
- Optimized desk geometry, corrected cabinet faces, and a clean selection outline.
- Configured Sneak + Use key support for gate-node settings.

This release packages the completed dev.17 functionality as 1.0.0. No gameplay changes were made for the release version. Install on both client and server alongside Create. Replace any earlier Locks & Dams JAR rather than keeping both.

Canals remain fixed water supplies; finite reservoirs and dam simulation are not included.
