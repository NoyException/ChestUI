package fun.polyvoxel.cui.crafting.producer.product;

import fun.polyvoxel.cui.crafting.CraftingContext;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 代表了一个合成表的产物。<br>
 * Represents a product of a recipe.
 */
public interface Product {
	record Result(ItemStack product, ItemStack spilled) {
	}
	/**
	 * 产生物品，如果产生成功则返回克隆并生产后的物品，否则返回null。<br>
	 * Produce the item, if successful, return a clone of the item after production,
	 * otherwise return null.
	 *
	 * @param ctx
	 *            合成上下文<br>
	 *            Crafting context
	 * @param itemStack
	 *            原先存在的物品<br>
	 *            Item that originally existed
	 * @param allowSpill
	 *            是否允许溢出<br>
	 *            Whether to allow overflow
	 * @return 生产后的物品，或null<br>
	 *         Produced item, or null
	 */
	@Nullable
	Result produce(@NotNull CraftingContext ctx, @Nullable ItemStack itemStack, boolean allowSpill);
}
