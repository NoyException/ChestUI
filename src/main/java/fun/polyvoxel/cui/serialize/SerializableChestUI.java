package fun.polyvoxel.cui.serialize;

import fun.polyvoxel.cui.ui.*;
import org.jetbrains.annotations.Nullable;

public class SerializableChestUI implements ChestUI<SerializableChestUI> {
	private CUIType<SerializableChestUI> type;
	private final CUIData cuiData;
	// 仅在单例模式下使用
	private Camera<SerializableChestUI> camera;

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
		this.type = type.edit().defaultTitle(cuiData.title).done();
	}

	@Override
	public @Nullable <S> Camera<SerializableChestUI> getDisplayedCamera(DisplayContext<S> context) {
		if (cuiData.singleton) {
			if (camera == null) {
				camera = type.createInstance(cuiData::toChestUI).createCamera(cuiData::toCamera);
			}
			return camera;
		}
		return type.createInstance(cuiData::toChestUI).createCamera(cuiData::toCamera);
	}
}
