package com.poisoncircleforge;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = PoisonCircleForge.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PoisonCircleClient {
    private static final Map<String, CircleSyncMessage> CIRCLES = new HashMap<>();
    public static void apply(CircleSyncMessage message) { if (message.active()) CIRCLES.put(message.dimension(), message); else CIRCLES.remove(message.dimension()); XaeroCompatibility.install(); }
    public static CircleSyncMessage current() { Minecraft m=Minecraft.getInstance(); return m.level == null ? null : CIRCLES.get(m.level.dimension().location().toString()); }
    @SubscribeEvent public static void render(RenderLevelStageEvent e) {
        if (e.getStage()!=RenderLevelStageEvent.Stage.AFTER_WEATHER) return; CircleSyncMessage c=current(); if(c==null) return;
        Camera camera=Minecraft.getInstance().gameRenderer.getMainCamera(); Vec3 p=camera.getPosition(); int n=Math.max(96, Math.min(256,(int)(c.radius()*2)));
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.disableCull(); RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b=Tesselator.getInstance().getBuilder(); b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        double pulse=0.55+0.45*Math.sin((Minecraft.getInstance().level.getGameTime()%80)*0.16); for(int i=0;i<n;i++){ double a=i*Math.PI*2/n, q=(i+1)*Math.PI*2/n; double x1=c.x()+Math.cos(a)*c.radius()-p.x,z1=c.z()+Math.sin(a)*c.radius()-p.z,x2=c.x()+Math.cos(q)*c.radius()-p.x,z2=c.z()+Math.sin(q)*c.radius()-p.z; quad(b,x1,z1,x2,z2,pulse,i); }
        BufferUploader.drawWithShader(b.end()); RenderSystem.enableCull(); RenderSystem.disableBlend();
    }
    private static void quad(BufferBuilder b,double x1,double z1,double x2,double z2,double pulse,int segment){ int alpha=(int)(85+95*pulse); int glow=(int)(55+80*pulse); double wave=Math.sin(segment*0.42+Minecraft.getInstance().level.getGameTime()*0.20)*16; b.vertex(x1,-128,z1).color(220,10,10,alpha).endVertex(); b.vertex(x2,-128,z2).color(220,10,10,alpha).endVertex(); b.vertex(x2,512+wave,z2).color(255,glow,35,alpha).endVertex(); b.vertex(x1,512+wave,z1).color(255,glow,35,alpha).endVertex(); }
}
