package fun.polyvoxel.cui.prebuilt.cui;

import fun.polyvoxel.cui.crafting.CraftingTable;
import fun.polyvoxel.cui.crafting.Recipe;
import fun.polyvoxel.cui.crafting.consumer.ShapedConsumer;
import fun.polyvoxel.cui.crafting.consumer.ShapelessConsumer;
import fun.polyvoxel.cui.crafting.consumer.ingredient.BucketIngredient;
import fun.polyvoxel.cui.crafting.consumer.ingredient.ExactIngredient;
import fun.polyvoxel.cui.crafting.consumer.ingredient.MaterialMatchedIngredient;
import fun.polyvoxel.cui.crafting.producer.ShapelessProducer;
import fun.polyvoxel.cui.crafting.producer.product.ExactProduct;
import fun.polyvoxel.cui.layer.Layer;
import fun.polyvoxel.cui.ui.*;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Boat;
import org.bukkit.event.block.Action;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

@CUI(name = "ew", singleton = true, icon = Material.CRAFTING_TABLE)
public class EnhancedWorkbench implements ChestUI<EnhancedWorkbench> {

	@Override
	public void onInitialize(CUIType<EnhancedWorkbench> type) {
		type.edit().triggerByDisplayCommand(
				player -> new CUIType.TriggerResult<>(CUIType.TriggerResultType.CREATE_NEW_CAMERA, camera -> {
				}));
		type.edit().triggerByBlock(event -> {
			if (event.getAction() != Action.RIGHT_CLICK_BLOCK
					|| !event.getClickedBlock().getState().hasMetadata("chestui:ew")) {
				return new CUIType.TriggerResult<>(CUIType.TriggerResultType.REJECTED, camera -> {
				});
			}
			return new CUIType.TriggerResult<>(CUIType.TriggerResultType.CREATE_NEW_CAMERA, camera -> {
			});
		});
	}

	@Override
	public ChestUI.@NotNull Handler<EnhancedWorkbench> createHandler() {
		return new Handler();
	}

	private static class Handler implements ChestUI.Handler<EnhancedWorkbench> {
		private CraftingTable craftingTable;

		@Override
		public void onInitialize(CUIInstance<EnhancedWorkbench> cui) {
			var woods = Arrays.stream(Boat.Type.values()).map(Boat.Type::getMaterial)
					.collect(Collectors.toUnmodifiableSet());
			craftingTable = CraftingTable.builder().mode(CraftingTable.Mode.AUTO).addInput(5, 5).addOutput(5, 3)
					.ioType(CraftingTable.IOType.CAMERA)
					// 木棍
					.addRecipe(Recipe.builder().strict(true)
							.addConsumer(ShapedConsumer.builder().strict(true).pattern("W", "W")
									.set('W', new MaterialMatchedIngredient(woods::contains)).build())
							.addProducer(ShapelessProducer.builder().add(new ExactProduct(Material.STICK, 4)).build())
							.build())
					// 打火石
					.addRecipe(Recipe.builder().strict(true)
							.addConsumer(ShapelessConsumer.builder().strict(true)
									.add(new ExactIngredient(Material.IRON_INGOT))
									.add(new ExactIngredient(Material.FLINT)).build())
							.addProducer(
									ShapelessProducer.builder().add(new ExactProduct(Material.FLINT_AND_STEEL)).build())
							.build())
					// 蛋糕
					.addRecipe(Recipe.builder().strict(true).addConsumer(ShapedConsumer.builder().strict(true)
							.pattern("MMM", "SES", "WWW").set('M', new BucketIngredient(Material.MILK_BUCKET))
							.set('S', new ExactIngredient(Material.SUGAR)).set('E', new ExactIngredient(Material.EGG))
							.set('W', new ExactIngredient(Material.WHEAT)).build())
							.addProducer(ShapelessProducer.builder().add(new ExactProduct(Material.CAKE)).build())
							.build())
					.build();
			cui.edit().setLayer(0, new Layer(5, 9).edit().editColumn(5, (slotHandler, integer) -> {
				slotHandler.button(
						builder -> builder.material(Material.BLACK_STAINED_GLASS_PANE).displayName(" ").build());
			}).editSlot(2, 5, slotHandler -> slotHandler.button(builder -> builder.material(Material.CRAFTING_TABLE)
					.displayName("Put a workbench under your feet").clickHandler(cuiClickEvent -> {
						var player = cuiClickEvent.getPlayer();
						var block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
						block.setType(Material.CRAFTING_TABLE);
						block.setMetadata("chestui:ew", new FixedMetadataValue(cui.getPlugin(), true));
					}).build())).finish()).finish();
		}

		@Override
		public void onCreateCamera(Camera<EnhancedWorkbench> camera) {
			camera.edit().setRowSize(5);
			var inputLayer = craftingTable.generateInputLayer(0, camera);
			var outputLayer = craftingTable.generateOutputLayer(0, camera).edit().marginLeft(6).finish();
			camera.edit().setLayer(-1, inputLayer).setLayer(-2, outputLayer).finish();
		}
	}
}
