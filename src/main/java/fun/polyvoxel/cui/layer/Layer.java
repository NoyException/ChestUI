package fun.polyvoxel.cui.layer;

import fun.polyvoxel.cui.event.CUIClickEvent;
import fun.polyvoxel.cui.slot.Slot;
import fun.polyvoxel.cui.slot.SlotHandler;
import fun.polyvoxel.cui.ui.CUIContents;

import fun.polyvoxel.cui.ui.Camera;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Layer {
	private int rowSize;
	private int columnSize;
	private int marginLeft;
	private int marginTop;
	// margin的位置是相对摄像头还是绝对值
	private boolean relative;
	private SlotHandler[][] slots;
	private boolean dirty;

	public Layer(int rowSize, int columnSize) {
		this.rowSize = rowSize;
		this.columnSize = columnSize;
		slots = new SlotHandler[rowSize][columnSize];
	}

	public int getRowSize() {
		return rowSize;
	}

	public int getColumnSize() {
		return columnSize;
	}

	public int getMarginLeft() {
		return marginLeft;
	}

	public int getMarginTop() {
		return marginTop;
	}

	public boolean isRelative() {
		return relative;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void markDirty() {
		dirty = true;
	}

	public void tickStart() {
		dirty = false;
	}

	public void tick() {
		for (int row = 0; row < rowSize; row++) {
			for (int column = 0; column < columnSize; column++) {
				var slot = slots[row][column];
				if (slot != null) {
					slot.getSlot().tick();
				}
			}
		}
	}

	// 检查坐标是否超出范围
	public boolean isValidPosition(int row, int column) {
		return row >= 0 && row < rowSize && column >= 0 && column < columnSize;
	}

	private @Nullable SlotHandler getSlotHandler(int row, int column) {
		if (!isValidPosition(row, column)) {
			return null;
		}
		var handler = slots[row][column];
		if (handler == null) {
			handler = new SlotHandler(this);
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

	public @Nullable Slot getRelativeSlot(Camera<?> camera, int row, int column) {
		if (relative) {
			return getSlot(row - marginTop, column - marginLeft);
		}
		var topLeft = camera.getTopLeft();
		return getSlot(row - marginTop + topLeft.row(), column - marginLeft + topLeft.column());
	}

	public void display(CUIContents<?> contents) {
		var camera = contents.getCamera();
		for (int row = 0; row < contents.getMaxRow(); row++) {
			for (int column = 0; column < contents.getMaxColumn(); column++) {
				var slot = getRelativeSlot(camera, row, column);
				if (slot == null) {
					continue;
				}
				var itemStack = contents.getItem(row, column);
				if (itemStack != null)
					itemStack = itemStack.clone();
				contents.setItem(row, column, slot.display(itemStack));
			}
		}
	}

	public void click(CUIClickEvent<?> event) {
		var position = event.getPosition();
		var row = position.row() - marginTop;
		var column = position.column() - marginLeft;
		if (relative) {
			var topLeft = event.getCamera().getTopLeft();
			row -= topLeft.row();
			column -= topLeft.column();
		}
		if (row < 0 || row >= rowSize || column < 0 || column >= columnSize) {
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

	@Deprecated
	public Layer deepClone() {
		var layer = new Layer(rowSize, columnSize);
		layer.marginLeft = marginLeft;
		layer.marginTop = marginTop;
		for (int row = 0; row < rowSize; row++) {
			for (int column = 0; column < columnSize; column++) {
				var slot = slots[row][column];
				if (slot != null) {
					layer.slots[row][column] = new SlotHandler(layer);
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

		public Editor resize(int rowSize, int columnSize) {
			var slots = new SlotHandler[rowSize][columnSize];
			for (int row = 0, maxRow = Math.min(rowSize, Layer.this.rowSize); row < maxRow; row++) {
				for (int column = 0,
						maxColumn = Math.min(columnSize, Layer.this.columnSize); column < maxColumn; column++) {
					slots[row][column] = Layer.this.slots[row][column];
				}
			}
			Layer.this.rowSize = rowSize;
			Layer.this.columnSize = columnSize;
			Layer.this.slots = slots;
			return this;
		}

		public Editor rowSize(int rowSize) {
			return resize(rowSize, Layer.this.columnSize);
		}

		public Editor columnSize(int columnSize) {
			return resize(Layer.this.rowSize, columnSize);
		}

		public Editor marginLeft(int marginLeft) {
			Layer.this.marginLeft = marginLeft;
			return this;
		}

		public Editor marginTop(int marginTop) {
			Layer.this.marginTop = marginTop;
			return this;
		}

		public Editor relative(boolean relative) {
			Layer.this.relative = relative;
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

		public Editor editRow(int row, BiConsumer<SlotHandler, Integer> onEdit) {
			for (int column = 0; column < columnSize; column++) {
				int finalColumn = column;
				editSlot(row, column, slotHandler -> onEdit.accept(slotHandler, finalColumn));
			}
			return this;
		}

		public Editor editColumn(int column, BiConsumer<SlotHandler, Integer> onEdit) {
			for (int row = 0; row < rowSize; row++) {
				int finalRow = row;
				editSlot(row, column, slotHandler -> onEdit.accept(slotHandler, finalRow));
			}
			return this;
		}

		public Editor editAll(TriConsumer<SlotHandler, Integer, Integer> onEdit) {
			for (int row = 0; row < rowSize; row++) {
				for (int column = 0; column < columnSize; column++) {
					int finalRow = row;
					int finalColumn = column;
					editSlot(row, column, slotHandler -> onEdit.accept(slotHandler, finalRow, finalColumn));
				}
			}
			return this;
		}

		public Editor tile(int maxIndex, boolean resizeIfOutOfBound, BiConsumer<SlotHandler, Integer> onEdit) {
			if (resizeIfOutOfBound) {
				int rowSize = (maxIndex - 1) / columnSize + 1;
				if (rowSize > Layer.this.rowSize) {
					resize(rowSize, columnSize);
				}
			}
			for (int row = 0; row < rowSize; row++) {
				for (int column = 0; column < columnSize; column++) {
					int index = row * columnSize + column;
					if (index >= maxIndex) {
						return this;
					}
					editSlot(row, column, slotHandler -> onEdit.accept(slotHandler, index));
				}
			}
			return this;
		}

		public Editor clear() {
			return editAll((slotHandler, row, column) -> slotHandler.empty());
		}
	}
}
