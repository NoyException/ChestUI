package fun.polyvoxel.cui.prebuilt;

import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class InventoryMonitorTest {
	// TODO: 因为MockBukkit还未支持放置物品，故无法测试放置物品的情况
	// private static ServerMock server;
	// private static CUIPlugin plugin;
	// private static PlayerMock a, b;
	// private static PlayerSimulation as, bs;
	// private static CUIInstance<PlayerMonitor> cui;
	// private static Camera<PlayerMonitor> camera;
	//
	// @BeforeAll
	// public static void setup() {
	// server = MockBukkit.mock();
	// plugin = MockBukkit.load(CUIPlugin.class);
	// a = server.addPlayer("a");
	// cui =
	// plugin.getCUIManager().getCUIType(PlayerMonitor.class).createInstance();
	// Assertions.assertNotNull(cui);
	// camera = cui.createCamera();
	// Assertions.assertTrue(camera.open(a, new InternalDisplaySource(), false),
	// "应当能打开默认摄像头");
	// server.getScheduler().performOneTick();
	// as = new PlayerSimulation(a);
	// InventoryView view = a.getOpenInventory();
	// view.getBottomInventory().setItem(0, ItemStack.of(Material.DIRT, 1));
	// view.getBottomInventory().setItem(1, ItemStack.of(Material.STONE, 64));
	// // 选择自己
	// as.simulateInventoryClick(view, ClickType.LEFT, 9);
	// b = server.addPlayer("b");
	// Assertions.assertTrue(camera.open(b, new InternalDisplaySource(), false),
	// "应当能打开默认摄像头");
	// bs = new PlayerSimulation(b);
	// server.getScheduler().performOneTick();
	// }
	//
	// @AfterAll
	// public static void teardown() {
	// MockBukkit.unmock();
	// }
	//
	// private void tick() {
	// server.getScheduler().performOneTick();
	// plugin.getCUIManager().onTickEnd(null);
	// }

	// @Test
	// public void testLeftClick() {
	// // A从CUI捡起泥土
	// as.simulateInventoryClick(a.getOpenInventory(), ClickType.LEFT, 45);
	// ItemStackAssertions.assertEmpty(a.getOpenInventory().getBottomInventory().getItem(0)),
	// "物品应当已被捡起");
	// // A将泥土放入CUI第二排第一格，应当同步放入A的背包的左上角
	// as.simulateInventoryClick(a.getOpenInventory(), ClickType.LEFT, 9);
	// server.getScheduler().performOneTick();
	// ItemStackAssertions.assertSame(ItemStack.of(Material.DIRT, 1),
	// a.getOpenInventory().getBottomInventory().getItem(9)),
	// "物品应当已被放入CUI，并同步放入a的背包");
	// // B将泥土从CUI左上角放到自己背包里
	// bs.simulateInventoryClick(b.getOpenInventory(), ClickType.LEFT, 9);
	// bs.simulateInventoryClick(b.getOpenInventory(), ClickType.LEFT, 81);
	// server.getScheduler().performOneTick();
	// printInventory(b.getOpenInventory());
	// ItemStackAssertions.assertSame(ItemStack.of(Material.DIRT, 1),
	// b.getOpenInventory().getBottomInventory().getItem(0)), "物品应当已被放入b的背包");
	// ItemStackAssertions.assertEmpty(a.getOpenInventory().getBottomInventory().getItem(9)),
	// "物品应当已被捡起");
	// ItemStackAssertions.assertEmpty(a.getOpenInventory().getItem(9)),
	// "物品应当已被捡起");
	// // B将泥土放回CUI左下角
	// bs.simulateInventoryClick(b.getOpenInventory(), ClickType.LEFT, 81);
	// bs.simulateInventoryClick(b.getOpenInventory(), ClickType.LEFT, 45);
	// server.getScheduler().performOneTick();
	// ItemStackAssertions.assertSame(ItemStack.of(Material.DIRT, 1),
	// a.getOpenInventory().getBottomInventory().getItem(0)), "物品应当已被放入a的背包");
	// }

	private void printInventory(InventoryView view) {
		for (int i = 0; i < view.countSlots(); i++) {
			ItemStack item = view.getItem(i);
			System.out.println(i + "(" + view.convertSlot(i) + "):\t" + (item == null ? "null" : item.getType()));
		}
	}
}
