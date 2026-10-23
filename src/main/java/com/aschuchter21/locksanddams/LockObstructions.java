package com.aschuchter21.locksanddams;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;

/** Callers restrict these checks to gate paths or catwalk space. */
final class LockObstructions {
    private LockObstructions() {}
    static boolean player(Entity entity) {
        return entity instanceof Player && entity.isAlive() && !entity.isSpectator();
    }
    static boolean blocks(Entity entity) {
        return entity.isAlive() && !entity.isSpectator()
            && (entity instanceof Player || entity instanceof Boat);
    }
}
