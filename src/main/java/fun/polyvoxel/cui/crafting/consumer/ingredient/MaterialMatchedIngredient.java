package fun.polyvoxel.cui.crafting.consumer.ingredient;

import fun.polyvoxel.cui.crafting.CraftingContext;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * 匹配物品的元数据。<br>
 * Match the metadata of an item.
 */
public class MaterialMatchedIngredient implements Ingredient {
	private final @NotNull Predicate<Material> predicate;
	private final int amount;

	public MaterialMatchedIngredient(@NotNull Predicate<Material> predicate) {
		this(predicate, 1);
	}

	public MaterialMatchedIngredient(@NotNull Predicate<Material> predicate, int amount) {
		this.predicate = predicate;
		this.amount = amount;
	}

	@Override
	public @Nullable ItemStack consume(@NotNull CraftingContext ctx, @Nullable ItemStack itemStack) {
		if (itemStack == null) {
			itemStack = ItemStack.of(Material.AIR);
		}
		if (!predicate.test(itemStack.getType())) {
			return null;
		}
		var amount = itemStack.getAmount() - this.amount;
		if (amount < 0) {
			return null;
		}
		var clone = itemStack.clone();
		clone.setAmount(amount);
		return clone;
	}
}
