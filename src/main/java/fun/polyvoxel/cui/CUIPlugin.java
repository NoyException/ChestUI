package fun.polyvoxel.cui;

import fun.polyvoxel.cui.cmd.CmdCUI;
import fun.polyvoxel.cui.cmd.CommandCUI;
import fun.polyvoxel.cui.ui.tool.Tools;
import fun.polyvoxel.cui.ui.CUIManager;

import fun.polyvoxel.cui.ui.CameraManager;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class CUIPlugin extends JavaPlugin {
	private static ComponentLogger logger;
	private final CUIManager cuiManager = new CUIManager(this);
	private final CameraManager cameraManager = new CameraManager(this);
	private final Tools tools = new Tools(this);
	public static final @NotNull Enchantment GLOW = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
			.get(CUIPluginBootstrap.GLOW);

	public static ComponentLogger logger() {
		return logger;
	}

	@Override
	public void onEnable() {
		logger = getComponentLogger();
		cuiManager.setup();
		// Experimental. MockBukkit does not support Brigadier
		try {
			new CommandCUI(this).register();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		bindCommand("cui-legacy", new CmdCUI(this));
	}

	@Override
	public void onDisable() {
		cuiManager.teardown();
	}

	private void bindCommand(String name, Object executor) {
		PluginCommand command = Bukkit.getPluginCommand(name);
		if (command == null) {
			getLogger().warning("Command `" + name + "` not found");
			return;
		}
		command.setExecutor(executor instanceof CommandExecutor ? (CommandExecutor) executor : null);
		command.setTabCompleter(executor instanceof TabCompleter ? (TabCompleter) executor : null);
	}

	public CUIManager getCUIManager() {
		return cuiManager;
	}

	public CameraManager getCameraManager() {
		return cameraManager;
	}

	public Tools getTools() {
		return tools;
	}
}
