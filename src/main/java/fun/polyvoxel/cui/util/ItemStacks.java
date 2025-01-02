package fun.polyvoxel.cui.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.common.collect.HashMultimap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class ItemStacks {
	public static boolean isEmpty(ItemStack itemStack) {
		return itemStack == null || itemStack.isEmpty();
	}

	public static boolean isFull(ItemStack itemStack) {
		if (isEmpty(itemStack)) {
			return false;
		}
		return itemStack.getAmount() >= itemStack.getMaxStackSize();
	}

	public static int getAmount(ItemStack itemStack) {
		return isEmpty(itemStack) ? 0 : itemStack.getAmount();
	}

	public static boolean isSimilar(ItemStack a, ItemStack b) {
		var aEmpty = isEmpty(a);
		var bEmpty = isEmpty(b);
		if (aEmpty && bEmpty) {
			return true;
		}
		if (aEmpty || bEmpty) {
			return false;
		}
		return a.isSimilar(b);
	}

	public static boolean isSame(ItemStack a, ItemStack b) {
		var aEmpty = isEmpty(a);
		var bEmpty = isEmpty(b);
		if (aEmpty && bEmpty) {
			return true;
		}
		if (aEmpty || bEmpty) {
			return false;
		}
		return a.isSimilar(b) && a.getAmount() == b.getAmount();
	}

	public static ItemStack clone(ItemStack itemStack) {
		return itemStack == null ? null : itemStack.clone();
	}

	public record PlaceResult(ItemStack placed, ItemStack remaining, boolean success) {
	}

	/**
	 * 尝试将source放入target中，返回放入后的target和剩余的source。无法放入则返回原target和source。<br>
	 * Try to place source into target, return the placed target and the remaining
	 * source. If it cannot be placed, return the original target and source.
	 * 
	 * @param target
	 *            放入的目标物品<br>
	 *            The target ItemStack to place into
	 * @param source
	 *            放入的源物品<br>
	 *            The source ItemStack to place
	 * @param ignoreAmountLimit
	 *            是否忽略物品堆叠上限<br>
	 *            Whether to ignore the ItemStack limit
	 * @return 放入后的target和剩余的source<br>
	 *         The placed target and the remaining source
	 */
	public static PlaceResult place(ItemStack target, ItemStack source, boolean ignoreAmountLimit) {
		source = isEmpty(source) ? null : source.clone();
		target = isEmpty(target) ? null : target.clone();
		if (source == null) {
			return new PlaceResult(target, source, true);
		}
		if (target == null) {
			return new PlaceResult(source, target, true);
		}
		if (!isSimilar(target, source)) {
			return new PlaceResult(target, source, false);
		}
		var amount = target.getAmount() + source.getAmount();
		if (ignoreAmountLimit) {
			target.setAmount(amount);
			return new PlaceResult(target, null, true);
		} else {
			var max = target.getMaxStackSize();
			if (amount <= max) {
				target.setAmount(amount);
				return new PlaceResult(target, null, true);
			}
			target.setAmount(max);
			source.setAmount(amount - max);
			return new PlaceResult(target, source, true);
		}
	}

	public static @NotNull Component cleanComponent(Component component) {
		if (component.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET)
			component = component.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
		if (component.color() == null)
			component = component.color(NamedTextColor.WHITE);
		return component;
	}

	public static Pair<Position, Position> trim(ItemStack[][] itemStacks) {
		return Array2Ds.trim(itemStacks, ItemStacks::isEmpty);
	}

	public static void addTag(ItemStack itemStack, String tag) {
		itemStack.editMeta(meta -> {
			meta.getPersistentDataContainer().set(new NamespacedKey("tag", tag), PersistentDataType.BYTE, (byte) 1);
		});
	}

	public static boolean hasTag(ItemStack itemStack, String tag) {
		if (isEmpty(itemStack)) {
			return false;
		}
		return itemStack.getItemMeta().getPersistentDataContainer().has(new NamespacedKey("tag", tag),
				PersistentDataType.BYTE);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private ItemStack itemStack = ItemStack.of(Material.GRASS_BLOCK);
		private int amount = 0;
		private Component displayName;
		private List<Component> lore;
		private ItemFlag[] itemFlags = new ItemFlag[0];
		private Consumer<ItemMeta> metaModifier;

		protected Builder() {
		}

		public ItemStack build() {
			var built = ItemStacks.clone(itemStack);
			if (amount != 0) {
				built.setAmount(amount);
			}
			built.editMeta(itemMeta -> {
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
			return built;
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
	}
}
