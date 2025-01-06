package fun.polyvoxel.cui.slot;

import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.event.CUIClickEvent;
import fun.polyvoxel.cui.event.CUIDropAllEvent;
import fun.polyvoxel.cui.util.ItemStacks;

import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Button extends Slot {
	private BiConsumer<CUIClickEvent<?>, Button> clickHandler;
	private ItemStack itemStack;

	private Button() {
	}

	@Override
	public ItemStack display(@Nullable ItemStack legacy) {
		return itemStack;
	}

	@Override
	public void set(ItemStack itemStack) {
		this.itemStack = itemStack;
		markDirty();
	}

	@Override
	public boolean prepareClick(@NotNull CUIClickEvent<?> event) {
		event.setSlot(this);
		return true;
	}

	@Override
	public void click(@NotNull CUIClickEvent<?> event) {
		if (clickHandler == null) {
			return;
		}

		try {
			clickHandler.accept(event, this);
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
				CUIPlugin.logger().warn("This is not a bug of ChestUI, please report it to the plugin author of {}.",
						plugin.getName());
			}
		}
	}

	@Override
	public void prepareDrop(CUIDropAllEvent<?> event) {
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
		private BiConsumer<CUIClickEvent<?>, Button> clickHandler;
		private final ItemStacks.Builder itemStackBuilder = ItemStacks.builder().material(Material.OAK_BUTTON)
				.itemFlags(ItemFlag.values());

		private Builder() {
		}

		public Button build() {
			var button = new Button();
			button.clickHandler = clickHandler;
			button.itemStack = itemStackBuilder.build();
			ItemStacks.addTag(button.itemStack, "cui");
			return button;
		}

		public Builder itemStack(ItemStack itemStack) {
			itemStackBuilder.itemStack(itemStack);
			return this;
		}

		@Deprecated
		public Builder skull(Player player) {
			itemStackBuilder.skull(player);
			return this;
		}

		@Deprecated
		public Builder skull(URL skullTexture) {
			itemStackBuilder.skull(skullTexture);
			return this;
		}

		@Deprecated
		public Builder material(Material material) {
			itemStackBuilder.material(material);
			return this;
		}

		@Deprecated
		public Builder amount(int amount) {
			itemStackBuilder.amount(amount);
			return this;
		}

		@Deprecated
		public Builder meta(Consumer<ItemMeta> metaModifier) {
			itemStackBuilder.meta(metaModifier);
			return this;
		}

		@Deprecated
		public Builder displayName(Component displayName) {
			itemStackBuilder.displayName(displayName);
			return this;
		}

		@Deprecated
		public Builder displayName(String displayName) {
			itemStackBuilder.displayName(displayName);
			return this;
		}

		@Deprecated
		public Builder lore(List<Component> lore) {
			itemStackBuilder.lore(lore);
			return this;
		}

		@Deprecated
		public Builder lore(Component... lore) {
			itemStackBuilder.lore(lore);
			return this;
		}

		@Deprecated
		public Builder lore(String... lore) {
			itemStackBuilder.lore(lore);
			return this;
		}

		@Deprecated
		public Builder itemFlags(ItemFlag... itemFlags) {
			itemStackBuilder.itemFlags(itemFlags);
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
		public Builder click(Consumer<CUIClickEvent<?>> clickHandler) {
			this.clickHandler = (event, button) -> clickHandler.accept(event);
			return this;
		}

		public Builder click(BiConsumer<CUIClickEvent<?>, Button> clickHandler) {
			this.clickHandler = clickHandler;
			return this;
		}
	}
}
