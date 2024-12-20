package cn.noy.cui.slot;

import cn.noy.cui.event.CUIClickEvent;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class Filter extends Slot {
	private Function<ItemStack, ItemStack> filter;
	private Consumer<CUIClickEvent<?>> clickHandler;

	@Override
	public ItemStack display(ItemStack legacy) {
		return filter.apply(legacy.clone());
	}

	@Override
	public void set(ItemStack itemStack, @Nullable Player player) {
	}

	@Override
	public void click(CUIClickEvent<?> event) {
		if (clickHandler != null) {
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
		var filter = new Filter();
		filter.filter = this.filter;
		filter.clickHandler = clickHandler;
		return filter;
	}

	public static Builder builder() {
		return new Filter.Builder();
	}

	public static class Builder {
		private final Filter filter = new Filter();

		Builder() {
		}

		public Builder filter(Function<ItemStack, ItemStack> filter) {
			this.filter.filter = filter;
			return this;
		}

		public Builder click(Consumer<CUIClickEvent<?>> clickHandler) {
			filter.clickHandler = clickHandler;
			return this;
		}

		public Filter build() {
			return filter;
		}
	}
}
