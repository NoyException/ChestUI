package fun.polyvoxel.cui.ui;

import fun.polyvoxel.cui.util.context.Context;
import org.jetbrains.annotations.NotNull;

/**
 * 你需要实现该接口来定义一个ChestUI。对于其实现A，其需要实现{@code ChestUI<A>}接口。此外，你必须为该类提供一个无参构造函数。<br>
 * You need to implement this interface to define a CUI. For its implementation
 * A, it needs to implement the {@code ChestUI<A>} interface. Additionally, you
 * must provide a no-args constructor for this class.
 * 
 * @param <T>
 *            你的ChestUI实现类。<br>
 *            Your ChestUI implementation class.
 */
public interface ChestUI<T extends ChestUI<T>> {

	/**
	 * 当CUI类型初始化时会被调用。你可以在此阶段调用edit()进行一些初始化设置。<br>
	 * Called when the CUI type is initialized. You can call edit() to do some
	 * initial settings at this stage.
	 * 
	 * @param type
	 *            CUI类型实例。<br>
	 *            CUI type instance.
	 */
	void onInitialize(CUIType<T> type);

	/**
	 * 创建一个CUI实例处理器。当一个{@link CUIInstance}被创建时，会调用此方法为其构造一个处理器。<br>
	 * Create a CUI instance handler. This method is called to construct a handler
	 * for it when a {@link CUIInstance} is created.
	 * 
	 * @return CUI实例处理器实例。<br>
	 *         CUI instance handler instance.
	 */
	@NotNull
	CUIInstanceHandler<T> createCUIInstanceHandler(Context context);

	default void onTickStart() {
	}

	default void onTick() {
	}

	default void onTickEnd() {
	}
}
