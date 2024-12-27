package fun.polyvoxel.cui.util;

import org.jetbrains.annotations.NotNull;

public interface Context {
	Context BACKGROUND = new Context() {
		@Override
		public <T> Context withValue(@NotNull String key, T value) {
			return new ValueContext(this, key, value);
		}

		@Override
		public <T> T get(@NotNull String key) {
			return null;
		}

		@Override
		public boolean has(@NotNull String key) {
			return false;
		}
	};
	<T> Context withValue(@NotNull String key, T value);
	<T> T get(@NotNull String key);
	boolean has(@NotNull String key);
}
