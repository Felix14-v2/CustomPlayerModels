package com.tom.cpm.shared.config;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.tom.cpl.util.Image;
import com.tom.cpm.shared.MinecraftObjectHolder;
import com.tom.cpm.shared.animation.AnimationEngine.AnimationMode;
import com.tom.cpm.shared.animation.AnimationHandler;
import com.tom.cpm.shared.animation.IPose;
import com.tom.cpm.shared.animation.VanillaPose;
import com.tom.cpm.shared.definition.ModelDefinition;
import com.tom.cpm.shared.model.SkinType;
import com.tom.cpm.shared.util.LegacySkinConverter;

public abstract class Player<P, M> {
	private static boolean enableRendering = true;
	private static boolean enableNames = true;

	private ModelDefinition definition;
	private EnumMap<AnimationMode, AnimationHandler> animHandler = new EnumMap<>(AnimationMode.class);
	private CompletableFuture<Image> skinFuture;
	public VanillaPose prevPose;
	public IPose currentPose;
	public String url;
	public boolean forcedSkin;

	public CompletableFuture<Image> getSkin() {
		if(skinFuture != null)return skinFuture;
		skinFuture = getSkin0();
		if(skinFuture == null)return CompletableFuture.completedFuture(null);
		return skinFuture;
	}

	private CompletableFuture<Image> getSkin0() {
		if(MinecraftObjectHolder.DEBUGGING && new File("skin_test.png").exists()) {
			return CompletableFuture.supplyAsync(() -> {
				try {
					return Image.loadFrom(new File("skin_test.png"));
				} catch (IOException e) {
					return null;
				}
			});
		}
		if(url == null)return null;
		return Image.download(url).thenApply(i -> new LegacySkinConverter().convertSkin(i)).exceptionally(e -> null);
	}

	public abstract SkinType getSkinType();
	public abstract void loadSkin(Runnable onLoaded);
	public abstract UUID getUUID();
	public abstract VanillaPose getPose();
	public abstract int getEncodedGestureId();
	public abstract M getModel();
	public abstract void updateFromPlayer(P player);

	public void setModelDefinition(ModelDefinition definition) {
		this.definition = definition;
	}

	public ModelDefinition getModelDefinition() {
		return enableRendering ? definition : null;
	}

	public ModelDefinition getAndResolveDefinition() {
		ModelDefinition def = getModelDefinition();
		if(def != null) {
			if(def.getResolveState() == 0)def.startResolve();
			else if(def.getResolveState() == 2) {
				if(def.doRender()) {
					return def;
				}
			}
		}
		return null;
	}

	public AnimationHandler getAnimationHandler(AnimationMode mode) {
		return animHandler.computeIfAbsent(mode, k -> new AnimationHandler(this));
	}

	public static boolean isEnableRendering() {
		return enableRendering;
	}

	public static void setEnableRendering(boolean enableRendering) {
		Player.enableRendering = enableRendering;
	}

	public static boolean isEnableNames() {
		return enableNames;
	}

	public static void setEnableNames(boolean enableNames) {
		Player.enableNames = enableNames;
	}
}
