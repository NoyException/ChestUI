package fun.polyvoxel.cui.serialize;

import fun.polyvoxel.cui.ui.*;
import fun.polyvoxel.cui.util.context.Context;
import org.jetbrains.annotations.NotNull;

public class SerializableChestUI implements ChestUI<SerializableChestUI> {
	private CUIType<SerializableChestUI> type;
	private final CUIData cuiData;

	public SerializableChestUI(CUIData cuiData) {
		this.cuiData = cuiData;
	}

	public CUIType<SerializableChestUI> getType() {
		return type;
	}

	public CUIData getData() {
		return cuiData;
	}

	@Override
	public void onInitialize(CUIType<SerializableChestUI> type) {
		this.type = type.edit().defaultTitle(cuiData.title).triggerByDisplay((cuiType, player, asChild) -> {
			CameraProvider<SerializableChestUI> provider = cuiData.singleton
					? CameraProvider.useDefaultCameraInDefaultCUIInstance()
					: CameraProvider.createCameraInNewCUIInstance();
			return provider.provide(cuiType, player, asChild);
		}).done();
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
