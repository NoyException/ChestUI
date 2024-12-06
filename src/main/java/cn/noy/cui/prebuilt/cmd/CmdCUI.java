package cn.noy.cui.prebuilt.cmd;

import cn.noy.cui.prebuilt.cui.CUIMonitor;
import cn.noy.cui.prebuilt.cui.InventoryMonitor;
import cn.noy.cui.ui.CUIManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CmdCUI implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player player)){
            return false;
        }
        if(args.length<=1){
            return false;
        }
        if(args[0].equals("open")){
            if(args[1].equals("cm")){
                var cui = CUIManager.getInstance().createCUI(CUIMonitor.class);
                cui.open(player);
                return true;
            }
            if(args[1].equals("im")){
                var cui = CUIManager.getInstance().createCUI(InventoryMonitor.class);
                cui.open(player);
                return true;
            }
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
