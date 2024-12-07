package cn.noy.cui.ui;

import cn.noy.cui.layer.Layer;
import cn.noy.cui.util.Position;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Camera<T extends CUIHandler> {
    private final ChestUI<T> chestUI;
    private final HashSet<Player> viewers = new HashSet<>();
    private final TreeMap<Integer, Layer> mask = new TreeMap<>();
    private final InventoryHolder holder = new DummyHolder();
    private Position position;
    private int rowSize, columnSize;
    private HorizontalAlign horizontalAlign;
    private VerticalAlign verticalAlign;
    private boolean keepAlive;
    private Component title;
    private Inventory inventory;

    private boolean recreate = true;
    private boolean dirty = true;

    public Camera(ChestUI<T> chestUI, Position position, int rowSize, int columnSize, HorizontalAlign horizontalAlign, VerticalAlign verticalAlign, Component title) {
        this.chestUI = chestUI;
        edit().setPosition(position).
                setRowSize(rowSize).
                setColumnSize(columnSize).
                setHorizontalAlign(horizontalAlign).
                setVerticalAlign(verticalAlign).
                setTitle(title);
    }

    public Editor edit() {
        return new Editor();
    }

    /**
     * 获取所有正在使用该Camera或暂时切换到其他CUI的玩家<br>
     * Get all players who are using this Camera or temporarily switched to other CUIs
     * @return 玩家列表<br>Player list
     */
    public List<Player> getViewers() {
        return new ArrayList<>(viewers);
    }

    public Position getPosition() {
        return position;
    }

    public int getRowSize() {
        return rowSize;
    }

    public int getColumnSize() {
        return columnSize;
    }

    public HorizontalAlign getHorizontalAlign() {
        return horizontalAlign;
    }

    public VerticalAlign getVerticalAlign() {
        return verticalAlign;
    }

    public Component getTitle() {
        return title;
    }

    public Position getTopLeft() {
        var row = switch (verticalAlign) {
            case TOP -> position.row();
            case MIDDLE -> position.row() - rowSize / 2;
            case BOTTOM -> position.row() - rowSize;
        };
        var column = switch (horizontalAlign) {
            case LEFT -> position.column();
            case MIDDLE -> position.column() - columnSize / 2;
            case RIGHT -> position.column() - columnSize;
        };
        return new Position(row, column);
    }

    //TODO: 测试切换Camera
    public void open(Player viewer) {
        viewers.add(viewer);
        if (viewer.getOpenInventory().getTopInventory() != inventory) {
            viewer.openInventory(inventory);
        }
        chestUI.getTrigger().notifyUseCamera(viewer, this);
    }

    public void close(Player viewer) {
        viewers.remove(viewer);
        if (viewer.getOpenInventory().getTopInventory().getHolder() == holder) {
            viewer.closeInventory();
        }
        if(!keepAlive && viewers.isEmpty()){
            chestUI.getTrigger().notifyReleaseCamera(this);
        }
    }

    public void sync(boolean force) {
        if (!recreate && !dirty && !force) {
            return;
        }
        if (recreate) {
            inventory = Bukkit.createInventory(holder, rowSize * columnSize, title);
            recreate = false;
        }
        dirty = false;

        var contents = chestUI.getContents();
        mask.descendingMap().forEach((index, layer) -> layer.display(contents));

        var topLeft = getTopLeft();
        for (int row = 0; row < rowSize; row++) {
            for (int column = 0; column < columnSize; column++) {
                var index = row * columnSize + column;
                var absoluteRow = row + topLeft.row();
                var absoluteColumn = column + topLeft.column();
                inventory.setItem(index, contents.getItem(absoluteRow, absoluteColumn));
            }
        }
    }

    public Camera<T> deepClone() {
        var clone = new Camera<>(chestUI, position, rowSize, columnSize, horizontalAlign, verticalAlign, title);
        mask.forEach((priority, layer)-> clone.edit().setMask(priority, layer.deepClone()));
        return clone;
    }

    public enum HorizontalAlign {
        LEFT, MIDDLE, RIGHT
    }

    public enum VerticalAlign {
        TOP, MIDDLE, BOTTOM
    }

    public class Editor {
        public Camera<T> finish() {
            return Camera.this;
        }

        public Editor setPosition(Position position) {
            Camera.this.position = position;
            dirty = true;
            return this;
        }

        public Editor setRowSize(int rowSize) {
            if (rowSize < 1 || rowSize > 6) {
                throw new IllegalArgumentException("Row size must be between 1 and 6");
            }
            Camera.this.rowSize = rowSize;
            recreate = true;
            dirty = true;
            return this;
        }

        public Editor setColumnSize(int columnSize) {
            if (columnSize < 1 || columnSize > 9) {
                throw new IllegalArgumentException("Column size must be between 1 and 9");
            }
            Camera.this.columnSize = columnSize;
            recreate = true;
            dirty = true;
            return this;
        }

        public Editor setHorizontalAlign(HorizontalAlign horizontalAlign) {
            Camera.this.horizontalAlign = horizontalAlign;
            dirty = true;
            return this;
        }

        public Editor setVerticalAlign(VerticalAlign verticalAlign) {
            Camera.this.verticalAlign = verticalAlign;
            dirty = true;
            return this;
        }

        public Editor setTitle(Component title) {
            Camera.this.title = title;
            recreate = true;
            dirty = true;
            return this;
        }

        public Editor setMask(int priority, Layer layer) {
            mask.put(priority, layer);
            dirty = true;
            return this;
        }

        public Editor removeMask(int priority) {
            mask.remove(priority);
            dirty = true;
            return this;
        }

        public Editor setKeepAlive(boolean keepAlive) {
            Camera.this.keepAlive = keepAlive;
            return this;
        }

        public Editor setRecreate(boolean recreate) {
            Camera.this.recreate = recreate;
            dirty = true;
            return this;
        }
    }

    private class DummyHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
