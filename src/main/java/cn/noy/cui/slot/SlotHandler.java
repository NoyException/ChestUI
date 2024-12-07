package cn.noy.cui.slot;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class SlotHandler {
    private Slot slot;

    public SlotHandler() {
        slot = new Button.Builder().build();
    }

    public Slot getSlot() {
        return slot;
    }

    public boolean isDirty() {
        return slot.isDirty();
    }

    public void empty() {
        slot = new Button.Builder().build();
    }

    public void deepClone(@NotNull SlotHandler handler) {
        slot = handler.slot.deepClone();
    }

    public void button(@NotNull Function<Button.Builder, Button> builder) {
        slot = builder.apply(new Button.Builder());
    }

    public void filter(@NotNull Function<Filter.Builder, Filter> builder) {
        slot = builder.apply(new Filter.Builder());
    }

    public void storage(@NotNull Function<Storage.Builder, Storage> builder) {
        slot = builder.apply(new Storage.Builder());
    }
}
