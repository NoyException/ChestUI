package fun.polyvoxel.cui.prebuilt;

import fun.polyvoxel.cui.ui.*;
import fun.polyvoxel.cui.util.Context;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

@CUI(name = "creator", autoRegister = false, icon = Material.COMMAND_BLOCK)
public class CUICreator implements ChestUI<CUICreator> {

	@Override
	public void onInitialize(CUIType<CUICreator> type) {

	}

	@Override
	public @NotNull CUIInstanceHandler<CUICreator> createCUIInstanceHandler(Context context) {
		return new InstanceHandler();
	}

	public static class InstanceHandler implements CUIInstanceHandler<CUICreator> {

		@Override
		public void onInitialize(CUIInstance<CUICreator> cui) {

		}

		@Override
		public @NotNull CameraHandler<CUICreator> createCameraHandler(Context context) {
			return camera -> {
			};
		}
	}
}
