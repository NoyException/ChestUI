package fun.polyvoxel.cui.serialize;

import fun.polyvoxel.cui.ui.*;
import fun.polyvoxel.cui.util.Context;
import org.jetbrains.annotations.NotNull;

public class SerializableChestUI implements ChestUI<SerializableChestUI> {
	private final CUIData cuiData;

	public SerializableChestUI(CUIData cuiData) {
		this.cuiData = cuiData;
	}

	@Override
	public void onInitialize(CUIType<SerializableChestUI> type) {
		type.edit().defaultTitle(cuiData.title).triggerByDisplayCommand(
				player -> new CUIType.TriggerResult<>(CUIType.TriggerResultType.USE_DEFAULT_CAMERA, camera -> {
				}));
	}

	@Override
	public @NotNull CUIInstanceHandler<SerializableChestUI> createCUIInstanceHandler(Context context) {
		return new InstanceHandler();
	}

	private class InstanceHandler implements CUIInstanceHandler<SerializableChestUI> {
		@Override
		public void onInitialize(CUIInstance<SerializableChestUI> cui) {
			cuiData.toChestUI(cui);
		}

		@Override
		public @NotNull CameraHandler<SerializableChestUI> createCameraHandler(Context context) {
			return cuiData::toCamera;
		}
	}
}
