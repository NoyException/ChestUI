package fun.polyvoxel.cui.slot;

import fun.polyvoxel.cui.event.CUIClickEvent;

import fun.polyvoxel.cui.event.CUIDropAllEvent;
import fun.polyvoxel.cui.util.ItemStacks;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class Transformer extends Slot {
	private List<Function<ItemStack, ItemStack>> transformations;
	private Consumer<CUIClickEvent<?>> clickHandler;
	private boolean enabled = true;

	@Override
	public ItemStack display(@Nullable ItemStack legacy) {
		if (!enabled) {
			return legacy;
		}
		var itemStack = ItemStacks.clone(legacy);
		for (Function<ItemStack, ItemStack> transformation : transformations) {
			itemStack = transformation.apply(itemStack);
		}
		// 继承防刷物品标记，如果无中生有也应该打上标记
		if ((ItemStacks.isEmpty(legacy) || ItemStacks.hasTag(legacy, "cui")) && !ItemStacks.isEmpty(itemStack)) {
			ItemStacks.addTag(itemStack, "cui");
		}
		return itemStack;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		markDirty();
	}

	@Override
	public boolean prepareClick(@NotNull CUIClickEvent<?> event) {
		if (clickHandler == null) {
			return false;
		}
		event.setSlot(this);
		return true;
	}

	@Override
	public void click(@NotNull CUIClickEvent<?> event) {
		if (clickHandler == null) {
			return;
		}
		clickHandler.accept(event);
	}

	@Override
	public void prepareDrop(CUIDropAllEvent<?> event) {
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

		public Builder changeItemStack(ItemStack itemStack) {
			var copy = ItemStacks.clone(itemStack);
			return filter(i -> copy);
		}

		public Builder changeMaterial(Material material) {
			if (material == null) {
				return filter(itemStack -> ItemStack.empty());
			}
			return filter(itemStack -> {
				if (ItemStacks.isEmpty(itemStack)) {
					return ItemStack.of(material);
				}
				return itemStack.withType(material);
			});
		}

		public Builder changeModel(Material material) {
			return changeModel(material.getKey());
		}

		public Builder changeModel(NamespacedKey model) {
			return filter(itemStack -> {
				itemStack.editMeta(itemMeta -> {
					itemMeta.setItemModel(model);
				});
				return itemStack;
			});
		}

		public Builder changeDisplayName(Component displayName) {
			return filter(itemStack -> {
				if (ItemStacks.isEmpty(itemStack)) {
					return itemStack;
				}
				itemStack.editMeta(meta -> meta.displayName(displayName));
				return itemStack;
			});
		}

		public Builder changeDisplayName(Function<Component, Component> displayName) {
			return filter(itemStack -> {
				if (ItemStacks.isEmpty(itemStack)) {
					return itemStack;
				}
				itemStack.editMeta(meta -> meta.displayName(displayName.apply(meta.displayName())));
				return itemStack;
			});
		}

		public Builder appendLore(Component... lore) {
			return filter(itemStack -> {
				if (ItemStacks.isEmpty(itemStack)) {
					return itemStack;
				}
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
				if (ItemStacks.isEmpty(itemStack)) {
					return itemStack;
				}
				itemStack.editMeta(meta -> meta.lore(List.of(lore)));
				return itemStack;
			});
		}

		public Builder enchant() {
			return filter(itemStack -> {
				if (ItemStacks.isEmpty(itemStack)) {
					return itemStack;
				}
				itemStack.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
				return itemStack;
			});
		}

		public Builder disenchant() {
			return filter(itemStack -> {
				if (ItemStacks.isEmpty(itemStack)) {
					return itemStack;
				}
				itemStack.editMeta(meta -> meta.setEnchantmentGlintOverride(false));
				return itemStack;
			});
		}

		public Builder click(Consumer<CUIClickEvent<?>> clickHandler) {
			this.clickHandler = clickHandler;
			return this;
		}
	}
}
