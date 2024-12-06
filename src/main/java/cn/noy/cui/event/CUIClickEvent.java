package cn.noy.cui.event;

import cn.noy.cui.ui.CUIHandler;
import cn.noy.cui.ui.ChestUI;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;

public class CUIClickEvent<T extends CUIHandler> extends CUIEvent<T> implements Cancellable {
    private final Player player;
    private final ClickType clickType;
    private final InventoryAction action;
    private final int row, column;
    private ItemStack cursor;
    private boolean cancel;

    public CUIClickEvent(
            ChestUI<T> chestUI, Player player, ClickType clickType, InventoryAction action,
            int row, int column, ItemStack cursor
    ) {
        super(chestUI);
        this.player = player;
        this.clickType = clickType;
        this.action = action;
        this.row = row;
        this.column = column;
        this.cursor = cursor;
    }

    public Player getPlayer() {
        return player;
    }

    public ClickType getClickType() {
        return clickType;
    }

    public InventoryAction getAction() {
        return action;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public ItemStack getCursor() {
        return cursor;
    }

    public void setCursor(ItemStack cursor) {
        this.cursor = cursor;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }
}
