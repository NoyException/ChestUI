package fun.polyvoxel.cui.crafting.consumer;

import fun.polyvoxel.cui.crafting.CraftingContext;
import fun.polyvoxel.cui.crafting.Recipe;
import fun.polyvoxel.cui.crafting.consumer.ingredient.ExactIngredient;
import fun.polyvoxel.cui.crafting.consumer.ingredient.MetaMatchedIngredient;
import fun.polyvoxel.cui.util.ItemStackAssertions;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

public class ShapelessConsumerTest {
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
	 * 原料严格对应，刚好匹配
	 */
	@Test
	public void testMatch() {
		var consumer = ShapelessConsumer.builder().strict(true).add(new ExactIngredient(Material.GOLD_NUGGET, 2))
				.add(new ExactIngredient(Material.GOLD_NUGGET)).add(new ExactIngredient(Material.GOLD_INGOT, 2))
				.build();
		// 位置刚好也在左上角
		var input1 = new ItemStack[][]{{new ItemStack(Material.GOLD_NUGGET, 2), new ItemStack(Material.GOLD_NUGGET),
				new ItemStack(Material.GOLD_INGOT, 2)}};
		var remaining = consumer.consume(createCtx(), input1);
		Assertions.assertNotNull(remaining, "原料匹配，应当能消耗成功");
		for (ItemStack[] itemStacks : remaining) {
			for (int column = 0; column < remaining[0].length; column++) {
				ItemStackAssertions.assertEmpty(itemStacks[column], "消耗后应当没有剩余");
			}
		}
		// 位置任意
		var input2 = new ItemStack[][]{{null, new ItemStack(Material.GOLD_NUGGET), null},
				{null, new ItemStack(Material.GOLD_INGOT, 2), new ItemStack(Material.GOLD_NUGGET, 2)},
				{null, null, null}};
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
		var consumer = ShapelessConsumer.builder().strict(true).add(new ExactIngredient(Material.GOLD_NUGGET, 2))
				.add(new ExactIngredient(Material.GOLD_NUGGET)).add(new ExactIngredient(Material.GOLD_INGOT, 2))
				.build();
		var input = new ItemStack[][]{{new ItemStack(Material.GOLD_NUGGET), new ItemStack(Material.GOLD_NUGGET, 3),
				new ItemStack(Material.GOLD_INGOT, 2)}};
		var remaining = consumer.consume(createCtx(), input);
		Assertions.assertNotNull(remaining, "原料匹配，应当消耗成功");
		ItemStackAssertions.assertEmpty(remaining[0][0], "消耗后应当没有剩余");
		ItemStackAssertions.assertSame(new ItemStack(Material.GOLD_NUGGET, 1), remaining[0][1], "消耗后应当剩余1个金粒");
		ItemStackAssertions.assertEmpty(remaining[0][2], "消耗后应当没有剩余");
	}

	/**
	 * 有额外的原料
	 */
	@Test
	public void testExtraIngredient() {
		// 严格时，不允许有额外的原料
		var consumer1 = ShapelessConsumer.builder().strict(true).add(new ExactIngredient(Material.GOLD_NUGGET, 2))
				.add(new ExactIngredient(Material.GOLD_NUGGET)).add(new ExactIngredient(Material.GOLD_INGOT, 2))
				.build();
		var input = new ItemStack[][]{{new ItemStack(Material.GOLD_NUGGET), new ItemStack(Material.GOLD_NUGGET, 2)},
				{new ItemStack(Material.GOLD_INGOT, 2), new ItemStack(Material.GOLD_NUGGET)}};
		var remaining = consumer1.consume(createCtx(), input);
		Assertions.assertNull(remaining, "原料多余，应当消耗失败");
		// 不严格时，允许有额外的原料
		var consumer2 = ShapelessConsumer.builder().strict(false).add(new ExactIngredient(Material.GOLD_NUGGET, 2))
				.add(new ExactIngredient(Material.GOLD_NUGGET)).add(new ExactIngredient(Material.GOLD_INGOT, 2))
				.build();
		remaining = consumer2.consume(createCtx(), input);
		Assertions.assertNotNull(remaining, "原料多余，但不严格，应当消耗成功");
		ItemStackAssertions.assertEmpty(remaining[0][0], "消耗后应当没有剩余");
		ItemStackAssertions.assertSame(new ItemStack(Material.GOLD_NUGGET, 1), remaining[1][1], "多余的原料不会被消耗");
	}

	/**
	 * 原料匹配区间交叉
	 */
	@Test
	public void testCrossMatch() {
		var consumer = ShapelessConsumer.builder().strict(true).add(new MetaMatchedIngredient(ItemMeta::hasDisplayName))
				.add(new MetaMatchedIngredient(itemMeta -> itemMeta.hasEnchant(Enchantment.UNBREAKING), 2))
				.add(new MetaMatchedIngredient(ItemMeta::isUnbreakable, 3)).build();
		var itemStack1 = new ItemStack(Material.WOODEN_PICKAXE, 4);
		itemStack1.editMeta(itemMeta -> {
			itemMeta.displayName(Component.text("1"));
			itemMeta.setUnbreakable(true);
		});
		var itemStack2 = new ItemStack(Material.STONE_PICKAXE, 4);
		itemStack2.editMeta(itemMeta -> {
			itemMeta.displayName(Component.text("2"));
			itemMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
			itemMeta.setUnbreakable(true);
		});
		var itemStack3 = new ItemStack(Material.IRON_PICKAXE, 4);
		itemStack3.editMeta(itemMeta -> {
			itemMeta.displayName(Component.text("3"));
		});
		var input = new ItemStack[][]{{itemStack1, null}, {itemStack2, itemStack3}};
		var remaining = consumer.consume(createCtx(), input);
		Assertions.assertNotNull(remaining, "原料匹配区间交叉，但刚好能匹配，应当消耗成功");
		Assertions.assertEquals(2, remaining[1][0].getAmount(), "itemStack2应当匹配到2号原料");
		Assertions.assertEquals(3, remaining[1][1].getAmount(), "itemStack3应当匹配到1号原料");
		Assertions.assertEquals(1, remaining[0][0].getAmount(), "itemStack1应当匹配到3号原料");
	}

	/**
	 * 缺少原料
	 */
	@Test
	public void testMissingIngredient() {
		var consumer = ShapelessConsumer.builder().strict(false).add(new ExactIngredient(Material.GOLD_NUGGET, 2))
				.add(new ExactIngredient(Material.GOLD_NUGGET)).add(new ExactIngredient(Material.GOLD_INGOT, 2))
				.build();
		var input = new ItemStack[][]{{new ItemStack(Material.GOLD_NUGGET, 3), new ItemStack(Material.GOLD_INGOT)}};
		var remaining = consumer.consume(createCtx(), input);
		Assertions.assertNull(remaining, "原料缺少，应当消耗失败");
	}

	/**
	 * 原料不符
	 */
	@Test
	public void testWrongIngredient() {
		var consumer = ShapelessConsumer.builder().strict(true).add(new ExactIngredient(Material.GOLD_NUGGET, 2))
				.add(new ExactIngredient(Material.GOLD_NUGGET)).add(new ExactIngredient(Material.GOLD_INGOT, 2))
				.build();
		var input = new ItemStack[][]{{new ItemStack(Material.GOLD_NUGGET), new ItemStack(Material.GOLD_NUGGET),
				new ItemStack(Material.GOLD_INGOT, 2)}};
		var remaining = consumer.consume(createCtx(), input);
		Assertions.assertNull(remaining, "原料不符，应当消耗失败");
	}
}
