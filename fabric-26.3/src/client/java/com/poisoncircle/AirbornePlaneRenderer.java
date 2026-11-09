package com.poisoncircle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/** The shared aircraft entity owns transforms and passenger positions; this class only draws it. */
public final class AirbornePlaneRenderer extends EntityRenderer<AirbornePlaneEntity, AirbornePlaneRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "textures/entity/airborne_plane.png");
    private final ModelPart model;

    public AirbornePlaneRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = AirbornePlaneModel.createLayer().bakeRoot();
        shadowRadius = 7F;
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(AirbornePlaneEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yaw = entity.getYRot(partialTick);
        state.door=entity.doorProgress(partialTick);
    }

    @Override protected AABB getBoundingBoxForCulling(AirbornePlaneEntity entity, float partialTick) {
        return entity.getBoundingBox().inflate(26, 10, 26);
    }

    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.isInvisible) return;
        model.getChild("cargo_door").xRot=-(float)Math.toRadians(100)*state.door;
        pose.pushPose();
        pose.rotateDegrees(Axis.YP, -state.yaw);
        pose.scale(16F, 16F, 16F);
        collector.submitModelPart(model, pose, RenderTypes.entityCutout(TEXTURE), state.lightCoords,
            OverlayTexture.NO_OVERLAY, null, -1, state.outlineColor);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public static final class State extends EntityRenderState { public float yaw,door; }
}
