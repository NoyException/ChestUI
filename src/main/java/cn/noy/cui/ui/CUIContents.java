package cn.noy.cui.ui;

import cn.noy.cui.util.Position;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class CUIContents<T extends CUIHandler<T>> {
	private final Camera<T> camera;
	private final int maxRow, maxColumn;
	private final HashMap<Position, ItemStack> contents = new HashMap<>();

	public CUIContents(Camera<T> camera) {
		this.camera = camera;
		this.maxRow = camera.getRowSize();
		this.maxColumn = camera.getColumnSize();
	}

	public Camera<T> getCamera() {
		return camera;
	}

	public int getMaxRow() {
		return maxRow;
	}

	public int getMaxColumn() {
		return maxColumn;
	}

	public boolean isValidPosition(Position position) {
		if (maxRow >= 0 && (position.row() < 0 || position.row() >= maxRow)) {
			return false;
		}
		if (maxColumn >= 0 && (position.column() < 0 || position.column() >= maxColumn)) {
			return false;
		}
		return true;
	}

	public ItemStack getItem(Position position) {
		return contents.get(position);
	}

	public ItemStack getItem(int row, int column) {
		return getItem(new Position(row, column));
	}

	public void setItem(Position position, ItemStack item) {
		if (!isValidPosition(position)) {
			return;
		}
		contents.put(position, item);
	}

	public void setItem(int row, int column, ItemStack item) {
		setItem(new Position(row, column), item);
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	public CUIContents<T> clone() {
		var clone = new CUIContents<>(camera);
		clone.contents.putAll(contents);
		return clone;
	}
}
