package cn.noy.cui.crafting;

import cn.noy.cui.crafting.consumer.ShapedConsumer;
import cn.noy.cui.crafting.consumer.ingredient.ExactIngredient;
import cn.noy.cui.crafting.producer.ShapelessProducer;
import cn.noy.cui.crafting.producer.product.ExactProduct;
import cn.noy.cui.layer.Layer;
import cn.noy.cui.util.ItemStackAssertions;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

public class CraftingTableTest {
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
		var workbench = CraftingTable.builder().addInput(3, 3).addOutput(1, 3).mode(CraftingTable.Mode.MANUAL)
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
				.build();

		Layer inputLayer = workbench.generateInputLayer(0, null);
		Layer outputLayer = workbench.generateOutputLayer(0, null);
		inputLayer.getSlot(0, 0).place(new ItemStack(Material.IRON_INGOT, 1), null);
		inputLayer.getSlot(0, 2).place(new ItemStack(Material.IRON_INGOT, 1), null);
		inputLayer.getSlot(1, 0).place(new ItemStack(Material.IRON_INGOT, 4), null);
		inputLayer.getSlot(1, 1).place(new ItemStack(Material.IRON_INGOT, 2), null);
		inputLayer.getSlot(1, 2).place(new ItemStack(Material.IRON_INGOT, 4), null);
		inputLayer.getSlot(2, 0).place(new ItemStack(Material.IRON_INGOT, 4), null);
		inputLayer.getSlot(2, 1).place(new ItemStack(Material.IRON_INGOT, 1), null);
		inputLayer.getSlot(2, 2).place(new ItemStack(Material.IRON_INGOT, 4), null);
		workbench.match(null, null);
		workbench.apply(null);
		workbench.match(null, null);
		workbench.apply(null);
		workbench.match(null, null);
		workbench.apply(null);
		workbench.match(null, null);
		workbench.apply(null);
		ItemStackAssertions.assertSame(new ItemStack(Material.IRON_CHESTPLATE), outputLayer.getSlot(0, 0).get(),
				"应当成功合成铁胸甲");
		ItemStackAssertions.assertSame(new ItemStack(Material.IRON_HELMET), outputLayer.getSlot(0, 1).get(),
				"应当成功合成铁头盔");
		ItemStackAssertions.assertSame(new ItemStack(Material.IRON_BOOTS), outputLayer.getSlot(0, 2).get(),
				"应当成功合成铁靴子");
		outputLayer.getSlot(0, 0).set(null, null);
		workbench.match(null, null);
		workbench.apply(null);
		ItemStackAssertions.assertSame(new ItemStack(Material.IRON_BOOTS), outputLayer.getSlot(0, 0).get(),
				"应当成功合成铁靴子");
	}

	// TODO: 模拟玩家使用工作台合成物品
}
