package fun.polyvoxel.cui.serialize;

import fun.polyvoxel.cui.ui.*;
import fun.polyvoxel.cui.util.context.Context;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class SerializableChestUI implements ChestUI<SerializableChestUI> {
	private final CUIData cuiData;
	private final @Nullable Path path;

	public SerializableChestUI(CUIData cuiData, @Nullable Path path) {
		this.cuiData = cuiData;
		this.path = path;
	}

	public CUIData getData() {
		return cuiData;
	}

	public @Nullable Path getPath() {
		return path;
	}

	@Override
	public void onInitialize(CUIType<SerializableChestUI> type) {
		type.edit().defaultTitle(cuiData.title).triggerByDisplay(CameraProvider.createCameraInDefaultCUIInstance());
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
