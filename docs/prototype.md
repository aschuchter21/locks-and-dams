# First playable lock: design and acceptance criteria

Status: planned. No gameplay features are implemented in the foundation build.

## Scope

- A standalone Forge 47.4.10 mod for Minecraft 1.20.1.
- Player-built rectangular chamber with a floor, walls, two gates, a controller,
  a fill valve, and a drain valve.
- Defined upper and lower water levels and bounded chamber dimensions.
- Controlled chamber water and boat support; ordinary block-by-block flooding
  alone is not sufficient to meet the boat-riding requirement.
- Vanilla boats and chest boats first. Other mods' vessels require separate tests.
- Redstone controls first. Create integration, hydropower, and dam/reservoir
  management are later milestones.

## Redstone contract

Each gate and valve has an independent input. Any nonzero signal requests open;
zero requests closed. Signal strength does not set opening percentage.

- A gate opens only when chamber water matches the water on its side.
- Fill admits water from the upper side; drain releases it toward the lower side.
- Valves may operate only after both gates are confirmed closed.
- If both valves are powered, neither operates and water movement pauses.
- Removing power closes the affected component; removing valve power pauses
  filling/draining at the current level, rather than resetting the chamber.
- Interlocks override an open request. A denied request is re-evaluated while
  power remains present, so automation does not require a new signal edge.

Opening/closing animation, obstruction handling, controller placement, chamber
recognition, and exact water tolerances are implementation decisions to resolve
during the prototype. Never close a gate through a boat or player.

## Engineering approach to evaluate

Use a bounded chamber state with persisted water height and server-authoritative
operations. Synchronize the visible surface and boat movement to clients. Ensure
normal entry/exit works at either canal level. Test this interaction before
investing in detailed gate models or general reservoir simulation.

Validate boundaries before changing world blocks. Invalid or unloaded structures
must stop operation without deleting player construction or forcing chunk loads.
Saving/reloading must preserve chamber level and safely re-evaluate live inputs.

## Acceptance checks for the playable milestone

1. Ride a boat through a full lower-to-upper cycle and the reverse cycle.
2. Repeat with a chest boat and cargo; passengers and contents remain intact.
3. Verify powered/unpowered behavior independently for both gates and valves.
4. Request gate opening with unequal levels; confirm the interlock blocks it.
5. Power both valves; confirm neither moves water, then recover with one input.
6. Remove power partway through filling and draining; resume without a level jump.
7. Obstruct a gate with a boat; verify it cannot crush or trap the occupants.
8. Save/reload and unload/reload the chamber during a cycle; verify safe recovery.
9. Break a chamber wall or component; verify operation stops without destructive
   cleanup or uncontrolled changes to nearby water.
10. Repeat ride-through checks on a dedicated server with a second player watching
    for boat jitter, incorrect water levels, or passenger desynchronization.

A successful compilation is not evidence that these gameplay checks pass.
