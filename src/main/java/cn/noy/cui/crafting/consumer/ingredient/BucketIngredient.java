package cn.noy.cui.crafting.consumer.ingredient;

import cn.noy.cui.crafting.CraftingContext;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BucketIngredient implements Ingredient {
	private final Material material;

	public BucketIngredient(@NotNull Material material) {
		if (material == Material.BUCKET) {
			throw new IllegalArgumentException("Material cannot be a bucket");
		}
		if (!material.name().contains("BUCKET")) {
			throw new IllegalArgumentException("Material must be a bucket type");
		}
		this.material = material;
	}

	@Override
	public @Nullable ItemStack consume(@NotNull CraftingContext ctx, @Nullable ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() != material || itemStack.getAmount() > 1) {
			return null;
		}
		return new ItemStack(Material.BUCKET);
	}
}
