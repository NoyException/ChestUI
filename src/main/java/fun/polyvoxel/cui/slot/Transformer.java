package fun.polyvoxel.cui.slot;

import fun.polyvoxel.cui.event.CUIClickEvent;

import fun.polyvoxel.cui.util.ItemStacks;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class Transformer extends Slot {
	private List<Function<ItemStack, ItemStack>> transformations;
	private Consumer<CUIClickEvent<?>> clickHandler;

	@Override
	public ItemStack display(ItemStack legacy) {
		var itemStack = ItemStacks.clone(legacy);
		for (Function<ItemStack, ItemStack> transformation : transformations) {
			itemStack = transformation.apply(itemStack);
		}
		return itemStack;
	}

	@Override
	public void set(ItemStack itemStack, @Nullable Player player) {
	}

	@Override
	public void click(CUIClickEvent<?> event) {
		if (clickHandler != null) {
			event.setCancelled(true);
			clickHandler.accept(event);
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
		var filter = new Transformer();
		filter.transformations = this.transformations;
		filter.clickHandler = clickHandler;
		return filter;
	}

	public static Builder builder() {
		return new Transformer.Builder();
	}

	public static class Builder {
		private final List<Function<ItemStack, ItemStack>> transformations = new ArrayList<>();
		private Consumer<CUIClickEvent<?>> clickHandler;

		private Builder() {
		}

		public Transformer build() {
			var filter = new Transformer();
			filter.transformations = new ArrayList<>(transformations);
			filter.clickHandler = clickHandler;
			return filter;
		}

		public Builder filter(Function<ItemStack, ItemStack> transformation) {
			this.transformations.add(transformation);
			return this;
		}

		public Builder changeDisplayName(Component displayName) {
			return filter(itemStack -> {
				itemStack.editMeta(meta -> meta.displayName(displayName));
				return itemStack;
			});
		}

		public Builder changeDisplayName(Function<Component, Component> displayName) {
			return filter(itemStack -> {
				itemStack.editMeta(meta -> meta.displayName(displayName.apply(meta.displayName())));
				return itemStack;
			});
		}

		public Builder appendLore(Component... lore) {
			return filter(itemStack -> {
				itemStack.editMeta(meta -> {
					var newLore = meta.hasLore()
							? new ArrayList<>(Objects.requireNonNull(meta.lore()))
							: new ArrayList<Component>();
					newLore.addAll(List.of(lore));
					meta.lore(newLore);
				});
				return itemStack;
			});
		}

		public Builder changeLore(Component... lore) {
			return filter(itemStack -> {
				itemStack.editMeta(meta -> meta.lore(List.of(lore)));
				return itemStack;
			});
		}

		// TODO: 自制一个GlowEnchant
		public Builder enchant() {
			return filter(itemStack -> {
				itemStack.editMeta(meta -> {
					meta.addEnchant(Enchantment.UNBREAKING, 1, true);
					meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
				});
				return itemStack;
			});
		}

		public Builder click(Consumer<CUIClickEvent<?>> clickHandler) {
			this.clickHandler = clickHandler;
			return this;
		}
	}
}
