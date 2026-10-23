# Large locks — beta.7

- Clear chamber width: 4–32 blocks (up to 34 across recessed hinges).
- Interior length: 3–127 blocks, with additional minimum length for gate swing pockets.
- Hinge rise and water lift: up to 48 blocks.
- Total managed depth and individual gate height: up to 64 blocks.
- Managed chamber volume: at most 131,072 blocks; maximum dimensions cannot all be combined.
- Inline circuits: at most 1,024 blocks and 256 blocks Manhattan reach.

The whole structure and its machinery must remain loaded. These limits do not force-load chunks. Keep the controller and desk within their existing linking range.

Water updates touch only changed layers after initialization, and repeated validation in the same tick is avoided. Full structure validation remains necessary. Large builds have not been performance-tested in Minecraft.

The saved 21-block-rise build is within the new height limits. Other construction rules still apply, so assemble with the controller and follow any subsequent error message.
