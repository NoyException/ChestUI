package fun.polyvoxel.cui.serialize;

import fun.polyvoxel.cui.ui.CUIInstance;
import fun.polyvoxel.cui.ui.CUIType;
import fun.polyvoxel.cui.ui.ChestUI;
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
	public @NotNull ChestUI.InstanceHandler<SerializableChestUI> createInstanceHandler() {
		return new InstanceHandler();
	}

	private class InstanceHandler implements ChestUI.InstanceHandler<SerializableChestUI> {
		@Override
		public void onInitialize(CUIInstance<SerializableChestUI> cui) {
			cuiData.toChestUI(cui);
		}

		@Override
		public @NotNull CameraHandler<SerializableChestUI> createCameraHandler() {
			return cuiData::toCamera;
		}
	}
}
