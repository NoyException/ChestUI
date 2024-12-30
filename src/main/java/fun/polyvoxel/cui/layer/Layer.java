package fun.polyvoxel.cui.layer;

import fun.polyvoxel.cui.event.CUIClickEvent;
import fun.polyvoxel.cui.slot.Empty;
import fun.polyvoxel.cui.slot.Slot;
import fun.polyvoxel.cui.ui.CUIContents;

import fun.polyvoxel.cui.ui.Camera;
import fun.polyvoxel.cui.util.ItemStacks;
import fun.polyvoxel.cui.util.Position;
import org.jetbrains.annotations.Nullable;

import java.util.function.*;

public class Layer {
	private int rowSize;
	private int columnSize;
	private int marginLeft;
	private int marginTop;
	// margin的位置是相对摄像头还是绝对值
	private boolean relative;
	private Slot[][] slots;
	private boolean dirty;

	public Layer(int rowSize, int columnSize) {
		this.rowSize = rowSize;
		this.columnSize = columnSize;
		slots = new Slot[rowSize][columnSize];
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
	}

	public void tick() {
		for (int row = 0; row < rowSize; row++) {
			for (int column = 0; column < columnSize; column++) {
				var slot = slots[row][column];
				if (slot != null) {
					slot.tick();
				}
			}
		}
	}

	public void tickEnd() {
		dirty = false;
	}

	/**
	 * 检查坐标是否超出范围。<br>
	 * Check if the coordinates are out of range.
	 * 
	 * @param row
	 *            行<br>
	 *            row
	 * @param column
	 *            列<br>
	 *            column
	 * @return 如果有效返回true<br>
	 *         true if valid
	 */
	public boolean isValidPosition(int row, int column) {
		return row >= 0 && row < rowSize && column >= 0 && column < columnSize;
	}

	public @Nullable Slot getSlot(int row, int column) {
		if (!isValidPosition(row, column)) {
			return null;
		}
		var handler = slots[row][column];
		if (handler == null) {
			handler = Empty.getInstance();
			slots[row][column] = handler;
		}
		return handler;
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
				var itemStack = ItemStacks.clone(contents.getItem(row, column));
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
			slot.click(event);
		}
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
					layer.slots[row][column] = slot.deepClone();
				}
			}
		}
		return layer;
	}

	public Editor edit() {
		return new Editor();
	}

	public class Editor {

		public Layer done() {
			return Layer.this;
		}

		public Editor resize(int rowSize, int columnSize) {
			var slots = new Slot[rowSize][columnSize];
			for (int row = 0, maxRow = Math.min(rowSize, Layer.this.rowSize); row < maxRow; row++) {
				for (int column = 0,
						maxColumn = Math.min(columnSize, Layer.this.columnSize); column < maxColumn; column++) {
					slots[row][column] = Layer.this.slots[row][column];
				}
			}
			Layer.this.rowSize = rowSize;
			Layer.this.columnSize = columnSize;
			Layer.this.slots = slots;
			markDirty();
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
			markDirty();
			return this;
		}

		public Editor marginTop(int marginTop) {
			Layer.this.marginTop = marginTop;
			markDirty();
			return this;
		}

		/**
		 * 设置margin的位置是相对摄像头还是绝对值<br>
		 * Set whether the position of the margin is relative to the camera or absolute
		 * 
		 * @param relative
		 *            是否相对摄像头<br>
		 *            Whether it is relative to the camera
		 * @return this
		 */
		public Editor relative(boolean relative) {
			Layer.this.relative = relative;
			markDirty();
			return this;
		}

		public Editor slot(int row, int column, Supplier<Slot> supplier) {
			if (!isValidPosition(row, column)) {
				throw new IndexOutOfBoundsException(
						"(row, column) must be between (0, 0) and (" + rowSize + ", " + columnSize + ")");
			}

			Slot slot = supplier.get();
			if (slot == null) {
				return this;
			}

			slot.bind(Layer.this::markDirty);
			slots[row][column] = slot;
			markDirty();
			return this;
		}

		public Editor slot(Position position, Supplier<Slot> supplier) {
			return slot(position.row(), position.column(), supplier);
		}

		public Editor row(int row, Function<Integer, Slot> supplier) {
			for (int column = 0; column < columnSize; column++) {
				int finalColumn = column;
				slot(row, column, () -> supplier.apply(finalColumn));
			}
			return this;
		}

		public Editor column(int column, Function<Integer, Slot> supplier) {
			for (int row = 0; row < rowSize; row++) {
				int finalRow = row;
				slot(row, column, () -> supplier.apply(finalRow));
			}
			return this;
		}

		public Editor all(BiFunction<Integer, Integer, Slot> supplier) {
			for (int row = 0; row < rowSize; row++) {
				for (int column = 0; column < columnSize; column++) {
					int finalRow = row;
					int finalColumn = column;
					slot(row, column, () -> supplier.apply(finalRow, finalColumn));
				}
			}
			return this;
		}

		public Editor rect(int rowStart, int columnStart, int rowEnd, int columnEnd,
				BiFunction<Integer, Integer, Slot> supplier) {
			for (int row = rowStart; row < rowEnd; row++) {
				for (int column = columnStart; column < columnEnd; column++) {
					int finalRow = row;
					int finalColumn = column;
					slot(row, column, () -> supplier.apply(finalRow, finalColumn));
				}
			}
			return this;
		}

		/**
		 * 按照书写的顺序（从左到右，从上到下）填充槽位。<br>
		 * Fill the slots in the writing order (from left to right, from top to bottom).
		 * 
		 * @param maxIndex
		 *            最大索引<br>
		 *            The maximum index
		 * @param resizeIfOutOfBound
		 *            如果超出边界是否调整大小<br>
		 *            Whether to resize if out of bounds
		 * @param supplier
		 *            索引->槽位<br>
		 *            Index->Slot
		 * @return this
		 */
		public Editor tile(int maxIndex, boolean resizeIfOutOfBound, Function<Integer, Slot> supplier) {
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
					slot(row, column, () -> supplier.apply(index));
				}
			}
			return this;
		}

		public Editor clear() {
			return all((row, column) -> Empty.getInstance());
		}
	}
}
