package com.aschuchter21.locksanddams;

import net.minecraft.client.renderer.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid=LocksAndDams.MOD_ID, bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Content.HINGE_ENTITY.get(), com.simibubi.create.content.contraptions.bearing.BearingRenderer::new);
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(Content.WATER.get(), RenderType.translucent()));
    }
}
