package com.poisoncircle.mixin;

import com.poisoncircle.CircleSyncPayload;
import com.poisoncircle.PoisonCircleClient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Xaero otherwise inherits Screen's singleplayer pause, stopping authoritative circle ticks. */
@Pseudo
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
abstract class XaeroWorldMapScreenMixin extends Screen {
    protected XaeroWorldMapScreenMixin(Component title) {
        super(title);
    }

    @Override
    public boolean isPauseScreen() {
        return !PoisonCircleClient.hasActiveCircle() && super.isPauseScreen();
    }
}
