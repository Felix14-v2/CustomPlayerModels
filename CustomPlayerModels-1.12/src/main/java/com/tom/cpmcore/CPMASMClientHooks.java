package com.tom.cpmcore;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.client.renderer.entity.layers.LayerElytra;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.util.ResourceLocation;

import com.mojang.authlib.GameProfile;

import com.tom.cpl.util.ItemSlot;
import com.tom.cpm.client.ClientProxy;
import com.tom.cpm.client.PlayerRenderManager;
import com.tom.cpm.shared.MinecraftObjectHolder;
import com.tom.cpm.shared.config.Player;
import com.tom.cpm.shared.definition.ModelDefinition;
import com.tom.cpm.shared.model.RootModelType;
import com.tom.cpm.shared.model.TextureSheetType;
import com.tom.cpm.shared.model.render.ItemTransform;
import com.tom.cpm.shared.network.NetH;

import io.netty.buffer.ByteBufInputStream;

public class CPMASMClientHooks {
	public static void renderSkull(ModelBase skullModel, GameProfile profile) {
		if(profile != null) {
			ClientProxy.INSTANCE.renderSkull(skullModel, profile);
		}
	}

	public static void renderSkullPost(ModelBase skullModel, GameProfile profile) {
		if(profile != null) {
			ClientProxy.INSTANCE.manager.unbind(skullModel);
		}
	}

	public static void renderArmor(ModelBase in, Entity entityIn, float p_78088_2_, float p_78088_3_, float p_78088_4_, float p_78088_5_, float p_78088_6_, float scale, RenderLivingBase<?> renderer) {
		if(in instanceof ModelBiped && renderer instanceof RenderPlayer) {
			ModelBiped player = (ModelBiped) renderer.getMainModel();
			PlayerRenderManager m = ClientProxy.mc.getPlayerRenderManager();
			ModelBiped armor = (ModelBiped) in;
			m.copyModelForArmor(player.bipedBody, armor.bipedBody);
			m.copyModelForArmor(player.bipedHead, armor.bipedHead);
			m.copyModelForArmor(player.bipedLeftArm, armor.bipedLeftArm);
			m.copyModelForArmor(player.bipedLeftLeg, armor.bipedLeftLeg);
			m.copyModelForArmor(player.bipedRightArm, armor.bipedRightArm);
			m.copyModelForArmor(player.bipedRightLeg, armor.bipedRightLeg);
			CPMClientAccess.setNoSetup(armor, true);
		}
		in.render(entityIn, p_78088_2_, p_78088_3_, p_78088_4_, p_78088_5_, p_78088_6_, scale);
	}

	public static void postRenderSkull(ModelRenderer r, float scale, RenderPlayer rpe) {
		if(rpe != null)
			rpe.getMainModel().bipedHead.postRender(scale);
		else
			r.postRender(scale);
	}

	public static void onLogout() {
		ClientProxy.INSTANCE.onLogout();
	}

	public static boolean onClientPacket(SPacketCustomPayload pckt, NetHandlerPlayClient handler) {
		if(pckt.getChannelName().startsWith(MinecraftObjectHolder.NETWORK_ID)) {
			ClientProxy.INSTANCE.netHandler.receiveClient(new ResourceLocation(pckt.getChannelName()), new ByteBufInputStream(pckt.getBufferData()), (NetH) handler);
			return true;
		}
		return false;
	}

	public static void onArmorPre(LayerArmorBase this0, EntityLivingBase entitylivingbaseIn) {
		if(entitylivingbaseIn instanceof AbstractClientPlayer) {
			ClientProxy.INSTANCE.renderArmor(this0.modelArmor, this0.modelLeggings, (EntityPlayer) entitylivingbaseIn);
		}
	}

	public static void onArmorPost(LayerArmorBase this0, EntityLivingBase entitylivingbaseIn) {
		if(entitylivingbaseIn instanceof AbstractClientPlayer) {
			ClientProxy.INSTANCE.manager.unbind(this0.modelArmor);
			ClientProxy.INSTANCE.manager.unbind(this0.modelLeggings);
		}
	}

	public static void onElytraPre(LayerElytra this0, EntityLivingBase entitylivingbaseIn) {
		if(entitylivingbaseIn instanceof AbstractClientPlayer)ClientProxy.INSTANCE.renderElytra((EntityPlayer) entitylivingbaseIn, this0.modelElytra);
	}

	public static void onElytraPost(LayerElytra this0, EntityLivingBase entitylivingbaseIn) {
		if(entitylivingbaseIn instanceof AbstractClientPlayer)ClientProxy.INSTANCE.manager.unbind(this0.modelElytra);
	}

	public static boolean renderCape(LayerCape this0, AbstractClientPlayer player, float partialTicks) {
		Player<?, ?> pl = ClientProxy.INSTANCE.manager.getBoundPlayer();
		if(pl != null) {
			ModelDefinition def = pl.getModelDefinition();
			if(def != null && def.hasRoot(RootModelType.CAPE)) {
				ItemStack chestplate = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
				if(!player.isInvisible() && player.isWearing(EnumPlayerModelParts.CAPE) && chestplate.getItem() != Items.ELYTRA) {
					ModelPlayer model = this0.playerRenderer.getMainModel();
					ClientProxy.mc.getPlayerRenderManager().rebindModel(model);
					ClientProxy.INSTANCE.manager.bindSkin(model, TextureSheetType.CAPE);
					ClientProxy.renderCape(player, partialTicks, model, def);
				}
				return true;
			}
		}
		return false;
	}

	public static boolean renderCape(LayerCape this0, EntityLivingBase entitylivingbaseIn, float partialTicks) {
		return renderCape(this0, (AbstractClientPlayer) entitylivingbaseIn, partialTicks);
	}

	public static void onHandRightPre(RenderPlayer this0, AbstractClientPlayer player) {
		ClientProxy.INSTANCE.manager.bindHand(player, null);
		ClientProxy.INSTANCE.manager.bindSkin(TextureSheetType.SKIN);
	}

	public static void onHandRightPost(RenderPlayer this0, AbstractClientPlayer player) {
		ClientProxy.INSTANCE.manager.unbindClear();
	}

	public static void onHandLeftPre(RenderPlayer this0, AbstractClientPlayer player) {
		ClientProxy.INSTANCE.manager.bindHand(player, null);
		ClientProxy.INSTANCE.manager.bindSkin(TextureSheetType.SKIN);
	}

	public static void onHandLeftPost(RenderPlayer this0, AbstractClientPlayer player) {
		ClientProxy.INSTANCE.manager.unbindClear();
	}

	public static void onRenderParrot(EntityPlayer entitylivingbaseIn, boolean leftShoulderIn) {
		Player pl = ClientProxy.INSTANCE.manager.getBoundPlayer();
		if(pl != null) {
			ModelDefinition def = pl.getModelDefinition();
			if(def != null) {
				ItemTransform tr = def.getTransform(leftShoulderIn ? ItemSlot.LEFT_SHOULDER : ItemSlot.RIGHT_SHOULDER);
				if(tr != null) {
					PlayerRenderManager.multiplyStacks(tr.getMatrix());
					if(entitylivingbaseIn.isSneaking())
						GlStateManager.translate(0, -0.2f, 0);
				}
			}
		}
	}
}
