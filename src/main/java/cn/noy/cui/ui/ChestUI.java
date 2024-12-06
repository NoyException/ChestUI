package cn.noy.cui.ui;

import cn.noy.cui.event.CUIAddItemEvent;
import cn.noy.cui.event.CUIClickEvent;
import cn.noy.cui.layer.Layer;
import cn.noy.cui.util.ItemStacks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ChestUI<T extends CUIHandler> {
    private final HashMap<Player, PlayerInfo> viewers = new HashMap<>();
    private Component title;
    private State state = State.UNINITIALIZED;

    private final int maxRow;
    private final int maxColumn;
    private final int maxDepth;
    private final TreeMap<Integer, LayerWrapper> layers = new TreeMap<>();
    private final HashMap<Layer, Integer> layerDepths = new HashMap<>();

    private boolean closable = true;
    private boolean keepAlive;
    private long ticks;

    private final T handler;
    private final Trigger trigger;
    private final DummyHolder holder;
    private Inventory chest;
    private boolean dirty;

    ChestUI(@NotNull T handler) {
        this.handler = handler;
        this.trigger = new Trigger();
        this.holder = new DummyHolder();

        var clazz = handler.getClass();
        if (clazz.isAnnotationPresent(ChestSize.class)) {
            ChestSize chestSize = clazz.getAnnotation(ChestSize.class);
            maxRow = chestSize.maxRow();
            maxColumn = chestSize.maxColumn();
            maxDepth = chestSize.maxDepth();
        } else {
            maxRow = 3;
            maxColumn = 9;
            maxDepth = 1;
        }
        if (clazz.isAnnotationPresent(ChestTitle.class)) {
            ChestTitle chestTitle = clazz.getAnnotation(ChestTitle.class);
            title = LegacyComponentSerializer.legacyAmpersand().deserialize(chestTitle.value());
        } else {
            title = Component.empty();
        }
    }

    public Trigger getTrigger() {
        return trigger;
    }

    @SuppressWarnings("unchecked")
    public Class<T> getHandlerClass() {
        return (Class<T>) handler.getClass();
    }

    public List<Player> getViewers() {
        return new ArrayList<>(viewers.keySet());
    }

    public Component getTitle() {
        return title;
    }

    public State getState() {
        return state;
    }

    public int getMaxRow() {
        return maxRow;
    }

    public int getMaxColumn() {
        return maxColumn;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public Layer getLayer(int index) {
        return layers.get(index).layer;
    }

    public int getLayerDepth(Layer layer) {
        return layerDepths.getOrDefault(layer, -1);
    }

    public List<Layer> getActiveLayers() {
        return layers.descendingMap().values().stream().
                filter(layerWrapper -> layerWrapper.active).
                map(layerWrapper -> layerWrapper.layer).toList();
    }

    public boolean isActive(int depth) {
        return layers.get(depth).active;
    }

    public boolean isActive(Layer layer) {
        var depth = getLayerDepth(layer);
        return depth >= 0 && isActive(depth);
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public long getTicksLived() {
        return ticks;
    }

    public boolean open(Player viewer) {
        if (viewers.containsKey(viewer)) {
            return false;
        }
        if (!handler.onOpen(viewer)) {
            return false;
        }
        viewers.put(viewer, new PlayerInfo());
        CUIManager.getInstance().notifyOpen(viewer, this);
        return true;
    }

    private boolean openFrom(Player viewer, ChestUI<?> from) {
        if (!open(viewer)) {
            return false;
        }

        var info = viewers.get(viewer);
        if (info == null) {
            return false;
        }

        info.from = from;
        return true;
    }

    // TODO: test
    public boolean switchTo(Player viewer, ChestUI<?> to) {
        var info = viewers.get(viewer);
        if (info == null) {
            return false;
        }
        if (!handler.onSwitchTo(viewer, to)) {
            return false;
        }

        if (!to.openFrom(viewer, this)) {
            return false;
        }

        info.to = to;
        info.state = PlayerInfo.State.VIEWING_OTHER;
        return true;
    }

    public boolean isClosable() {
        return closable;
    }

    private boolean switchBack(Player viewer) {
        var info = viewers.get(viewer);
        if (info == null) {
            Bukkit.getLogger().severe("Player " + viewer.getName() + " is not viewing " + this);
            return false;
        }
        if (!handler.onSwitchBack(viewer, info.to)) {
            return false;
        }
        info.to = null;
        info.state = PlayerInfo.State.VIEWING;
        CUIManager.getInstance().notifyOpen(viewer, this);
        return true;
    }

    public boolean close(Player viewer, boolean force) {
        if (!closable && !force) {
            return false;
        }

        var info = viewers.get(viewer);
        if (info == null) {
            return false;
        }

        if (info.to != null && !force) {
            return false;
        }

        var oldState = info.state;
        info.state = PlayerInfo.State.CLOSING;

        if (!handler.onClose(viewer)) {
            info.state = oldState;
            return false;
        }

        if(info.from != null && !info.from.switchBack(viewer)){
            info.state = oldState;
            return false;
        }

        if(info.to != null){
            info.to.close(viewer, true);
        }
        viewers.remove(viewer);
        CUIManager.getInstance().notifyClose(viewer, this);
        if (viewer.getOpenInventory().getTopInventory().getHolder() == holder) {
            viewer.closeInventory();
        }
        return true;
    }

    /**
     * 尝试关闭所有CUI，返回第一个无法关闭的CUI<br>
     * Try to close all CUIs and return the first CUI that cannot be closed
     * @param viewer 玩家<br>Player
     * @param force 是否强制关闭<br>Whether to force close
     * @return 无法关闭的CUI，如果所有CUI都关闭了则返回null<br>
     * CUI that cannot be closed, return null if all CUIs are closed
     */
    public ChestUI<?> closeAll(Player viewer, boolean force) {
        var info = viewers.get(viewer);
        if (info == null) {
            return this;
        }

        if(!close(viewer, force)){
            return this;
        }

        if (info.from != null) {
            return info.from.closeAll(viewer, force);
        }
        return null;
    }

    public void destroy() {
        handler.onDestroy();
        var players = new ArrayList<>(viewers.keySet());
        players.forEach(player -> closeAll(player, true));
        CUIManager.getInstance().notifyDestroy(this);
    }

    public Editor edit() {
        return new Editor();
    }

    public class Editor {
        private Editor() {
        }

        @SuppressWarnings("unchecked")
        public <T2 extends CUIHandler> ChestUI<T2> finish() {
            return (ChestUI<T2>) ChestUI.this;
        }

        public Editor setTitle(String title) {
            return setTitle(LegacyComponentSerializer.legacyAmpersand().deserialize(title));
        }

        public Editor setTitle(Component title) {
            ChestUI.this.title = title;
            state = State.REFRESHING;
            return this;
        }

        //TODO: test
        public Editor setClosable(boolean closable) {
            ChestUI.this.closable = closable;
            return this;
        }

        //TODO: test
        public Editor setKeepAlive(boolean keepAlive) {
            ChestUI.this.keepAlive = keepAlive;
            return this;
        }

        public Editor setLayer(int depth, Layer layer) {
            if (depth < 0 || depth >= maxDepth)
                throw new IllegalArgumentException("depth must be between 0 and " + maxDepth);

            if (layer == null) {
                return removeLayer(depth);
            }

            var legacy = layers.put(depth, new LayerWrapper(layer));
            if (legacy != null) {
                layerDepths.remove(legacy.layer);
            }
            layerDepths.put(layer, depth);
            dirty = true;
            return this;
        }

        public Editor removeLayer(int depth) {
            var legacy = layers.remove(depth);
            if (legacy != null) {
                layerDepths.remove(legacy.layer);
            }
            dirty = true;
            return this;
        }

        public Editor setActive(int depth, boolean active) {
            LayerWrapper wrapper = layers.get(depth);
            if (wrapper == null) {
                return this;
            }
            if (wrapper.active == active)
                return this;
            wrapper.active = active;
            dirty = true;
            return this;
        }

        public Editor setActive(Layer layer, boolean active) {
            var depth = getLayerDepth(layer);
            if (depth < 0)
                return this;
            return setActive(depth, active);
        }
    }

    public enum State {
        UNINITIALIZED,
        REFRESHING,
        READY
    }

    public class Trigger {
        private Trigger() {
        }

        public void tick() {
            ticks++;
            handler.onTick();
            if (!keepAlive && viewers.isEmpty()) {
                destroy();
            }
        }

        private void sync(boolean force) {
            List<Layer> activeLayers = getActiveLayers();
            if (!force && !dirty) {
                if (activeLayers.stream().noneMatch(Layer::isDirty))
                    return;
            }
            var itemStacks = new ItemStack[maxRow][maxColumn];
            activeLayers.forEach(layer -> layer.display(itemStacks));
            for (int row = 0; row < maxRow; row++) {
                for (int column = 0; column < maxColumn; column++) {
                    var itemStack = itemStacks[row][column];
                    chest.setItem(row * maxColumn + column, itemStack);
                }
            }
            dirty = false;
        }

        /**
         * 更新并同步界面状态<br>
         * Update and synchronize interface status
         */
        public void update() {
            if (state == State.REFRESHING || state == State.UNINITIALIZED) {
                chest = Bukkit.createInventory(holder, maxRow * maxColumn, title);
                sync(true);
            } else {
                sync(false);
            }
            state = State.READY;
            // openInventory可能会触发事件，从而导致ConcurrentModificationException
            var copy = new HashMap<>(viewers);
            copy.forEach((player, info) -> {
                if (info.state != PlayerInfo.State.VIEWING){
                    return;
                }

                if (player.getOpenInventory().getTopInventory() != chest) {
                    player.openInventory(chest);
                }
            });
        }

        public void click(Player player, ClickType clickType, InventoryAction action,
                          int row, int column, ItemStack cursor) {
            var event = new CUIClickEvent<>(ChestUI.this, player,
                    clickType, action, row, column, cursor);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }

            for (Layer layer : getActiveLayers()) {
                layer.click(event);
                if (event.isCancelled()) {
                    player.setItemOnCursor(event.getCursor());
                    return;
                }
            }
        }

        public ItemStack place(Player player, ItemStack itemStack, int row, int column) {
            if (ItemStacks.isEmpty(itemStack)) {
                return null;
            }

            for (Layer layer : getActiveLayers()) {
                var slot = layer.getRelativeSlot(row, column);
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

        public ItemStack addItem(Player player, ItemStack itemStack) {
            if (ItemStacks.isEmpty(itemStack)) {
                return null;
            }

            var event = new CUIAddItemEvent<>(ChestUI.this, player, itemStack);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return itemStack;
            }
            itemStack = event.getItemStack();
            if (ItemStacks.isEmpty(itemStack)) {
                return null;
            }

            var activeLayers = getActiveLayers();
            for (int row = 0; row < maxRow; row++) {
                for (int column = 0; column < maxColumn; column++) {
                    for (Layer layer : activeLayers) {
                        var slot = layer.getRelativeSlot(row, column);
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
         * 收集物品（双击物品时触发）<br>
         * Collect items (triggered when double-clicking items)
         *
         * @param player          玩家<br>
         *                        The player
         * @param itemStack       待收集的物品<br>
         *                        The item to collect
         * @param collectBackpack 是否收集背包中的物品<br>
         *                        Whether to collect items in the backpack
         * @return 收集到的物品，如果没有收集到则返回null<br>
         * The collected item, if there is no collected item, return null
         */
        public ItemStack collect(Player player, ItemStack itemStack, boolean collectBackpack) {
            if (ItemStacks.isEmpty(itemStack)) {
                return null;
            }
            interface Collectable {
                ItemStack collect(ItemStack itemStack);
            }
            var list = new ArrayList<Pair<Integer, Collectable>>();

            var activeLayers = getActiveLayers();
            for (int row = 0; row < maxRow; row++) {
                for (int column = 0; column < maxColumn; column++) {
                    for (Layer layer : activeLayers) {
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
    }

    private class DummyHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return chest;
        }
    }

    private static class LayerWrapper {
        private final Layer layer;
        private boolean active = true;

        LayerWrapper(Layer layer) {
            this.layer = layer;
        }
    }

    public static class PlayerInfo {
        private @Nullable ChestUI<?> from;
        private @Nullable ChestUI<?> to;
        private State state = State.VIEWING;

        public enum State {
            CLOSING,
            VIEWING,
            /**
             * 玩家正在查看其他CUI，但是当前CUI仍然处于打开状态。这通常发生在玩家从一个CUI切换到另一个CUI时。<br>
             * Player is viewing another CUI, but the current CUI is still open.
             * This usually happens when a player switches from one CUI to another.
             */
            VIEWING_OTHER,
        }
    }

    @Override
    public String toString() {
        return "ChestUI{" +
                "handler=" + handler.getClass().getCanonicalName() +
                ", title=" + title +
                ", state=" + state +
                ", ticks=" + ticks +
                '}';
    }
}
