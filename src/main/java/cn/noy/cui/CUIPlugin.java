package cn.noy.cui;

import cn.noy.cui.ui.CUIManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class CUIPlugin extends JavaPlugin {
    private static CUIPlugin instance;
    public static CUIPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        CUIManager.getInstance().initialize();
    }

    @Override
    public void onDisable() {
        CUIManager.getInstance().uninitialize();
    }

    private void bindCommand(String name, Object executor){
        PluginCommand command = Bukkit.getPluginCommand(name);
        if(command == null){
            getLogger().warning("Command `"+ name +"` not found");
            return;
        }
        command.setExecutor(executor instanceof CommandExecutor ? (CommandExecutor) executor : null);
        command.setTabCompleter(executor instanceof TabCompleter ? (TabCompleter) executor : null);
    }
}
