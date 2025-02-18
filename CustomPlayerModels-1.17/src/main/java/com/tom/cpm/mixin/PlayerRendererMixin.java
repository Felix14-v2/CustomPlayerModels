package com.tom.cpm.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;

import com.tom.cpm.client.ClientProxy;
import com.tom.cpm.client.ModelTexture;
import com.tom.cpm.shared.config.Player;
import com.tom.cpm.shared.model.TextureSheetType;

@Mixin(value = PlayerRenderer.class, priority = 900)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

	public PlayerRendererMixin(Context p_174289_, PlayerModel<AbstractClientPlayer> p_174290_, float p_174291_) {
		super(p_174289_, p_174290_, p_174291_);
	}

	@Inject(
			at = @At("RETURN"),
			method = {
					"getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;"
			},
			cancellable = true)
	public void onGetEntityTexture(AbstractClientPlayer entity, CallbackInfoReturnable<ResourceLocation> cbi) {
		ClientProxy.mc.getPlayerRenderManager().bindSkin(getModel(), new ModelTexture(cbi), TextureSheetType.SKIN);
	}

	@Redirect(at =
			@At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/player/AbstractClientPlayer;"
							+ "getSkinTextureLocation()Lnet/minecraft/resources/ResourceLocation;"
					),
			method = "renderHand("
					+ "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
					+ "ILnet/minecraft/client/player/AbstractClientPlayer;"
					+ "Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;)V"
			)
	public ResourceLocation getSkinTex(AbstractClientPlayer player) {
		return getTextureLocation(player);
	}

	@Inject(at = @At("HEAD"), method = "renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V")
	public void onRenderRightArmPre(PoseStack matrices, MultiBufferSource vertexConsumers, int light, AbstractClientPlayer player, CallbackInfo cbi) {
		ClientProxy.INSTANCE.renderHand(vertexConsumers);
	}

	@Inject(at = @At("HEAD"), method = "renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V")
	public void onRenderLeftArmPre(PoseStack matrices, MultiBufferSource vertexConsumers, int light, AbstractClientPlayer player, CallbackInfo cbi) {
		ClientProxy.INSTANCE.renderHand(vertexConsumers);
	}

	@Inject(at = @At("RETURN"), method = "renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V")
	public void onRenderRightArmPost(PoseStack matrices, MultiBufferSource vertexConsumers, int light, AbstractClientPlayer player, CallbackInfo cbi) {
		ClientProxy.INSTANCE.manager.unbindClear(getModel());
	}

	@Inject(at = @At("RETURN"), method = "renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V")
	public void onRenderLeftArmPost(PoseStack matrices, MultiBufferSource vertexConsumers, int light, AbstractClientPlayer player, CallbackInfo cbi) {
		ClientProxy.INSTANCE.manager.unbindClear(getModel());
	}

	@Inject(at = @At("HEAD"), method = "renderNameTag(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", cancellable = true)
	public void onRenderName(AbstractClientPlayer entityIn, Component displayNameIn, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, CallbackInfo cbi) {
		if(!Player.isEnableNames())cbi.cancel();
	}

	@Redirect(at =
			@At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/RenderType;entitySolid("
							+ "Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
					),
			method = "renderHand("
					+ "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
					+ "ILnet/minecraft/client/player/AbstractClientPlayer;"
					+ "Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;)V",
					require = 0
			)
	public RenderType getArmLayer(ResourceLocation loc, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, AbstractClientPlayer playerIn, ModelPart rendererArmIn, ModelPart rendererArmwearIn) {
		return ClientProxy.mc.getPlayerRenderManager().isBound(getModel()) ? RenderType.entityTranslucent(getTextureLocation(playerIn)) : RenderType.entitySolid(getTextureLocation(playerIn));
	}
}
