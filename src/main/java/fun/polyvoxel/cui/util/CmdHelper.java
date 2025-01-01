package fun.polyvoxel.cui.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
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
import java.util.Set;
import java.util.UUID;

@ApiStatus.Experimental
public class CmdHelper {

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

	private static class AsOpSender implements CommandSender {
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
	}
}
