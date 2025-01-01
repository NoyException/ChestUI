package fun.polyvoxel.cui.slot;

import fun.polyvoxel.cui.event.CUIClickEvent;
import fun.polyvoxel.cui.util.ItemStacks;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Storage extends Slot {
	public interface Source {
		ItemStack get();

		void set(ItemStack itemStack, @Nullable Player player);

		void setDirtyMarker(Runnable dirtyMarker);

		default void tick() {
		}

		Source deepClone();

		default boolean placeable(ItemStack itemStack) {
			if (ItemStacks.isEmpty(itemStack)) {
				return false;
			}
			var current = get();
			if (ItemStacks.isEmpty(current)) {
				return true;
			}
			return current.getAmount() < current.getMaxStackSize() && current.isSimilar(itemStack);
		}

		default boolean bidirectional() {
			return true;
		}
	}

	private final Source source;

	private Storage(Source source) {
		this.source = source;
		source.setDirtyMarker(this::markDirty);
	}

	@Override
	public void tick() {
		source.tick();
	}

	@Override
	public ItemStack display(ItemStack legacy) {
		return source.get();
	}

	@Override
	public void set(ItemStack itemStack, @Nullable Player player) {
		source.set(itemStack, player);
	}

	@Override
	public void click(@NotNull CUIClickEvent<?> event) {
		event.setCancelled(true);
		var player = event.getPlayer();
		// Bukkit.getLogger().warning("Click "+event.getClickType());
		if (source.bidirectional()) {
			var itemStack = source.get();
			if (itemStack == null) {
				itemStack = ItemStack.empty();
			}
			var cursor = event.getCursor();
			if (cursor == null) {
				cursor = ItemStack.empty();
			}
			switch (event.getClickType()) {
				case DROP -> {
					// 丢弃【一个】
					if (itemStack.getAmount() > 0) {
						var drop = itemStack.clone();
						drop.setAmount(1);
						itemStack.setAmount(itemStack.getAmount() - 1);
						var location = player.getLocation();
						location.getWorld().dropItemNaturally(location, drop);
					}
				}
				case CONTROL_DROP -> {
					// 丢弃【全部】
					var location = player.getLocation();
					location.getWorld().dropItemNaturally(location, itemStack);
					itemStack = null;
				}
				case LEFT -> {
					// 补充【全部】或交换【全部】
					if (source.placeable(cursor)) {
						var result = ItemStacks.place(itemStack, cursor, false);
						itemStack = result.placed();
						cursor = result.remaining();
					} else {
						var tmp = cursor;
						cursor = itemStack;
						itemStack = tmp;
					}
				}
				case RIGHT -> {
					// 补充【一个】或交换【全部】
					if (source.placeable(cursor)) {
						var tmp = cursor.clone();
						tmp.setAmount(1);
						var result = ItemStacks.place(itemStack, tmp, false);
						if (ItemStacks.isEmpty(result.remaining())) {
							itemStack = result.placed();
							cursor.setAmount(cursor.getAmount() - 1);
						}
					} else {
						var tmp = cursor;
						cursor = itemStack;
						itemStack = tmp;
					}
				}
				case SHIFT_LEFT, SHIFT_RIGHT -> {
					// 取出【全部】
					var first = player.getInventory().addItem(itemStack).values().stream().findFirst();
					itemStack = first.orElse(null);
				}
				case MIDDLE -> {
					// 克隆【一组】
					if (player.getGameMode() == GameMode.CREATIVE && ItemStacks.isEmpty(cursor)) {
						cursor = itemStack.clone();
						cursor.setAmount(cursor.getMaxStackSize());
					}
				}
				case DOUBLE_CLICK -> {
					// 收集【全部】
					var camera = event.getCamera();
					if (camera != null) {
						cursor = camera.collect(player, cursor, true);
					}
				}
			}
			source.set(itemStack, player);
			event.setCursor(cursor);
		} else {
			switch (event.getClickType()) {
				case LEFT, RIGHT -> {
					// 捡起【全部】
					var itemStack = source.get();
					var cursor = event.getCursor();
					var result = ItemStacks.place(cursor, itemStack, false);
					if (result.remaining() == null) {
						cursor = result.placed();
						itemStack = null;
					}
					source.set(itemStack, player);
					event.setCursor(cursor);
				}
				case SHIFT_LEFT, SHIFT_RIGHT -> {
					// 在Similar的情况下尽可能生产
					ItemStack last = null;
					while (true) {
						var itemStack = source.get();
						if (last != null && !ItemStacks.isSimilar(last, itemStack))
							break;
						last = itemStack;
						source.set(null, player);

						if (!ItemStacks.isEmpty(itemStack)) {
							player.getInventory().addItem(itemStack);
						}
					}
				}
			}
		}
	}

	@Override
	public ItemStack place(ItemStack itemStack, @Nullable Player player) {
		if (!source.bidirectional()) {
			return ItemStacks.clone(itemStack);
		}
		var result = ItemStacks.place(source.get(), itemStack, false);
		source.set(result.placed(), player);
		return result.remaining();
	}

	@Override
	public ItemStack collect(ItemStack itemStack, @Nullable Player player) {
		if (!source.bidirectional()) {
			return ItemStacks.clone(itemStack);
		}
		var result = ItemStacks.place(itemStack, source.get(), false);
		source.set(result.remaining(), player);
		return result.placed();
	}

	@Override
	public Slot deepClone() {
		return new Storage(source.deepClone());
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private Source source;

		private Builder() {
		}

		public Storage build() {
			return new Storage(source);
		}

		public Builder source(Source source) {
			this.source = source;
			return this;
		}

		public Builder replicate(Storage storage) {
			return source(storage.source);
		}

		public Builder empty() {
			return withInitial(null);
		}

		public Builder withInitial(ItemStack initial) {
			source = new ItemStackSource(initial);
			return this;
		}

		public Builder fromInventory(Inventory inventory, int rawSlot) {
			source = new InventorySource(inventory, rawSlot);
			return this;
		}
	}

	private static class ItemStackSource implements Source {
		private ItemStack itemStack;
		private Runnable dirtyMarker;

		public ItemStackSource(ItemStack initial) {
			this.itemStack = initial;
		}

		@Override
		public ItemStack get() {
			return itemStack;
		}

		@Override
		public void set(ItemStack itemStack, @Nullable Player player) {
			this.itemStack = itemStack;
			dirtyMarker.run();
		}

		@Override
		public void setDirtyMarker(Runnable dirtyMarker) {
			this.dirtyMarker = dirtyMarker;
		}

		@Override
		public Source deepClone() {
			return new ItemStackSource(ItemStacks.clone(itemStack));
		}
	}

	private static class InventorySource implements Source {
		private final Inventory inventory;
		private final int rawSlot;
		private Runnable dirtyMarker;
		private ItemStack last;

		public InventorySource(Inventory inventory, int rawSlot) {
			this.inventory = inventory;
			this.rawSlot = rawSlot;
		}

		@Override
		public ItemStack get() {
			return inventory.getItem(rawSlot);
		}

		@Override
		public void set(ItemStack itemStack, @Nullable Player player) {
			inventory.setItem(rawSlot, itemStack);
			dirtyMarker.run();
		}

		@Override
		public void setDirtyMarker(Runnable dirtyMarker) {
			this.dirtyMarker = dirtyMarker;
		}

		@Override
		public void tick() {
			var current = get();
			if (!ItemStacks.isSame(current, last)) {
				dirtyMarker.run();
				last = ItemStacks.clone(current);
			}
		}

		@Override
		public Source deepClone() {
			return new InventorySource(inventory, rawSlot);
		}
	}
}
