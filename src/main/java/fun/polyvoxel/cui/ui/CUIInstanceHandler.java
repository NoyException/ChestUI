package fun.polyvoxel.cui.ui;

import fun.polyvoxel.cui.util.Context;
import org.jetbrains.annotations.NotNull;

/**
 * 你需要实现该接口来定义一个CUI实例处理器。每个CUI实例都会有一个对应的处理器。<br>
 * You need to implement this interface to define a CUI instance handler. Each
 * CUI instance will have a corresponding handler.
 * 
 * @param <T>
 *            你的ChestUI实现类。<br>
 *            Your ChestUI implementation class.
 */
public interface CUIInstanceHandler<T extends ChestUI<T>> {

	void onInitialize(CUIInstance<T> cui);

	default void onDestroy() {
	}

	default void onTickStart() {
	}

	default void onTick() {
	}

	default void onTickEnd() {
	}

	/**
	 * 创建一个摄像机处理器。当一个{@link Camera}被创建时，会调用此方法为其构造一个处理器。<br>
	 * Create a camera handler. This method is called to construct a handler for it
	 * when a {@link Camera} is created.
	 * 
	 * @return 摄像机处理器实例。<br>
	 *         Camera handler instance.
	 */
	@NotNull
	CameraHandler<T> createCameraHandler(Context context);
}
