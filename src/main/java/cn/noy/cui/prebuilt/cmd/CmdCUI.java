package cn.noy.cui.prebuilt.cmd;

import cn.noy.cui.CUIPlugin;
import cn.noy.cui.prebuilt.cui.CUIMonitor;
import cn.noy.cui.prebuilt.cui.InventoryMonitor;
import cn.noy.cui.prebuilt.cui.TestCUI;
import cn.noy.cui.ui.CUIManager;

import cn.noy.cui.ui.Camera;
import cn.noy.cui.ui.ChestUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CmdCUI implements CommandExecutor, TabCompleter {
	private final CUIPlugin plugin;
	private final ChestUI<CUIMonitor> cuiMonitor;

	public CmdCUI(CUIPlugin plugin) {
		this.plugin = plugin;
		this.cuiMonitor = plugin.getCUIManager().createCUI(CUIMonitor.class);
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
			@NotNull String[] args) {
		if (!(sender instanceof Player player)) {
			return false;
		}
		if (args.length <= 1) {
			return false;
		}
		if (args[0].equals("open")) {
			switch (args[1]) {
				case "cm" -> {
					Camera<CUIMonitor> camera = cuiMonitor.newCamera();
					camera.open(player, false);
					return true;
				}
				case "im" -> {
					var cui = plugin.getCUIManager().createCUI(InventoryMonitor.class);
					cui.getDefaultCamera().open(player, false);
					return true;
				}
				case "test" -> {
					for (int i = 0; i < 9; i++) {
						plugin.getCUIManager().createCUI(TestCUI.class);
					}
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
			@NotNull String label, @NotNull String[] args) {
		return switch (args.length) {
			case 1 -> List.of("open");
			case 2 -> List.of("cm", "im", "test");
			default -> List.of();
		};
	}
}
