package cn.noy.cui.crafting.producer.product;

import cn.noy.cui.crafting.CraftingContext;
import cn.noy.cui.util.ItemStacks;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExactProduct implements Product {
	private final boolean ignoreAmountLimit;
	private final @NotNull ItemStack exact;

	public ExactProduct(@Nullable ItemStack exact, boolean ignoreAmountLimit) {
		if (exact == null)
			exact = new ItemStack(Material.AIR);
		this.exact = exact.clone();
		this.ignoreAmountLimit = ignoreAmountLimit;
	}

	public ExactProduct(@Nullable ItemStack exact) {
		this(exact, false);
	}

	public ExactProduct(@Nullable ItemStack exact, int amount, boolean ignoreAmountLimit) {
		if (exact == null)
			exact = new ItemStack(Material.AIR);
		this.exact = exact.clone();
		this.exact.setAmount(amount);
		this.ignoreAmountLimit = ignoreAmountLimit;
	}

	public ExactProduct(@Nullable ItemStack exact, int amount) {
		this(exact, amount, false);
	}

	public ExactProduct(Material material) {
		this(new ItemStack(material));
	}

	public ExactProduct(Material material, int amount) {
		this(new ItemStack(material), amount);
	}

	public ExactProduct(Material material, boolean ignoreAmountLimit) {
		this(new ItemStack(material), ignoreAmountLimit);
	}

	public ExactProduct(Material material, int amount, boolean ignoreAmountLimit) {
		this(new ItemStack(material), amount, ignoreAmountLimit);
	}

	@Override
	public @Nullable Result produce(@NotNull CraftingContext ctx, @Nullable ItemStack itemStack, boolean allowSpill) {
		var result = ItemStacks.place(itemStack, exact, ignoreAmountLimit);
		if (!result.success()) {
			return null;
		}
		if (result.remaining() != null) {
			if (!allowSpill) {
				return null;
			}
		}
		return new Result(result.placed(), result.remaining());
	}
}
