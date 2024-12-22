package fun.polyvoxel.cui.crafting.producer;

import fun.polyvoxel.cui.crafting.CraftingContext;
import fun.polyvoxel.cui.crafting.producer.product.Product;
import fun.polyvoxel.cui.util.Array2Ds;
import fun.polyvoxel.cui.util.ItemStacks;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 按照一定的形状生产物品，不允许平移（只能在左上角生产）。<br>
 * Produce items according to a certain shape, no translation allowed (can only
 * be produced in the upper left corner).
 */
public class ShapedProducer implements Producer {
	private boolean strict = true;
	private Pattern pattern;
	private final Map<Character, Product> byChar = new HashMap<>();

	@Override
	public ItemStack[][] produce(@NotNull CraftingContext ctx, @NotNull ItemStack[][] output) {
		if (output.length < pattern.height || output[0].length < pattern.width) {
			return null;
		}
		var outputAfter = new ItemStack[output.length][output[0].length];
		// 与consumer不同，producer只在左上角生产物品
		for (int row = 0; row < pattern.height; row++) {
			for (int column = 0; column < pattern.width; column++) {
				var c = pattern.pattern[row][column];
				var product = byChar.get(c);
				var itemStack = output[row][column];
				if (product == null) {
					if (c != ' ') {
						throw new IllegalArgumentException("No product for character " + c);
					}
					if (!ItemStacks.isEmpty(itemStack)) {
						if (strict) {
							return null;
						} else {
							outputAfter[row][column] = itemStack.clone();
						}
					}
					continue;
				}
				var afterProduce = product.produce(ctx, itemStack, false);
				if (afterProduce == null) {
					return null;
				}
				outputAfter[row][column] = afterProduce.product();
			}
		}
		return outputAfter;
	}

	public static Builder builder() {
		return new ShapedProducer.Builder();
	}

	public static class Builder {
		private final ShapedProducer producer = new ShapedProducer();

		Builder() {
		}

		/**
		 * 是否允许空格位置已经存在产品<br>
		 * Whether to allow products to exist in space positions
		 * 
		 * @param strict
		 *            是否严格匹配。默认为true<br>
		 *            Whether to match strictly. The default is true
		 * @return this
		 */
		public Builder strict(boolean strict) {
			producer.strict = strict;
			return this;
		}

		public Builder pattern(String... pattern) {
			producer.pattern = new Pattern(pattern);
			return this;
		}

		public Builder set(char c, Product product) {
			if (c == ' ') {
				throw new IllegalArgumentException("Cannot map space character");
			}
			producer.byChar.put(c, product);
			return this;
		}

		public ShapedProducer build() {
			return producer;
		}
	}

	private static class Pattern {
		private final char[][] pattern;
		private final int width;
		private final int height;

		public Pattern(String[] lines) {
			var raw = new Character[lines.length][];
			for (int i = 0; i < lines.length; i++) {
				raw[i] = lines[i].chars().mapToObj(c -> (char) c).toArray(Character[]::new);
			}
			var trim = Array2Ds.trim(raw, c -> c == ' ');
			var leftTop = trim.getLeft();
			var rightBottom = trim.getRight();
			width = rightBottom.column() - leftTop.column() + 1;
			height = rightBottom.row() - leftTop.row() + 1;
			pattern = new char[height][width];
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					pattern[i][j] = raw[i + leftTop.row()][j + leftTop.column()];
				}
			}
		}
	}
}
