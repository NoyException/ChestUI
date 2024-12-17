package cn.noy.cui.crafting.consumer;

import cn.noy.cui.crafting.CraftingContext;
import cn.noy.cui.crafting.consumer.ingredient.Ingredient;
import cn.noy.cui.util.ItemStacks;
import cn.noy.cui.util.Position;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 按照无序的方式消耗物品。<br>
 * Consume items in an unordered way.
 */
public class ShapelessConsumer implements Consumer {
	private boolean strict = true;
	private List<Ingredient> ingredients;

	@Override
	public ItemStack[][] consume(@NotNull CraftingContext ctx, @NotNull ItemStack[][] input) {
		// 先获得有哪些输入
		var in = new HashMap<Position, ItemStack>();
		for (int row = 0; row < input.length; row++) {
			for (int column = 0; column < input[0].length; column++) {
				if (ItemStacks.isEmpty(input[row][column]))
					continue;
				in.put(new Position(row, column), input[row][column]);
			}
		}
		// 如果输入的物品数量小于原料数量，直接返回null
		if (ingredients.size() > in.size()) {
			return null;
		}
		// 如果输入的物品数量不等于原料数量，且strict为true，直接返回null
		if (strict && ingredients.size() != in.size()) {
			return null;
		}
		// 遍历原料，尝试消耗输入的物品，获取每个物品都能适配哪些原料和消耗后的物品
		var matched = new TreeMap<Position, HashMap<Ingredient, ItemStack>>();
		for (Ingredient ingredient : ingredients) {
			in.forEach((position, itemStack) -> {
				var after = ingredient.consume(ctx, itemStack);
				if (after == null)
					return;
				matched.computeIfAbsent(position, k -> new HashMap<>()).put(ingredient, after);
			});
		}
		// 如果能够被匹配的物品数量小于原料数量，直接返回null
		if (ingredients.size() > matched.size()) {
			return null;
		}
		// 不断从matched中取走匹配原料最少的组合，直到所有原料都被匹配
		var unsatisfied = new HashSet<>(ingredients);
		var finalMatch = new HashMap<Position, ItemStack>();
		while (!unsatisfied.isEmpty()) {
			// 如果没有能够匹配的物品，说明匹配失败
			if (matched.isEmpty()) {
				return null;
			}
			// 找到匹配原料最少的位置
			var min = Integer.MAX_VALUE;
			Position minPosition = null;
			for (var entry : matched.entrySet()) {
				var position = entry.getKey();
				var candidates = entry.getValue();
				if (candidates.size() < min) {
					min = candidates.size();
					minPosition = position;
				}
			}
			// 记录并移除
			var candidates = matched.remove(minPosition);
			if (candidates.isEmpty()) {
				continue;
			}
			var candidate = candidates.entrySet().iterator().next();
			Ingredient ingredient = candidate.getKey();
			finalMatch.put(minPosition, candidate.getValue());
			unsatisfied.remove(ingredient);
			matched.forEach((position, map) -> map.remove(ingredient));
		}
		// 生成输出
		var output = new ItemStack[input.length][input[0].length];
		for (int row = 0; row < input.length; row++) {
			for (int column = 0; column < input[0].length; column++) {
				output[row][column] = finalMatch.getOrDefault(new Position(row, column), input[row][column]);
			}
		}
		return output;
	}

	public static Builder builder() {
		return new ShapelessConsumer.Builder();
	}

	public static class Builder {
		private final ShapelessConsumer consumer = new ShapelessConsumer();

		Builder() {
			consumer.ingredients = new ArrayList<>(16);
		}

		/**
		 * 原料外的物品是否会导致匹配失败。<br>
		 * Whether items outside the ingredients will cause the match to fail.
		 *
		 * @param strict
		 *            是否严格匹配。默认为true。<br>
		 *            Whether to match strictly. The default is true.
		 * @return this
		 */
		public Builder strict(boolean strict) {
			consumer.strict = strict;
			return this;
		}

		public Builder add(Ingredient ingredient) {
			consumer.ingredients.add(ingredient);
			return this;
		}

		public ShapelessConsumer build() {
			return consumer;
		}
	}
}
