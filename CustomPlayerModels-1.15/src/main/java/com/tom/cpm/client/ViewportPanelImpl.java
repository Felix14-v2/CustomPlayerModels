package com.tom.cpm.client;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Quaternion;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel.ArmPose;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import com.tom.cpl.math.Box;
import com.tom.cpl.math.Vec2i;
import com.tom.cpl.util.Image;
import com.tom.cpm.client.PlayerRenderManager.RDH;
import com.tom.cpm.shared.MinecraftClientAccess;
import com.tom.cpm.shared.gui.ViewportPanelBase;
import com.tom.cpm.shared.gui.ViewportPanelBase.ViewportCamera;
import com.tom.cpm.shared.gui.ViewportPanelBase.ViewportPanelNative;

public class ViewportPanelImpl extends ViewportPanelNative {
	private Minecraft mc;
	private FakePlayer playerObj;
	private MatrixStack matrixstack;
	public ViewportPanelImpl(ViewportPanelBase panel) {
		super(panel);
		mc = Minecraft.getInstance();
		playerObj = new FakePlayer();
	}

	@Override
	public void renderSetup() {
		ViewportCamera cam = panel.getCamera();
		float pitch = (float) Math.asin(cam.look.y);
		float yaw = cam.look.getYaw();

		RenderSystem.pushMatrix();
		Box bounds = getBounds();
		Vec2i off = panel.getGui().getOffset();
		RenderSystem.translatef(off.x + bounds.w / 2, off.y + bounds.h / 2, -50);
		RenderSystem.enableDepthTest();
		float scale = cam.camDist;
		RenderSystem.scalef((-scale), scale, 0.1f);
		matrixstack = new MatrixStack();
		matrixstack.scale(1, 1, 1);
		Quaternion quaternion = Vector3f.ZP.rotationDegrees(180.0F);
		Quaternion quaternion1 = Vector3f.XP.rotation(pitch);
		quaternion.multiply(quaternion1);
		matrixstack.rotate(quaternion);
		matrixstack.rotate(Vector3f.YP.rotation(yaw));
		matrixstack.translate(-cam.position.x, -cam.position.y, -cam.position.z);
		RenderSystem.color4f(1, 1, 1, 1);
	}

	@Override
	public void renderFinish() {
		RenderSystem.disableDepthTest();
		RenderSystem.popMatrix();
		matrixstack = null;
	}

	@Override
	public void renderBase() {
		//mc.getTextureManager().bindTexture(new ResourceLocation("cpm", "textures/gui/area.png"));
		//RenderSystem.disableCull();
		//Tessellator tes = Tessellator.getInstance();
		//BufferBuilder t = tes.getBuffer();
		RenderType rt = CustomRenderTypes.getTexCutout(new ResourceLocation("cpm", "textures/gui/area.png"));
		IVertexBuilder t = mc.getRenderTypeBuffers().getBufferSource().getBuffer(rt);
		Matrix4f m = matrixstack.getLast().getMatrix();
		t.pos(m, 4,  0,  4).tex(1, 1).endVertex();
		t.pos(m, 4,  0, -3).tex(0, 1).endVertex();
		t.pos(m, -3, 0, -3).tex(0, 0).endVertex();
		t.pos(m, -3, 0,  4).tex(1, 0).endVertex();
		//RenderSystem.enableCull();
		mc.getRenderTypeBuffers().getBufferSource().finish(rt);

		mc.getTextureManager().bindTexture(new ResourceLocation("cpm", "textures/gui/base.png"));
		Render.drawTexturedCube(matrixstack, 0, -1.001f, 0, 1, 1, 1);
	}

	@Override
	public void render(float partialTicks) {
		PlayerRenderer rp = mc.getRenderManager().getSkinMap().get(panel.getSkinType().getName());
		float scale = 1;//0.0625F
		matrixstack.translate(0.5f, 1.5f, 0.5f);
		matrixstack.rotate(Vector3f.YP.rotationDegrees(90));
		matrixstack.scale((-scale), -scale, scale);
		PlayerModel<AbstractClientPlayerEntity> p = rp.getEntityModel();
		panel.preRender();
		try {
			ClientProxy.mc.getPlayerRenderManager().bindModel(p, mc.getRenderTypeBuffers().getBufferSource(), panel.getDefinition(), null, null);
			CallbackInfoReturnable<ResourceLocation> cbi = new CallbackInfoReturnable<>(null, true);
			cbi.setReturnValue(DefaultPlayerSkin.getDefaultSkin(playerObj.getUniqueID()));
			ClientProxy.mc.getPlayerRenderManager().bindSkin(p, cbi);
			setupModel(p);
			int overlay = OverlayTexture.getPackedUV(OverlayTexture.getU(0), OverlayTexture.getV(false));
			int light = LightTexture.packLight(15, 15);
			RenderType rt = !panel.applyLighting() ? CustomRenderTypes.getEntityTranslucentCullNoLight(cbi.getReturnValue()) : RenderType.getEntityTranslucent(cbi.getReturnValue());
			IVertexBuilder buffer = mc.getRenderTypeBuffers().getBufferSource().getBuffer(rt);
			((RDH)ClientProxy.mc.getPlayerRenderManager().getHolder(p)).defaultType = rt;
			p.setRotationAngles(playerObj, 0, 0, 0, 0, 0);

			if(panel.isTpose()) {
				p.bipedRightArm.rotateAngleZ = (float) Math.toRadians(90);
				p.bipedLeftArm.rotateAngleZ = (float) Math.toRadians(-90);
			}

			float lsa = 0.75f;
			float ls = MinecraftClientAccess.get().getPlayerRenderManager().getAnimationEngine().getTicks() * 0.2f - 1.5f * (1.0F - partialTicks);

			panel.applyRenderPoseForAnim(pose -> {
				switch (pose) {
				case SLEEPING:
					matrixstack.translate(0.0D, 1.501F, 0.0D);
					matrixstack.rotate(Vector3f.ZP.rotationDegrees(-90));
					matrixstack.rotate(Vector3f.YP.rotationDegrees(270.0F));
					break;

				case SNEAKING:
					p.isSneak = true;
					p.setRotationAngles(playerObj, 0, 0, 0, 0, 0);
					break;

				case RIDING:
					p.isSitting = true;
					p.setRotationAngles(playerObj, 0, 0, 0, 0, 0);
					break;
				case CUSTOM:
				case DYING:
				case FALLING:
				case STANDING:
					break;

				case FLYING:
					break;

				case RUNNING:
					p.setRotationAngles(playerObj, ls, 1f, 0, 0, 0);
					break;

				case SWIMMING:
					break;

				case WALKING:
					p.setRotationAngles(playerObj, ls, lsa, 0, 0, 0);
					break;

				case SKULL_RENDER:
					p.setVisible(false);
					p.bipedHead.showModel = true;
					matrixstack.translate(0.0D, 1.501F, 0.0D);
					break;

				default:
					break;
				}
			});

			p.render(matrixstack, buffer, light, overlay, 1, 1, 1, 1);
			mc.getRenderTypeBuffers().getBufferSource().getBuffer(rt);
			mc.getRenderTypeBuffers().getBufferSource().finish();
		} finally {
			ClientProxy.mc.getPlayerRenderManager().unbindModel(p);
		}
	}

	private void setupModel(PlayerModel<AbstractClientPlayerEntity> p) {
		p.isChild = false;
		p.leftArmPose = ArmPose.EMPTY;
		p.rightArmPose = ArmPose.EMPTY;
		p.setVisible(true);
		p.bipedHeadwear.showModel = false;
		p.bipedBodyWear.showModel = false;
		p.bipedLeftLegwear.showModel = false;
		p.bipedRightLegwear.showModel = false;
		p.bipedLeftArmwear.showModel = false;
		p.bipedRightArmwear.showModel = false;
		p.isSneak = false;
		p.swingProgress = 0;
		p.isSitting = false;
	}

	@Override
	public int getColorUnderMouse() {
		FloatBuffer buffer = BufferUtils.createFloatBuffer(3);
		GL11.glReadPixels((int) mc.mouseHelper.getMouseX(), mc.getMainWindow().getFramebufferHeight() - (int) mc.mouseHelper.getMouseY(), 1, 1, GL11.GL_RGB, GL11.GL_FLOAT, buffer);
		int colorUnderMouse = (((int)(buffer.get(0) * 255)) << 16) | (((int)(buffer.get(1) * 255)) << 8) | ((int)(buffer.get(2) * 255));
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		return colorUnderMouse;
	}

	@Override
	public Image takeScreenshot(Vec2i size) {
		GuiImpl gui = (GuiImpl) panel.getGui();
		int dw = mc.getMainWindow().getWidth();
		int dh = mc.getMainWindow().getHeight();
		float multiplierX = dw / (float)gui.width;
		float multiplierY = dh / (float)gui.height;
		int width = (int) (multiplierX * size.x);
		int height = (int) (multiplierY * size.y);
		FloatBuffer buffer = BufferUtils.createFloatBuffer(width * height * 3);
		GL11.glReadPixels((int) (multiplierX * renderPos.x), mc.getMainWindow().getFramebufferHeight() - height - (int) (multiplierY * renderPos.y), width, height, GL11.GL_RGB, GL11.GL_FLOAT, buffer);
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for(int y = 0;y<height;y++) {
			for(int x = 0;x<width;x++) {
				float r = buffer.get((x + y * width) * 3);
				float g = buffer.get((x + y * width) * 3 + 1);
				float b = buffer.get((x + y * width) * 3 + 2);
				int color = 0xff000000 | (((int)(r * 255)) << 16) | (((int)(g * 255)) << 8) | ((int)(b * 255));
				img.setRGB(x, height - y - 1, color);
			}
		}
		BufferedImage rImg = new BufferedImage(size.x, size.y, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = rImg.createGraphics();
		gr.drawImage(img, 0, 0, size.x, size.y, null);
		gr.dispose();
		return new Image(rImg);
	}
}
