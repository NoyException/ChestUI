package fun.polyvoxel.cui.serialize;

import fun.polyvoxel.cui.ui.CUIInstance;
import fun.polyvoxel.cui.ui.CUIType;
import fun.polyvoxel.cui.ui.Camera;
import fun.polyvoxel.cui.ui.ChestUI;
import org.jetbrains.annotations.NotNull;

public class SerializableChestUI implements ChestUI<SerializableChestUI> {
	private final CUIData cuiData;

	public SerializableChestUI(CUIData cuiData) {
		this.cuiData = cuiData;
	}

	@Override
	public void onInitialize(CUIType<SerializableChestUI> type) {
		type.edit().triggerByDisplayCommand(
				player -> new CUIType.TriggerResult<>(CUIType.TriggerResultType.USE_DEFAULT_CAMERA, camera -> {
				}));
	}

	@Override
	public ChestUI.@NotNull Handler<SerializableChestUI> createHandler() {
		return new Handler();
	}

	private class Handler implements ChestUI.Handler<SerializableChestUI> {
		@Override
		public void onInitialize(CUIInstance<SerializableChestUI> cui) {
			cuiData.toChestUI(cui);
		}

		@Override
		public void onCreateCamera(Camera<SerializableChestUI> camera) {
			cuiData.toCamera(camera);
		}
	}
}
