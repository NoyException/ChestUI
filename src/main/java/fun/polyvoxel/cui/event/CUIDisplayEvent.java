package fun.polyvoxel.cui.event;

import fun.polyvoxel.cui.ui.CUIType;
import fun.polyvoxel.cui.ui.ChestUI;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CUIDisplayEvent<T extends ChestUI<T>> extends Event implements Cancellable {
	private static final HandlerList HANDLERS = new HandlerList();
	private final CUIType<T> type;
	private final Player player;
	private boolean cancel;

	public CUIDisplayEvent(CUIType<T> type, Player player) {
		this.type = type;
		this.player = player;
	}

	public CUIType<T> getCUIType() {
		return type;
	}

	public Player getPlayer() {
		return player;
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
