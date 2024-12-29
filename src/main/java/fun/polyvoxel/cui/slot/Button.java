package fun.polyvoxel.cui.slot;

import com.google.common.collect.HashMultimap;
import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.event.CUIClickEvent;
import fun.polyvoxel.cui.util.ItemStacks;

import com.destroystokyo.paper.profile.PlayerProfile;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class Button extends Slot {
	private Consumer<CUIClickEvent<?>> clickHandler;
	private ItemStack itemStack;

	private Button() {
	}

	@Override
	public ItemStack display(ItemStack legacy) {
		return itemStack;
	}

	@Override
	public void set(ItemStack itemStack, @Nullable Player player) {
		this.itemStack = itemStack;
		markDirty();
	}

	@Override
	public void click(CUIClickEvent<?> event) {
		event.setCancelled(true);
		if (clickHandler != null) {
			try {
				clickHandler.accept(event);
			} catch (Exception e) {
				e.printStackTrace();
				CUIPlugin.logger().warn("An error occurred while handling a button click event.");
				var plugin = event.getCUIInstance().getType().getPlugin();
				if (plugin == null) {
					CUIPlugin.logger().warn(
							"This is a bug of the ChestUI loaded from json. Please check the json file, or report it to the plugin author.");
				} else if (plugin.getClass() == CUIPlugin.class) {
					CUIPlugin.logger().warn("This is a bug of ChestUI, please report it to the plugin author.");
				} else {
					CUIPlugin.logger().warn(
							"This is not a bug of ChestUI, please report it to the plugin author of {}.",
							plugin.getName());
				}
			}
		}
	}

	@Override
	public ItemStack place(ItemStack itemStack, @Nullable Player player) {
		return itemStack;
	}

	@Override
	public ItemStack collect(ItemStack itemStack, @Nullable Player player) {
		return itemStack;
	}

	@Override
	public Slot deepClone() {
		var button = new Button();
		button.clickHandler = clickHandler;
		button.itemStack = ItemStacks.clone(itemStack);
		return button;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private Consumer<CUIClickEvent<?>> clickHandler;
		private ItemStack itemStack = ItemStack.of(Material.STONE_BUTTON);
		private int amount = 1;
		private Component displayName;
		private List<Component> lore;
		private ItemFlag[] itemFlags = ItemFlag.values();
		private Consumer<ItemMeta> metaModifier;

		private Builder() {
		}

		public Button build() {
			var button = new Button();
			button.clickHandler = clickHandler;
			button.itemStack = ItemStacks.clone(itemStack);
			button.itemStack.setAmount(amount);
			button.itemStack.editMeta(itemMeta -> {
				if (displayName != null) {
					itemMeta.displayName(displayName);
				}
				if (lore != null) {
					itemMeta.lore(lore);
				}
				// if(itemMeta instanceof ArmorMeta)
				var modifiers = itemMeta.getAttributeModifiers();
				if (modifiers == null) {
					modifiers = HashMultimap.create();
					itemMeta.setAttributeModifiers(modifiers);
				}
				itemMeta.addItemFlags(itemFlags);
				if (metaModifier != null) {
					metaModifier.accept(itemMeta);
				}
			});
			return button;
		}

		public Builder itemStack(ItemStack itemStack) {
			this.itemStack = itemStack;
			return this;
		}

		public Builder skull(Player player) {
			PlayerProfile profile = Bukkit.getServer().createProfile(player.getUniqueId(), player.getName());
			PlayerTextures textures = profile.getTextures();
			profile.setTextures(textures);
			itemStack = ItemStack.of(Material.PLAYER_HEAD);
			itemStack.editMeta(meta -> ((SkullMeta) meta).setPlayerProfile(profile));
			return this;
		}

		public Builder skull(URL skullTexture) {
			PlayerProfile profile = Bukkit.getServer().createProfileExact(UUID.randomUUID(),
					UUID.randomUUID().toString());
			PlayerTextures textures = profile.getTextures();
			textures.setSkin(skullTexture);
			profile.setTextures(textures);
			itemStack = ItemStack.of(Material.PLAYER_HEAD);;
			itemStack.editMeta(meta -> ((SkullMeta) meta).setPlayerProfile(profile));
			return this;
		}

		public Builder material(Material material) {
			itemStack = ItemStack.of(material);
			return this;
		}

		public Builder amount(int amount) {
			this.amount = amount;
			return this;
		}

		public Builder meta(Consumer<ItemMeta> metaModifier) {
			this.metaModifier = metaModifier;
			return this;
		}

		public Builder displayName(Component displayName) {
			this.displayName = ItemStacks.cleanComponent(displayName);
			return this;
		}

		public Builder displayName(String displayName) {
			return displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName));
		}

		public Builder lore(List<Component> lore) {
			this.lore = lore.stream().map(ItemStacks::cleanComponent).toList();
			return this;
		}

		public Builder lore(Component... lore) {
			return lore(List.of(lore));
		}

		public Builder lore(String... lore) {
			var list = Arrays.stream(lore)
					.map(s -> (Component) LegacyComponentSerializer.legacyAmpersand().deserialize(s)).toList();
			return lore(list);
		}

		public Builder itemFlags(ItemFlag... itemFlags) {
			this.itemFlags = itemFlags;
			return this;
		}

		/**
		 * 设置按钮的点击事件。触发时事件会被自动拦截取消，如果你想让点击事件继续传递给下一层，请使用{@link CUIClickEvent#setCancelled(boolean)}。
		 * <br>
		 * Set the click event of the button. The event will be automatically
		 * intercepted and canceled when triggered. If you want the click event to
		 * continue to be passed to the next layer, please use
		 * {@link CUIClickEvent#setCancelled(boolean)}.
		 *
		 * @param clickHandler
		 *            点击事件处理器<br>
		 *            Click event handler
		 * @return 构建器<br>
		 *         Builder
		 */
		public Builder clickHandler(Consumer<CUIClickEvent<?>> clickHandler) {
			this.clickHandler = clickHandler;
			return this;
		}
	}
}
