package com.poisoncircle;

import net.minecraft.client.model.geom.builders.LayerDefinition;

/** The Blockbench-authored transport plane geometry, kept separate from renderer state. */
public final class AirbornePlaneModel {
    private AirbornePlaneModel() { }

    public static LayerDefinition createLayer() {
        return AirbornePlaneModelGeometry.createLayer();
    }
}
