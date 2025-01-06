package fun.polyvoxel.cui.event;

import fun.polyvoxel.cui.slot.Slot;
import fun.polyvoxel.cui.ui.CUIInstance;
import fun.polyvoxel.cui.ui.Camera;
import fun.polyvoxel.cui.ui.ChestUI;
import fun.polyvoxel.cui.util.Position;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CUIPlaceItemEvent<T extends ChestUI<T>> extends Event implements Cancellable {
	private static final HandlerList HANDLERS = new HandlerList();
	private final @NotNull Camera<T> camera;
	private final @Nullable Player player;
	private final @NotNull Position position;
	private ItemStack itemStack;
	private @Nullable Slot slot;
	private boolean cancel;

	public CUIPlaceItemEvent(@NotNull Camera<T> camera, @Nullable Player player, @NotNull Position position,
			@NotNull ItemStack itemStack) {
		this.camera = camera;
		this.player = player;
		this.position = position;
		this.itemStack = itemStack;
	}

	public CUIInstance<T> getCUIInstance() {
		return camera.getCUIInstance();
	}

	public @NotNull Camera<T> getCamera() {
		return camera;
	}

	public @Nullable Player getPlayer() {
		return player;
	}

	public @NotNull Position getPosition() {
		return position;
	}

	public ItemStack getItemStack() {
		return itemStack;
	}

	public void setItemStack(ItemStack itemStack) {
		this.itemStack = itemStack;
	}

	public @Nullable Slot getSlot() {
		return slot;
	}

	public void setSlot(@Nullable Slot slot) {
		this.slot = slot;
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
