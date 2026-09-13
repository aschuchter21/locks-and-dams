# Approved control desk - dev.15

The approved Blender control desk now supplies the in-game model and inventory
item. It retains the three-part layout and uses the existing block states for
selectors, indicator lamps and flashing emergency-stop indication. Water lamps
read WARN, FLOW, CLOSED from left to right. The gate headings and emergency-stop
label remain inside their plates.

No desk interaction, collision, linking, timing or redstone logic changed.
The dev.14 Sneak + Use settings fix is included. Install by closing Minecraft,
replacing the earlier Locks & Dams JAR, and restarting. Existing desks receive
the new appearance automatically.

The build was compiled and packaged; in-game tests were skipped at the user's
request. The editable source is tools/approved-art/Industrial-Control-Desk.blend;
tools/export_approved_desk.py exports the static sections and state-driven parts.
