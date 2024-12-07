package cn.noy.cui.ui;

import cn.noy.cui.util.Position;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class CUIContents {
    private final int maxRow, maxColumn;
    private final HashMap<Position, ItemStack> contents = new HashMap<>();

    public CUIContents(int maxRow, int maxColumn) {
        this.maxRow = maxRow;
        this.maxColumn = maxColumn;
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
    public CUIContents clone() {
        var clone = new CUIContents(maxRow, maxColumn);
        clone.contents.putAll(contents);
        return clone;
    }
}
