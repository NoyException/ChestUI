package cn.noy.cui;

import cn.noy.cui.prebuilt.cmd.CmdCUI;
import cn.noy.cui.ui.CUIManager;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public class CUIPlugin extends JavaPlugin {

	private final CUIManager cuiManager = new CUIManager(this);

	@Override
	public void onEnable() {
		cuiManager.setup();
		bindCommand("cui", new CmdCUI(this));
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
}
