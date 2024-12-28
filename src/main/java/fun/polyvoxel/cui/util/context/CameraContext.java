package fun.polyvoxel.cui.util.context;

import fun.polyvoxel.cui.ui.Camera;
import org.jetbrains.annotations.NotNull;

public record CameraContext(Context parent, Camera<?> camera) implements Context {

	@Override
	public @NotNull Context parent() {
		return parent;
	}
}
