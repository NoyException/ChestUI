package fun.polyvoxel.cui.event;

import fun.polyvoxel.cui.ui.CUIType;
import fun.polyvoxel.cui.ui.ChestUI;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CUIRegisterEvent<T extends ChestUI<T>> extends Event {
	private static final HandlerList HANDLERS = new HandlerList();
	private final CUIType<T> type;

	public CUIRegisterEvent(CUIType<T> type) {
		this.type = type;
	}

	public CUIType<T> getCUIType() {
		return type;
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
