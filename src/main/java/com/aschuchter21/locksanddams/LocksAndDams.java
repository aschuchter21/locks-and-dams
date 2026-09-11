package com.aschuchter21.locksanddams;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Entry point for the standalone Forge mod. */
@Mod(LocksAndDams.MOD_ID)
public final class LocksAndDams {
    public static final String MOD_ID = "locksanddams";
    private static final Logger LOGGER = LogUtils.getLogger();

    public LocksAndDams() {
        LOGGER.info("Locks & Dams foundation loaded");
    }
}
