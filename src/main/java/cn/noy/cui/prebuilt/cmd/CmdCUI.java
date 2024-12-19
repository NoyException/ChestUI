package cn.noy.cui.prebuilt.cmd;

import cn.noy.cui.CUIPlugin;
import cn.noy.cui.prebuilt.cui.CUIMonitor;

import cn.noy.cui.ui.Camera;
import cn.noy.cui.ui.ChestUI;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CmdCUI implements CommandExecutor, TabCompleter {
	private final CUIPlugin plugin;
	private final ChestUI<CUIMonitor> cuiMonitor;

	public CmdCUI(CUIPlugin plugin) {
		this.plugin = plugin;
		this.cuiMonitor = plugin.getCUIManager().createCUI(CUIMonitor.class);
	}

	public void printHelp(@NotNull CommandSender sender) {
		sender.sendMessage("Usage:");
		sender.sendMessage("/cui help: 显示帮助信息.");

		sender.sendMessage("/cui close <player> <top|all> (force): (强制)关闭指定玩家的顶部/所有摄像头.");

		sender.sendMessage("/cui create <cui> (keepAlive): 创造一个<cui>类型的实例.");
		sender.sendMessage("/cui create <cui>#<id> (keepAlive): 为指定<cui>类型实例创造一个摄像头.");

		sender.sendMessage("/cui destroy <cui>: 摧毁全部的<cui>类型的实例.");
		sender.sendMessage("/cui destroy <cui>#<id>: 摧毁指定的<cui>类型的实例.");
		sender.sendMessage("/cui destroy <cui>#<id>#<camera>: 摧毁指定的<cui>类型的实例的摄像头.");

		sender.sendMessage("/cui list <cui>: 列出指定<cui>类型的所有实例.");
		sender.sendMessage("/cui list <cui>#<id>: 列出指定<cui>类型实例的所有摄像头.");

		sender.sendMessage("/cui monitor: 打开CUI监视器.");

		sender.sendMessage("/cui open <cui>#<id> (<player>): 打开指定<cui>类型实例的默认摄像头.");
		sender.sendMessage("/cui open <cui>#<id>#<camera> (<player>): 打开指定<cui>类型实例的指定摄像头.");
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
			@NotNull String[] args) {
		if (args.length == 0) {
			return false;
		}
		switch (args[0]) {
			default -> {
				sender.sendMessage("Unknown command: " + args[0]);
				return false;
			}
			case "help" -> {
				printHelp(sender);
				return true;
			}
			case "close" -> {
				if (args.length < 3) {
					return false;
				}
				var entities = Bukkit.getServer().selectEntities(sender, args[1]);
				var mode = switch (args[2]) {
					case "top" -> 1;
					case "all" -> 2;
					default -> -1;
				};
				if (mode == -1) {
					sender.sendMessage("Invalid close mode: " + args[2]);
					return false;
				}
				var force = args.length == 4 && args[3].equals("force");
				for (Entity entity : entities) {
					if (!(entity instanceof Player player)) {
						sender.sendMessage("Only player can close camera");
						continue;
					}
					var success = mode == 1
							? Camera.Manager.closeTop(player, force)
							: Camera.Manager.closeAll(player, force);
					if (!success) {
						sender.sendMessage("Failed to close camera for " + player.getName());
					}
				}
				sender.sendMessage("Closed");
				return true;
			}
			case "create" -> {
				if (args.length == 1) {
					return false;
				}
				var name = args[1];
				NamespacedKey key;
				if (!name.contains("#")) {
					try {
						key = NamespacedKey.fromString(name);
					} catch (IllegalArgumentException e) {
						sender.sendMessage("Invalid CUI name: " + name);
						return false;
					}

					var cui = plugin.getCUIManager().createCUI(key);
					if (cui == null) {
						sender.sendMessage("CUI not found: " + name);
						return true;
					}
					if (args.length == 3 && args[2].equals("keepAlive")) {
						cui.edit().setKeepAlive(true).finish();
					}
					sender.sendMessage("Create CUI instance " + cui.getName());
				} else {
					var cui = plugin.getCUIManager().getCUI(name);
					if (cui == null) {
						sender.sendMessage("CUI instance not found: " + name);
						return true;
					}
					var camera = cui.createCamera();
					if (camera == null) {
						sender.sendMessage("Failed to create camera for CUI instance: " + name);
						return true;
					}
					sender.sendMessage("Create camera " + camera.getName());
				}
				return true;
			}
			case "destroy" -> {
				if (args.length == 1) {
					return false;
				}
				var name = args[1];
				String[] split = name.split("#");
				if (split.length == 2) {
					var cui = plugin.getCUIManager().getCUI(name);
					if (cui == null) {
						sender.sendMessage("CUI instance not found: " + name);
						return true;
					}
					cui.destroy();
					sender.sendMessage("Destroyed");
					return true;
				} else if (split.length == 3) {
					var camera = Camera.Manager.getCamera(name);
					if (camera == null) {
						sender.sendMessage("Camera not found: " + name);
						return true;
					}
					camera.destroy();
					sender.sendMessage("Destroyed");
					return true;
				} else {
					sender.sendMessage("Invalid CUI instance or camera name: " + name);
					return false;
				}
			}
			case "list" -> {
				if (args.length == 1) {
					return false;
				}
				var name = args[1];
				String[] split = name.split("#");
				if (split.length == 1) {
					var key = NamespacedKey.fromString(name);
					if (key == null) {
						sender.sendMessage("Invalid CUI name: " + name);
						return true;
					}
					var cuis = plugin.getCUIManager().getCUIs(key);
					if (cuis.isEmpty()) {
						sender.sendMessage("No CUI instance found for " + name);
						return true;
					}
					sender.sendMessage("CUI instances of " + name + ":");
					sender.sendMessage(" - " + String.join(", ", cuis.stream().map(ChestUI::getName).toList()));
					return true;
				} else if (split.length == 2) {
					var cui = plugin.getCUIManager().getCUI(name);
					if (cui == null) {
						sender.sendMessage("CUI instance not found: " + name);
						return true;
					}
					var cameras = cui.getCameras();
					if (cameras.isEmpty()) {
						sender.sendMessage("No camera found for CUI instance: " + name);
						return true;
					}
					sender.sendMessage("Cameras of " + name + ":");
					sender.sendMessage(" - " + String.join(", ", cameras.stream().map(Camera::getName).toList()));
					return true;
				} else {
					sender.sendMessage("Invalid CUI instance or camera name: " + name);
					return false;
				}
			}
			case "monitor" -> {
				if (!(sender instanceof Player player)) {
					sender.sendMessage("Player required");
					return false;
				}
				var success = cuiMonitor.getDefaultCamera().open(player, false);
				if (!success) {
					sender.sendMessage("Failed to open CUI monitor");
				}
				return true;
			}
			case "open" -> {
				if (args.length == 1) {
					return false;
				}
				var name = args[1];
				String[] split = name.split("#");
				if (split.length == 2) {
					var cui = plugin.getCUIManager().getCUI(name);
					if (cui == null) {
						sender.sendMessage("CUI instance not found: " + name);
						return true;
					}
					var camera = cui.getDefaultCamera();
					if (camera == null) {
						sender.sendMessage("Default camera not found for CUI instance: " + name);
						return true;
					}
					if (args.length == 3) {
						var entities = Bukkit.getServer().selectEntities(sender, args[2]);
						for (Entity entity : entities) {
							if (!(entity instanceof Player player)) {
								sender.sendMessage("Only player can open camera");
								continue;
							}
							var success = camera.open(player, false);
							if (!success) {
								sender.sendMessage(
										"Failed to open camera " + camera.getName() + " for " + player.getName());
							}
						}
					} else {
						if (!(sender instanceof Player player)) {
							sender.sendMessage("Player required");
							return false;
						}
						var success = camera.open(player, false);
						if (!success) {
							sender.sendMessage("Failed to open camera " + camera.getName());
						}
					}
					sender.sendMessage("Opened");
					return true;
				} else if (split.length == 3) {
					var camera = Camera.Manager.getCamera(name);
					if (camera == null) {
						sender.sendMessage("Camera not found: " + name);
						return true;
					}
					if (args.length == 4) {
						var target = plugin.getServer().getPlayer(args[3]);
						if (target == null) {
							sender.sendMessage("Player not found: " + args[3]);
							return true;
						}
						var success = camera.open(target, false);
						if (!success) {
							sender.sendMessage(
									"Failed to open camera " + camera.getName() + " for " + target.getName());
						}
					} else {
						var success = camera.open((Player) sender, false);
						if (!success) {
							sender.sendMessage("Failed to open camera " + camera.getName());
						}
					}
					sender.sendMessage("Opened");
					return true;
				} else {
					sender.sendMessage("Invalid CUI instance or camera name: " + name);
					return false;
				}
			}
		}
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
			@NotNull String label, @NotNull String[] args) {
		return switch (args.length) {
			case 1 -> List.of("close", "create", "destroy", "help", "list", "monitor", "open");
			case 2 -> {
				switch (args[0]) {
					case "close" -> {
						var options = new ArrayList<>(List.of("@a", "@p", "@s"));
						options.addAll(Bukkit.getServer().getOnlinePlayers().stream().map(Player::getName).toList());
						yield options;
					}
					case "create", "list" -> {
						yield plugin.getCUIManager().getRegisteredCUINames().stream().map(NamespacedKey::toString)
								.toList();
					}
					default -> {
						yield List.of();
					}
				}
			}
			case 3 -> {
				switch (args[0]) {
					case "close" -> {
						yield List.of("top", "all");
					}
					case "create" -> {
						yield List.of("keepAlive");
					}
					case "open" -> {
						var options = new ArrayList<>(List.of("@a", "@p", "@s"));
						options.addAll(Bukkit.getServer().getOnlinePlayers().stream().map(Player::getName).toList());
						yield options;
					}
					default -> {
						yield List.of();
					}
				}
			}
			case 4 -> {
				switch (args[0]) {
					case "close" -> {
						yield List.of("force");
					}
					default -> {
						yield List.of();
					}
				}
			}
			default -> List.of();
		};
	}
}
