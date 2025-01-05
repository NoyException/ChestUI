package fun.polyvoxel.cui.prebuilt;

import fun.polyvoxel.cui.event.CUIClickEvent;
import fun.polyvoxel.cui.layer.Layer;
import fun.polyvoxel.cui.serialize.CUIData;
import fun.polyvoxel.cui.serialize.LayerData;
import fun.polyvoxel.cui.serialize.SlotData;
import fun.polyvoxel.cui.slot.Button;
import fun.polyvoxel.cui.slot.Transformer;
import fun.polyvoxel.cui.ui.*;
import fun.polyvoxel.cui.util.ItemStacks;
import fun.polyvoxel.cui.util.Position;
import fun.polyvoxel.cui.util.context.Context;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@CUI(name = "creator", icon = Material.COMMAND_BLOCK)
public class CUICreator implements ChestUI<CUICreator> {
	private CUIType<CUICreator> type;
	private final Map<String, CUIData> byName = new HashMap<>();
	private CUIManager manager;

	@Override
	public void onInitialize(CUIType<CUICreator> type) {
		this.type = type.edit().defaultTitle(Component.text("CUI Creator", NamedTextColor.LIGHT_PURPLE))
				.triggerByDisplay(CameraProvider.createCameraInNewCUIInstance()).done();
		this.manager = type.getCUIPlugin().getCUIManager();

		var plugin = type.getCUIPlugin();
		var cuiFolder = new File(plugin.getDataFolder(), "json");
		if (!cuiFolder.exists()) {
			return;
		}
		if (!cuiFolder.isDirectory()) {
			plugin.getLogger().warning("json folder is not a directory");
			return;
		}
		var files = cuiFolder.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (!file.getName().endsWith(".json")) {
				continue;
			}
			try (FileReader reader = new FileReader(file)) {
				var data = CUIData.fromJson(reader);
				byName.put(data.name, data);
			} catch (IOException e) {
				plugin.getLogger().warning("Failed to read json file: " + file.getName());
				e.printStackTrace();
			}
		}
	}

	private Component toComponent(String s) {
		return ItemStacks.cleanComponent(LegacyComponentSerializer.legacyAmpersand().deserialize(s));
	}

	private CUIData getData(String name) {
		return byName.get(name);
	}

	@Override
	public @NotNull CUIInstanceHandler<CUICreator> createCUIInstanceHandler(Context context) {
		var data = context.get("payload");
		return switch (data) {
			case CUIData cuiData -> new EditCUIHandler(cuiData);
			case LayerData layerData -> new EditLayerHandler(context.get("cui"), layerData);
			case SlotData slotData -> new EditSlotHandler(slotData);
			case null, default -> new DisplayAllJsonHandler();
		};
	}

	public class DisplayAllJsonHandler implements CUIInstanceHandler<CUICreator> {
		private CUIInstance<CUICreator> cui;
		private int size;

		@Override
		public void onInitialize(CUIInstance<CUICreator> cui) {
			this.cui = cui.edit()
					.layer(0,
							new Layer(1, 9).edit().relative(true)
									.all((row, column) -> Button.builder().material(Material.BLACK_STAINED_GLASS_PANE)
											.displayName(" ").build())
									.slot(0, 0, () -> Button.builder().material(Material.RED_STAINED_GLASS_PANE)
											.displayName("Previous").click(event -> {
												var position = event.getCamera().getPosition();
												if (position.row() <= 0)
													return;
												event.getCamera().edit().move(-5, 0);
											}).build())
									.slot(0, 8, () -> Button.builder().material(Material.GREEN_STAINED_GLASS_PANE)
											.displayName("Next").click(event -> {
												var position = event.getCamera().getPosition();
												if ((position.row() / 5 + 1) * 45 >= size)
													return;
												event.getCamera().edit().move(5, 0);
											}).build())
									.done())
					.done();
		}

		private void update() {
			var dataList = new ArrayList<>(byName.values());
			size = dataList.size() + 1;
			cui.edit().layer(1, new Layer(0, 9).edit().marginTop(1).tile(size, true, index -> {
				if (index == 0) {
					return Button.builder().material(Material.GREEN_CONCRETE)
							.displayName(Component.text("Create new chest UI", NamedTextColor.GREEN))
							.click(cuiClickEvent -> {
								var player = cuiClickEvent.getPlayer();
								cui.getCUIPlugin().getTools().createAnvilTextInput(player, name -> {
									name = name.toUpperCase(Locale.ROOT);
									var cuiData = new CUIData();
									try {
										new NamespacedKey("json", name);
									} catch (Exception e) {
										player.sendMessage(Component.text("Invalid name", NamedTextColor.RED));
										return;
									}
									cuiData.name = name;
									var path = cuiData.getDefaultPath(cui.getCUIPlugin());
									if (path.toFile().exists()) {
										player.sendMessage(
												Component.text("The name already exists", NamedTextColor.RED));
										return;
									}
									byName.put(name, cuiData);
									update();
									type.createInstance(Context.background().withValue("payload", cuiData))
											.createCamera().open(player, true);
								}, Component.text("Name (0~9, a~z):")).open(player, true);
							}).build();
				}
				var data = dataList.get(index - 1);
				return Button.builder().material(data.icon).displayName(data.getKey().toString())
						.lore(Component.text("Left click to edit", NamedTextColor.GREEN),
								Component.text("Right click to save and (re)register", NamedTextColor.GREEN),
								Component.text("※ Any changes will be lost if not saved", NamedTextColor.DARK_RED,
										TextDecoration.BOLD),
								Component.text("Drop(Q) to delete forever", NamedTextColor.RED))
						.click(event -> {
							if (event.getClickType() == ClickType.DROP) {
								var path = data.getDefaultPath(cui.getCUIPlugin());
								if (path.toFile().delete()) {
									manager.unregisterCUI(data.getKey());
									byName.remove(data.name);
									update();
								} else {
									event.getPlayer().sendMessage(
											Component.text("Failed to delete the json file", NamedTextColor.RED));
								}
							} else if (event.getClickType().isLeftClick()) {
								CUICreator.this.type.createInstance(Context.background().withValue("payload", data))
										.createCamera().open(event.getPlayer(), true);
							} else if (event.getClickType().isRightClick()) {
								var path = data.getDefaultPath(cui.getCUIPlugin());
								var json = data.toJson();
								try {
									Files.writeString(path, json);
									event.getPlayer().sendMessage(Component.text("Saved", NamedTextColor.GREEN));
								} catch (IOException e) {
									event.getPlayer().sendMessage(Component.text("Failed to save", NamedTextColor.RED));
									e.printStackTrace();
								}
								manager.unregisterCUI(data.getKey());
								manager.registerCUI(path.toFile());
							}
						}).build();
			}).done());
		}

		@Override
		public @NotNull CameraHandler<CUICreator> createCameraHandler(Context context) {
			return new CameraHandler<>() {
				@Override
				public void onInitialize(Camera<CUICreator> camera) {
					camera.edit().rowSize(6);
				}

				@Override
				public void onOpenInventory(Inventory inventory) {
					update();
				}
			};
		}
	}

	public class EditCUIHandler implements CUIInstanceHandler<CUICreator> {
		private CUIInstance<CUICreator> cui;
		private final CUIData toEdit;
		private Layer buttonLayer;
		private Layer effectLayer;
		private Layer layersLayer;

		public EditCUIHandler(CUIData data) {
			this.toEdit = data;
		}

		private void setButton(int index, BiConsumer<CUIClickEvent<?>, Runnable> onClick,
				Supplier<Transformer> transformer) {
			var row = index / 7;
			var column = index % 7;
			Runnable setEffect = () -> effectLayer.edit().slot(row, column, transformer::get);
			buttonLayer.edit().slot(row, column, () -> Button.builder().click(cuiClickEvent -> {
				onClick.accept(cuiClickEvent, setEffect);
			}).build());
			setEffect.run();
		}

		private void updateLayerData(int depth) {
			layersLayer.edit().slot(depth / 7, depth % 7, () -> {
				var layerData = toEdit.layers.get(depth);
				if (layerData == null) {
					return Button.builder().material(Material.STRUCTURE_VOID).displayName("Empty in Depth " + depth)
							.lore(Component.text("Click to create a new layer", NamedTextColor.GREEN)).click(event -> {
								var layer = new LayerData();
								toEdit.layers.put(depth, layer);
								updateLayerData(depth);
							}).build();
				}
				return Button.builder().material(Material.PAINTING).displayName("Layer " + depth)
						.lore(Component.text("Left click to edit", NamedTextColor.GREEN),
								Component.text("Drop(Q) to delete", NamedTextColor.RED))
						.click(event -> {
							if (event.getClickType() == ClickType.DROP) {
								toEdit.layers.remove(depth);
								updateLayerData(depth);
							} else {
								type.createInstance(
										Context.background().withValue("payload", layerData).withValue("cui", toEdit))
										.createCamera().open(event.getPlayer(), true);
							}
						}).build();
			});
		}

		private void update() {
			for (int i = 0; i < 32; i++) {
				updateLayerData(i);
			}
		}

		@Override
		public void onInitialize(CUIInstance<CUICreator> cui) {
			buttonLayer = new Layer(6, 7).edit().marginLeft(2).done();
			effectLayer = new Layer(6, 7).edit().marginLeft(2).done();
			layersLayer = new Layer(6, 7).edit().marginLeft(9).done();
			this.cui = cui.edit().layer(0, new Layer(6, 2).edit().relative(true).column(1,
					row -> Button.builder().material(Material.BLACK_STAINED_GLASS_PANE).displayName(" ").build())
					.slot(0, 0, () -> Button.builder().material(Material.COMMAND_BLOCK).displayName("Basic Settings")
							.click(event -> {
								event.getCamera().edit().position(new Position(0, 0));
							}).build())
					.slot(1, 0, () -> Button.builder().material(Material.BOOKSHELF).displayName("Layer Settings")
							.click(event -> {
								event.getCamera().edit().position(new Position(0, 7));
							}).build())
					.done()).layer(1, effectLayer).layer(2, buttonLayer).layer(3, layersLayer).done();

			setButton(0, (event, refresh) -> {
				ItemStack cursor = event.getCursor();
				if (ItemStacks.isEmpty(cursor)) {
					return;
				}
				var material = cursor.getType();
				if (material == Material.AIR) {
					return;
				}
				toEdit.icon = material;
				refresh.run();
			}, () -> Transformer.builder()
					.changeItemStack(ItemStacks.builder().material(toEdit.icon).displayName(Component.text("Icon"))
							.lore(Component.text("Click with item in cursor to change")).build())
					.enchant().build());

			setButton(1, (event, refresh) -> {
				var player = event.getPlayer();
				cui.getCUIPlugin().getTools().createAnvilTextInput(player, title -> {
					toEdit.title = title;
					refresh.run();
				}, Component.text("Edit title")).open(player, true);
			}, () -> Transformer.builder()
					.changeItemStack(ItemStacks.builder().material(Material.NAME_TAG)
							.displayName(Component.text("Title: ").append(toComponent(toEdit.title)))
							.lore(Component.text("Click to edit title")).build())
					.build());

			setButton(2, (event, refresh) -> {
				toEdit.singleton = !toEdit.singleton;
				refresh.run();
			}, () -> Transformer.builder()
					.changeItemStack(ItemStacks.builder()
							.material(toEdit.singleton ? Material.PRISMARINE_SHARD : Material.PRISMARINE_CRYSTALS)
							.displayName(toComponent(toEdit.singleton ? "Singleton" : "Not Singleton"))
							.lore(Component.text("Click to change")).build())
					.build());

			setButton(3, (event, refresh) -> {
				var player = event.getPlayer();
				cui.getCUIPlugin().getTools().createAnvilTextInput(player, row -> {
					int rowSize;
					try {
						rowSize = Integer.parseInt(row);
					} catch (NumberFormatException e) {
						player.sendMessage(Component.text("Invalid number", NamedTextColor.RED));
						return;
					}
					if (rowSize <= 0 || rowSize > 6) {
						player.sendMessage(Component.text("Row size must between 1 and 6", NamedTextColor.RED));
						return;
					}
					toEdit.maxRow = rowSize;
					refresh.run();
				}, Component.text("Row size (int,1..6):")).open(player, true);
			}, () -> Transformer.builder()
					.changeItemStack(ItemStacks.builder().material(Material.CHEST)
							.displayName(toComponent("Size: ").append(
									Component.text(toEdit.maxRow + "*" + toEdit.maxColumn, NamedTextColor.AQUA)))
							.lore(Component.text("Click to edit size")).build())
					.build());
		}

		@Override
		public @NotNull CameraHandler<CUICreator> createCameraHandler(Context context) {
			return new CameraHandler<>() {
				@Override
				public void onInitialize(Camera<CUICreator> camera) {
					camera.edit().rowSize(6);
				}

				@Override
				public void onOpenInventory(Inventory inventory) {
					update();
				}
			};
		}
	}

	public class EditLayerHandler implements CUIInstanceHandler<CUICreator> {
		private CUIInstance<CUICreator> cui;
		private final CUIData cuiData;
		private final LayerData toEdit;
		private Layer buttonLayer;
		private Layer effectLayer;
		private Layer slotsLayer;
		private Position position = Position.ZERO;

		public EditLayerHandler(CUIData cuiData, LayerData toEdit) {
			this.cuiData = cuiData;
			this.toEdit = toEdit;
		}

		private void setButton(int index, BiConsumer<CUIClickEvent<?>, Runnable> onClick,
				Supplier<Transformer> transformer) {
			var row = index / 7;
			var column = index % 7;
			Runnable setEffect = () -> effectLayer.edit().slot(row, column, transformer::get);
			buttonLayer.edit().slot(row, column, () -> Button.builder().click(cuiClickEvent -> {
				onClick.accept(cuiClickEvent, setEffect);
			}).build());
			setEffect.run();
		}

		private void updateSlotData(int row, int column) {
			slotsLayer.edit().slot(row, column, () -> {
				var pos = position.add(row, column);
				if (pos.row() < 0 || pos.row() >= toEdit.maxRow || pos.column() < 0
						|| pos.column() >= toEdit.maxColumn) {
					return Button.builder().material(Material.BARRIER).displayName("Out of bounds").build();
				}
				var slotData = toEdit.slots.get(pos);
				if (slotData == null) {
					return Button.builder().material(Material.STRUCTURE_VOID)
							.displayName("Empty in (" + row + ", " + column + ")")
							.lore(Component.text("Click to create a new slot", NamedTextColor.GREEN),
									Component.text("The appearance will be"), Component.text("the item in your ")
											.append(Component.text("cursor", NamedTextColor.YELLOW)))
							.click(event -> {
								var slot = new SlotData();
								toEdit.slots.put(pos, slot);
								var cursor = event.getCursor();
								if (!ItemStacks.isEmpty(cursor)) {
									slot.setItemStack(cursor);
								}
								updateSlotData(row, column);
							}).build();
				}
				return Button.builder().itemStack(slotData.getDemo()).displayName("Slot (" + row + ", " + column + ")")
						.lore(Component.text("Left click to edit", NamedTextColor.GREEN),
								Component.text("Drop(Q) to delete", NamedTextColor.RED))
						.click(event -> {
							if (event.getClickType() == ClickType.DROP) {
								toEdit.slots.remove(pos);
								updateSlotData(row, column);
							} else if (event.getClickType().isLeftClick()) {
								type.createInstance(Context.background().withValue("payload", slotData)).createCamera()
										.open(event.getPlayer(), true);
							}
						}).build();
			});
		}

		private void update() {
			for (int row = 0; row < 6; row++) {
				for (int column = 0; column < 7; column++) {
					updateSlotData(row, column);
				}
			}
		}

		@Override
		public void onInitialize(CUIInstance<CUICreator> cui) {
			buttonLayer = new Layer(6, 7).edit().done();
			effectLayer = new Layer(6, 7).edit().done();
			slotsLayer = new Layer(6, 7).edit().marginLeft(7).done();
			this.cui = cui.edit().layer(0, new Layer(6, 2).edit().relative(true).marginLeft(7)
					.column(0, row -> Button.builder().material(Material.RAIL).displayName(" ").build())
					.column(1, row -> Button.builder().material(Material.AIR).build()).slot(0, 1, () -> Button.builder()
							.material(Material.COMMAND_BLOCK).displayName("Basic Settings").click(event -> {
								event.getCamera().edit().position(new Position(0, 0));
							}).build())
					.slot(1, 1,
							() -> Button.builder().material(Material.BOOKSHELF).displayName("Slot Settings")
									.lore(Component.text("Middle click to reset position", NamedTextColor.YELLOW),
											Component.text("Left click to move left"),
											Component.text("Right click to move right"),
											Component.text("Shift + Left click to move up"),
											Component.text("Shift + Right click to move down"))
									.click(event -> {
										position = switch (event.getClickType()) {
											case MIDDLE -> Position.ZERO;
											case LEFT -> position.add(0, -1);
											case RIGHT -> position.add(0, 1);
											case SHIFT_LEFT -> position.add(-1, 0);
											case SHIFT_RIGHT -> position.add(1, 0);
											default -> position;
										};
										position = position.clamp(0, 0, toEdit.maxRow - 1, toEdit.maxColumn - 1);
										update();

										event.getCamera().edit().position(new Position(0, 7));
									}).build())
					.done()).layer(1, effectLayer).layer(2, buttonLayer).layer(3, slotsLayer).done();

			// set relative
			setButton(0, (event, refresh) -> {
				toEdit.relative = !toEdit.relative;
				refresh.run();
			}, () -> Transformer.builder().changeItemStack(ItemStacks.builder()
					.material(toEdit.relative ? Material.STICKY_PISTON : Material.PISTON)
					.displayName(toComponent(toEdit.relative ? "Relative" : "Not Relative"))
					.lore(Component.text("Click to change", NamedTextColor.GREEN),
							Component.text("If relative, this layer will"), Component.text("be relative to the camera"))
					.build()).build());

			// set size
			setButton(1, (event, refresh) -> {
				var player = event.getPlayer();
				cui.getCUIPlugin().getTools().createAnvilTextInput(player, row -> {
					int rowSize;
					try {
						rowSize = Integer.parseInt(row);
					} catch (NumberFormatException e) {
						player.sendMessage(Component.text("Invalid number", NamedTextColor.RED));
						return;
					}
					if (rowSize <= 0) {
						player.sendMessage(Component.text("Row size must be greater than zero", NamedTextColor.RED));
						return;
					}
					cui.getCUIPlugin().getTools().createAnvilTextInput(player, column -> {
						int columnSize;
						try {
							columnSize = Integer.parseInt(column);
						} catch (NumberFormatException e) {
							player.sendMessage(Component.text("Invalid number", NamedTextColor.RED));
							return;
						}
						if (columnSize <= 0) {
							player.sendMessage(
									Component.text("Column size must be greater than zero", NamedTextColor.RED));
							return;
						}
						toEdit.maxRow = rowSize;
						toEdit.maxColumn = columnSize;
						refresh.run();
					}, Component.text("Column size (int,>0):")).open(player, true);
				}, Component.text("Row size (int,>0):")).open(player, true);
			}, () -> Transformer.builder()
					.changeItemStack(ItemStacks.builder().material(Material.CHEST)
							.displayName(toComponent("Size: ").append(
									Component.text(toEdit.maxRow + "*" + toEdit.maxColumn, NamedTextColor.AQUA)))
							.lore(Component.text("Click to edit size")).build())
					.build());

			// set margin
			setButton(2, (event, refresh) -> {
				var player = event.getPlayer();
				cui.getCUIPlugin().getTools().createAnvilTextInput(player, top -> {
					int marginTop;
					try {
						marginTop = Integer.parseInt(top);
					} catch (NumberFormatException e) {
						player.sendMessage(Component.text("Invalid number", NamedTextColor.RED));
						return;
					}
					if (marginTop < 0) {
						player.sendMessage(
								Component.text("Margin top must be greater than or equal to zero", NamedTextColor.RED));
						return;
					}
					cui.getCUIPlugin().getTools().createAnvilTextInput(player, left -> {
						int marginLeft;
						try {
							marginLeft = Integer.parseInt(left);
						} catch (NumberFormatException e) {
							player.sendMessage(Component.text("Invalid number", NamedTextColor.RED));
							return;
						}
						if (marginLeft < 0) {
							player.sendMessage(Component.text("Margin left must be greater than or equal to zero",
									NamedTextColor.RED));
							return;
						}
						toEdit.marginTop = marginTop;
						toEdit.marginLeft = marginLeft;
						refresh.run();
					}, Component.text("Margin left (int,>=0):")).open(player, true);
				}, Component.text("Margin top (int,>=0):")).open(player, true);
			}, () -> Transformer.builder()
					.changeItemStack(ItemStacks.builder().material(Material.TARGET)
							.displayName(toComponent("Margin: ").append(
									Component.text(toEdit.marginTop + "*" + toEdit.marginLeft, NamedTextColor.AQUA)))
							.lore(Component.text("Click to edit margin")).build())
					.build());

			// change depth
			setButton(3, (event, refresh) -> {
				var player = event.getPlayer();
				cui.getCUIPlugin().getTools().createAnvilTextInput(player, depth -> {
					int depthInt;
					try {
						depthInt = Integer.parseInt(depth);
					} catch (NumberFormatException e) {
						player.sendMessage(Component.text("Invalid number", NamedTextColor.RED));
						return;
					}
					if (depthInt < 0) {
						player.sendMessage(
								Component.text("Depth must be greater than or equal to zero", NamedTextColor.RED));
						return;
					}
					Integer oldDepth = cuiData.layers.inverse().remove(toEdit);
					LayerData oldLayer = cuiData.layers.put(depthInt, toEdit);
					if (oldDepth != null && oldLayer != null) {
						cuiData.layers.put(oldDepth, oldLayer);
					}
					refresh.run();
				}, Component.text("Depth (int,>=0):")).open(player, true);
			}, () -> Transformer.builder()
					.changeItemStack(ItemStacks.builder().material(Material.LADDER)
							.displayName(toComponent("Depth: ")
									.append(Component.text(cuiData.layers.inverse().get(toEdit), NamedTextColor.AQUA)))
							.lore(Component.text("Click to edit depth")).build())
					.build());
		}

		@Override
		public @NotNull CameraHandler<CUICreator> createCameraHandler(Context context) {
			return new CameraHandler<>() {
				@Override
				public void onInitialize(Camera<CUICreator> camera) {
					camera.edit().rowSize(6);
				}

				@Override
				public void onOpenInventory(Inventory inventory) {
					update();
				}
			};
		}
	}

	public class EditSlotHandler implements CUIInstanceHandler<CUICreator> {
		private CUIInstance<CUICreator> cui;
		private final SlotData toEdit;
		private Layer baseLayer;
		private Layer buttonLayer;
		private Layer effectLayer;
		private Layer typeLayer;
		private Layer onClickLayer;

		public EditSlotHandler(SlotData toEdit) {
			this.toEdit = toEdit;
		}

		private void setButton(int index, BiConsumer<CUIClickEvent<?>, Runnable> onClick,
				Supplier<Transformer> transformer) {
			Runnable setEffect = () -> effectLayer.edit().slot(0, index, transformer::get);
			buttonLayer.edit().slot(0, index, () -> Button.builder().click(cuiClickEvent -> {
				onClick.accept(cuiClickEvent, setEffect);
			}).build());
			setEffect.run();
		}

		private void update() {
			baseLayer.edit().slot(1, 1, () -> Button.builder().itemStack(toEdit.getDemo()).build());
			typeLayer.edit().slot(2, 1, () -> Button.builder()
					.material(toEdit.type == SlotData.SlotType.EMPTY ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
					.displayName("").build());
			typeLayer.edit().slot(2, 3, () -> Button.builder()
					.material(toEdit.type == SlotData.SlotType.BUTTON ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
					.displayName("").build());
			typeLayer.edit().slot(2, 5,
					() -> Button.builder().material(
							toEdit.type == SlotData.SlotType.STORAGE ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
							.displayName("").build());
			typeLayer.edit().slot(2, 7, () -> Button.builder().material(
					toEdit.type == SlotData.SlotType.TRANSFORMER ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
					.displayName("").build());
		}

		private void updateEditingOnClick(ClickType clickType) {
			var onClicks = toEdit.onClicks.computeIfAbsent(clickType, key -> new ArrayList<>());
			var size = onClicks.size();
			onClickLayer.edit().clear().tile(size + 1, false, index -> {
				if (index == size) {
					return Button.builder().material(Material.GREEN_CONCRETE).displayName("Add new action")
							.click(event -> {
								var action = new SlotData.OnClick();
								onClicks.add(action);
								updateEditingOnClick(clickType);
							}).build();
				} else {
					SlotData.OnClick onClick = onClicks.get(index);
					var material = switch (onClick.action) {
						case COMMAND -> Material.COMMAND_BLOCK;
						case COMMAND_OP -> Material.CHAIN_COMMAND_BLOCK;
						case COMMAND_CONSOLE -> Material.REPEATING_COMMAND_BLOCK;
						case CHEST_UI -> Material.CHEST;
						case null -> Material.BARRIER;
					};
					var explain = switch (onClick.action) {
						case COMMAND -> "Run command as viewer (don't type /)";
						case COMMAND_OP -> "Run command as viewer with OP permission";
						case COMMAND_CONSOLE -> "Run command as console";
						case CHEST_UI -> "Chest UI relative operations";
						case null -> "Unknown action";
					};
					return Button.builder().material(material)
							.displayName("Action " + index + " Triggered by " + clickType.name())
							.lore(Component.text("Left click to change action", NamedTextColor.GREEN),
									Component.text("Right click to set value", NamedTextColor.GREEN),
									Component.text("Drop(Q) to delete", NamedTextColor.RED),
									Component.text("Current action: ")
											.append(Component.text(onClick.action.name(), NamedTextColor.AQUA)),
									Component.text(explain, NamedTextColor.YELLOW),
									Component.text("Current value: ")
											.append(Component.text(onClick.value, NamedTextColor.AQUA)))
							.click(event -> {
								if (event.getClickType().isLeftClick()) {
									var actions = SlotData.OnClick.Action.values();
									onClick.action = actions[(onClick.action.ordinal() + 1) % actions.length];
								} else if (event.getClickType().isRightClick()) {
									var player = event.getPlayer();
									cui.getCUIPlugin().getTools().createAnvilTextInput(player, value -> {
										onClick.value = value;
										updateEditingOnClick(clickType);
									}, Component.text("Set value")).open(player, true);
								} else if (event.getClickType() == ClickType.DROP) {
									onClicks.remove(onClick);
									updateEditingOnClick(clickType);
								}
								updateEditingOnClick(clickType);
							}).build();
				}
			});
		}

		@Override
		public void onInitialize(CUIInstance<CUICreator> cui) {
			baseLayer = new Layer(3, 9).edit().relative(true).row(0,
					column -> Button.builder().material(Material.GRAY_STAINED_GLASS_PANE).displayName(" ").build()).row(
							2,
							column -> Button.builder().material(Material.GRAY_STAINED_GLASS_PANE).displayName(" ")
									.build())
					.rect(0, 0, 3, 3, (row, column) -> Button.builder().material(Material.BLACK_STAINED_GLASS_PANE)
							.displayName(" ").build())
					.done();
			buttonLayer = new Layer(1, 6).edit().relative(true).marginTop(1).marginLeft(3)
					.slot(0, 0,
							() -> Button.builder().material(Material.ITEM_FRAME)
									.displayName(Component.text("Slot Type", NamedTextColor.GOLD))
									.lore(Component.text("Click to select")).click(cuiClickEvent -> {
										cuiClickEvent.getCamera().edit().position(new Position(0, 9));
									}).build())
					.slot(0, 5,
							() -> Button.builder().material(Material.ITEM_FRAME)
									.displayName(Component.text("On Click", NamedTextColor.GOLD))
									.lore(Component.text("The way you click this slot"),
											Component.text("will be the way those actions are triggered"))
									.click(cuiClickEvent -> {
										updateEditingOnClick(cuiClickEvent.getClickType());
										cuiClickEvent.getCamera().edit().position(new Position(0, 18));
									}).build())
					.done();
			effectLayer = new Layer(1, 6).edit().relative(true).marginTop(1).marginLeft(3).done();
			typeLayer = new Layer(3, 9).edit().marginTop(3).marginLeft(9).slot(1, 1,
					() -> Button.builder().material(Material.STRUCTURE_VOID).displayName("Empty").click(event -> {
						toEdit.type = SlotData.SlotType.EMPTY;
						update();
					}).build()).slot(1, 3,
							() -> Button.builder().material(Material.OAK_BUTTON).displayName("Button").click(event -> {
								toEdit.type = SlotData.SlotType.BUTTON;
								update();
							}).build())
					.slot(1, 5, () -> Button.builder().material(Material.CHEST).displayName("Storage").click(event -> {
						toEdit.type = SlotData.SlotType.STORAGE;
						update();
					}).build()).slot(1, 7, () -> Button.builder().material(Material.ENCHANTING_TABLE)
							.displayName("Transformer").click(event -> {
								toEdit.type = SlotData.SlotType.TRANSFORMER;
								update();
							}).build())
					.done();
			onClickLayer = new Layer(3, 9).edit().marginTop(3).marginLeft(18).done();

			this.cui = cui.edit().layer(0, baseLayer).layer(1, effectLayer).layer(2, buttonLayer).layer(3, typeLayer)
					.layer(4, onClickLayer).done();

			// material
			setButton(1, (event, refresh) -> {
				if (event.getClickType() == ClickType.DROP) {
					toEdit.material = null;
					refresh.run();
					update();
					return;
				}
				ItemStack cursor = event.getCursor();
				if (ItemStacks.isEmpty(cursor)) {
					return;
				}
				var material = cursor.getType();
				if (material == Material.AIR) {
					return;
				}
				toEdit.material = material;
				refresh.run();
				update();
			}, () -> Transformer.builder()
					.changeItemStack(ItemStacks.builder().itemStack(toEdit.getDemo()).displayName("Material")
							.lore(Component.text("Click with item in cursor to change", NamedTextColor.GREEN),
									Component.text("Drop(Q) to set AIR", NamedTextColor.RED))
							.build())
					.build());

			// display name
			setButton(2, (event, refresh) -> {
				var player = event.getPlayer();
				cui.getCUIPlugin().getTools().createAnvilTextInput(player, name -> {
					toEdit.displayName = name;
					refresh.run();
					update();
				}, Component.text("Edit display name")).open(player, true);
			}, () -> Transformer.builder()
					.changeItemStack(ItemStacks.builder().material(Material.NAME_TAG)
							.displayName(toComponent("Display Name: ").append(toComponent(toEdit.displayName)))
							.lore(Component.text("Click to edit display name")).build())
					.build());

			// lore
			setButton(3, (event, refresh) -> {
				var player = event.getPlayer();
				if (event.getClickType() == ClickType.DROP) {
					toEdit.lore = null;
					refresh.run();
					update();
					return;
				}
				cui.getCUIPlugin().getTools().createAnvilTextInput(player, lore -> {
					if (toEdit.lore == null) {
						toEdit.lore = new ArrayList<>();
					}
					toEdit.lore.add(lore);
					refresh.run();
					update();
				}, Component.text("Edit lore")).open(player, true);
			}, () -> {
				var lore = new LinkedList<Component>(
						List.of(Component.text("Click to append lore", NamedTextColor.GREEN),
								Component.text("Drop(Q) to reset", NamedTextColor.RED),
								Component.text("Current lore:", NamedTextColor.GRAY)));
				if (toEdit.lore != null) {
					lore.addAll(toEdit.lore.stream().map(CUICreator.this::toComponent).toList());
				}
				return Transformer.builder()
						.changeItemStack(
								ItemStacks.builder().material(Material.PAPER).displayName("Lore").lore(lore).build())
						.build();
			});

			// amount
			setButton(4, (event, refresh) -> {
				var player = event.getPlayer();
				cui.getCUIPlugin().getTools().createAnvilTextInput(player, amount -> {
					int amountInt;
					try {
						amountInt = Integer.parseInt(amount);
					} catch (NumberFormatException e) {
						player.sendMessage(Component.text("Invalid number", NamedTextColor.RED));
						return;
					}
					if (amountInt <= 0) {
						player.sendMessage(Component.text("Amount must be greater than zero", NamedTextColor.RED));
						return;
					}
					toEdit.amount = amountInt;
					refresh.run();
					update();
				}, Component.text("Amount (int,>0):")).open(player, true);
			}, () -> Transformer.builder()
					.changeItemStack(ItemStacks.builder().material(Material.REPEATER).amount(toEdit.amount)
							.displayName(
									toComponent("Amount: ").append(Component.text(toEdit.amount, NamedTextColor.AQUA)))
							.lore(Component.text("Click to edit amount")).build())
					.build());
		}

		@Override
		public @NotNull CameraHandler<CUICreator> createCameraHandler(Context context) {
			return new CameraHandler<>() {
				@Override
				public void onInitialize(Camera<CUICreator> camera) {
					camera.edit().rowSize(6);
				}

				@Override
				public void onOpenInventory(Inventory inventory) {
					update();
				}
			};
		}
	}
}
