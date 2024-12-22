package fun.polyvoxel.cui.crafting.producer;

import fun.polyvoxel.cui.crafting.CraftingContext;
import fun.polyvoxel.cui.crafting.producer.product.Product;
import fun.polyvoxel.cui.util.ItemStacks;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 按照顺序的方式生产物品
 */
public class ShapelessProducer implements Producer {
	private boolean strict;
	private List<Product> products;

	@Override
	public ItemStack[][] produce(@NotNull CraftingContext ctx, @NotNull ItemStack[][] output) {
		// 先将产品区的物品复制一份
		var outputAfter = new ItemStack[output.length][output[0].length];
		for (int row = 0; row < output.length; row++) {
			for (int column = 0; column < output[0].length; column++) {
				var itemStack = output[row][column];
				if (strict && !ItemStacks.isEmpty(itemStack)) {
					return null;
				}
				outputAfter[row][column] = itemStack == null ? null : itemStack.clone();
			}
		}
		// 遍历所有产品，尝试生产
		for (Product product : products) {
			ItemStack spilled = null;
			var flag = true;
			SEARCH : for (int row = 0; row < output.length; row++) {
				for (int column = 0; column < output[0].length; column++) {
					var itemStack = outputAfter[row][column];
					var afterProduce = product.produce(ctx, itemStack, true);
					if (afterProduce == null) {
						continue;
					}
					spilled = afterProduce.spilled();
					outputAfter[row][column] = afterProduce.product();
					flag = false;
					break SEARCH;
				}
			}
			// 检查是否有产品生产失败
			if (flag) {
				return null;
			}
			// 尝试将溢出的物品放入产品区
			SEARCH : for (int row = 0; row < output.length; row++) {
				for (int column = 0; column < output[0].length; column++) {
					if (ItemStacks.isEmpty(spilled)) {
						break SEARCH;
					}
					var itemStack = outputAfter[row][column];
					var result = ItemStacks.place(itemStack, spilled, false);
					outputAfter[row][column] = result.placed();
					spilled = result.remaining();
				}
			}
			// 如果还有溢出的物品，说明无法放入产品区
			if (!ItemStacks.isEmpty(spilled)) {
				return null;
			}
		}
		return outputAfter;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private boolean strict;
		private final List<Product> products = new LinkedList<>();

		/**
		 * 是否允许已经存在产品。<br>
		 * Whether to allow existing products.
		 * 
		 * @param strict
		 *            是否严格匹配。默认为false<br>
		 *            Whether to match strictly. The default is false
		 * @return this
		 */
		public Builder strict(boolean strict) {
			this.strict = strict;
			return this;
		}

		/**
		 * 添加一个产品。<br>
		 * Add a product.
		 *
		 * @param product
		 *            产品<br>
		 *            The product
		 * @return this
		 */
		public Builder add(Product product) {
			products.add(product);
			return this;
		}

		public ShapelessProducer build() {
			ShapelessProducer producer = new ShapelessProducer();
			producer.strict = strict;
			producer.products = new ArrayList<>(products);
			return producer;
		}
	}
}
