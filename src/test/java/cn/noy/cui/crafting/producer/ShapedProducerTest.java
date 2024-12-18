package cn.noy.cui.crafting.producer;

import cn.noy.cui.crafting.CraftingContext;
import cn.noy.cui.crafting.Recipe;
import cn.noy.cui.crafting.producer.product.ExactProduct;
import cn.noy.cui.util.ItemStackAssertions;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

public class ShapedProducerTest {
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
	 * 测试严格模式下的生产
	 */
	@Test
	public void testStrict() {
		var producer = ShapedProducer.builder().strict(true).pattern(" A", "AB")
				.set('A', new ExactProduct(Material.STONE)).set('B', new ExactProduct(Material.DIRT, 2)).build();
		// 测试全部都是空位的情况
		var output = new ItemStack[][]{{null, null}, {null, null}};
		var result1 = producer.produce(createCtx(), output);
		Assertions.assertNotNull(result1, "初始全是空位，应当能成功产出");
		ItemStackAssertions.assertEmpty(result1[0][0], "空格位置应当没有产物");
		ItemStackAssertions.assertSame(new ItemStack(Material.STONE), result1[0][1], "产物应当是石头");
		ItemStackAssertions.assertSame(new ItemStack(Material.STONE), result1[1][0], "产物应当是石头");
		ItemStackAssertions.assertSame(new ItemStack(Material.DIRT, 2), result1[1][1], "产物应当是2个泥土");
		// 测试在原有的产物上继续生产的情况
		var result2 = producer.produce(createCtx(), result1);
		Assertions.assertNotNull(result2, "继续生产，应当能成功产出");
		ItemStackAssertions.assertEmpty(result2[0][0], "空格位置应当没有产物");
		ItemStackAssertions.assertSame(new ItemStack(Material.STONE, 2), result2[0][1], "产物应当是2个石头");
		ItemStackAssertions.assertSame(new ItemStack(Material.STONE, 2), result2[1][0], "产物应当是2个石头");
		ItemStackAssertions.assertSame(new ItemStack(Material.DIRT, 4), result2[1][1], "产物应当是4个泥土");
		// 测试在一个产物（即将）到达上限时的情况
		result2[1][1].setAmount(63);
		var result3 = producer.produce(createCtx(), result2);
		Assertions.assertNull(result3, "超出上限，应当不能成功产出");
		result2[1][1].setAmount(4);
		// 测试在一个空位上有产物的情况
		result2[0][0] = new ItemStack(Material.DIRT);
		var result4 = producer.produce(createCtx(), result2);
		Assertions.assertNull(result4, "空位上有产物，应当不能成功产出");
	}

	/**
	 * 测试非严格模式下的生产
	 */
	@Test
	public void testNotStrict() {
		var producer = ShapedProducer.builder().strict(false).pattern(" A", "AB")
				.set('A', new ExactProduct(Material.STONE)).set('B', new ExactProduct(Material.DIRT, 2)).build();
		// 测试全部都是空位的情况
		var output = new ItemStack[][]{{null, null}, {null, null}};
		var result1 = producer.produce(createCtx(), output);
		Assertions.assertNotNull(result1, "初始全是空位，应当能成功产出");
		ItemStackAssertions.assertEmpty(result1[0][0], "空格位置应当没有产物");
		ItemStackAssertions.assertSame(new ItemStack(Material.STONE), result1[0][1], "产物应当是石头");
		ItemStackAssertions.assertSame(new ItemStack(Material.STONE), result1[1][0], "产物应当是石头");
		ItemStackAssertions.assertSame(new ItemStack(Material.DIRT, 2), result1[1][1], "产物应当是2个泥土");
		// 测试在一个空位上有产物的情况
		result1[0][0] = new ItemStack(Material.DIRT);
		var result2 = producer.produce(createCtx(), result1);
		Assertions.assertNotNull(result2, "空位上有产物，但不严格，应当能成功产出");
		ItemStackAssertions.assertSame(new ItemStack(Material.DIRT), result2[0][0], "空位上的产物应当保留");
		ItemStackAssertions.assertSame(new ItemStack(Material.STONE, 2), result2[0][1], "产物应当是2个石头");
		ItemStackAssertions.assertSame(new ItemStack(Material.STONE, 2), result2[1][0], "产物应当是2个石头");
		ItemStackAssertions.assertSame(new ItemStack(Material.DIRT, 4), result2[1][1], "产物应当是4个泥土");
	}
}
