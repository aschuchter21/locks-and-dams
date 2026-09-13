# Fill/drain operation — dev.12

The plumbing route is **valve → Culvert Pipe → canal surface Culvert Port**.
Place pipe immediately beneath each valve. Keep the fill and drain networks separate.
At the canal, connect the pipe to the back of the port, with its mouth facing a
source-water block at the canal surface. A port does not belong beneath the valve.

New `/locks demo_desk` structures start at the lower water level with DRAIN selected
and no active command. Click the FILL/DRAIN selector once to request FILL, then
allow 10 seconds of unpaused game time before water starts moving. Switching again
restarts that warning. Existing desks keep their saved selector position.

Right-click the lock controller to see the current state:

- **Fill/Drain warning:** the countdown is running; alarm output is active.
- **Filling/Draining:** the chamber is moving toward the selected target.
- **At upper/lower water level:** the requested target has already been reached.
- **Gate open or moving:** both gate pairs must close before water can move.
- **Idle:** operate the selector to issue a new command, including after a reload or reset.
- **Emergency stop latched:** shift-right-click the desk's red button to reset, then issue a new command.
- **Paused:** repair the reported structure or pipe connection. Port-back errors now include the port coordinates.

To update, close Minecraft, replace the previous Locks & Dams JAR with dev.12,
and restart. Create a new demo in a clear area to test the updated initial selector
position. The update does not rebuild existing plumbing or alter saved worlds.
