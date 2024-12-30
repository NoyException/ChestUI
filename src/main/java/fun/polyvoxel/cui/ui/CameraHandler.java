package fun.polyvoxel.cui.ui;

import org.bukkit.entity.Player;

/**
 * 你需要实现该接口来定义一个摄像机处理器。每个摄像机都会有一个对应的处理器。<br>
 * You need to implement this interface to define a camera handler. Each camera
 * will have a corresponding handler.
 * 
 * @param <T>
 *            你的ChestUI实现类。<br>
 *            Your ChestUI implementation class.
 */
public interface CameraHandler<T extends ChestUI<T>> {

	void onInitialize(Camera<T> camera);

	default void onDestroy() {
	}

	default void onTickStart() {
	}

	default void onTick() {
	}

	default void onTickEnd() {
	}

	default boolean canOpen(Player viewer) {
		return true;
	}

	default void onOpen(Player viewer) {
	}

	default boolean canClose(Player viewer) {
		return true;
	}

	default void onClose(Player viewer) {
	}
}
