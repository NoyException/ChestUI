package fun.polyvoxel.cui.ui;

import org.bukkit.entity.Player;

/**
 * 你需要实现该接口来定义一个CUI。对于其实现A，其需要实现{@code CUIHandler<A>}接口。<br>
 * You need to implement this interface to define a CUI. For its implementation
 * A, it needs to implement the {@code CUIHandler<A>} interface.
 *
 * @param <T>
 *            你的CUIHandler实现类。<br>
 *            Your CUIHandler implementation class.
 */
public interface CUIHandler<T extends CUIHandler<T>> {
	void onInitialize(ChestUI<T> cui);

	default void onTick() {
	}

	default void onDestroy() {
	}

	default void onCreateCamera(Camera<T> camera) {
	}

	default void onDestroyCamera(Camera<T> camera) {
	}

	default boolean onOpen(Player viewer, Camera<T> camera) {
		return true;
	}

	default boolean onClose(Player viewer, Camera<T> camera) {
		return true;
	}

	default boolean onSwitchTo(Player viewer, ChestUI<?> to) {
		return true;
	}

	default boolean onSwitchBack(Player viewer, ChestUI<?> from) {
		return true;
	}
}
