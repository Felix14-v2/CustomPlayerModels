package com.tom.cpm.client;

import java.util.Map.Entry;
import java.util.concurrent.Executor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCustomizeSkin;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelElytra;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ChatType;

import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.PlayerSPPushOutOfBlocksEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import com.google.common.collect.Iterables;

import com.tom.cpm.CommonProxy;
import com.tom.cpm.shared.config.ConfigKeys;
import com.tom.cpm.shared.config.ModConfig;
import com.tom.cpm.shared.config.Player;
import com.tom.cpm.shared.definition.ModelDefinition;
import com.tom.cpm.shared.editor.gui.EditorGui;
import com.tom.cpm.shared.gui.GestureGui;
import com.tom.cpm.shared.model.RenderManager;
import com.tom.cpm.shared.model.TextureSheetType;
import com.tom.cpm.shared.network.NetHandler;

import io.netty.buffer.Unpooled;

public class ClientProxy extends CommonProxy {
	public static final ResourceLocation DEFAULT_CAPE = new ResourceLocation("cpm:textures/template/cape.png");
	public static MinecraftObject mc;
	private Minecraft minecraft;
	public static ClientProxy INSTANCE;
	public RenderManager<GameProfile, EntityPlayer, ModelBase, Void> manager;
	public NetHandler<ResourceLocation, EntityPlayer, NetHandlerPlayClient> netHandler;

	@Override
	public void init() {
		super.init();
		INSTANCE = this;
		minecraft = Minecraft.getMinecraft();
		mc = new MinecraftObject(minecraft);
		MinecraftForge.EVENT_BUS.register(this);
		KeyBindings.init();
		manager = new RenderManager<>(mc.getPlayerRenderManager(), mc.getDefinitionLoader(), EntityPlayer::getGameProfile);
		manager.setGetSkullModel(profile -> {
			Property property = Iterables.getFirst(profile.getProperties().get("cpm:model"), null);
			if(property != null)return property.getValue();
			return null;
		});
		netHandler = new NetHandler<>(ResourceLocation::new);
		Executor ex = minecraft::addScheduledTask;
		netHandler.setExecutor(() -> ex);
		netHandler.setSendPacket(d -> new PacketBuffer(Unpooled.wrappedBuffer(d)), (c, rl, pb) -> c.sendPacket(new CPacketCustomPayload(rl.toString(), pb)), null);
		netHandler.setPlayerToLoader(EntityPlayer::getGameProfile);
		netHandler.setGetPlayerById(id -> {
			Entity ent = Minecraft.getMinecraft().world.getEntityByID(id);
			if(ent instanceof EntityPlayer) {
				return (EntityPlayer) ent;
			}
			return null;
		});
		netHandler.setGetClient(() -> minecraft.player);
		netHandler.setGetNet(c -> ((EntityPlayerSP)c).connection);
		netHandler.setDisplayText(f -> minecraft.ingameGUI.addChatMessage(ChatType.SYSTEM, f.remap()));
	}

	@SubscribeEvent
	public void playerRenderPre(RenderPlayerEvent.Pre event) {
		manager.bindPlayer(event.getEntityPlayer(), null);
		manager.bindSkin(TextureSheetType.SKIN);
	}

	@SubscribeEvent
	public void playerRenderPost(RenderPlayerEvent.Post event) {
		manager.unbindClear();
	}

	@SubscribeEvent
	public void initGui(GuiScreenEvent.InitGuiEvent.Post evt) {
		if((evt.getGui() instanceof GuiMainMenu && ModConfig.getCommonConfig().getSetBoolean(ConfigKeys.TITLE_SCREEN_BUTTON, true)) ||
				evt.getGui() instanceof GuiCustomizeSkin) {
			evt.getButtonList().add(new Button(0, 0));
		}
	}

	@SubscribeEvent
	public void buttonPress(GuiScreenEvent.ActionPerformedEvent.Pre evt) {
		if(evt.getButton() instanceof Button) {
			Minecraft.getMinecraft().displayGuiScreen(new GuiImpl(EditorGui::new, evt.getGui()));
		}
	}

	@SubscribeEvent
	public void openGui(GuiOpenEvent openGui) {
		if(openGui.getGui() == null && minecraft.currentScreen instanceof GuiImpl.Overlay) {
			openGui.setGui(((GuiImpl.Overlay) minecraft.currentScreen).getGui());
		}
		if(openGui.getGui() instanceof GuiMainMenu && EditorGui.doOpenEditor()) {
			openGui.setGui(new GuiImpl(EditorGui::new, openGui.getGui()));
		}
	}

	public void renderSkull(ModelBase skullModel, GameProfile profile) {
		manager.bindSkull(profile, null, skullModel);
		manager.bindSkin(skullModel, TextureSheetType.SKIN);
	}

	public void renderElytra(EntityPlayer player, ModelElytra model) {
		manager.bindElytra(player, null, model);
		manager.bindSkin(model, TextureSheetType.ELYTRA);
	}

	public void renderArmor(ModelBase modelArmor, ModelBase modelLeggings,
			EntityPlayer player) {
		manager.bindArmor(player, null, modelArmor, 1);
		manager.bindArmor(player, null, modelLeggings, 2);
		manager.bindSkin(modelArmor, TextureSheetType.ARMOR1);
		manager.bindSkin(modelLeggings, TextureSheetType.ARMOR2);
	}

	@SubscribeEvent
	public void renderTick(RenderTickEvent evt) {
		if(evt.phase == Phase.START) {
			mc.getPlayerRenderManager().getAnimationEngine().update(evt.renderTickTime);
		}
	}

	@SubscribeEvent
	public void clientTick(ClientTickEvent evt) {
		if(evt.phase == Phase.START && !minecraft.isGamePaused()) {
			mc.getPlayerRenderManager().getAnimationEngine().tick();
		}

		if (minecraft.player == null || evt.phase == Phase.START)
			return;

		if(KeyBindings.gestureMenuBinding.isPressed()) {
			minecraft.displayGuiScreen(new GuiImpl(GestureGui::new, null));
		}

		if(KeyBindings.renderToggleBinding.isPressed()) {
			Player.setEnableRendering(!Player.isEnableRendering());
		}

		for (Entry<Integer, KeyBinding> e : KeyBindings.quickAccess.entrySet()) {
			if(e.getValue().isPressed()) {
				mc.getPlayerRenderManager().getAnimationEngine().onKeybind(e.getKey());
			}
		}
	}

	@SubscribeEvent
	public void onRenderName(RenderLivingEvent.Specials.Pre<AbstractClientPlayer> evt) {
		if(evt.getEntity() instanceof AbstractClientPlayer) {
			if(!Player.isEnableNames())
				evt.setCanceled(true);
		}
	}

	public static class Button extends GuiButton {

		public Button(int x, int y) {
			super(99, x, y, 100, 20, I18n.format("button.cpm.open_editor"));
		}

	}

	public void onLogout() {
		mc.onLogOut();
	}

	@SubscribeEvent
	public void updateJump(PlayerSPPushOutOfBlocksEvent evt) {
		if(minecraft.player.onGround && minecraft.player.movementInput.jump) {
			manager.jump(minecraft.player);
		}
	}

	//Copy from LayerCape
	public static void renderCape(AbstractClientPlayer playerIn, float partialTicks, ModelPlayer model, ModelDefinition modelDefinition) {
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.pushMatrix();
		float f1, f2, f3;

		if(playerIn != null) {
			double lvt_10_1_ = playerIn.prevChasingPosX
					+ (playerIn.chasingPosX - playerIn.prevChasingPosX) * partialTicks
					- (playerIn.prevPosX + (playerIn.posX - playerIn.prevPosX) * partialTicks);
			double lvt_12_1_ = playerIn.prevChasingPosY
					+ (playerIn.chasingPosY - playerIn.prevChasingPosY) * partialTicks
					- (playerIn.prevPosY + (playerIn.posY - playerIn.prevPosY) * partialTicks);
			double lvt_14_1_ = playerIn.prevChasingPosZ
					+ (playerIn.chasingPosZ - playerIn.prevChasingPosZ) * partialTicks
					- (playerIn.prevPosZ + (playerIn.posZ - playerIn.prevPosZ) * partialTicks);
			float lvt_16_1_ = playerIn.prevRenderYawOffset
					+ (playerIn.renderYawOffset - playerIn.prevRenderYawOffset) * partialTicks;
			double lvt_17_1_ = MathHelper.sin(lvt_16_1_ * 0.017453292F);
			double lvt_19_1_ = (-MathHelper.cos(lvt_16_1_ * 0.017453292F));
			f1 = (float) lvt_12_1_ * 10.0F;
			f1 = MathHelper.clamp(f1, -6.0F, 32.0F);
			f2 = (float) (lvt_10_1_ * lvt_17_1_ + lvt_14_1_ * lvt_19_1_) * 100.0F;
			f3 = (float) (lvt_10_1_ * lvt_19_1_ - lvt_14_1_ * lvt_17_1_) * 100.0F;
			if (f2 < 0.0F) {
				f2 = 0.0F;
			}

			float lvt_24_1_ = playerIn.prevCameraYaw
					+ (playerIn.cameraYaw - playerIn.prevCameraYaw) * partialTicks;
			f1 += MathHelper.sin((playerIn.prevDistanceWalkedModified
					+ (playerIn.distanceWalkedModified - playerIn.prevDistanceWalkedModified) * partialTicks)
					* 6.0F) * 32.0F * lvt_24_1_;
			if (playerIn.isSneaking()) {
				f1 += 25.0F;
				model.bipedCape.rotationPointY = 2.0F;
			} else {
				model.bipedCape.rotationPointY = 0.0F;
			}
		} else {
			f1 = 0;
			f2 = 0;
			f3 = 0;
		}

		model.bipedCape.rotateAngleX = (float) -Math.toRadians(6.0F + f2 / 2.0F + f1);
		model.bipedCape.rotateAngleY = (float) Math.toRadians(180.0F - f3 / 2.0F);
		model.bipedCape.rotateAngleZ = (float) Math.toRadians(f3 / 2.0F);
		mc.getPlayerRenderManager().setModelPose(model);
		model.bipedCape.rotateAngleX = 0;
		model.bipedCape.rotateAngleY = 0;
		model.bipedCape.rotateAngleZ = 0;
		model.renderCape(0.0625F);
		GlStateManager.popMatrix();
	}
}
