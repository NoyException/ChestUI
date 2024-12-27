package fun.polyvoxel.cui.crafting.consumer.ingredient;

import fun.polyvoxel.cui.crafting.CraftingContext;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * 匹配物品的元数据。<br>
 * Match the metadata of an item.
 */
public class MetaMatchedIngredient implements Ingredient {
	private final @NotNull Predicate<ItemMeta> predicate;
	private final int amount;

	public MetaMatchedIngredient(@NotNull Predicate<ItemMeta> predicate) {
		this(predicate, 1);
	}

	public MetaMatchedIngredient(@NotNull Predicate<ItemMeta> predicate, int amount) {
		this.predicate = predicate;
		this.amount = amount;
	}

	@Override
	public @Nullable ItemStack consume(@NotNull CraftingContext ctx, @Nullable ItemStack itemStack) {
		if (itemStack == null) {
			itemStack = ItemStack.of(Material.AIR);
		}
		if (!predicate.test(itemStack.getItemMeta())) {
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
