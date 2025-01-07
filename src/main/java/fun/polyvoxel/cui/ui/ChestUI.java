package fun.polyvoxel.cui.ui;

import org.jetbrains.annotations.Nullable;

/**
 * 你需要实现该接口来定义一个ChestUI。对于其实现A，其需要实现{@code ChestUI<A>}接口。此外，你必须为该类提供一个无参构造函数。<br>
 * You need to implement this interface to define a CUI. For its implementation
 * A, it needs to implement the {@code ChestUI<A>} interface. Additionally, you
 * must provide a no-args constructor for this class.
 *
 * @param <T>
 *            你的ChestUI实现类<br>
 *            Your ChestUI implementation class
 */
public interface ChestUI<T extends ChestUI<T>> {

	/**
	 * 当CUI类型初始化时会被调用。你可以在此阶段调用edit()进行一些初始化设置。<br>
	 * Called when the CUI type is initialized. You can call edit() to do some
	 * initial settings at this stage.
	 *
	 * @param type
	 *            CUI类型实例<br>
	 *            CUI type instance
	 */
	void onInitialize(CUIType<T> type);

	/**
	 * 获取显示的摄像机。当一个CUI实例被显示时，会调用此方法获取其显示的摄像机。<br>
	 * Get the displayed camera. This method is called to get the displayed camera.
	 *
	 * @param context
	 *            显示上下文<br>
	 *            Display context
	 * @return 使用的摄像机<br>
	 *         Camera used
	 */
	@Nullable
	<S> Camera<T> getDisplayedCamera(DisplayContext<S> context);

	default void onCreateInstance(CUIInstance<T> instance) {
	}

	default void onTickStart() {
	}

	default void onTick() {
	}

	default void onTickEnd() {
	}
}
