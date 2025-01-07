package fun.polyvoxel.cui.ui.provider;

import fun.polyvoxel.cui.ui.CUIType;
import fun.polyvoxel.cui.ui.ChestUI;

public abstract class CUIProvider<T extends ChestUI<T>> {
	private CUIType<T> cuiType;
	public final void enable(CUIType<T> cuiType) {
		this.cuiType = cuiType;
		onEnable();
	}

	public final void disable() {
		onDisable();
	}

	public CUIType<T> getCUIType() {
		return cuiType;
	}

	protected abstract void onEnable();
	protected abstract void onDisable();
}
