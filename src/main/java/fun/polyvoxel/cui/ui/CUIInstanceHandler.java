package fun.polyvoxel.cui.ui;

/**
 * 你需要实现该接口来定义一个CUI实例处理器。每个CUI实例都会有一个对应的处理器。<br>
 * You need to implement this interface to define a CUI instance handler. Each
 * CUI instance will have a corresponding handler.
 * 
 * @param <T>
 *            你的ChestUI实现类<br>
 *            Your ChestUI implementation class
 */
public interface CUIInstanceHandler<T extends ChestUI<T>> {

	void onInitialize(CUIInstance<T> cui);

	default void onCreateCamera(Camera<T> camera) {
	}

	default void onDestroy() {
	}

	default void onTickStart() {
	}

	default void onTick() {
	}

	default void onTickEnd() {
	}
}
