# Gate settings, water contact and desk wiring — dev.10

Replace the older Locks & Dams JAR with dev.10. Minecraft 1.20.1, Forge 47.4.10
and Create 6.0.8 remain the supported test combination.

## Gate Control Node settings

Shift-right-click the separate Gate Control Node with an empty hand. A settings
window opens with Upper gate and Lower gate buttons. Changes apply immediately;
the window also explains the front CLUTCH and top REVERSE outputs.

## Water against closed gates

Closed gate cells now render water up to each face of the thin gate. The chamber
side follows the moving chamber level; the canal side retains its own level.
The visual water does not connect the two fluid volumes through the closed gate.
Existing assembled gates receive the visual state from their controller.

## Wiring under the desk

The end beneath the upper-gate selector outputs ALARM throughout the 10-second
fill/drain warning. The end beneath the lower-gate selector outputs HORN for
2 seconds, starting 4 seconds after a commanded gate fully opens.

Both ends now strongly power a conducting full block directly beneath them.
Redstone dust in the space below that support block, resting on another block,
receives the signal. Stone works; glass and slabs are not equivalent. The center
desk section does not output either signal. The existing side contacts remain
available. Emergency stop cancels both outputs.

## Verification fixtures

Verified on September 12, 2026: client fixture PASS at 19:14:58 and server
fixture PASS at 19:29:59. The closed upper/lower water previews and settings
window were also inspected visually.

The opt-in `-PdeskPreview runClient` fixture opens the node menu and sends both
selection button packets, then verifies the server's selected gate. It also
captures both closed gate water faces and exercises the desk controls, gate
editing and desk placement/linking.

The opt-in `-PdeskChecks runServer` fixture runs four orientations with real
Create clutches and gearshifts. It checks actual redstone dust below the desk
supports, warning and horn timing, full fill/drain cycles, emergency stops and
closed-gate water depths. Require PASS in each fixture's result file; Gradle's
exit code alone does not establish that the game assertions passed.

Package with `clean build` without fixture flags so test code is not shipped.
