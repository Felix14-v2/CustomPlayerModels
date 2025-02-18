package com.tom.cpm.bukkit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import com.tom.cpl.config.ConfigEntry.ModConfigFile;
import com.tom.cpl.text.TextRemapper;
import com.tom.cpl.util.ILogger;
import com.tom.cpm.bukkit.Commands.CommandHandler;
import com.tom.cpm.shared.MinecraftCommonAccess;
import com.tom.cpm.shared.MinecraftObjectHolder;
import com.tom.cpm.shared.MinecraftServerAccess;
import com.tom.cpm.shared.PlatformFeature;
import com.tom.cpm.shared.network.NetHandler;

public class CPMBukkitPlugin extends JavaPlugin {
	public ModConfigFile config;
	private Network net;
	public I18n i18n;
	private BukkitLogger log;
	private CommandHandler cmd;

	@Override
	public void onDisable() {
		super.onDisable();
		MinecraftObjectHolder.setCommonObject(null);
		MinecraftObjectHolder.setServerObject(null);
		config.save();
	}

	@Override
	public void onEnable() {
		super.onEnable();
		getDataFolder().mkdirs();
		log = new BukkitLogger(getLogger());
		config = new ModConfigFile(new File(getDataFolder(), "cpm.json"));
		File tr = new File(getDataFolder(), "cpm.lang");
		if(tr.exists()) {
			try {
				i18n = I18n.loadLocaleData(new FileInputStream(tr));
			} catch (IOException e) {
				log.warn("Failed to load localization from cpm.lang", e);
			}
		}
		if(i18n == null) {
			try {
				i18n = I18n.loadLocaleData(CPMBukkitPlugin.class.getResourceAsStream("/assets/cpm/lang/en_us.lang"));
			} catch (IOException e) {
				log.error("Failed to load localization from builtin lang file", e);
				i18n = new I18n() {
					@Override
					public String format(String translateKey, Object... parameters) {
						return "Server failed to load builtin localization. This is a BUG, please report it to the server owner.";
					}
				};
			}
		}
		cmd = new CommandHandler(this);
		MinecraftObjectHolder.setCommonObject(new MinecraftCommonAccess() {

			@Override
			public ModConfigFile getConfig() {
				return config;
			}

			@Override
			public ILogger getLogger() {
				return log;
			}

			@Override
			public EnumSet<PlatformFeature> getSupportedFeatures() {
				return EnumSet.noneOf(PlatformFeature.class);
			}

			@Override
			public String getPlatformVersionString() {
				return "Bukkit (" + getServer().getVersion() + "/" + getServer().getBukkitVersion() + ") " + getDescription().getVersion();
			}

			@Override
			public TextRemapper<String> getTextRemapper() {
				return new TextRemapper<>(i18n::format, Function.identity(), (a, b) -> a + b, null);
			}
		});
		MinecraftObjectHolder.setServerObject(new MinecraftServerAccess() {

			@Override
			public ModConfigFile getConfig() {
				return config;
			}

			@Override
			public NetHandler<?, ?, ?> getNetHandler() {
				return net.netHandler;
			}
		});
		net = new Network(this);
		net.register();
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new PlayerTracker(net), 0, 20);
		log.info("Customizable Player Models Initialized");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		return cmd.onCommand(sender, command, label, args);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return cmd.onTabComplete(sender, command, alias, args);
	}
}
