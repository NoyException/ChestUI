package fun.polyvoxel.cui.crafting.producer;

import fun.polyvoxel.cui.crafting.CraftingContext;
import fun.polyvoxel.cui.crafting.Recipe;
import fun.polyvoxel.cui.crafting.producer.product.ExactProduct;
import fun.polyvoxel.cui.util.ItemStacks;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

public class ShapelessProducerTest {
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
		var producer = ShapelessProducer.builder().strict(true).add(new ExactProduct(Material.STONE))
				.add(new ExactProduct(Material.DIRT, 2)).build();
		// 测试全部都是空位的情况
		var output1 = new ItemStack[][]{{null, null}, {null, null}};
		var result1 = producer.produce(createCtx(), output1);
		Assertions.assertNotNull(result1, "初始全是空位，应当能成功产出");
		Assertions.assertTrue(ItemStacks.isSame(ItemStack.of(Material.STONE), result1[0][0]), "产物应当是石头");
		Assertions.assertTrue(ItemStacks.isSame(ItemStack.of(Material.DIRT, 2), result1[0][1]), "产物应当是2个泥土");
		Assertions.assertTrue(ItemStacks.isEmpty(result1[1][0]), "应当没有产物");
		Assertions.assertTrue(ItemStacks.isEmpty(result1[1][1]), "应当没有产物");
		// 测试在空位有物品的情况
		var output2 = new ItemStack[][]{{null, null}, {null, ItemStack.of(Material.STONE)}};
		var result2 = producer.produce(createCtx(), output2);
		Assertions.assertNull(result2, "严格模式下，应当不能继续生产");
		// 测试产出位置不够的情况
		var output3 = new ItemStack[][]{{null}};
		var result3 = producer.produce(createCtx(), output3);
		Assertions.assertNull(result3, "产出位置不够，应当不能成功产出");
	}

	/**
	 * 测试非严格模式下的生产
	 */
	@Test
	public void testNotStrict() {
		var producer = ShapelessProducer.builder().strict(false).add(new ExactProduct(Material.STONE))
				.add(new ExactProduct(Material.DIRT, 2)).build();
		// 测试全部都是空位的情况
		var output1 = new ItemStack[][]{{null, null}, {null, null}};
		var result1 = producer.produce(createCtx(), output1);
		Assertions.assertNotNull(result1, "初始全是空位，应当能成功产出");
		Assertions.assertTrue(ItemStacks.isSame(ItemStack.of(Material.STONE), result1[0][0]), "产物应当是石头");
		Assertions.assertTrue(ItemStacks.isSame(ItemStack.of(Material.DIRT, 2), result1[0][1]), "产物应当是2个泥土");
		Assertions.assertTrue(ItemStacks.isEmpty(result1[1][0]), "应当没有产物");
		Assertions.assertTrue(ItemStacks.isEmpty(result1[1][1]), "应当没有产物");
		// 测试在原有产物上继续生产的情况
		var result2 = producer.produce(createCtx(), result1);
		Assertions.assertNotNull(result2, "继续生产，应当能成功产出");
		Assertions.assertTrue(ItemStacks.isSame(ItemStack.of(Material.STONE, 2), result2[0][0]), "产物应当是2个石头");
		Assertions.assertTrue(ItemStacks.isSame(ItemStack.of(Material.DIRT, 4), result2[0][1]), "产物应当是4个泥土");
		Assertions.assertTrue(ItemStacks.isEmpty(result2[1][0]), "应当没有产物");
		Assertions.assertTrue(ItemStacks.isEmpty(result2[1][1]), "应当没有产物");
		// 测试在原有产物（即将）到达上限时的情况
		result2[0][1].setAmount(63);
		var result3 = producer.produce(createCtx(), result2);
		Assertions.assertNotNull(result3, "超出上限，但还有空位，应当能成功产出");
		Assertions.assertTrue(ItemStacks.isSame(ItemStack.of(Material.STONE, 3), result3[0][0]), "产物应当是3个石头");
		Assertions.assertTrue(ItemStacks.isSame(ItemStack.of(Material.DIRT, 64), result3[0][1]), "产物应当是64个泥土");
		Assertions.assertTrue(ItemStacks.isSame(ItemStack.of(Material.DIRT, 1), result3[1][0]), "产物应当是1个泥土");
		Assertions.assertTrue(ItemStacks.isEmpty(result3[1][1]), "应当没有产物");
		// 测试再溢出时位置不够的情况
		result3[0][0].setAmount(64);
		result3[1][1] = ItemStack.of(Material.DIRT);
		var result4 = producer.produce(createCtx(), result3);
		Assertions.assertNull(result4, "溢出，但位置不够，应当不能成功产出");
	}
}
