package fun.polyvoxel.cui.ui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

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

	/**
	 * 与{@link #onOpen(Player)}不同，本方法在每次玩家打开摄像头提供的界面（而非摄像头）时调用。比如当玩家从另一个摄像头切换回来，或者尝试关闭但被阻止时也会触发。<br>
	 * Unlike {@link #onOpen(Player)}, this method is called every time a player
	 * opens the inventory provided by the camera (not the camera). For example,
	 * when a player switches back from another camera, or tries to close but is
	 * prevented from doing so.
	 */
	default void onOpenInventory(Inventory inventory) {
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
