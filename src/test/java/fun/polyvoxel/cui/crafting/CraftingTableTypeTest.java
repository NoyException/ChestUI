package fun.polyvoxel.cui.crafting;

import fun.polyvoxel.cui.crafting.consumer.ShapedConsumer;
import fun.polyvoxel.cui.crafting.consumer.ingredient.ExactIngredient;
import fun.polyvoxel.cui.crafting.producer.ShapelessProducer;
import fun.polyvoxel.cui.crafting.producer.product.ExactProduct;
import fun.polyvoxel.cui.layer.Layer;
import fun.polyvoxel.cui.util.ItemStackAssertions;
import fun.polyvoxel.cui.util.context.Context;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

public class CraftingTableTypeTest {
	private static PlayerMock player;

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
	 * 模拟一个简易的工作台：尝试连续合成铁胸甲、铁头盔、铁靴子、铁靴子，铁锭分布如下：<br>
	 * 1 0 1<br>
	 * 4 2 4<br>
	 * 4 1 4<br>
	 * 产品区给3个空位，所以合成第四个时会失败
	 */
	@Test
	public void testWorkbench() {
		var workbench = CraftingTableType.builder().addInput(3, 3).addOutput(1, 3).mode(CraftingTableType.Mode.MANUAL)
				.addRecipe(Recipe.builder()
						.addConsumer(ShapedConsumer.builder().pattern("I I", "III", "III")
								.set('I', new ExactIngredient(Material.IRON_INGOT)).build())
						.addProducer(
								ShapelessProducer.builder().add(new ExactProduct(Material.IRON_CHESTPLATE)).build())
						.build())
				.addRecipe(Recipe.builder()
						.addConsumer(ShapedConsumer.builder().pattern("III", "I I")
								.set('I', new ExactIngredient(Material.IRON_INGOT)).build())
						.addProducer(ShapelessProducer.builder().add(new ExactProduct(Material.IRON_HELMET)).build())
						.build())
				.addRecipe(Recipe.builder()
						.addConsumer(ShapedConsumer.builder().pattern("I I", "I I")
								.set('I', new ExactIngredient(Material.IRON_INGOT)).build())
						.addProducer(ShapelessProducer.builder().add(new ExactProduct(Material.IRON_BOOTS)).build())
						.build())
				.addRecipe(Recipe.builder()
						.addConsumer(ShapedConsumer.builder().pattern("III", "I I", "I I")
								.set('I', new ExactIngredient(Material.IRON_INGOT)).build())
						.addProducer(ShapelessProducer.builder().add(new ExactProduct(Material.IRON_LEGGINGS)).build())
						.build())
				.build().createInstance();

		Layer inputLayer = workbench.generateInputLayer(0);
		Layer outputLayer = workbench.generateOutputLayer(0);
		inputLayer.getSlot(0, 0).place(ItemStack.of(Material.IRON_INGOT, 1), null);
		inputLayer.getSlot(0, 2).place(ItemStack.of(Material.IRON_INGOT, 1), null);
		inputLayer.getSlot(1, 0).place(ItemStack.of(Material.IRON_INGOT, 4), null);
		inputLayer.getSlot(1, 1).place(ItemStack.of(Material.IRON_INGOT, 2), null);
		inputLayer.getSlot(1, 2).place(ItemStack.of(Material.IRON_INGOT, 4), null);
		inputLayer.getSlot(2, 0).place(ItemStack.of(Material.IRON_INGOT, 4), null);
		inputLayer.getSlot(2, 1).place(ItemStack.of(Material.IRON_INGOT, 1), null);
		inputLayer.getSlot(2, 2).place(ItemStack.of(Material.IRON_INGOT, 4), null);
		workbench.match(Context.background().withPlayer(player));
		workbench.apply();
		workbench.match(Context.background().withPlayer(player));
		workbench.apply();
		workbench.match(Context.background().withPlayer(player));
		workbench.apply();
		workbench.match(Context.background().withPlayer(player));
		workbench.apply();
		ItemStackAssertions.assertSame(ItemStack.of(Material.IRON_CHESTPLATE), outputLayer.getSlot(0, 0).get(),
				"应当成功合成铁胸甲");
		ItemStackAssertions.assertSame(ItemStack.of(Material.IRON_HELMET), outputLayer.getSlot(0, 1).get(),
				"应当成功合成铁头盔");
		ItemStackAssertions.assertSame(ItemStack.of(Material.IRON_BOOTS), outputLayer.getSlot(0, 2).get(), "应当成功合成铁靴子");
		outputLayer.getSlot(0, 0).set(null, null);
		workbench.match(Context.background());
		workbench.apply();
		ItemStackAssertions.assertSame(ItemStack.of(Material.IRON_BOOTS), outputLayer.getSlot(0, 0).get(), "应当成功合成铁靴子");
	}

	// TODO: 模拟玩家使用工作台合成物品
}
