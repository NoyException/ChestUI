package fun.polyvoxel.cui.crafting;

import fun.polyvoxel.cui.crafting.consumer.ShapedConsumer;
import fun.polyvoxel.cui.crafting.consumer.ShapelessConsumer;
import fun.polyvoxel.cui.crafting.consumer.ingredient.BucketIngredient;
import fun.polyvoxel.cui.crafting.consumer.ingredient.ExactIngredient;
import fun.polyvoxel.cui.crafting.consumer.ingredient.MaterialMatchedIngredient;
import fun.polyvoxel.cui.crafting.producer.ShapedProducer;
import fun.polyvoxel.cui.crafting.producer.ShapelessProducer;
import fun.polyvoxel.cui.crafting.producer.product.ExactProduct;
import fun.polyvoxel.cui.crafting.producer.product.RandomProduct;
import fun.polyvoxel.cui.util.ItemStackAssertions;
import fun.polyvoxel.cui.util.context.Context;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.*;
import java.util.stream.Collectors;

public class RecipeTest {
	private static PlayerMock player, player2;
	private static CraftingContext createCtx(Player player) {
		return CraftingContext.background().withContext(Context.background().withPlayer(player));
	}

	@BeforeAll
	public static void setup() {
		var server = MockBukkit.mock();
		player = server.addPlayer();
		player2 = server.addPlayer();
	}

	@AfterAll
	public static void teardown() {
		MockBukkit.unmock();
	}

	/**
	 * 测试原版MC的魔改版木棍配方：1任意类型木板->4个木棍
	 */
	@Test
	public void testStickRecipe() {
		var woods = Arrays.stream(Boat.Type.values()).map(Boat.Type::getMaterial)
				.collect(Collectors.toUnmodifiableSet());
		var recipe = Recipe.builder().strict(true)
				.addConsumer(ShapelessConsumer.builder().strict(true)
						.add(new MaterialMatchedIngredient(woods::contains)).build())
				.addProducer(
						ShapedProducer.builder().pattern("A").set('A', new ExactProduct(Material.STICK, 4)).build())
				.build();
		// 测试堆叠生产
		var io = new CraftingTableType.IO(new CraftingTableType.Inputs(), new CraftingTableType.Outputs());
		io.inputs().addInput(2, 2);
		io.inputs().getInput(0)[0][1] = ItemStack.of(Material.OAK_PLANKS, 2);
		io.outputs().addOutput(2, 2);
		var used = recipe.use(createCtx(player), io);
		Assertions.assertNotNull(used, "应当能使用成功");
		ItemStackAssertions.assertSame(ItemStack.of(Material.OAK_PLANKS, 1), used.inputs().getInput(0)[0][1],
				"应当还剩下1个橡木板");
		ItemStackAssertions.assertSame(ItemStack.of(Material.STICK, 4), used.outputs().getOutput(0)[0][0], "应当能生产4个木棍");
		used = recipe.use(createCtx(player), used);
		Assertions.assertNotNull(used, "应当能再次使用成功");
		ItemStackAssertions.assertSame(ItemStack.of(Material.STICK, 8), used.outputs().getOutput(0)[0][0], "应当能生产8个木棍");
		Assertions.assertNull(recipe.use(createCtx(player), used), "应当不能第三次使用成功");
		// 测试最初的io是否受到了影响
		used = recipe.use(createCtx(player), io);
		Assertions.assertNotNull(used, "应当不影响最初的io");
		used = recipe.use(createCtx(player), used);
		Assertions.assertNotNull(used, "应当不影响最初的io");
		Assertions.assertNull(recipe.use(createCtx(player), used), "应当不能第三次使用成功");
		// 测试原料区异常
		io.inputs().getInput(0)[0][0] = ItemStack.of(Material.OAK_PLANKS, 1);
		Assertions.assertNull(recipe.use(createCtx(player), io), "应当不能使用成功");
		io.inputs().getInput(0)[0][0] = ItemStack.of(Material.OAK_LOG, 1);
		io.inputs().getInput(0)[0][1] = null;
		Assertions.assertNull(recipe.use(createCtx(player), io), "应当不能使用成功");
		io.inputs().getInput(0)[0][0] = ItemStack.of(Material.OAK_PLANKS, 64);
		Assertions.assertNotNull(recipe.use(createCtx(player), io), "应当能使用成功");
		// 测试产品区异常
		io.outputs().getOutput(0)[0][0] = ItemStack.of(Material.STICK, 63);
		Assertions.assertNull(recipe.use(createCtx(player), io), "应当不能使用成功");
		io.outputs().getOutput(0)[0][0] = ItemStack.of(Material.OAK_LOG, 1);
		Assertions.assertNull(recipe.use(createCtx(player), io), "应当不能使用成功");
		io.outputs().getOutput(0)[0][0] = ItemStack.of(Material.STICK, 3);
		Assertions.assertNotNull(recipe.use(createCtx(player), io), "应当能使用成功");
	}

	/**
	 * 测试原版MC的打火石配方：1铁锭+1燧石->1打火石
	 */
	@Test
	public void testFlintAndSteelRecipe() {
		var recipe = Recipe.builder().strict(true)
				.addConsumer(ShapelessConsumer.builder().strict(true).add(new ExactIngredient(Material.IRON_INGOT))
						.add(new ExactIngredient(Material.FLINT)).build())
				.addProducer(ShapelessProducer.builder().add(new ExactProduct(Material.FLINT_AND_STEEL)).build())
				.build();
		// 测试生产
		var io = new CraftingTableType.IO(new CraftingTableType.Inputs(), new CraftingTableType.Outputs());
		io.inputs().addInput(3, 3);
		io.inputs().getInput(0)[2][2] = ItemStack.of(Material.IRON_INGOT, 2);
		io.inputs().getInput(0)[0][1] = ItemStack.of(Material.FLINT, 3);
		io.outputs().addOutput(1, 1);
		var used = recipe.use(createCtx(player), io);
		Assertions.assertNotNull(used, "应当能使用成功");
		ItemStackAssertions.assertSame(ItemStack.of(Material.IRON_INGOT, 1), used.inputs().getInput(0)[2][2],
				"应当还剩下1个铁锭");
		ItemStackAssertions.assertSame(ItemStack.of(Material.FLINT, 2), used.inputs().getInput(0)[0][1], "应当还剩下2个燧石");
		ItemStackAssertions.assertSame(ItemStack.of(Material.FLINT_AND_STEEL, 1), used.outputs().getOutput(0)[0][0],
				"应当能生产1个打火石");
		// 由于打火石不能堆叠，无法堆叠生产
		Assertions.assertNull(recipe.use(createCtx(player), used), "应当不能再次使用成功");
		used.outputs().getOutput(0)[0][0] = null;
		Assertions.assertNotNull(recipe.use(createCtx(player), used), "应当能使用成功");
	}

	/**
	 * 测试原版MC的蛋糕配方：<br>
	 * 牛奶 牛奶 牛奶<br>
	 * 糖 鸡蛋 糖<br>
	 * 小麦 小麦 小麦<br>
	 * -> 蛋糕
	 */
	@Test
	public void testCakeRecipe() {
		var recipe = Recipe.builder().strict(true)
				.addConsumer(ShapedConsumer.builder().strict(true).pattern("MMM", "SES", "WWW")
						.set('M', new BucketIngredient(Material.MILK_BUCKET))
						.set('S', new ExactIngredient(Material.SUGAR)).set('E', new ExactIngredient(Material.EGG))
						.set('W', new ExactIngredient(Material.WHEAT)).build())
				.addProducer(ShapelessProducer.builder().add(new ExactProduct(Material.CAKE)).build()).build();
		// 测试生产
		var io = new CraftingTableType.IO(new CraftingTableType.Inputs(), new CraftingTableType.Outputs());
		io.inputs().addInput(3, 3);
		ItemStack[][] input = io.inputs().getInput(0);
		input[0][0] = ItemStack.of(Material.MILK_BUCKET, 1);
		input[0][1] = ItemStack.of(Material.MILK_BUCKET, 1);
		input[0][2] = ItemStack.of(Material.MILK_BUCKET, 1);
		input[1][0] = ItemStack.of(Material.SUGAR, 3);
		input[1][1] = ItemStack.of(Material.EGG, 3);
		input[1][2] = ItemStack.of(Material.SUGAR, 3);
		input[2][0] = ItemStack.of(Material.WHEAT, 3);
		input[2][1] = ItemStack.of(Material.WHEAT, 3);
		input[2][2] = ItemStack.of(Material.WHEAT, 3);
		io.outputs().addOutput(2, 1);
		var used = recipe.use(createCtx(player), io);
		Assertions.assertNotNull(used, "应当能使用成功");
		ItemStackAssertions.assertSame(ItemStack.of(Material.BUCKET, 1), used.inputs().getInput(0)[0][0], "应当剩下1个桶");
		ItemStackAssertions.assertSame(ItemStack.of(Material.CAKE, 1), used.outputs().getOutput(0)[0][0], "应当能生产1个蛋糕");
		// 因为牛奶桶变成了桶，故无法继续生产
		Assertions.assertNull(recipe.use(createCtx(player), used), "应当不能再次使用成功");
		used.inputs().getInput(0)[0][0] = ItemStack.of(Material.MILK_BUCKET, 1);
		used.inputs().getInput(0)[0][1] = ItemStack.of(Material.MILK_BUCKET, 1);
		used.inputs().getInput(0)[0][2] = ItemStack.of(Material.MILK_BUCKET, 1);
		// 改回牛奶桶后应当能成功
		used = recipe.use(createCtx(player), used);
		Assertions.assertNotNull(used, "应当能再次使用成功");
		Assertions.assertEquals(ItemStack.of(Material.EGG, 1), used.inputs().getInput(0)[1][1]);
		// 产品区已满，应当不能再次使用成功
		used.inputs().getInput(0)[0][0] = ItemStack.of(Material.MILK_BUCKET, 1);
		used.inputs().getInput(0)[0][1] = ItemStack.of(Material.MILK_BUCKET, 1);
		used.inputs().getInput(0)[0][2] = ItemStack.of(Material.MILK_BUCKET, 1);
		Assertions.assertNull(recipe.use(createCtx(player), used), "产品区已满，应当不能再次使用成功");
	}

	/**
	 * 测试一个产物随机的配方：1石头->泥土或石头
	 */
	@Test
	public void testRandom() {
		var recipe = Recipe.builder().strict(true)
				.addConsumer(ShapelessConsumer.builder().strict(true).add(new ExactIngredient(Material.STONE)).build())
				.addProducer(ShapedProducer.builder().pattern("A")
						.set('A',
								new RandomProduct(List.of(Pair.of(ItemStack.of(Material.STONE), 30),
										Pair.of(ItemStack.of(Material.DIRT), 70)), new Random(0), false))
						.build())
				.build();
		// 测试生产
		var io = new CraftingTableType.IO(new CraftingTableType.Inputs(), new CraftingTableType.Outputs());
		io.inputs().addInput(1, 1);
		io.inputs().getInput(0)[0][0] = ItemStack.of(Material.STONE, 64);
		io.outputs().addOutput(1, 1);
		var used = recipe.use(createCtx(player), io);
		Assertions.assertNotNull(used, "应当能使用成功");
		recipe.onApply(player);
		ItemStackAssertions.assertSame(ItemStack.of(Material.STONE, 63), used.inputs().getInput(0)[0][0], "应当还剩下63个石头");
		ItemStackAssertions.assertSame(ItemStack.of(Material.DIRT, 1), used.outputs().getOutput(0)[0][0], "应当能生产1个泥土");
		// 在该种子下，还会再生产1个泥土
		used = recipe.use(createCtx(player), used);
		Assertions.assertNotNull(used, "应当能再次使用成功");
		recipe.onApply(player);
		ItemStackAssertions.assertSame(ItemStack.of(Material.STONE, 62), used.inputs().getInput(0)[0][0], "应当还剩下62个石头");
		ItemStackAssertions.assertSame(ItemStack.of(Material.DIRT, 2), used.outputs().getOutput(0)[0][0], "应当能生产2个泥土");
		// 在该种子下，下次将生产1个石头，没有放置空间
		for (int i = 0; i < 10; i++) {
			Assertions.assertNull(recipe.use(createCtx(player), used), "应当不能再次使用成功");
		}
		// 但是另一个玩家则可以
		used = recipe.use(createCtx(player2), used);
		Assertions.assertNotNull(used, "应当能使用成功");
		recipe.onApply(player2);
		ItemStackAssertions.assertSame(ItemStack.of(Material.STONE, 61), used.inputs().getInput(0)[0][0], "应当还剩下61个石头");
		ItemStackAssertions.assertSame(ItemStack.of(Material.DIRT, 3), used.outputs().getOutput(0)[0][0], "应当能生产3个泥土");
	}
}
