package fun.polyvoxel.cui.util.context;

import org.jetbrains.annotations.NotNull;

public final class BackgroundContext implements Context {
	public static final Context BACKGROUND = new BackgroundContext();

	private BackgroundContext() {
	}

	@Override
	public @NotNull Context parent() {
		return this;
	}

	@Override
	public <T> T get(@NotNull String key) {
		return null;
	}

	@Override
	public boolean has(@NotNull String key) {
		return false;
	}
}
