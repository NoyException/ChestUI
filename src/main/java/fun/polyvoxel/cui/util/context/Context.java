package fun.polyvoxel.cui.util.context;

import fun.polyvoxel.cui.ui.Camera;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface Context {
	static Context background() {
		return BackgroundContext.BACKGROUND;
	}

	default <T> Context withValue(@NotNull String key, T value) {
		return new ValueContext(this, key, value);
	}

	default Context withPlayer(@NotNull Player player) {
		return new PlayerContext(player);
	}

	default Context withCamera(@NotNull Camera<?> camera) {
		return new CameraContext(this, camera);
	}

	default @NotNull Context parent() {
		return BackgroundContext.BACKGROUND;
	}

	default <T> T get(@NotNull String key) {
		return parent().get(key);
	}

	default boolean has(@NotNull String key) {
		return parent().has(key);
	}

	default Player player() {
		return parent().player();
	}

	default Camera<?> camera() {
		return parent().camera();
	}
}
