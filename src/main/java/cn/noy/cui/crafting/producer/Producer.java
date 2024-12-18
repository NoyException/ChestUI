package cn.noy.cui.crafting.producer;

import cn.noy.cui.crafting.CraftingContext;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface Producer {
	/**
	 * 尝试生产物品，如果无法生产则返回null，反之返回克隆的生产后的输出<br>
	 * Try to produce items, return null if it cannot be produced, otherwise return
	 * a cloned output after production
	 *
	 * @param ctx
	 *            合成上下文<br>
	 *            Crafting context
	 * @param output
	 *            输出<br>
	 *            Output
	 * @return 生产后的输出<br>
	 *         Output after production
	 */
	ItemStack[][] produce(@NotNull CraftingContext ctx, @NotNull ItemStack[][] output);
}
