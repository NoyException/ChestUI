package fun.polyvoxel.cui.util.context;

import fun.polyvoxel.cui.ui.Camera;
import org.bukkit.entity.Player;
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

	@Override
	public Player player() {
		return null;
	}

	@Override
	public Camera<?> camera() {
		return null;
	}
}
