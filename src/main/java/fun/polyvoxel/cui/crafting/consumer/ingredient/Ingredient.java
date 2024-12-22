package fun.polyvoxel.cui.crafting.consumer.ingredient;

import fun.polyvoxel.cui.crafting.CraftingContext;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 代表了一个合成表的原料。<br>
 * Represents an ingredient of a recipe.
 */
public interface Ingredient {
	/**
	 * 匹配并消耗物品，如果匹配成功则返回克隆并消耗后的物品，否则返回null。<br>
	 * Match and consume the item, if matched, return a clone of the item after
	 * consumption, otherwise return null.
	 *
	 * @param ctx
	 *            合成上下文<br>
	 *            Crafting context
	 * @param itemStack
	 *            物品<br>
	 *            Item
	 * @return 克隆并消耗后的物品，或null<br>
	 *         A clone of the item after consumption, or null
	 */
	@Nullable
	ItemStack consume(@NotNull CraftingContext ctx, @Nullable ItemStack itemStack);
}
