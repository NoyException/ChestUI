package fun.polyvoxel.cui.ui;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 你需要实现该接口来定义一个CUI。对于其实现A，其需要实现{@code ChestUI<A>}接口。此外，你必须为该类提供一个无参构造函数。<br>
 * You need to implement this interface to define a CUI. For its implementation
 * A, it needs to implement the {@code ChestUI<A>} interface. Additionally, you
 * must provide a no-args constructor for this class.
 * 
 * @param <T>
 *            你的CUIHandler实现类。<br>
 *            Your CUIHandler implementation class.
 */
public interface ChestUI<T extends ChestUI<T>> {

	void onInitialize(CUIType<T> type);

	@NotNull
	Handler<T> createHandler();

	interface Handler<T extends ChestUI<T>> {

		void onInitialize(CUIInstance<T> cui);

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
	}
}
