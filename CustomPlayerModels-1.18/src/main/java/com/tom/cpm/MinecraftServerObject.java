package com.tom.cpm;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import com.tom.cpl.config.ConfigEntry.ModConfigFile;
import com.tom.cpm.common.ServerHandler;
import com.tom.cpm.shared.MinecraftServerAccess;
import com.tom.cpm.shared.network.NetHandler;

public class MinecraftServerObject implements MinecraftServerAccess {
	public static final LevelResource CONFIG = new LevelResource("data/cpm.json");
	private MinecraftServer server;
	private ModConfigFile cfg;

	public MinecraftServerObject(MinecraftServer server) {
		this.server = server;
		cfg = new ModConfigFile(server.getWorldPath(CONFIG).toFile());
	}

	@Override
	public ModConfigFile getConfig() {
		return cfg;
	}

	@Override
	public NetHandler<?, ?, ?> getNetHandler() {
		return ServerHandler.netHandler;
	}
}
