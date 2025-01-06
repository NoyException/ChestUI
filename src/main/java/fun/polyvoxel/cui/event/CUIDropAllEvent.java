package fun.polyvoxel.cui.event;

import fun.polyvoxel.cui.slot.Slot;
import fun.polyvoxel.cui.ui.Camera;
import fun.polyvoxel.cui.ui.ChestUI;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class CUIDropAllEvent<T extends ChestUI<T>> extends Event implements Cancellable {
	private static final HandlerList HANDLERS = new HandlerList();
	private final Camera<T> camera;
	private final @Nullable Player player;
	private @NotNull Location location;
	private final @NotNull Map<Slot, ItemStack> drops;
	private boolean cancel;

	public CUIDropAllEvent(@Nullable Camera<T> camera, @Nullable Player player, @NotNull Location location,
			@NotNull Map<Slot, ItemStack> drops) {
		this.camera = camera;
		this.player = player;
		this.location = location;
		this.drops = drops;
	}

	public Camera<T> getCamera() {
		return camera;
	}

	public @Nullable Player getPlayer() {
		return player;
	}

	public @NotNull Location getLocation() {
		return location;
	}

	public void setLocation(@NotNull Location location) {
		this.location = location;
	}

	/**
	 * 获取所有掉落物品和对应的槽位。对该Map的修改会影响最终掉落的物品。移除槽位对应的物品即可阻止该槽位物品清空，如果想不掉落但是清空槽位，请使用{@link ItemStack#empty()}代替。<br>
	 * Get all dropped items and their corresponding slots. Modifying this Map will
	 * affect the final dropped items. Removing the item corresponding to the slot
	 * will prevent the slot item from being cleared. If you want to clear the slot
	 * without dropping it, please use {@link ItemStack#empty()} instead.
	 * 
	 * @return 所有掉落物品和对应的槽位。<br>
	 *         All dropped items and their corresponding slots.
	 */
	public @NotNull Map<Slot, ItemStack> getDrops() {
		return drops;
	}

	@Override
	public boolean isCancelled() {
		return cancel;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancel = cancel;
	}

	@SuppressWarnings("unused")
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}
}
