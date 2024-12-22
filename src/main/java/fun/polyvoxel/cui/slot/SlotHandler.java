package fun.polyvoxel.cui.slot;

import fun.polyvoxel.cui.layer.Layer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class SlotHandler {
	private final Layer layer;
	private Slot slot;

	public SlotHandler(Layer layer) {
		this.layer = layer;
		setSlot(new Button.Builder().build());
	}

	private void setSlot(@NotNull Slot slot) {
		slot.bind(layer::markDirty);
		this.slot = slot;
	}

	public Slot getSlot() {
		return slot;
	}

	public void deepClone(@NotNull SlotHandler handler) {
		setSlot(handler.slot.deepClone());
	}

	public void empty() {
		setSlot(Empty.getInstance());
	}

	public void button(@NotNull Function<Button.Builder, Button> builder) {
		setSlot(builder.apply(new Button.Builder()));
	}

	public void filter(@NotNull Function<Filter.Builder, Filter> builder) {
		setSlot(builder.apply(new Filter.Builder()));
	}

	public void storage(@NotNull Function<Storage.Builder, Storage> builder) {
		setSlot(builder.apply(new Storage.Builder()));
	}
}
