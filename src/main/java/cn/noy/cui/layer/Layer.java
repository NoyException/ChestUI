package cn.noy.cui.layer;

import cn.noy.cui.event.CUIClickEvent;
import cn.noy.cui.slot.Slot;
import cn.noy.cui.slot.SlotHandler;
import cn.noy.cui.ui.CUIContents;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public class Layer {
    private final int maxRow;
    private final int maxColumn;
    private int marginLeft;
    private int marginTop;
    private final SlotHandler[][] slots;

    public Layer(int maxRow, int maxColumn) {
        this.maxRow = maxRow;
        this.maxColumn = maxColumn;
        slots = new SlotHandler[maxRow][maxColumn];
    }

    public int getMaxRow() {
        return maxRow;
    }

    public int getMaxColumn() {
        return maxColumn;
    }

    public int getMarginLeft() {
        return marginLeft;
    }

    public int getMarginTop() {
        return marginTop;
    }

    public boolean isDirty() {
        return Arrays.stream(slots).flatMap(Arrays::stream).
                filter(Objects::nonNull).anyMatch(SlotHandler::isDirty);
    }

    private @Nullable SlotHandler getSlotHandler(int row, int column) {
        if (row < 0 || row >= maxRow || column < 0 || column >= maxColumn) {
            return null;
        }
        var handler = slots[row][column];
        if (handler == null) {
            handler = new SlotHandler();
            slots[row][column] = handler;
        }
        return handler;
    }

    public @Nullable Slot getSlot(int row, int column) {
        SlotHandler slotHandler = getSlotHandler(row, column);
        if (slotHandler == null) {
            return null;
        }
        return slotHandler.getSlot();
    }

    public @Nullable Slot getRelativeSlot(int row, int column) {
        return getSlot(row - marginTop, column - marginLeft);
    }

    public void display(CUIContents contents, int rowOffset, int columnOffset) {
        for (int row = 0; row < maxRow; row++) {
            for (int column = 0; column < maxColumn; column++) {
                var slot = slots[row][column];
                if (slot == null)
                    continue;

                var absoluteRow = marginTop + row + rowOffset;
                var absoluteColumn = marginLeft + column + columnOffset;
                var itemStack = contents.getItem(absoluteRow, absoluteColumn);
                if (itemStack != null)
                    itemStack = itemStack.clone();
                contents.setItem(absoluteRow, absoluteColumn, slot.getSlot().display(itemStack));
            }
        }
    }

    public void click(CUIClickEvent<?> event, int rowOffset, int columnOffset) {
        var position = event.getPosition();
        var row = position.row() - marginTop - rowOffset;
        var column = position.column() - marginLeft - columnOffset;
        if (row < 0 || row >= maxRow || column < 0 || column >= maxColumn) {
            return;
        }
        var slot = slots[row][column];
        if (slot != null) {
            slot.getSlot().click(event);
        }
    }

    public Editor edit() {
        return new Editor();
    }

    public Layer deepClone() {
        var layer = new Layer(maxRow, maxColumn);
        layer.marginLeft = marginLeft;
        layer.marginTop = marginTop;
        for (int row = 0; row < maxRow; row++) {
            for (int column = 0; column < maxColumn; column++) {
                var slot = slots[row][column];
                if (slot != null) {
                    layer.slots[row][column] = new SlotHandler();
                    layer.slots[row][column].deepClone(slot);
                }
            }
        }
        return layer;
    }

    public class Editor {

        public Layer finish() {
            return Layer.this;
        }

        public Editor marginLeft(int marginLeft) {
            Layer.this.marginLeft = marginLeft;
            return this;
        }

        public Editor marginTop(int marginTop) {
            Layer.this.marginTop = marginTop;
            return this;
        }

        public Editor editSlot(int row, int column, Consumer<SlotHandler> onEdit) {
            var handler = getSlotHandler(row, column);
            if (handler == null) {
                return this;
            }
            onEdit.accept(handler);
            return this;
        }

        public Editor editRow(int row, Consumer<SlotHandler> onEdit) {
            for (int column = 0; column < maxColumn; column++) {
                editSlot(row, column, onEdit);
            }
            return this;
        }

        public Editor editColumn(int column, Consumer<SlotHandler> onEdit) {
            for (int row = 0; row < maxRow; row++) {
                editSlot(row, column, onEdit);
            }
            return this;
        }

        public Editor editAll(Consumer<SlotHandler> onEdit) {
            for (int row = 0; row < maxRow; row++) {
                for (int column = 0; column < maxColumn; column++) {
                    editSlot(row, column, onEdit);
                }
            }
            return this;
        }

        public Editor clear() {
            return editAll(SlotHandler::empty);
        }
    }
}
