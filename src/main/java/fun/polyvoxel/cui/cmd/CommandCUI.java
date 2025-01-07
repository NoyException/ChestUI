package fun.polyvoxel.cui.cmd;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.prebuilt.CUIMonitor;
import fun.polyvoxel.cui.ui.CUIInstance;
import fun.polyvoxel.cui.ui.CUIType;
import fun.polyvoxel.cui.ui.Camera;
import fun.polyvoxel.cui.ui.DisplayContext;
import fun.polyvoxel.cui.ui.source.CommandDisplaySource;
import fun.polyvoxel.cui.util.ItemStacks;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class CommandCUI {
	private final CUIPlugin plugin;
	private final CUITypeArgument cuiTypeArgument = new CUITypeArgument();
	private final CUIInstanceArgument cuiInstanceArgument = new CUIInstanceArgument();

	public CommandCUI(CUIPlugin plugin) {
		this.plugin = plugin;
	}

	public class CUITypeArgument implements CustomArgumentType.Converted<CUIType<?>, NamespacedKey> {
		@Override
		public @NotNull CUIType<?> convert(@NotNull NamespacedKey nativeType) throws CommandSyntaxException {
			CUIType<?> cuiType;
			try {
				cuiType = plugin.getCUIManager().getCUIType(nativeType);
			} catch (IllegalArgumentException ignored) {
				Message message = MessageComponentSerializer.message().serialize(
						Component.text("Invalid namespaced key: %s".formatted(nativeType), NamedTextColor.RED));
				throw new SimpleCommandExceptionType(message).create();
			}
			if (cuiType == null) {
				Message message = MessageComponentSerializer.message()
						.serialize(Component.text("CUI type not found: %s".formatted(nativeType), NamedTextColor.RED));
				throw new SimpleCommandExceptionType(message).create();
			}
			return cuiType;
		}

		@Override
		public @NotNull ArgumentType<NamespacedKey> getNativeType() {
			return ArgumentTypes.namespacedKey();
		}

		@Override
		public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context,
				@NotNull SuggestionsBuilder builder) {
			for (CUIType<?> cuiType : plugin.getCUIManager().getRegisteredCUITypes()) {
				builder.suggest(cuiType.getKey().toString(),
						MessageComponentSerializer.message().serialize(Component.text("CUI Type Key")));
			}
			return builder.buildFuture();
		}
	}

	public interface CUIInstanceArgumentResolver {
		@NotNull
		CUIInstance<?> resolve(@NotNull CUIType<?> cuiType) throws CommandSyntaxException;
	}

	public static class CUIInstanceArgument
			implements
				CustomArgumentType.Converted<CUIInstanceArgumentResolver, Integer> {

		@Override
		public @NotNull CUIInstanceArgumentResolver convert(@NotNull Integer nativeType) {
			return cuiType -> {
				CUIInstance<?> instance = cuiType.getInstance(nativeType);
				if (instance == null) {
					throw new SimpleCommandExceptionType(
							MessageComponentSerializer.message()
									.serialize(Component.text("Instance not found for %s-#%d"
											.formatted(cuiType.getKey().asString(), nativeType), NamedTextColor.RED)))
							.create();
				}
				return instance;
			};
		}

		@Override
		public @NotNull ArgumentType<Integer> getNativeType() {
			return IntegerArgumentType.integer();
		}

		@Override
		public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context,
				@NotNull SuggestionsBuilder builder) {
			try {
				CUIType<?> cuiType = context.getChild().getArgument("cui", CUIType.class);
				for (var instance : cuiType.getInstances()) {
					builder.suggest(instance.getId(),
							MessageComponentSerializer.message().serialize(Component.text("CUI Instance ID")));
				}
			} catch (Throwable ignored) {
			}
			return builder.buildFuture();
		}
	}

	public interface CameraArgumentResolver {
		@NotNull
		Camera<?> resolve(@NotNull CUIInstance<?> instance) throws CommandSyntaxException;
	}

	public static class CameraArgument implements CustomArgumentType.Converted<CameraArgumentResolver, Integer> {
		@Override
		public @NotNull CameraArgumentResolver convert(@NotNull Integer nativeType) {
			return instance -> {
				Camera<?> camera = instance.getCamera(nativeType);
				if (camera == null) {
					throw new SimpleCommandExceptionType(MessageComponentSerializer.message()
							.serialize(Component.text("Camera not found for %s-#%d-#%d"
									.formatted(instance.getType().getKey().asString(), instance.getId(), nativeType),
									NamedTextColor.RED)))
							.create();
				}
				return camera;
			};
		}

		@Override
		public @NotNull ArgumentType<Integer> getNativeType() {
			return IntegerArgumentType.integer();
		}

		@Override
		public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context,
				@NotNull SuggestionsBuilder builder) {
			try {
				CUIInstance<?> instance = context.getChild().getArgument("inst-id", CUIInstanceArgumentResolver.class)
						.resolve(context.getChild().getArgument("cui", CUIType.class));
				for (var camera : instance.getCameras()) {
					builder.suggest(camera.getId(),
							MessageComponentSerializer.message().serialize(Component.text("Camera ID")));
				}
			} catch (Throwable ignored) {
			}
			return builder.buildFuture();
		}
	}

	public List<Player> getPlayers(CommandContext<CommandSourceStack> ctx, boolean isPlayerSpecified)
			throws CommandSyntaxException {
		if (isPlayerSpecified) {
			var resolver = ctx.getArgument("players", PlayerSelectorArgumentResolver.class);
			return resolver.resolve(ctx.getSource());
		} else {
			var executor = ctx.getSource().getExecutor();
			if (!(executor instanceof Player player)) {
				throw new SimpleCommandExceptionType(MessageComponentSerializer.message()
						.serialize(Component.text("Player required", NamedTextColor.RED))).create();
			}
			return List.of(player);
		}
	}

	public static void printHelp(@NotNull CommandSender sender) {
		sender.sendMessage("Usage:");
		sender.sendMessage("/cui help: 显示帮助信息.");
		sender.sendMessage("/cui bind: 将手中的物品绑定到<cui>.");
		sender.sendMessage("/cui close: (强制)关闭指定玩家的顶部/所有摄像头.");
		// sender.sendMessage("/cui create: 创造一个<cui>类型的实例/指定实例的摄像头.");
		sender.sendMessage("/cui destroy: 摧毁<cui>类型的全部实例/指定实例/指定实例的指定摄像头.");
		sender.sendMessage("/cui display: 为指定玩家一键显示<cui>.");
		sender.sendMessage("/cui monitor: 打开CUI监视器.");
		sender.sendMessage("/cui open: 打开指定<cui>类型的指定实例的指定摄像头.");
	}

	private LiteralArgumentBuilder<CommandSourceStack> bind() {
		return Commands.literal("bind").then(Commands.argument("cui", cuiTypeArgument).executes(ctx -> {
			var executor = ctx.getSource().getExecutor();
			if (!(executor instanceof Player player)) {
				throw new SimpleCommandExceptionType(MessageComponentSerializer.message()
						.serialize(Component.text("Player required", NamedTextColor.RED))).create();
			}
			ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
			if (ItemStacks.isEmpty(itemInMainHand)) {
				throw new SimpleCommandExceptionType(MessageComponentSerializer.message()
						.serialize(Component.text("Main hand item required", NamedTextColor.RED))).create();
			}
			// TODO: bind item to CUI
			return Command.SINGLE_SUCCESS;
		}));
	}

	private LiteralArgumentBuilder<CommandSourceStack> close() {
		return Commands.literal("close").then(Commands.argument("players", ArgumentTypes.players())
				.then(Commands.literal("top").then(Commands.literal("force").executes(ctx -> {
					// close <players> top force
					var resolver = ctx.getArgument("players", PlayerSelectorArgumentResolver.class);
					var players = resolver.resolve(ctx.getSource());
					players.forEach(player -> plugin.getCameraManager().closeTop(player, true));
					return Command.SINGLE_SUCCESS;
				})).executes(ctx -> {
					// close <players> top
					var resolver = ctx.getArgument("players", PlayerSelectorArgumentResolver.class);
					var players = resolver.resolve(ctx.getSource());
					players.forEach(player -> plugin.getCameraManager().closeTop(player, false));
					return Command.SINGLE_SUCCESS;
				})).then(Commands.literal("all").then(Commands.literal("force").executes(ctx -> {
					// close <players> all force
					var resolver = ctx.getArgument("players", PlayerSelectorArgumentResolver.class);
					var players = resolver.resolve(ctx.getSource());
					players.forEach(player -> plugin.getCameraManager().closeAll(player, true));
					return Command.SINGLE_SUCCESS;
				})).executes(ctx -> {
					// close <players> all
					var resolver = ctx.getArgument("players", PlayerSelectorArgumentResolver.class);
					var players = resolver.resolve(ctx.getSource());
					players.forEach(player -> plugin.getCameraManager().closeAll(player, false));
					return Command.SINGLE_SUCCESS;
				})));
	}

	// private LiteralArgumentBuilder<CommandSourceStack> create() {
	// return Commands.literal("create").then(Commands.argument("cui",
	// cuiTypeArgument).then(
	// Commands.argument("inst-id",
	// cuiInstanceArgument).then(Commands.literal("keep-alive").executes(ctx -> {
	// // create <cui> <inst-id> keep-alive
	// var cuiType = ctx.getArgument("cui", CUIType.class);
	// var instance = ctx.getArgument("inst-id",
	// CUIInstanceArgumentResolver.class).resolve(cuiType);
	// var camera = instance.createCamera().edit().keepAlive(true).done();
	// ctx.getSource().getSender()
	// .sendMessage(Component.text("Camera created: ")
	// .append(Component.text("#" + camera.getId(), NamedTextColor.AQUA))
	// .append(Component.text(" (KeepAlive)", NamedTextColor.RED)));
	// return Command.SINGLE_SUCCESS;
	// })).executes(ctx -> {
	// // create <cui> <inst-id>
	// var cuiType = ctx.getArgument("cui", CUIType.class);
	// var instance = ctx.getArgument("inst-id",
	// CUIInstanceArgumentResolver.class).resolve(cuiType);
	// var camera = instance.createCamera();
	// ctx.getSource().getSender().sendMessage(Component.text("Camera created: ")
	// .append(Component.text("#" + camera.getId(), NamedTextColor.AQUA)));
	// return Command.SINGLE_SUCCESS;
	// })).then(Commands.literal("keep-alive").executes(ctx -> {
	// // create <cui> keep-alive
	// var cuiType = ctx.getArgument("cui", CUIType.class);
	// var instance = cuiType.createInstance().edit().keepAlive(true).done();
	// ctx.getSource().getSender()
	// .sendMessage(Component.text("Instance created: ")
	// .append(Component.text("#" + instance.getId(), NamedTextColor.AQUA))
	// .append(Component.text(" (KeepAlive)", NamedTextColor.RED)));
	// return Command.SINGLE_SUCCESS;
	// })).executes(ctx -> {
	// // create <cui>
	// var cuiType = ctx.getArgument("cui", CUIType.class);
	// var instance = cuiType.createInstance();
	// ctx.getSource().getSender().sendMessage(Component.text("Instance created: ")
	// .append(Component.text("#" + instance.getId(), NamedTextColor.AQUA)));
	// return Command.SINGLE_SUCCESS;
	// }));
	// }

	private LiteralArgumentBuilder<CommandSourceStack> destroy() {
		return Commands.literal("destroy")
				.then(Commands.argument("cui", cuiTypeArgument).then(Commands.argument("inst-id", cuiInstanceArgument)
						.then(Commands.argument("camera-id", new CameraArgument()).executes(ctx -> {
							// destroy <cui> <inst-id> <camera>
							var cuiType = ctx.getArgument("cui", CUIType.class);
							var instance = ctx.getArgument("inst-id", CUIInstanceArgumentResolver.class)
									.resolve(cuiType);
							var camera = ctx.getArgument("camera-id", CameraArgumentResolver.class).resolve(instance);
							camera.destroy();
							ctx.getSource().getSender().sendMessage(Component.text("Camera destroyed: ")
									.append(Component.text("#" + camera.getId(), NamedTextColor.AQUA)));
							return Command.SINGLE_SUCCESS;
						})).executes(ctx -> {
							// destroy <cui> <inst-id>
							var cuiType = ctx.getArgument("cui", CUIType.class);
							var instance = ctx.getArgument("inst-id", CUIInstanceArgumentResolver.class)
									.resolve(cuiType);
							instance.destroy();
							ctx.getSource().getSender().sendMessage(Component.text("Instance destroyed: ")
									.append(Component.text("#" + instance.getId(), NamedTextColor.AQUA)));
							return Command.SINGLE_SUCCESS;
						})).executes(ctx -> {
							// destroy <cui>
							CUIType<?> cuiType = ctx.getArgument("cui", CUIType.class);
							cuiType.getInstances().forEach(CUIInstance::destroy);
							ctx.getSource().getSender().sendMessage(Component.text("All instances destroyed: ")
									.append(Component.text(cuiType.getKey().asString(), NamedTextColor.AQUA)));
							return Command.SINGLE_SUCCESS;
						}));
	}

	private boolean display(CommandContext<CommandSourceStack> ctx, boolean isPlayerSpecified, boolean asChild)
			throws CommandSyntaxException {
		var players = getPlayers(ctx, isPlayerSpecified);
		var cuiType = (CUIType<?>) ctx.getArgument("cui", CUIType.class);
		players.forEach(player -> cuiType
				.display(new DisplayContext<>(player, asChild, new CommandDisplaySource(ctx.getSource()))));
		return true;
	}

	private LiteralArgumentBuilder<CommandSourceStack> display() {
		return Commands.literal("display").then(Commands.argument("cui", cuiTypeArgument).then(Commands
				.argument("players", ArgumentTypes.players()).then(Commands.literal("as-child").executes(ctx -> {
					// display <cui> <players> as-child
					if (!display(ctx, true, true)) {
						throw new SimpleCommandExceptionType(MessageComponentSerializer.message()
								.serialize(Component.text("Failed to display", NamedTextColor.RED))).create();
					}
					return Command.SINGLE_SUCCESS;
				})).executes(ctx -> {
					// display <cui> <players>
					if (!display(ctx, true, false)) {
						throw new SimpleCommandExceptionType(MessageComponentSerializer.message()
								.serialize(Component.text("Failed to display", NamedTextColor.RED))).create();
					}
					return Command.SINGLE_SUCCESS;
				})).then(Commands.literal("as-child").executes(ctx -> {
					// display <cui> as-child
					if (!display(ctx, false, true)) {
						throw new SimpleCommandExceptionType(MessageComponentSerializer.message()
								.serialize(Component.text("Failed to display", NamedTextColor.RED))).create();
					}
					return Command.SINGLE_SUCCESS;
				})).executes(ctx -> {
					// display <cui>
					if (!display(ctx, false, false)) {
						throw new SimpleCommandExceptionType(MessageComponentSerializer.message()
								.serialize(Component.text("Failed to display", NamedTextColor.RED))).create();
					}
					return Command.SINGLE_SUCCESS;
				}));
	}

	private LiteralArgumentBuilder<CommandSourceStack> monitor() {
		return Commands.literal("monitor").executes(ctx -> {
			// monitor
			var executor = ctx.getSource().getExecutor();
			if (!(executor instanceof Player player)) {
				throw new SimpleCommandExceptionType(MessageComponentSerializer.message()
						.serialize(Component.text("Player required", NamedTextColor.RED))).create();
			}
			var cuiType = plugin.getCUIManager().getCUIType(CUIMonitor.class);
			var res = cuiType.display(new DisplayContext<>(player, false, new CommandDisplaySource(ctx.getSource())));
			if (!res) {
				throw new SimpleCommandExceptionType(MessageComponentSerializer.message()
						.serialize(Component.text("Failed to open monitor", NamedTextColor.RED))).create();
			}
			return Command.SINGLE_SUCCESS;
		});
	}

	private void open(CommandContext<CommandSourceStack> ctx, boolean isPlayerSpecified, boolean asChild)
			throws CommandSyntaxException {
		var players = getPlayers(ctx, isPlayerSpecified);
		var cuiType = ctx.getArgument("cui", CUIType.class);
		var instance = ctx.getArgument("inst-id", CUIInstanceArgumentResolver.class).resolve(cuiType);
		var camera = ctx.getArgument("camera-id", CameraArgumentResolver.class).resolve(instance);
		players.forEach(player -> camera.open(player, asChild, new CommandDisplaySource(ctx.getSource())));
	}

	private LiteralArgumentBuilder<CommandSourceStack> open() {
		return Commands.literal("open")
				.then(Commands.argument("cui", cuiTypeArgument)
						.then(Commands.argument("inst-id", cuiInstanceArgument)
								.then(Commands.argument("camera-id", new CameraArgument())
										.then(Commands.argument("players", ArgumentTypes.players())
												.then(Commands.literal("as-child").executes(ctx -> {
													// open <cui> <inst-id> <camera> <players> as-child
													open(ctx, true, true);
													return Command.SINGLE_SUCCESS;
												})).executes(ctx -> {
													// open <cui> <inst-id> <camera> <players>
													open(ctx, true, false);
													return Command.SINGLE_SUCCESS;
												}))
										.then(Commands.literal("as-child").executes(ctx -> {
											// open <cui> <inst-id> <camera> as-child
											open(ctx, false, true);
											return Command.SINGLE_SUCCESS;
										})).executes(ctx -> {
											// open <cui> <inst-id> <camera>
											open(ctx, false, false);
											return Command.SINGLE_SUCCESS;
										}))));
	}

	public void register() {
		plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
			event.registrar()
					.register(Commands.literal("cui")
							.requires(commandSourceStack -> commandSourceStack.getSender().isOp())
							.then(Commands.literal("help").executes(ctx -> {
								printHelp(ctx.getSource().getSender());
								return Command.SINGLE_SUCCESS;
							})).then(bind()).then(close()).then(destroy()).then(display()).then(monitor()).then(open())
							.build(), "Manage Chest UI");
		});
	}
}
