package cn.noy.cui.crafting.consumer;

import cn.noy.cui.crafting.CraftingContext;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface Consumer {
	/**
	 * 尝试从输入中消耗材料，如果无法消耗则返回null，反之返回克隆的消耗后的输入<br>
	 * Try to consume materials from the input, return null if it cannot be
	 * consumed, otherwise return a cloned input after consumption
	 *
	 * @param ctx
	 *            合成上下文<br>
	 *            Crafting context
	 * @param input
	 *            输入<br>
	 *            Input
	 * @return 消耗后的输入<br>
	 *         Input after consumption
	 */
	ItemStack[][] consume(@NotNull CraftingContext ctx, @NotNull ItemStack[][] input);
}
