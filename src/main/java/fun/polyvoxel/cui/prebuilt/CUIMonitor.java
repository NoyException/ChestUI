package fun.polyvoxel.cui.prebuilt;

import fun.polyvoxel.cui.event.CUIRegisterEvent;
import fun.polyvoxel.cui.layer.Layer;

import fun.polyvoxel.cui.slot.Button;
import fun.polyvoxel.cui.ui.*;
import fun.polyvoxel.cui.util.context.Context;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CUI(name = "monitor", icon = Material.BARRIER)
public class CUIMonitor implements ChestUI<CUIMonitor> {
	private CUIType<CUIMonitor> type;
	private CUIInstance<CUIMonitor> mainInstance;
	private final Map<CUIType<?>, CUIInstance<CUIMonitor>> byType = new HashMap<>();
	private final Map<CUIInstance<?>, CUIInstance<CUIMonitor>> byInst = new HashMap<>();

	@Override
	public void onInitialize(CUIType<CUIMonitor> type) {
		this.type = type.edit().defaultTitle(Component.text("CUI Monitor", NamedTextColor.RED))
				.triggerByDisplay((cuiType, player, asChild) -> getMainInstance().createCamera()).done();
	}

	@Override
	public @NotNull CUIInstanceHandler<CUIMonitor> createCUIInstanceHandler(Context context) {
		CUIType<?> cuiType = context.get("cuiType");
		CUIInstance<?> cuiInstance = context.get("cuiInstance");
		if (cuiType != null) {
			return new MonitorCUITypeHandler(cuiType);
		}
		if (cuiInstance != null) {
			return new MonitorCUIInstanceHandler(cuiInstance);
		}
		return new MainHandler();
	}

	private CUIInstance<CUIMonitor> getMainInstance() {
		if (mainInstance == null) {
			mainInstance = type.createInstance();
		}
		return mainInstance;
	}

	private CUIInstance<CUIMonitor> getByCUIType(CUIType<?> type) {
		return byType.computeIfAbsent(type,
				t -> this.type.createInstance(Context.background().withValue("cuiType", t)));
	}

	private CUIInstance<CUIMonitor> getByCUIInstance(CUIInstance<?> instance) {
		return byInst.computeIfAbsent(instance,
				i -> this.type.createInstance(Context.background().withValue("cuiInstance", i)));
	}

	private abstract static class InstanceHandler implements CUIInstanceHandler<CUIMonitor> {
		protected CUIInstance<CUIMonitor> cui;
		protected int size;

		@Override
		public void onInitialize(CUIInstance<CUIMonitor> cui) {
			this.cui = cui.edit()
					.layer(0,
							new Layer(1, 9).edit().relative(true)
									.all((row, column) -> Button.builder().material(Material.BLACK_STAINED_GLASS_PANE)
											.displayName(" ").build())
									.slot(0, 0, () -> Button.builder().material(Material.RED_STAINED_GLASS_PANE)
											.displayName("Previous").clickHandler(event -> {
												var position = event.getCamera().getPosition();
												if (position.row() <= 0)
													return;
												event.getCamera().edit().move(-5, 0);
											}).build())
									.slot(0, 8, () -> Button.builder().material(Material.GREEN_STAINED_GLASS_PANE)
											.displayName("Next").clickHandler(event -> {
												var position = event.getCamera().getPosition();
												if ((position.row() / 5 + 1) * 45 >= size)
													return;
												event.getCamera().edit().move(5, 0);
											}).build())
									.done())
					.done();
			refresh();
		}

		@Override
		public @NotNull CameraHandler<CUIMonitor> createCameraHandler(Context context) {
			return camera -> camera.edit().rowSize(6);
		}

		public abstract void refresh();

		@Override
		public void onTick() {
			if (cui.getTicksLived() % 4 != 0) {
				return;
			}
			refresh();
		}
	}

	private class MainHandler extends InstanceHandler {
		@Override
		public void onInitialize(CUIInstance<CUIMonitor> cui) {
			super.onInitialize(cui);
			Bukkit.getPluginManager().registerEvents(new Listener() {
				@EventHandler
				public void onRegister(CUIRegisterEvent<?> event) {
					refresh();
				}
			}, cui.getCUIPlugin());
		}

		@Override
		public void onDestroy() {
			mainInstance = null;
		}

		@Override
		public void refresh() {
			var cuiTypes = cui.getCUIPlugin().getCUIManager().getRegisteredCUITypes();
			size = cuiTypes.size();
			cuiTypes.remove(type);

			cui.edit().layer(1, new Layer(0, 9).edit().marginTop(1).tile(size, true, index -> {
				if (index == 0) {
					return Button.builder().material(Material.WRITTEN_BOOK)
							.displayName(Component.text("About", NamedTextColor.GOLD))
							.lore(Component.text("This is a monitor for ChestUI"),
									Component.text("You can manage all registered CUI types,"),
									Component.text("instances and cameras here"),
									Component.text("---", NamedTextColor.GRAY),
									Component.text("Developed by PolyVoxel", NamedTextColor.GREEN),
									Component.text("Maintained by NoyException", NamedTextColor.GREEN),
									Component.text("GitHub: https://github.com/PolyVoxel/ChestUI", NamedTextColor.BLUE))
							.build();
				}
				var cuiType = cuiTypes.get(index - 1);
				var lore = new ArrayList<Component>(List.of(Component.text("Left click to manage")));
				if (cuiType.canDisplay()) {
					lore.add(Component.text("Right click to display", NamedTextColor.GREEN));
				}
				if (cuiType.isSingleton()) {
					lore.add(Component.text("- ", NamedTextColor.GRAY)
							.append(Component.text("Singleton", NamedTextColor.RED)));
				}
				if (cuiType.isSerializable()) {
					lore.add(Component.text("- ", NamedTextColor.GRAY)
							.append(Component.text("Loaded from json", NamedTextColor.BLUE)));
				} else if (cuiType.getPlugin() != null) {
					lore.add(Component.text("- Registered by plugin: ", NamedTextColor.GRAY)
							.append(Component.text(cuiType.getPlugin().getName(), NamedTextColor.AQUA)));
				}
				lore.addAll(List.of(
						Component.text("- Default title: ", NamedTextColor.GRAY).append(cuiType.getDefaultTitle()),
						Component.text("- Current instance count: ", NamedTextColor.GRAY)
								.append(Component.text(cuiType.getInstanceCount(), NamedTextColor.AQUA))));
				return Button.builder().material(cuiType.getIcon())
						.displayName(Component.text(cuiType.getKey().toString(), NamedTextColor.GOLD)).lore(lore)
						.clickHandler(event -> {
							Player player = event.getPlayer();
							if (event.getClickType().isLeftClick()) {
								getByCUIType(cuiType).createCamera().open(player, true);
							} else if (event.getClickType().isRightClick()) {
								if (cuiType.canDisplay()) {
									cuiType.display(player, true);
								}
							}
						}).build();
			}).done()).done();
		}
	}

	private class MonitorCUITypeHandler extends InstanceHandler {
		private final CUIType<?> cuiType;

		private MonitorCUITypeHandler(CUIType<?> cuiType) {
			this.cuiType = cuiType;
		}

		@Override
		public void onInitialize(CUIInstance<CUIMonitor> cui) {
			super.onInitialize(cui);
			cui.edit().defaultTitle(Component.text("Managing " + cuiType.getKey(), NamedTextColor.AQUA)).done();
		}

		@Override
		public void onDestroy() {
			byType.remove(cuiType);
		}

		@Override
		public void refresh() {
			var instances = cuiType.getInstances();
			size = instances.size() + 1;
			cui.edit().layer(1, new Layer(0, 9).edit().marginTop(1).tile(size, true, index -> {
				if (index == 0) {
					if (cuiType.isSingleton() && size > 1) {
						return Button.builder().material(Material.RED_CONCRETE)
								.displayName(Component.text("New instance", NamedTextColor.RED)).lore(Component
										.text("Singleton ChestUI can only have one instance", NamedTextColor.RED))
								.build();
					}
					return Button.builder().material(Material.GREEN_CONCRETE)
							.displayName(Component.text("New instance", NamedTextColor.GREEN))
							.lore(Component.text("Click to create a new instance"),
									Component.text("Noticed that this will be kept alive", NamedTextColor.RED),
									Component.text("until you destroy it manually", NamedTextColor.RED))
							.clickHandler(event -> cuiType.createInstance().edit().keepAlive(true)).build();
				}
				var instance = instances.get(index - 1);
				var lore = new ArrayList<Component>(List.of(Component.text("Left click to manage"),
						Component.text("Drop(Q) to destroy", NamedTextColor.RED)));
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
				return Button.builder().material(cuiType.getIcon())
						.displayName(Component.text("#" + instance.getId(), NamedTextColor.GOLD)).lore(lore)
						.clickHandler(event -> {
							if (event.getClickType().isLeftClick()) {
								getByCUIInstance(instance).createCamera().open(event.getPlayer(), true);
							} else if (event.getClickType() == ClickType.DROP) {
								instance.destroy();
							}
						}).build();
			}).done());
		}
	}

	private class MonitorCUIInstanceHandler extends InstanceHandler {
		private final CUIInstance<?> cuiInstance;

		private MonitorCUIInstanceHandler(CUIInstance<?> cuiInstance) {
			this.cuiInstance = cuiInstance;
		}

		@Override
		public void onInitialize(CUIInstance<CUIMonitor> cui) {
			super.onInitialize(cui);
			cui.edit().defaultTitle(Component.text("Managing " + cuiInstance.getName(), NamedTextColor.AQUA)).done();
		}

		@Override
		public void onDestroy() {
			byInst.remove(cuiInstance);
		}

		@Override
		public void refresh() {
			var cameras = cuiInstance.getCameras();
			size = cameras.size() + 1;
			cui.edit().layer(1, new Layer(0, 9).edit().marginTop(1).tile(size, true, index -> {
				if (index == 0) {
					return Button.builder().material(Material.GREEN_CONCRETE)
							.displayName(Component.text("New camera", NamedTextColor.GREEN))
							.lore(Component.text("Click to create a new camera"),
									Component.text("Noticed that this will be kept alive", NamedTextColor.RED),
									Component.text("until you destroy it manually", NamedTextColor.RED))
							.clickHandler(event -> cuiInstance.createCamera().edit().keepAlive(true)).build();
				}
				var camera = cameras.get(index - 1);
				var lore = new ArrayList<Component>(List.of(Component.text("Left click to open"),
						Component.text("Drop(Q) to destroy", NamedTextColor.RED)));
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

				return Button.builder().material(Material.SPYGLASS)
						.displayName(Component.text("#" + camera.getId(), NamedTextColor.GOLD)).lore(lore)
						.clickHandler(event -> {
							if (event.getClickType().isLeftClick()) {
								camera.open(event.getPlayer(), true);
							} else if (event.getClickType() == ClickType.DROP) {
								camera.destroy();
							}
						}).build();
			}).done());
		}

		@Override
		public void onTickStart() {
			if (cuiInstance.getState() == CUIInstance.State.DESTROYED) {
				cui.destroy();
			}
		}
	}
}
