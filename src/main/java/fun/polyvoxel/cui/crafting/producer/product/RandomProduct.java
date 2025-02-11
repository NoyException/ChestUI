package fun.polyvoxel.cui.crafting.producer.product;

import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.crafting.CraftingContext;
import fun.polyvoxel.cui.util.ItemStacks;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 根据权重随机产生一个物品。如果物品无法放入玩家背包，将缓存该物品，直到使用成功。如果传入List初始化且Random是固定的，那么将会产生相同的结果。<br>
 * Produce an item randomly based on the weight. If the item cannot be placed in
 * the player's backpack, the item will be cached until it is used successfully.
 * If the List is passed in for initialization and Random is fixed, the same
 * result will be generated.
 */
public class RandomProduct implements Product {
	private final boolean ignoreAmountLimit;
	private final Random random;
	private final TreeMap<Integer, ItemStack> products = new TreeMap<>();
	private final int sum;
	private final HashMap<Player, ItemStack> cache = new HashMap<>();

	public RandomProduct(Map<ItemStack, Integer> weights) {
		this(weights, new Random(), false);
	}

	public RandomProduct(Map<ItemStack, Integer> weights, Random random) {
		this(weights, random, false);
	}

	public RandomProduct(Map<ItemStack, Integer> weights, boolean ignoreAmountLimit) {
		this(weights, new Random(), ignoreAmountLimit);
	}

	public RandomProduct(Map<ItemStack, Integer> weights, Random random, boolean ignoreAmountLimit) {
		this.random = random;
		this.ignoreAmountLimit = ignoreAmountLimit;
		int sum = 0;
		for (var entry : weights.entrySet()) {
			sum += entry.getValue();
			products.put(sum, entry.getKey());
		}
		this.sum = sum;
	}

	public RandomProduct(List<Pair<ItemStack, Integer>> weights, Random random, boolean ignoreAmountLimit) {
		this.random = random;
		this.ignoreAmountLimit = ignoreAmountLimit;
		int sum = 0;
		for (var entry : weights) {
			products.put(sum, entry.getLeft());
			sum += entry.getRight();
		}
		this.sum = sum;
	}

	@Override
	public @Nullable Result produce(@NotNull CraftingContext context, @Nullable ItemStack itemStack,
			boolean allowSpill) {
		Player player = context.player();
		if (player != null) {
			var cached = cache.get(player);
			if (cached != null) {
				var result = ItemStacks.place(itemStack, cached, ignoreAmountLimit);
				if (result.remaining() != null) {
					if (!allowSpill) {
						return null;
					}
				}
				return new Result(result.placed(), result.remaining());
			}
		}
		// 产生随机结果
		int r = random.nextInt(sum);
		var entry = products.floorEntry(r);
		if (entry == null) {
			CUIPlugin.logger().warn("RandomProduct: No product found for random number {}", r);
			return null;
		}
		cache.put(player, entry.getValue());
		// 只有当配方使用成功时，才会清除缓存
		context.recipe().addOnApply(user -> {
			if (user == player) {
				cache.remove(player);
				return true;
			}
			return false;
		});

		var result = ItemStacks.place(itemStack, entry.getValue(), ignoreAmountLimit);
		if (result.remaining() != null) {
			if (!allowSpill) {
				return null;
			}
		}
		return new Result(result.placed(), result.remaining());
	}
}
