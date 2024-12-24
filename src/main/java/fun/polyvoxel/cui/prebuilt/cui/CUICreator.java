package fun.polyvoxel.cui.prebuilt.cui;

import fun.polyvoxel.cui.ui.*;
import org.jetbrains.annotations.NotNull;

@CUI(name = "creator")
public class CUICreator implements ChestUI<CUICreator> {

	@Override
	public void onInitialize(CUIType<CUICreator> type) {

	}

	@Override
	public @NotNull ChestUI.Handler<CUICreator> createHandler() {
		return new Handler();
	}

	public static class Handler implements ChestUI.Handler<CUICreator> {

		@Override
		public void onInitialize(CUIInstance<CUICreator> cui) {

		}
	}
}
