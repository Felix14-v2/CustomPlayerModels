package com.tom.cpm.client;

import java.util.OptionalDouble;

import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;

public class CustomRenderTypes extends RenderType {
	public static final RenderType LINES_NO_NEPTH = create("cpm:lines_no_depth", DefaultVertexFormats.POSITION_COLOR, 1, 256, RenderType.State.builder().setLineState(new RenderState.LineState(OptionalDouble.empty())).setLayeringState(VIEW_OFFSET_Z_LAYERING).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setOutputState(ITEM_ENTITY_TARGET).setDepthTestState(NO_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));
	public static final RenderType ENTITY_COLOR = entityTranslucent(new ResourceLocation("forge:textures/white.png"));
	public static final RenderType ENTITY_COLOR_EYES = eyes(new ResourceLocation("forge:textures/white.png"));

	public CustomRenderTypes(String nameIn, VertexFormat formatIn, int drawModeIn, int bufferSizeIn,
			boolean useDelegateIn, boolean needsSortingIn, Runnable setupTaskIn, Runnable clearTaskIn) {
		super(nameIn, formatIn, drawModeIn, bufferSizeIn, useDelegateIn, needsSortingIn, setupTaskIn, clearTaskIn);
	}

	public static RenderType getLinesNoDepth() {
		return LINES_NO_NEPTH;
	}

	public static RenderType getEntityColorTranslucentCull() {
		return ENTITY_COLOR;
	}

	public static RenderType getEntityColorEyes() {
		return ENTITY_COLOR_EYES;
	}

	public static RenderType getEntityTranslucentCullNoLight(ResourceLocation locationIn) {
		RenderType.State rendertype$state = RenderType.State.builder().setTextureState(new RenderState.TextureState(locationIn, false, false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDiffuseLightingState(NO_DIFFUSE_LIGHTING).setAlphaState(DEFAULT_ALPHA).setLightmapState(NO_LIGHTMAP).setOverlayState(NO_OVERLAY).createCompositeState(true);
		return create("cpm:entity_translucent_cull_nolight", DefaultVertexFormats.NEW_ENTITY, 7, 256, true, true, rendertype$state);
	}
}
