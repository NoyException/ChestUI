package fun.polyvoxel.cui.ui;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface CameraProvider<T extends ChestUI<T>> {
	@Nullable
	Camera<T> provide(CUIType<T> cuiType, Player player, boolean asChild);

	@Contract(pure = true)
	static <T extends ChestUI<T>> @NotNull CameraProvider<T> reject() {
		return (cuiType, player, asChild) -> null;
	}

	@Contract(pure = true)
	static <T extends ChestUI<T>> @NotNull CameraProvider<T> useDefaultCameraInDefaultCUIInstance() {
		return (cuiType, player, asChild) -> cuiType.getInstance().getCamera();
	}

	@Contract(pure = true)
	static <T extends ChestUI<T>> @NotNull CameraProvider<T> createCameraInDefaultCUIInstance() {
		return (cuiType, player, asChild) -> cuiType.getInstance().createCamera();
	}

	@Contract(pure = true)
	static <T extends ChestUI<T>> @NotNull CameraProvider<T> createCameraInNewCUIInstance() {
		return (cuiType, player, asChild) -> cuiType.createInstance().createCamera();
	}
}
