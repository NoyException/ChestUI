package fun.polyvoxel.cui.util.context;

import org.jetbrains.annotations.NotNull;

public interface Context {
	static Context background() {
		return BackgroundContext.BACKGROUND;
	}

	default <T> Context with(@NotNull String key, T value) {
		return new ValueContext(this, key, value);
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
}
