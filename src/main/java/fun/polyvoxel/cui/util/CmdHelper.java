package fun.polyvoxel.cui.util;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@ApiStatus.Experimental
public class CmdHelper {

	public static class EnumArgument<E extends Enum<E>> implements CustomArgumentType.Converted<E, String> {
		private final Class<E> enumClass;
		private final E[] values;
		private final @Nullable Function<E, Component> suggestionProvider;

		public EnumArgument(@NotNull Class<E> enumClass) {
			this(enumClass, null);
		}

		public EnumArgument(@NotNull Class<E> enumClass, @Nullable Function<E, Component> suggestionProvider) {
			this.enumClass = enumClass;
			this.values = enumClass.getEnumConstants();
			this.suggestionProvider = suggestionProvider;
		}

		@Override
		public E convert(String nativeType) throws CommandSyntaxException {
			try {
				return E.valueOf(enumClass, nativeType.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException ignored) {
				Message message = MessageComponentSerializer.message()
						.serialize(Component.text("Invalid argument: %s".formatted(nativeType), NamedTextColor.RED));

				throw new CommandSyntaxException(new SimpleCommandExceptionType(message), message);
			}
		}

		@Override
		public ArgumentType<String> getNativeType() {
			return StringArgumentType.word();
		}

		@Override
		public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context,
				SuggestionsBuilder builder) {
			if (suggestionProvider == null) {
				for (E e : values) {
					builder.suggest(e.name());
				}
			} else {
				for (E e : values) {
					builder.suggest(e.name(),
							MessageComponentSerializer.message().serialize(suggestionProvider.apply(e)));
				}
			}
			return builder.buildFuture();
		}
	}

	public static CommandSender asOpSender(Player player) {
		return new AsOpSender(player);
	}

	public static List<Entity> selectEntities(CommandSender sender, String selector) {
		if (sender instanceof AsOpSender op) {
			return op.selectEntities(selector);
		} else {
			return Bukkit.selectEntities(sender, selector);
		}
	}

	public static void performCommandAsOp(Player player, String command) {
		Bukkit.dispatchCommand(asOpSender(player), "execute as " + player.getName() + " at @s run " + command);
	}

	private static class AsOpSender implements ConsoleCommandSender {
		private final Player player;
		private final PermissibleBase perm = new PermissibleBase(this);

		public AsOpSender(Player player) {
			this.player = player;
		}

		public List<Entity> selectEntities(@NotNull String selector) {
			return Bukkit.selectEntities(player, selector);
		}

		@Override
		public void sendMessage(@NotNull String message) {
			player.sendMessage(message);
		}

		@Override
		public void sendMessage(@NotNull String... messages) {
			player.sendMessage(messages);
		}

		@Deprecated
		@Override
		public void sendMessage(@Nullable UUID sender, @NotNull String message) {
			player.sendMessage(sender, message);
		}

		@Deprecated
		@Override
		public void sendMessage(@Nullable UUID sender, @NotNull String... messages) {
			player.sendMessage(sender, messages);
		}

		@Override
		public @NotNull Server getServer() {
			return player.getServer();
		}

		@Override
		public @NotNull String getName() {
			return player.getName();
		}

		@NotNull
		@Override
		public Spigot spigot() {
			return player.spigot();
		}

		@Override
		public @NotNull Component name() {
			return player.name();
		}

		@Override
		public boolean isPermissionSet(@NotNull String name) {
			return this.perm.isPermissionSet(name);
		}

		@Override
		public boolean isPermissionSet(@NotNull Permission perm) {
			return this.perm.isPermissionSet(perm);
		}

		@Override
		public boolean hasPermission(@NotNull String name) {
			return this.perm.hasPermission(name);
		}

		@Override
		public boolean hasPermission(@NotNull Permission perm) {
			return this.perm.hasPermission(perm);
		}

		@Override
		public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name,
				boolean value) {
			return this.perm.addAttachment(plugin, name, value);
		}

		@Override
		public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) {
			return this.perm.addAttachment(plugin);
		}

		@Override
		public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value,
				int ticks) {
			return this.perm.addAttachment(plugin, name, value, ticks);
		}

		@Override
		public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) {
			return this.perm.addAttachment(plugin, ticks);
		}

		@Override
		public void removeAttachment(@NotNull PermissionAttachment attachment) {
			this.perm.removeAttachment(attachment);
		}

		@Override
		public void recalculatePermissions() {
			this.perm.recalculatePermissions();
		}

		@Override
		public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
			return this.perm.getEffectivePermissions();
		}

		@Override
		public boolean isOp() {
			return true;
		}

		@Override
		public void setOp(boolean value) {
		}

		@Override
		public boolean isConversing() {
			return player.isConversing();
		}

		@Override
		public void acceptConversationInput(@NotNull String input) {
			player.acceptConversationInput(input);
		}

		@Override
		public boolean beginConversation(@NotNull Conversation conversation) {
			return player.beginConversation(conversation);
		}

		@Override
		public void abandonConversation(@NotNull Conversation conversation) {
			player.abandonConversation(conversation);
		}

		@Override
		public void abandonConversation(@NotNull Conversation conversation,
				@NotNull ConversationAbandonedEvent details) {
			player.abandonConversation(conversation, details);
		}

		@Override
		public void sendRawMessage(@NotNull String message) {
			player.sendRawMessage(message);
		}

		@Deprecated
		@Override
		public void sendRawMessage(@Nullable UUID sender, @NotNull String message) {
			player.sendRawMessage(sender, message);
		}
	}
}
