package cn.noy.cui.prebuilt.cmd;

import cn.noy.cui.CUIPlugin;

import cn.noy.cui.prebuilt.cui.CUIMonitor;
import cn.noy.cui.ui.*;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CmdCUI implements TabExecutor {
	private final CUIPlugin plugin;
	private final Permission permission;

	public CmdCUI(CUIPlugin plugin) {
		this.plugin = plugin;
		permission = new Permission("cui", PermissionDefault.OP);
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

		sender.sendMessage("/cui open <cui>#<id> (<player>) (asChild): 打开指定<cui>类型实例的默认摄像头.");
		sender.sendMessage("/cui open <cui>#<id>#<camera> (<player>) (asChild): 打开指定<cui>类型实例的指定摄像头.");
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
			@NotNull String[] args) {
		if (!sender.hasPermission(permission)) {
			sender.sendMessage("No permission.");
			return true;
		}
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
							? plugin.getCameraManager().closeTop(player, force)
							: plugin.getCameraManager().closeAll(player, force);
					if (!success) {
						sender.sendMessage("Failed to close camera for " + player.getName());
					}
				}
				return true;
			}
			case "create" -> {
				if (args.length == 1) {
					return false;
				}
				CUIManager.ParseResult parsed;
				try {
					parsed = plugin.getCUIManager().parse(args[1]);
				} catch (IllegalArgumentException e) {
					sender.sendMessage("Invalid CUI name: " + args[1]);
					return false;
				}

				if (parsed.typeHandler() == null) {
					sender.sendMessage("CUI type not found: " + args[1]);
					return true;
				} else if (parsed.instanceId() == null) {
					try {
						var cui = parsed.typeHandler().createInstance();
						if (args.length == 3 && args[2].equals("keepAlive")) {
							cui.edit().setKeepAlive(true).finish();
						}
					} catch (Exception e) {
						sender.sendMessage("Failed to create CUI instance: " + e.getMessage());
						return true;
					}
				} else if (parsed.cameraId() == null) {
					var cui = parsed.typeHandler().getInstance(parsed.instanceId());
					if (cui == null) {
						sender.sendMessage("CUI instance not found: " + args[1]);
						return true;
					}
					var camera = cui.createCamera();
					if (camera == null) {
						sender.sendMessage("Failed to create camera for CUI instance: " + args[1]);
						return true;
					}
				} else {
					sender.sendMessage("Do not specify camera id when creating CUI instance or Camera");
				}
				return true;
			}
			case "destroy" -> {
				if (args.length == 1) {
					return false;
				}
				CUIManager.ParseResult parsed;
				try {
					parsed = plugin.getCUIManager().parse(args[1]);
				} catch (IllegalArgumentException e) {
					sender.sendMessage("Invalid CUI name: " + args[1]);
					return false;
				}
				if (parsed.typeHandler() == null) {
					sender.sendMessage("CUI type not found: " + args[1]);
					return true;
				} else if (parsed.instanceId() == null) {
					var cuis = parsed.typeHandler().getInstances();
					for (var cui : cuis) {
						cui.destroy();
					}
				} else if (parsed.cameraId() == null) {
					var cui = parsed.typeHandler().getInstance(parsed.instanceId());
					if (cui == null) {
						sender.sendMessage("CUI instance not found: " + args[1]);
						return true;
					}
					cui.destroy();
				} else {
					var cui = parsed.typeHandler().getInstance(parsed.instanceId());
					if (cui == null) {
						sender.sendMessage("CUI instance not found: " + args[1]);
						return true;
					}
					var camera = cui.getCamera(parsed.cameraId());
					if (camera == null) {
						sender.sendMessage("Camera not found: " + args[1]);
						return true;
					}
					camera.destroy();
				}
				return true;
			}
			case "list" -> {
				if (args.length == 1) {
					return false;
				}
				CUIManager.ParseResult parsed;
				try {
					parsed = plugin.getCUIManager().parse(args[1]);
				} catch (IllegalArgumentException e) {
					sender.sendMessage("Invalid CUI name: " + args[1]);
					return false;
				}
				if (parsed.typeHandler() == null) {
					sender.sendMessage("CUI type not found: " + args[1]);
					return true;
				} else if (parsed.instanceId() == null) {
					var cuis = parsed.typeHandler().getInstances();
					sender.sendMessage("CUI instances of " + args[1] + ":");
					sender.sendMessage(" - " + String.join(", ", cuis.stream().map(ChestUI::getName).toList()));
				} else if (parsed.cameraId() == null) {
					var cui = parsed.typeHandler().getInstance(parsed.instanceId());
					if (cui == null) {
						sender.sendMessage("CUI instance not found: " + args[1]);
						return true;
					}
					var cameras = cui.getCameras();
					sender.sendMessage("Cameras of " + args[1] + ":");
					sender.sendMessage(" - " + String.join(", ", cameras.stream().map(Camera::getName).toList()));
				} else {
					sender.sendMessage("Do not specify camera id when listing CUI instance or Camera");
				}
				return true;
			}
			case "monitor" -> {
				if (!(sender instanceof Player player)) {
					sender.sendMessage("Player required");
					return false;
				}
				var cui = plugin.getCUIManager().getCUITypeHandler(CUIMonitor.class).getInstance();
				var success = cui.getDefaultCamera().open(player, false);
				if (!success) {
					sender.sendMessage("Failed to open CUI monitor");
				}
				return true;
			}
			case "open" -> {
				if (args.length == 1) {
					return false;
				}
				CUIManager.ParseResult parsed;
				try {
					parsed = plugin.getCUIManager().parse(args[1]);
				} catch (IllegalArgumentException e) {
					sender.sendMessage("Invalid CUI name: " + args[1]);
					return false;
				}
				if (parsed.typeHandler() == null) {
					sender.sendMessage("CUI type not found: " + args[1]);
					return true;
				}
				Camera<?> camera;

				if (parsed.instanceId() == null) {
					if (parsed.typeHandler().isSingleton()) {
						camera = parsed.typeHandler().getInstance().getDefaultCamera();
					} else {
						sender.sendMessage("Instance id required for non-singleton CUI type");
						return true;
					}
				} else if (parsed.cameraId() == null) {
					var cui = parsed.typeHandler().getInstance(parsed.instanceId());
					if (cui == null) {
						sender.sendMessage("CUI instance not found: " + args[1]);
						return true;
					}
					camera = cui.getDefaultCamera();
				} else {
					var cui = parsed.typeHandler().getInstance(parsed.instanceId());
					if (cui == null) {
						sender.sendMessage("CUI instance not found: " + args[1]);
						return true;
					}
					camera = cui.getCamera(parsed.cameraId());
				}

				if (camera == null) {
					sender.sendMessage("Camera not found: " + args[1]);
					return true;
				}

				var entities = args.length == 3 ? Bukkit.getServer().selectEntities(sender, args[2]) : List.of(sender);
				var asChild = args.length == 4 && args[3].equals("asChild");
				for (var entity : entities) {
					if (!(entity instanceof Player player)) {
						sender.sendMessage("Only player can open camera");
						continue;
					}
					var success = camera.open(player, asChild);
					if (!success) {
						sender.sendMessage("Failed to open camera for " + player.getName());
					}
				}
				return true;
			}
		}
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
			@NotNull String label, @NotNull String[] args) {
		if (!sender.hasPermission(permission)) {
			return List.of();
		}
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
					case "open" -> {
						yield List.of("asChild");
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
