package cn.noy.cui.slot;

import cn.noy.cui.event.CUIClickEvent;
import cn.noy.cui.ui.CUIManager;
import cn.noy.cui.util.ItemStacks;
import org.bukkit.GameMode;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class Storage extends Slot {
    public interface Source {
        ItemStack get();

        void set(ItemStack itemStack);

        boolean isDirty();

        void clean();

        default boolean placeable(ItemStack itemStack){
            if (ItemStacks.isEmpty(itemStack)){
                return false;
            }
            var current = get();
            if (ItemStacks.isEmpty(current)){
                return true;
            }
            return current.getAmount() < current.getMaxStackSize() && current.isSimilar(itemStack);
        }
    }

    private Source source;

    @Override
    public boolean isDirty() {
        return source.isDirty();
    }

    @Override
    public ItemStack display(ItemStack legacy) {
        source.clean();
        return source.get();
    }

    @Override
    public void click(@NotNull CUIClickEvent<?> event) {
        event.setCancelled(true);
        var itemStack = source.get();
        var player = event.getPlayer();
        var cursor = event.getCursor();
        if (cursor != null && cursor.isEmpty())
            cursor = null;
//        Bukkit.getLogger().warning("Click "+event.getClickType());
        switch (event.getClickType()) {
            case DROP -> {
                // 丢弃【一个】
                if (itemStack.getAmount() > 0) {
                    var drop = itemStack.clone();
                    drop.setAmount(1);
                    itemStack.setAmount(itemStack.getAmount()-1);
                    var location = player.getLocation();
                    location.getWorld().dropItemNaturally(location, drop);
                }
            }
            case CONTROL_DROP -> {
                // 丢弃【全部】
                var location = player.getLocation();
                location.getWorld().dropItemNaturally(location, itemStack);
                itemStack = null;
            }
            case LEFT -> {
                // 补充【全部】或交换【全部】
                if(source.placeable(cursor)) {
                    var result = ItemStacks.place(itemStack, cursor);
                    itemStack = result.placed();
                    cursor = result.remaining();
                } else {
                    var tmp = cursor;
                    cursor = itemStack;
                    itemStack = tmp;
                }
            }
            case RIGHT -> {
                // 补充【一个】或交换【全部】
                if(source.placeable(cursor)) {
                    assert cursor != null;
                    var tmp = cursor.clone();
                    tmp.setAmount(1);
                    var result = ItemStacks.place(itemStack, tmp);
                    if (ItemStacks.isEmpty(result.remaining())){
                        itemStack = result.placed();
                        cursor.setAmount(cursor.getAmount()-1);
                    }
                } else {
                    var tmp = cursor;
                    cursor = itemStack;
                    itemStack = tmp;
                }
            }
            case SHIFT_LEFT, SHIFT_RIGHT -> {
                // 取出【全部】
                var first = player.getInventory().addItem(itemStack).values()
                        .stream().findFirst();
                itemStack = first.orElse(null);
            }
            case MIDDLE -> {
                // 克隆【一组】
                if (player.getGameMode() == GameMode.CREATIVE && cursor == null) {
                    cursor = itemStack.clone();
                    cursor.setAmount(cursor.getMaxStackSize());
                }
            }
            case DOUBLE_CLICK -> {
                // 收集【全部】
                var cui = CUIManager.getInstance().getViewingCUI(player);
                if(cui != null){
                    cursor = cui.getCamera(player).collect(player, cursor, true);
                }
            }
        }
        source.set(itemStack);
        event.setCursor(cursor);
    }

    @Override
    public ItemStack place(ItemStack itemStack) {
        var result = ItemStacks.place(source.get(), itemStack);
        source.set(result.placed());
        return result.remaining();
    }

    @Override
    public ItemStack collect(ItemStack itemStack) {
        var result = ItemStacks.place(itemStack, source.get());
        source.set(result.remaining());
        return result.placed();
    }

    @Override
    public Slot deepClone() {
        var storage = new Storage();
        storage.source = source;
        return storage;
    }

    public static class Builder {
        private final Storage storage = new Storage();

        Builder() {
        }

        public Builder source(Source source) {
            storage.source = source;
            return this;
        }

        public Builder source(Storage storage) {
            return source(storage.source);
        }

        public Builder source() {
            return source((ItemStack) null);
        }

        public Builder source(ItemStack initial) {
            storage.source = new Source() {
                private ItemStack itemStack = initial;
                private boolean dirty = true;

                @Override
                public ItemStack get() {
                    return itemStack;
                }

                @Override
                public void set(ItemStack itemStack) {
                    dirty = true;
                    this.itemStack = itemStack;
                }

                @Override
                public boolean isDirty() {
                    return dirty;
                }

                @Override
                public void clean() {
                    dirty = false;
                }
            };
            return this;
        }

        public Builder source(Inventory inventory, int rawSlot) {
            storage.source = new Source() {
                private boolean init = true;
                private ItemStack itemStack;
                private ItemStack expected;

                @Override
                public ItemStack get() {
                    return inventory.getItem(rawSlot);
                }

                @Override
                public void set(ItemStack itemStack) {
                    inventory.setItem(rawSlot, itemStack);
                }

                @Override
                public boolean isDirty() {
                    expected = inventory.getItem(rawSlot);
                    if (init) {
                        return true;
                    }
                    return !ItemStacks.isSame(expected, itemStack);
                }

                @Override
                public void clean() {
                    init = false;
                    if(expected == null){
                        itemStack = null;
                        return;
                    }
                    itemStack = expected.clone();
                }
            };
            return this;
        }

        public Storage build() {
            return storage;
        }
    }
}
