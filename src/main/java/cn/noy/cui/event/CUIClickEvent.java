package cn.noy.cui.event;

import cn.noy.cui.ui.CUIHandler;
import cn.noy.cui.ui.ChestUI;
import cn.noy.cui.util.Position;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;

public class CUIClickEvent<T extends CUIHandler> extends CUIEvent<T> implements Cancellable {
    private final Player player;
    private final ClickType clickType;
    private final InventoryAction action;
    // 相对于ChestUI的行列
    private final Position position;
    private ItemStack cursor;
    private boolean cancel;

    public CUIClickEvent(
            ChestUI<T> chestUI, Player player, ClickType clickType, InventoryAction action,
            Position position, ItemStack cursor
    ) {
        super(chestUI);
        this.player = player;
        this.clickType = clickType;
        this.action = action;
        this.position = position;
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

    public Position getPosition() {
        return position;
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
