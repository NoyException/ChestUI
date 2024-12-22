package fun.polyvoxel.cui.crafting.consumer;

import fun.polyvoxel.cui.crafting.CraftingContext;
import fun.polyvoxel.cui.crafting.Recipe;
import fun.polyvoxel.cui.crafting.consumer.ingredient.ExactIngredient;
import fun.polyvoxel.cui.util.ItemStackAssertions;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

public class ShapedConsumerTest {
	private static PlayerMock player;
	private static CraftingContext createCtx() {
		return new CraftingContext(Recipe.builder().build(), null, player);
	}

	@BeforeAll
	public static void setup() {
		var server = MockBukkit.mock();
		player = server.addPlayer();
	}

	@AfterAll
	public static void teardown() {
		MockBukkit.unmock();
	}

	/**
	 * 刚好匹配
	 */
	@Test
	public void testMatch() {
		var consumer = ShapedConsumer.builder().pattern(" A ", "ABA", " A ").strict(true)
				.set('A', new ExactIngredient(Material.GOLD_NUGGET))
				.set('B', new ExactIngredient(Material.GOLD_INGOT, 2)).build();
		// 位置刚好也在左上角
		var input1 = new ItemStack[][]{
				{null, new ItemStack(Material.GOLD_NUGGET), null}, {new ItemStack(Material.GOLD_NUGGET),
						new ItemStack(Material.GOLD_INGOT, 2), new ItemStack(Material.GOLD_NUGGET)},
				{null, new ItemStack(Material.GOLD_NUGGET), null}};
		var remaining = consumer.consume(createCtx(), input1);
		Assertions.assertNotNull(remaining, "原料匹配，应当能消耗成功");
		for (ItemStack[] itemStacks : remaining) {
			for (int column = 0; column < remaining[0].length; column++) {
				ItemStackAssertions.assertEmpty(itemStacks[column], "消耗后应当没有剩余");
			}
		}
		// 位置不在左上角（大小拓宽到5*5）
		var input2 = new ItemStack[][]{{null, null, null, null, null},
				{null, null, new ItemStack(Material.GOLD_NUGGET), null, null},
				{null, new ItemStack(Material.GOLD_NUGGET), new ItemStack(Material.GOLD_INGOT, 2),
						new ItemStack(Material.GOLD_NUGGET), null},
				{null, null, new ItemStack(Material.GOLD_NUGGET), null, null}, {null, null, null, null, null}};
		remaining = consumer.consume(createCtx(), input2);
		Assertions.assertNotNull(remaining, "原料匹配，应当能消耗成功");
		for (ItemStack[] itemStacks : remaining) {
			for (int column = 0; column < remaining[0].length; column++) {
				ItemStackAssertions.assertEmpty(itemStacks[column], "消耗后应当没有剩余");
			}
		}
	}

	/**
	 * 数量有盈余
	 */
	@Test
	public void testSurplusIngredient() {
		var consumer = ShapedConsumer.builder().strict(true).pattern(" A ", "ABA", " A ")
				.set('A', new ExactIngredient(Material.GOLD_NUGGET))
				.set('B', new ExactIngredient(Material.GOLD_INGOT, 2)).build();
		// 位置刚好也在左上角
		var input1 = new ItemStack[][]{
				{null, new ItemStack(Material.GOLD_NUGGET), null}, {new ItemStack(Material.GOLD_NUGGET),
						new ItemStack(Material.GOLD_INGOT, 3), new ItemStack(Material.GOLD_NUGGET)},
				{null, new ItemStack(Material.GOLD_NUGGET), null}};
		var remaining = consumer.consume(createCtx(), input1);
		Assertions.assertNotNull(remaining, "原料匹配，应当能消耗成功");
		ItemStackAssertions.assertSame(new ItemStack(Material.GOLD_INGOT), remaining[1][1], "消耗后应当还有剩余");
		ItemStackAssertions.assertEmpty(remaining[0][1], "其他位置应当没有剩余");
		// 位置不在左上角（大小拓宽到5*5）
		var input2 = new ItemStack[][]{{null, null, null, null, null},
				{null, null, new ItemStack(Material.GOLD_NUGGET), null, null},
				{null, new ItemStack(Material.GOLD_NUGGET), new ItemStack(Material.GOLD_INGOT, 3),
						new ItemStack(Material.GOLD_NUGGET), null},
				{null, null, new ItemStack(Material.GOLD_NUGGET), null, null}, {null, null, null, null, null}};
		remaining = consumer.consume(createCtx(), input2);
		Assertions.assertNotNull(remaining, "原料匹配，应当能消耗成功");
		ItemStackAssertions.assertSame(new ItemStack(Material.GOLD_INGOT), remaining[2][2], "消耗后应当还有剩余");
		ItemStackAssertions.assertEmpty(remaining[1][2], "其他位置应当没有剩余");
	}

	/**
	 * 有额外的原料（空格上有原料）
	 */
	@Test
	public void testExtraIngredient() {
		// 严格的情况下，空格位置有原料，应当消耗失败
		var consumer1 = ShapedConsumer.builder().strict(true).pattern(" A ", "ABA", " A ")
				.set('A', new ExactIngredient(Material.GOLD_NUGGET))
				.set('B', new ExactIngredient(Material.GOLD_INGOT, 2)).build();
		var input = new ItemStack[][]{{null, new ItemStack(Material.GOLD_NUGGET), null},
				{new ItemStack(Material.GOLD_NUGGET), new ItemStack(Material.GOLD_INGOT, 2),
						new ItemStack(Material.GOLD_NUGGET)},
				{null, new ItemStack(Material.GOLD_NUGGET), new ItemStack(Material.GOLD_NUGGET)}};
		var remaining = consumer1.consume(createCtx(), input);
		Assertions.assertNull(remaining, "空格位置有原料，应当消耗失败");
		// 非严格的情况下，空格位置有原料，应当消耗成功
		var consumer2 = ShapedConsumer.builder().strict(false).pattern(" A ", "ABA", " A ")
				.set('A', new ExactIngredient(Material.GOLD_NUGGET))
				.set('B', new ExactIngredient(Material.GOLD_INGOT, 2)).build();
		remaining = consumer2.consume(createCtx(), input);
		Assertions.assertNotNull(remaining, "空格位置有原料，但不严格，应当消耗成功");
		ItemStackAssertions.assertEmpty(remaining[0][1], "消耗后应当没有剩余");
		ItemStackAssertions.assertSame(new ItemStack(Material.GOLD_NUGGET), remaining[2][2], "空格位置的原料不应被消耗");
	}

	/**
	 * 缺少原料
	 */
	@Test
	public void testMissingIngredient() {
		var consumer = ShapedConsumer.builder().strict(true).pattern(" A ", "ABA", " A ")
				.set('A', new ExactIngredient(Material.GOLD_NUGGET))
				.set('B', new ExactIngredient(Material.GOLD_INGOT, 2)).build();
		var input = new ItemStack[][]{{null, null, null}, {new ItemStack(Material.GOLD_NUGGET),
				new ItemStack(Material.GOLD_INGOT, 1), new ItemStack(Material.GOLD_NUGGET)},
				{null, new ItemStack(Material.GOLD_NUGGET), null}};
		var remaining = consumer.consume(createCtx(), input);
		Assertions.assertNull(remaining, "缺少原料，应当消耗失败");
	}

	/**
	 * 原料不符
	 */
	@Test
	public void testWrongIngredient() {
		var consumer = ShapedConsumer.builder().strict(true).pattern(" A ", "ABA", " A ")
				.set('A', new ExactIngredient(Material.GOLD_NUGGET))
				.set('B', new ExactIngredient(Material.GOLD_INGOT, 2)).build();
		// 数量不符
		var input1 = new ItemStack[][]{
				{null, new ItemStack(Material.GOLD_NUGGET), null}, {new ItemStack(Material.GOLD_NUGGET),
						new ItemStack(Material.GOLD_INGOT, 1), new ItemStack(Material.GOLD_NUGGET)},
				{null, new ItemStack(Material.GOLD_NUGGET), null}};
		var remaining = consumer.consume(createCtx(), input1);
		Assertions.assertNull(remaining, "原料不符，应当消耗失败");
		// 类型不符
		var input2 = new ItemStack[][]{
				{null, new ItemStack(Material.GOLD_NUGGET), null}, {new ItemStack(Material.GOLD_NUGGET),
						new ItemStack(Material.GOLD_BLOCK, 2), new ItemStack(Material.GOLD_NUGGET)},
				{null, new ItemStack(Material.GOLD_NUGGET), null}};
		remaining = consumer.consume(createCtx(), input2);
		Assertions.assertNull(remaining, "原料不符，应当消耗失败");
	}
}
