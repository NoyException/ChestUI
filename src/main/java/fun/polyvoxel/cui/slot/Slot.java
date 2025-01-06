package fun.polyvoxel.cui.slot;

import fun.polyvoxel.cui.event.CUIClickEvent;

import fun.polyvoxel.cui.event.CUIDropAllEvent;
import fun.polyvoxel.cui.event.CUIPickItemEvent;
import fun.polyvoxel.cui.event.CUIPlaceItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Slot {
	private Runnable dirtyMarker;

	public void bind(Runnable dirtyMarker) {
		this.dirtyMarker = dirtyMarker;
		markDirty();
	}

	protected void markDirty() {
		dirtyMarker.run();
	}

	public void tick() {
	}

	/**
	 * 展示槽位中的物品<br>
	 * Display the item in the slot
	 *
	 * @param legacy
	 *            原先槽位中的物品<br>
	 *            The original item in the slot
	 * @return 展示在槽位中的物品<br>
	 *         The item to display in the slot
	 */
	public abstract ItemStack display(@Nullable ItemStack legacy);

	/**
	 * 获取槽位中的物品<br>
	 * Get the item in the slot
	 *
	 * @return 槽位中的物品<br>
	 *         The item in the slot
	 */
	public ItemStack get() {
		return display(null);
	}

	/**
	 * 设置槽位中的物品<br>
	 * Set the item in the slot
	 *
	 * @param itemStack
	 *            要设置的物品<br>
	 *            The item to set
	 */
	public void set(ItemStack itemStack) {
	}

	/**
	 * 准备点击事件。如果事件没有被拦截，它将会被传递到下一层<br>
	 * Prepare the click event. If the event is not intercepted, it will be passed
	 * to the next layer
	 *
	 * @param event
	 *            点击事件<br>
	 *            The click event
	 * @return 是否拦截事件<br>
	 *         Whether to intercept the event
	 */
	public boolean prepareClick(@NotNull CUIClickEvent<?> event) {
		return false;
	}

	public void click(@NotNull CUIClickEvent<?> event) {
	}

	public boolean preparePlace(@NotNull CUIPlaceItemEvent<?> event) {
		return false;
	}

	/**
	 * 尝试将物品放入槽位<br>
	 * Try to place the item in the slot
	 *
	 * @param event
	 *            放入事件<br>
	 *            The place event
	 * @return 剩余的物品，如果没有剩余则返回null<br>
	 *         The remaining item, if there is no remaining item, return null
	 */
	public ItemStack place(@NotNull CUIPlaceItemEvent<?> event) {
		return event.getItemStack();
	}

	public boolean preparePick(@NotNull CUIPickItemEvent<?> event) {
		return false;
	}

	/**
	 * 尝试拾起槽位中的物品<br>
	 * Try to pick the item in the slot
	 *
	 * @param event
	 *            拾起事件<br>
	 *            The pick event
	 * @return 收集到的物品，如果没有收集到则返回最开始的要收集的物品<br>
	 *         The collected item, if there is no collected item, return the
	 *         original item to collect
	 */
	public ItemStack pick(@NotNull CUIPickItemEvent<?> event) {
		return event.getCursor();
	}

	public abstract void prepareDrop(CUIDropAllEvent<?> event);

	/**
	 * 深度克隆槽位<br>
	 * Deep clone the slot
	 *
	 * @return 克隆的槽位<br>
	 *         The cloned slot
	 */
	@Deprecated
	public abstract Slot deepClone();
}
