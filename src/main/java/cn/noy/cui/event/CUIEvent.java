package cn.noy.cui.event;

import cn.noy.cui.ui.CUIHandler;
import cn.noy.cui.ui.ChestUI;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public abstract class CUIEvent<T extends CUIHandler> extends Event {
	private static final HandlerList HANDLERS = new HandlerList();

	private final ChestUI<T> chestUI;

	public CUIEvent(ChestUI<T> chestUI) {
		this.chestUI = chestUI;
	}

	public ChestUI<?> getCUI() {
		return chestUI;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}
}
