package com.poisoncircle;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = PoisonCircleMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PoisonCircleEntityClientEvents {
    private PoisonCircleEntityClientEvents() { }
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) { event.registerEntityRenderer(PoisonCircleMod.POISON_CIRCLE_VISUAL.get(), PoisonCircleVisualRenderer::new); }
}
