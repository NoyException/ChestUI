package cn.noy.cui.slot;

import cn.noy.cui.event.CUIClickEvent;
import org.bukkit.inventory.ItemStack;

public abstract class Slot {
    /**
     * 检查槽位是否脏<br>
     * Check if the slot is dirty
     *
     * @return 如果槽位脏，返回true，否则返回false<br>
     * If the slot is dirty, return true, otherwise return false
     */
    public abstract boolean isDirty();

    /**
     * 展示槽位中的物品<br>
     * Display the item in the slot
     *
     * @param legacy 原先槽位中的物品<br>
     *               The original item in the slot
     * @return 展示在槽位中的物品<br>
     * The item to display in the slot
     */
    public abstract ItemStack display(ItemStack legacy);

    /**
     * 获取槽位中的物品<br>
     * Get the item in the slot
     *
     * @return 槽位中的物品<br>
     * The item in the slot
     */
    public ItemStack get() {
        return display(null);
    }

    /**
     * 处理点击事件。如果事件没有被取消，它将会被传递到下一层<br>
     * Handle the click event. If the event is not cancelled, it will be passed to the next layer
     *
     * @param event 点击事件<br>
     *              The click event
     */
    public abstract void click(CUIClickEvent<?> event);

    /**
     * 尝试将物品放入槽位<br>
     * Try to place the item in the slot
     *
     * @param itemStack 要放入的物品<br>
     *                  The item to place
     * @return 剩余的物品，如果没有剩余则返回null<br>
     * The remaining item, if there is no remaining item, return null
     */
    public abstract ItemStack place(ItemStack itemStack);

    /**
     * 尝试收集槽位中的物品<br>
     * Try to collect the item in the slot
     *
     * @param itemStack 要收集的物品<br>
     *                  The item to collect
     * @return 收集到的物品，如果没有收集到则返回null<br>
     * The collected item, if there is no collected item, return null
     */
    public abstract ItemStack collect(ItemStack itemStack);

    /**
     * 深度克隆槽位<br>
     * Deep clone the slot
     *
     * @return 克隆的槽位<br>
     * The cloned slot
     */
    public abstract Slot deepClone();
}
