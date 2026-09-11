package com.aschuchter21.locksanddams;

import net.minecraft.core.*;
import net.minecraft.world.phys.AABB;

/** Prototype: five blocks wide, nine long, six high, with a three-block lift. */
public record LockLayout(BlockPos origin, Direction forward) {
    public static final int LOW = 14, HIGH = 62;
    public BlockPos at(int x, int y, int z) { return origin.relative(forward, z).relative(forward.getClockWise(), x).above(y); }
    public BlockPos lowerDrive() { return at(0, 1, 0); }
    public BlockPos upperDrive() { return at(0, 1, 10); }
    public BlockPos fill() { return at(0, 1, 3); }
    public BlockPos drain() { return at(0, 1, 7); }
    public AABB gateBox(int z) { return box(1, 0, z, 5, 5, z); }
    public AABB bounds() { return box(0, -1, 0, 6, 5, 10); }
    public AABB box(int x0, int y0, int z0, int x1, int y1, int z1) {
        BlockPos a = at(x0,y0,z0), b = at(x1,y1,z1);
        return new AABB(Math.min(a.getX(),b.getX()), Math.min(a.getY(),b.getY()), Math.min(a.getZ(),b.getZ()),
            Math.max(a.getX(),b.getX())+1, Math.max(a.getY(),b.getY())+1, Math.max(a.getZ(),b.getZ())+1);
    }
    public static int depth(int units, int y) { return Math.max(0, Math.min(16, units-y*16)); }
}
