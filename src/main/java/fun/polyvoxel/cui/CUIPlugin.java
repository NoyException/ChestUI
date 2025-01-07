package fun.polyvoxel.cui;

import fun.polyvoxel.cui.cmd.CommandCUI;
import fun.polyvoxel.cui.ui.tool.Tools;
import fun.polyvoxel.cui.ui.CUIManager;

import fun.polyvoxel.cui.ui.CameraManager;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.plugin.java.JavaPlugin;

public class CUIPlugin extends JavaPlugin {
	private static ComponentLogger logger;
	private final CUIManager cuiManager = new CUIManager(this);
	private final CameraManager cameraManager = new CameraManager(this);
	private CommandCUI command;
	private final Tools tools = new Tools(this);

	public static ComponentLogger logger() {
		return logger;
	}

	@Override
	public void onEnable() {
		logger = getComponentLogger();
		cuiManager.setup();
		// Experimental. MockBukkit does not support Brigadier
		try {
			command = new CommandCUI(this);
			command.register();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDisable() {
		cuiManager.teardown();
	}

	public CUIManager getCUIManager() {
		return cuiManager;
	}

	public CameraManager getCameraManager() {
		return cameraManager;
	}

	public CommandCUI getCommand() {
		return command;
	}

	public Tools getTools() {
		return tools;
	}
}
