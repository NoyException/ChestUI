package fun.polyvoxel.cui.cmd;

import fun.polyvoxel.cui.CUIPlugin;

import fun.polyvoxel.cui.prebuilt.CUIMonitor;
import fun.polyvoxel.cui.ui.CUIManager;
import fun.polyvoxel.cui.ui.CUIType;
import fun.polyvoxel.cui.ui.Camera;
import fun.polyvoxel.cui.ui.CUIInstance;
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

		sender.sendMessage("/cui display <cui> (<player>) (asChild): 为指定玩家一键显示<cui>.");

		sender.sendMessage("/cui list <cui>: 列出指定<cui>类型的所有实例.");
		sender.sendMessage("/cui list <cui>#<id>: 列出指定<cui>类型实例的所有摄像头.");

		sender.sendMessage("/cui monitor: 打开CUI监视器.");

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

				if (parsed.cuiType() == null) {
					sender.sendMessage("CUI type not found: " + args[1]);
					return true;
				} else if (parsed.instanceId() == null) {
					try {
						var cui = parsed.cuiType().createInstance();
						if (args.length == 3 && args[2].equals("keepAlive")) {
							cui.edit().keepAlive(true).done();
						}
					} catch (Exception e) {
						sender.sendMessage("Failed to create CUI instance: " + e.getMessage());
						return true;
					}
				} else if (parsed.cameraId() == null) {
					var cui = parsed.cuiType().getInstance(parsed.instanceId());
					if (cui == null) {
						sender.sendMessage("CUI instance not found: " + args[1]);
						return true;
					}
					var camera = cui.createCamera();
					if (camera == null) {
						sender.sendMessage("Failed to create camera for CUI instance: " + args[1]);
						return true;
					}
					var keepAlive = args.length == 3 && args[2].equals("keepAlive");
					if (keepAlive) {
						camera.edit().keepAlive(true).done();
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
				if (parsed.cuiType() == null) {
					sender.sendMessage("CUI type not found: " + args[1]);
					return true;
				} else if (parsed.instanceId() == null) {
					var cuis = parsed.cuiType().getInstances();
					for (var cui : cuis) {
						cui.destroy();
					}
				} else if (parsed.cameraId() == null) {
					var cui = parsed.cuiType().getInstance(parsed.instanceId());
					if (cui == null) {
						sender.sendMessage("CUI instance not found: " + args[1]);
						return true;
					}
					cui.destroy();
				} else {
					var cui = parsed.cuiType().getInstance(parsed.instanceId());
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
			case "display" -> {
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
				if (parsed.cuiType() == null) {
					sender.sendMessage("CUI type not found: " + args[1]);
					return true;
				}
				if (parsed.instanceId() != null) {
					sender.sendMessage("Do not specify instance id when displaying CUI");
					return false;
				}
				if (!parsed.cuiType().canDisplay()) {
					sender.sendMessage("CUI type " + args[1] + " cannot be displayed directly.");
					sender.sendMessage("If you are the developer of this CUI, please add a display trigger.");
					return true;
				}
				var entities = args.length == 3 ? Bukkit.getServer().selectEntities(sender, args[2]) : List.of(sender);
				var asChild = args.length == 4 && args[3].equals("asChild");
				for (var entity : entities) {
					if (!(entity instanceof Player player)) {
						sender.sendMessage("Only player can display CUI");
						continue;
					}
					var camera = parsed.cuiType().display(player, asChild);
					if (camera == null) {
						sender.sendMessage("Failed to display CUI for " + player.getName());
					}
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
				if (parsed.cuiType() == null) {
					sender.sendMessage("CUI type not found: " + args[1]);
					return true;
				} else if (parsed.instanceId() == null) {
					var cuis = parsed.cuiType().getInstances();
					sender.sendMessage("CUI instances of " + args[1] + ":");
					sender.sendMessage(" - " + String.join(", ", cuis.stream().map(CUIInstance::getName).toList()));
				} else if (parsed.cameraId() == null) {
					var cui = parsed.cuiType().getInstance(parsed.instanceId());
					if (cui == null) {
						sender.sendMessage("CUI instance not found: " + args[1]);
						return true;
					}
					var cameras = cui.getCameras();
					sender.sendMessage("Cameras of " + args[1] + ":");
					sender.sendMessage(" - " + String.join(", ", cameras.stream().map(Camera::getName).toList()));
				} else {
					sender.sendMessage("Do not specify camera id when listing CUI instance or camera");
				}
				return true;
			}
			case "monitor" -> {
				if (!(sender instanceof Player player)) {
					sender.sendMessage("Player required");
					return false;
				}
				var cuiType = plugin.getCUIManager().getCUIType(CUIMonitor.class);
				var camera = cuiType.display(player, false);
				if (camera == null) {
					sender.sendMessage("Failed to display CUI monitor for " + player.getName());
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
				if (parsed.cuiType() == null) {
					sender.sendMessage("CUI type not found: " + args[1]);
					return true;
				}
				Camera<?> camera;

				if (parsed.instanceId() == null || parsed.cameraId() == null) {
					sender.sendMessage("Instance id and camera id required");
					return false;
				} else {
					var cui = parsed.cuiType().getInstance(parsed.instanceId());
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
			case 1 -> List.of("close", "create", "destroy", "display", "help", "list", "monitor", "open");
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
					case "display" -> {
						yield plugin.getCUIManager().getRegisteredCUITypes().stream().filter(CUIType::canDisplay)
								.map(cuiType -> cuiType.getKey().toString()).toList();
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
					case "display", "open" -> {
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
					case "display", "open" -> {
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
