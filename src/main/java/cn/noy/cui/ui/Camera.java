package cn.noy.cui.ui;

import cn.noy.cui.event.CUIAddItemEvent;
import cn.noy.cui.event.CUIClickEvent;
import cn.noy.cui.layer.Layer;
import cn.noy.cui.util.ItemStacks;
import cn.noy.cui.util.Position;

import net.kyori.adventure.text.Component;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Camera<T extends CUIHandler> {
	private final ChestUI<T> chestUI;
	private final HashSet<Player> viewers = new HashSet<>();
	private final TreeMap<Integer, LayerWrapper> mask = new TreeMap<>();
	private final HashMap<Layer, Integer> layerPriority = new HashMap<>();
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

	public Camera(ChestUI<T> chestUI, Position position, int rowSize, int columnSize, HorizontalAlign horizontalAlign,
			VerticalAlign verticalAlign, Component title) {
		this.chestUI = chestUI;
		edit().setPosition(position).setRowSize(rowSize).setColumnSize(columnSize).setHorizontalAlign(horizontalAlign)
				.setVerticalAlign(verticalAlign).setTitle(title);
	}

	public Editor edit() {
		return new Editor();
	}

	/**
	 * 获取所有正在使用该Camera或暂时切换到其他CUI的玩家<br>
	 * Get all players who are using this Camera or temporarily switched to other
	 * CUIs
	 *
	 * @return 玩家列表<br>
	 *         Player list
	 */
	public List<Player> getViewers() {
		return new ArrayList<>(viewers);
	}

	public List<Layer> getActiveMasks(boolean ascending) {
		if (ascending) {
			return mask.values().stream().filter(wrapper -> wrapper.active).map(wrapper -> wrapper.layer).toList();
		}
		return mask.descendingMap().values().stream().filter(wrapper -> wrapper.active).map(wrapper -> wrapper.layer)
				.toList();
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

	public int getPriority(Layer layer) {
		return layerPriority.getOrDefault(layer, -1);
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

	// TODO: 测试切换Camera
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
		if (!keepAlive && viewers.isEmpty()) {
			chestUI.getTrigger().notifyReleaseCamera(this);
		}
	}

	/**
	 * 点击事件处理<br>
	 * Click event processing
	 *
	 * @param player
	 *            玩家<br>
	 *            Player
	 * @param clickType
	 *            点击类型<br>
	 *            Click type
	 * @param action
	 *            点击行为<br>
	 *            Click action
	 * @param row
	 *            相对于Camera的行<br>
	 *            Relative to Camera row
	 * @param column
	 *            相对于Camera的列<br>
	 *            Relative to Camera column
	 * @param cursor
	 *            鼠标上的物品<br>
	 *            Item on cursor
	 */
	public void click(Player player, ClickType clickType, InventoryAction action, int row, int column,
			ItemStack cursor) {
		var topLeft = getTopLeft();
		var event = new CUIClickEvent<>(chestUI, player, clickType, action,
				new Position(row + topLeft.row(), column + topLeft.column()), cursor);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return;
		}

		// 深度低的先click
		for (Layer layer : getActiveMasks(true)) {
			layer.click(event, topLeft.row(), topLeft.column());
			if (event.isCancelled()) {
				player.setItemOnCursor(event.getCursor());
				return;
			}
		}
		for (Layer layer : chestUI.getActiveLayers(true)) {
			layer.click(event, 0, 0);
			if (event.isCancelled()) {
				player.setItemOnCursor(event.getCursor());
				return;
			}
		}
	}

	/**
	 * 放置物品<br>
	 * Place item
	 *
	 * @param player
	 *            玩家<br>
	 *            Player
	 * @param itemStack
	 *            物品<br>
	 *            ItemStack
	 * @param row
	 *            相对于Camera的行<br>
	 *            Relative to Camera row
	 * @param column
	 *            相对于Camera的列<br>
	 *            Relative to Camera column
	 * @return 剩余物品<br>
	 *         Remaining ItemStack
	 */
	public ItemStack place(Player player, ItemStack itemStack, int row, int column) {
		if (ItemStacks.isEmpty(itemStack)) {
			return null;
		}

		var topLeft = getTopLeft();

		// 深度低的先place
		for (Layer layer : getActiveMasks(true)) {
			var slot = layer.getRelativeSlot(row, column);
			if (slot == null) {
				continue;
			}
			itemStack = slot.place(itemStack);
			if (ItemStacks.isEmpty(itemStack)) {
				return null;
			}
		}
		for (Layer layer : chestUI.getActiveLayers(true)) {
			var slot = layer.getRelativeSlot(row + topLeft.row(), column + topLeft.column());
			if (slot == null) {
				continue;
			}
			itemStack = slot.place(itemStack);
			if (ItemStacks.isEmpty(itemStack)) {
				return null;
			}
		}
		return itemStack;
	}

	/**
	 * 尝试添加物品。仅添加到Camera视野范围内的物品槽<br>
	 * Try to add items. Only add to the item slots within the Camera's field of
	 * view
	 *
	 * @param player
	 *            玩家<br>
	 *            Player
	 * @param itemStack
	 *            物品<br>
	 *            ItemStack
	 * @return 剩余物品，如果没有剩余则返回null<br>
	 *         Remaining ItemStack, return null if there is no remaining
	 */
	public ItemStack addItem(Player player, ItemStack itemStack) {
		if (ItemStacks.isEmpty(itemStack)) {
			return null;
		}

		var event = new CUIAddItemEvent<>(chestUI, player, itemStack);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return itemStack;
		}
		itemStack = event.getItemStack();
		if (ItemStacks.isEmpty(itemStack)) {
			return null;
		}

		// 深度低的先addItem
		var topLeft = getTopLeft();
		var activeMasks = getActiveMasks(true);
		var activeLayers = chestUI.getActiveLayers(true);
		for (int row = 0; row < rowSize; row++) {
			for (int column = 0; column < columnSize; column++) {
				for (Layer layer : activeMasks) {
					var slot = layer.getRelativeSlot(row, column);
					if (slot == null) {
						continue;
					}
					itemStack = slot.place(itemStack);
					if (ItemStacks.isEmpty(itemStack)) {
						return null;
					}
				}
				for (Layer layer : activeLayers) {
					var slot = layer.getRelativeSlot(row + topLeft.row(), column + topLeft.column());
					if (slot == null) {
						continue;
					}
					itemStack = slot.place(itemStack);
					if (ItemStacks.isEmpty(itemStack)) {
						return null;
					}
				}
			}
		}
		return itemStack;
	}

	/**
	 * 收集物品（双击物品时触发），仅收集Camera视野范围内的物品<br>
	 * Collect items (triggered when double-clicking items), only collect items
	 * within the Camera's field of view
	 *
	 * @param player
	 *            玩家<br>
	 *            The player
	 * @param itemStack
	 *            待收集的物品<br>
	 *            The item to collect
	 * @param collectBackpack
	 *            是否收集背包中的物品<br>
	 *            Whether to collect items in the backpack
	 * @return 收集到的物品，如果没有收集到则返回null<br>
	 *         The collected item, if there is no collected item, return null
	 */
	public ItemStack collect(Player player, ItemStack itemStack, boolean collectBackpack) {
		if (ItemStacks.isEmpty(itemStack)) {
			return null;
		}
		interface Collectable {
			ItemStack collect(ItemStack itemStack);
		}
		var list = new ArrayList<Pair<Integer, Collectable>>();

		var topLeft = getTopLeft();
		var activeMasks = getActiveMasks(true);
		var activeLayers = chestUI.getActiveLayers(true);
		for (int row = 0; row < rowSize; row++) {
			for (int column = 0; column < columnSize; column++) {
				for (Layer layer : activeMasks) {
					var slot = layer.getRelativeSlot(row, column);
					if (slot == null) {
						continue;
					}
					var inSlot = slot.get();
					if (ItemStacks.isEmpty(inSlot)) {
						continue;
					}
					list.add(Pair.of(inSlot.getAmount(), slot::collect));
				}
				for (Layer layer : activeLayers) {
					var slot = layer.getRelativeSlot(row + topLeft.row(), column + topLeft.column());
					if (slot == null) {
						continue;
					}
					var inSlot = slot.get();
					if (ItemStacks.isEmpty(inSlot)) {
						continue;
					}
					list.add(Pair.of(inSlot.getAmount(), slot::collect));
				}
			}
		}

		if (collectBackpack) {
			var inventory = player.getInventory();
			for (int slot = 0; slot < inventory.getSize(); slot++) {
				var item = inventory.getItem(slot);
				if (ItemStacks.isEmpty(item)) {
					continue;
				}
				int finalSlot = slot;
				list.add(Pair.of(item.getAmount(), itemStack1 -> {
					var result = ItemStacks.place(itemStack1, item);
					itemStack1 = result.placed();
					inventory.setItem(finalSlot, result.remaining());
					return itemStack1;
				}));
			}
		}

		list.sort(Comparator.comparingInt(Pair::getLeft));
		for (var pair : list) {
			itemStack = pair.getRight().collect(itemStack);
			if (ItemStacks.isFull(itemStack)) {
				return itemStack;
			}
		}
		return itemStack;
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

		var topLeft = getTopLeft();
		var contents = chestUI.getContents();
		getActiveMasks(false).forEach(layer -> layer.display(contents, topLeft.row(), topLeft.column()));

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
		mask.forEach((priority, layerWrapper) -> clone.edit().setMask(priority, layerWrapper.layer.deepClone()));
		return clone;
	}

	public enum HorizontalAlign {
		LEFT, MIDDLE, RIGHT
	}

	public enum VerticalAlign {
		TOP, MIDDLE, BOTTOM
	}

	private static class LayerWrapper {
		private final Layer layer;
		private boolean active = true;

		LayerWrapper(Layer layer) {
			this.layer = layer;
		}
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

		public Editor move(int row, int column) {
			Camera.this.position = position.add(row, column);
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
			var legacy = mask.put(priority, new LayerWrapper(layer));
			if (legacy != null) {
				layerPriority.remove(legacy.layer);
			}
			layerPriority.put(layer, priority);
			dirty = true;
			return this;
		}

		public Editor removeMask(int priority) {
			var wrapper = mask.remove(priority);
			if (wrapper != null) {
				layerPriority.remove(wrapper.layer);
			}
			dirty = true;
			return this;
		}

		public Editor setActive(int priority, boolean active) {
			var wrapper = mask.get(priority);
			if (wrapper != null) {
				wrapper.active = active;
				dirty = true;
			}
			return this;
		}

		public Editor setActive(Layer layer, boolean active) {
			var priority = getPriority(layer);
			if (priority >= 0) {
				mask.get(priority).active = active;
				dirty = true;
			}
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
