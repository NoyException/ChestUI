package fun.polyvoxel.cui.prebuilt.cui;

import fun.polyvoxel.cui.event.CUIRegisterEvent;
import fun.polyvoxel.cui.layer.Layer;

import fun.polyvoxel.cui.ui.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@CUI(name = "monitor", singleton = true, icon = Material.BARRIER)
public class CUIMonitor implements ChestUI<CUIMonitor> {

	@Override
	public void onInitialize(CUIType<CUIMonitor> type) {
		type.edit().defaultTitle(Component.text("CUI Monitor", NamedTextColor.RED)).triggerByDisplayCommand(
				player -> new CUIType.TriggerResult<>(CUIType.TriggerResultType.CREATE_NEW_CAMERA, camera -> {
				}));
	}

	@Override
	public @NotNull ChestUI.InstanceHandler<CUIMonitor> createInstanceHandler() {
		return new InstanceHandler();
	}

	private static class InstanceHandler implements ChestUI.InstanceHandler<CUIMonitor> {
		private CUIInstance<CUIMonitor> cui;
		private int size;

		@Override
		public void onInitialize(CUIInstance<CUIMonitor> cui) {
			this.cui = cui.edit().keepAlive(true).finish();
			refreshCUIList();
			Bukkit.getPluginManager().registerEvents(new Listener() {
				@EventHandler
				public void onRegister(CUIRegisterEvent<?> event) {
					refreshCUIList();
				}
			}, cui.getCUIPlugin());
		}

		private void refreshCUIList() {
			var cuiTypes = cui.getCUIPlugin().getCUIManager().getRegisteredCUITypes();
			size = cuiTypes.size();

			cui.edit().layer(1, new Layer(0, 9).edit().marginTop(1).tile(size, true, (slotHandler, index) -> {
				var cuiType = cuiTypes.get(index);
				if (cuiType.getChestUI().getClass() == CUIMonitor.class) {
					slotHandler.button(builder -> builder.material(Material.BARRIER)
							.displayName(Component.text(cuiType.getKey().toString(), NamedTextColor.RED))
							.lore("&7A CUI Monitor like" + " this", "&cCannot be monitored").build());
				} else {
					var lore = new ArrayList<Component>(
							List.of(Component.text("Left click to manage"), Component.text("Right click to display")));
					if (cuiType.isSingleton()) {
						lore.add(Component.text("- ", NamedTextColor.GRAY)
								.append(Component.text("Singleton", NamedTextColor.RED)));
					}
					if (cuiType.isSerializable()) {
						lore.add(Component.text("- ", NamedTextColor.GRAY)
								.append(Component.text("Loaded from json", NamedTextColor.GREEN)));
					} else if (cuiType.getPlugin() != null) {
						lore.add(Component.text("- ", NamedTextColor.GRAY)
								.append(Component.text("Registered by plugin: "))
								.append(Component.text(cuiType.getPlugin().getName(), NamedTextColor.AQUA)));
					}
					lore.add(
							Component.text("- Default title: ", NamedTextColor.GRAY).append(cuiType.getDefaultTitle()));
					slotHandler.button(builder -> builder.material(cuiType.getIcon())
							.displayName(Component.text(cuiType.getKey().toString(), NamedTextColor.GOLD)).lore(lore)
							.clickHandler(event -> {
								Player player = event.getPlayer();
								if (event.getClickType().isLeftClick()) {
									var camera = cui.createCamera().edit()
											.title(Component.text("Managing ").append(
													Component.text(cuiType.getKey().toString(), NamedTextColor.AQUA)))
											.finish();
									var handler = (CameraHandler) camera.getHandler();
									handler.cuiType = cuiType;
									handler.refresh();
									camera.open(player, true);
								} else if (event.getClickType().isRightClick()) {
									cuiType.display(player, true);
								}
							}).build());
				}
			}).finish()).finish();
		}

		@Override
		public void onTick() {
			if (cui.getTicksLived() % 4 != 0) {
				return;
			}
			refreshCUIList();
		}

		@Override
		public @NotNull ChestUI.CameraHandler<CUIMonitor> createCameraHandler() {
			return new CameraHandler();
		}

		private class CameraHandler implements ChestUI.CameraHandler<CUIMonitor> {
			private Camera<CUIMonitor> camera;
			private CUIType<?> cuiType;
			private CUIInstance<?> cuiInstance;
			private int size;

			@Override
			public void onInitialize(Camera<CUIMonitor> camera) {
				this.camera = camera.edit().rowSize(6)
						.layer(0,
								new Layer(1, 9).edit().relative(true)
										.editAll((slotHandler, row, column) -> slotHandler.button(builder -> builder
												.material(Material.BLACK_STAINED_GLASS_PANE).displayName(" ").build()))
										.editSlot(0, 0,
												slotHandler -> slotHandler.button(
														builder -> builder.material(Material.RED_STAINED_GLASS_PANE)
																.displayName("Previous").clickHandler(event -> {
																	var position = camera.getPosition();
																	if (position.row() <= 0)
																		return;
																	camera.edit().move(-5, 0);
																}).build()))
										.editSlot(0, 8,
												slotHandler -> slotHandler.button(
														builder -> builder.material(Material.GREEN_STAINED_GLASS_PANE)
																.displayName("Next").clickHandler(event -> {
																	var position = camera.getPosition();
																	if ((position.row() / 5 + 1) * 45 >= size)
																		return;
																	camera.edit().move(5, 0);
																}).build()))
										.finish())
						.finish();
			}

			private void manageCUI(CUIType<?> cuiType) {
				var instances = cuiType.getInstances();
				size = instances.size() + 1;
				camera.edit().layer(1, new Layer(0, 9).edit().marginTop(1).tile(size, true, (slotHandler, index) -> {
					if (index == 0) {
						if (cuiType.isSingleton() && size > 1) {
							slotHandler.button(builder -> builder.material(Material.RED_CONCRETE)
									.displayName(Component.text("New instance", NamedTextColor.RED)).lore(Component
											.text("Singleton ChestUI can only have one instance", NamedTextColor.RED))
									.build());
						} else {
							slotHandler.button(builder -> builder.material(Material.GREEN_CONCRETE)
									.displayName(Component.text("New instance", NamedTextColor.GREEN))
									.lore(Component.text("Click to create a new instance"),
											Component.text("Noticed that this will be kept alive", NamedTextColor.RED),
											Component.text("until you destroy it manually", NamedTextColor.RED))
									.clickHandler(event -> cuiType.createInstance().edit().keepAlive(true)).build());
						}
						return;
					}
					var instance = instances.get(index - 1);
					var lore = new ArrayList<Component>(List.of(Component.text("Left click to manage"),
							Component.text("Right click to destroy", NamedTextColor.RED)));
					if (instance.isKeepAlive()) {
						lore.add(Component.text("- ", NamedTextColor.GRAY)
								.append(Component.text("Keep Alive", NamedTextColor.RED)));
					}
					lore.addAll(List.of(
							Component.text("- State: ", NamedTextColor.GRAY)
									.append(Component.text(instance.getState().name(), NamedTextColor.AQUA)),
							Component.text("- Default title: ", NamedTextColor.GRAY)
									.append(instance.getType().getDefaultTitle()),
							Component.text("- Current camera count: ", NamedTextColor.GRAY)
									.append(Component.text(instance.getCameraCount(), NamedTextColor.AQUA)),
							Component.text("- Ticks lived: ", NamedTextColor.GRAY)
									.append(Component.text(instance.getTicksLived(), NamedTextColor.AQUA))));
					slotHandler.button(builder -> builder.material(cuiType.getIcon())
							.displayName(Component.text("#" + instance.getId(), NamedTextColor.GOLD)).lore(lore)
							.clickHandler(event -> {
								if (event.getClickType().isLeftClick()) {
									var camera = cui.createCamera().edit()
											.title(Component.text("Managing ")
													.append(Component.text(instance.getName(), NamedTextColor.AQUA)))
											.finish();
									var handler = (CameraHandler) camera.getHandler();
									handler.cuiInstance = instance;
									handler.refresh();
									camera.open(event.getPlayer(), true);
								} else if (event.getClickType().isRightClick()) {
									instance.destroy();
								}
							}).build());
				}).finish());
			}

			private void manageCUIInstance(CUIInstance<?> instance) {
				var cameras = instance.getCameras();
				size = cameras.size() + 1;
				camera.edit().layer(1, new Layer(0, 9).edit().marginTop(1).tile(size, true, (slotHandler, index) -> {
					if (index == 0) {
						slotHandler.button(builder -> builder.material(Material.GREEN_CONCRETE)
								.displayName(Component.text("New camera", NamedTextColor.GREEN))
								.lore(Component.text("Click to create a new camera"),
										Component.text("Noticed that this will be kept alive", NamedTextColor.RED),
										Component.text("until you destroy it manually", NamedTextColor.RED))
								.clickHandler(event -> instance.createCamera().edit().keepAlive(true)).build());
						return;
					}
					var camera = cameras.get(index - 1);
					var lore = new ArrayList<Component>(List.of(Component.text("Left click to open"),
							Component.text("Right click to destroy", NamedTextColor.RED)));
					if (camera.isKeepAlive()) {
						lore.add(Component.text("- ", NamedTextColor.GRAY)
								.append(Component.text("Keep Alive", NamedTextColor.RED)));
					}
					if (!camera.isClosable()) {
						lore.add(Component.text("- ", NamedTextColor.GRAY)
								.append(Component.text("Not Closable", NamedTextColor.RED)));
					}
					lore.addAll(List.of(
							Component.text("- State: ", NamedTextColor.GRAY)
									.append(Component.text(camera.getState().name(), NamedTextColor.AQUA)),
							Component.text("- Title: ", NamedTextColor.GRAY).append(camera.getTitle()),
							Component.text("- Current viewer count: ", NamedTextColor.GRAY)
									.append(Component.text(camera.getViewerCount(), NamedTextColor.AQUA)),
							Component.text("- Ticks lived: ", NamedTextColor.GRAY)
									.append(Component.text(camera.getTicksLived(), NamedTextColor.AQUA))));

					slotHandler.button(builder -> builder.material(Material.SPYGLASS)
							.displayName(Component.text("#" + camera.getId(), NamedTextColor.GOLD)).lore(lore)
							.clickHandler(event -> {
								if (event.getClickType().isLeftClick()) {
									camera.open(event.getPlayer(), true);
								} else if (event.getClickType().isRightClick()) {
									camera.destroy();
								}
							}).build());
				}).finish());
			}

			private void refresh() {
				if (cuiType != null) {
					manageCUI(cuiType);
				} else if (cuiInstance != null) {
					manageCUIInstance(cuiInstance);
				} else {
					size = InstanceHandler.this.size;
				}
			}

			@Override
			public void onTick() {
				if (camera.getTicksLived() % 4 != 0) {
					return;
				}
				if (camera.getViewerCount() == 0) {
					return;
				}
				refresh();
			}
		}
	}
}
