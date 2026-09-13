# Approved gates and piping — dev.11

The approved Blender design is now adapted to player-built modular gates and
connected culvert blocks. Gate sections have a steel skin, structural girders,
diagonal braces and fasteners. Only the top row carries open metal grating,
yellow handrails and toe plates. The walkway remains continuous across a closed
gate pair. Handrails have collision and the existing occupied-catwalk interlock
continues to stop gate movement.

Culverts choose a rounded fitting from their actual connections: straight runs,
swept elbows, tees and junctions, including vertical branches. The ends carry
bolted flanges. Intake, outlet, valve, hinge and desk designs remain unchanged.

## Install and try

Replace the previous Locks & Dams JAR with dev.11, using Minecraft 1.20.1,
Forge 47.4.10 and Create 6.0.8. Run `/locks demo_desk` in clear space for a wired
example, or `/locks demo_custom` for the manually operated version.

Gate opening keeps the existing approach-wall recess direction, matching the
corrected review preview. Positive hinge RPM opens; negative closes.

Existing gates receive the new models automatically. Clear **two blocks above
the catwalk** throughout the swing path and wall pocket to accommodate the
handrails. Movement stops against obstructions; the mod does not carve existing
player walls. Newly generated demos include this clearance. An existing demo
may need stairs above the approach pocket removed.

## Editable source

`tools/approved-art/Lock-Gates-and-Piping.blend` preserves the approved design.
`tools/approved-art/design.json` records its original components and materials.
Run Blender in background mode with `tools/export_approved_art.py` to regenerate
the game-sized OBJ meshes and models. These meshes use Forge's OBJ loader and
Create's ordinary baked contraption renderer; no additional rendering mod is
required. The game meshes repeat structurally across arbitrary gate sizes;
they do not stretch a fixed gate image.

Run this exporter after the older infrastructure resource generator, which
contains the superseded gate and pipe models.

## Validation — September 12, 2026

- Real client PASS: crossed the center seam of the closed gate pair; walked
  against the handrail without leaving the catwalk. Gate and pipe screenshots
  were inspected after loading the new OBJ models.
- Dedicated server PASS: four orientations with real Create clutches and
  gearshifts, full fill/drain cycles, emergency stop, warning restart, alarm
  output through the desk support blocks, departure delay and horn pulse.
- Recess checks PASS: clear chamber widths 4, 7, 10 and 16, opening and closing
  in four orientations with the gates ending parallel to the pocket walls.
- Model output covers all 64 pipe connection combinations. Test fixtures are
  opt-in and excluded from the normal clean distribution build.
