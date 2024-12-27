package fun.polyvoxel.cui.crafting.consumer.ingredient;

import fun.polyvoxel.cui.crafting.CraftingContext;
import fun.polyvoxel.cui.util.ItemStacks;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 精确的原料。只有完全相同、且数量大于等于参考原料的物品才能匹配。<br>
 * Exact ingredient. Only items that are exactly the same and have an amount
 * greater than or equal to the reference ingredient can match.
 */
public class ExactIngredient implements Ingredient {
	private final @NotNull ItemStack exact;

	public ExactIngredient(ItemStack exact) {
		if (exact == null)
			exact = ItemStack.of(Material.AIR);
		this.exact = exact.clone();
	}

	public ExactIngredient(ItemStack exact, int amount) {
		if (exact == null)
			exact = ItemStack.of(Material.AIR);
		this.exact = exact.clone();
		this.exact.setAmount(amount);
	}

	public ExactIngredient(Material material) {
		this(ItemStack.of(material));
	}

	public ExactIngredient(Material material, int amount) {
		this(ItemStack.of(material), amount);
	}

	@Override
	public @Nullable ItemStack consume(@NotNull CraftingContext ctx, @Nullable ItemStack itemStack) {
		if (itemStack == null) {
			itemStack = ItemStack.of(Material.AIR);
		}
		if (!ItemStacks.isSimilar(itemStack, exact)) {
			return null;
		}
		var amount = itemStack.getAmount() - exact.getAmount();
		if (amount < 0) {
			return null;
		}
		var clone = itemStack.clone();
		clone.setAmount(amount);
		return clone;
	}
}
