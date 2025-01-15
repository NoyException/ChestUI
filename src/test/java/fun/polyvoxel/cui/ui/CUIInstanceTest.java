package fun.polyvoxel.cui.ui;

import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.layer.Layer;
import fun.polyvoxel.cui.slot.Button;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

//TODO: 由于MockBukkit尚未有setItemModel，故测试永远失败，先注释掉
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CUIInstanceTest {
	private static ServerMock server;
	private static CUIPlugin plugin;
	private static PlayerMock a, b;
	private static CUIInstance<TestCUI> cui;
	private static Camera<TestCUI> camera1, camera2;

	@BeforeAll
	public static void setup() {
		server = MockBukkit.mock();
		plugin = MockBukkit.load(CUIPlugin.class);
		plugin.getCUIManager().registerCUI(TestCUI.class, plugin, "test", false, Material.CHEST);
		a = server.addPlayer("a");
		b = server.addPlayer("b");
		// 因为mock的player的getTopInventory会返回null，故手动先打开一个inventory
		a.openInventory(a.getEnderChest());
		b.openInventory(b.getEnderChest());
		// server.getServerTickManager().setFrozen(true);
	}

	@AfterAll
	public static void teardown() {
		MockBukkit.unmock();
	}

	private void tick() {
		server.getScheduler().performOneTick();
		plugin.getCUIManager().onTickEnd(null);
	}

	// @Test
	@Order(1)
	public void testCreate() {
		cui = plugin.getCUIManager().getCUIType(TestCUI.class).createInstance(new TestCUI.InstanceHandler());
		Assertions.assertNotNull(cui);
	}

	// @Test
	@Order(2)
	public void testOpen() {
		camera1 = cui.createCamera(camera -> {
		}).edit().keepAlive(true).done();
		Assertions.assertEquals(1, cui.getCameraCount(), "应当只有一个相机");
		Assertions.assertTrue(camera1.open(a, false), "应当能成功使用新建摄像头1");
		camera2 = cui.createCamera(camera -> {
		});
		Assertions.assertTrue(camera2.open(b, false), "应当能成功使用新建摄像头2");
		Assertions.assertEquals(2, cui.getCameraCount(), "添加相机后应该有两个相机");
		Assertions.assertNotEquals(camera2.getInventory(), b.getOpenInventory().getTopInventory(),
				"服务器还没有tick，当前玩家应该还未展示CUI");
		tick();
		Assertions.assertEquals(camera2.getInventory(), b.getOpenInventory().getTopInventory(),
				"打开CUI后的下一tick，玩家应当已经看见CUI");
		camera2.setClosable(false);
		Assertions.assertFalse(camera1.open(b, false), "不以子摄像头方式打开时，无法关闭原有摄像头");
		camera2.setClosable(true);
		Assertions.assertTrue(camera1.open(b, true), "以子摄像头方式打开时，应当打开成功");
		Assertions.assertTrue(camera2.open(b, true), "以子摄像头方式打开时，应当打开成功");
		Assertions.assertTrue(camera1.open(b, true), "以子摄像头方式打开时，应当打开成功");
	}

	// @Test
	@Order(3)
	public void testClose() {
		// a看1，b看2->1->2->1
		Assertions.assertFalse(camera2.close(a, false), "玩家A看的不是摄像头2，不应该能成功关闭");
		Assertions.assertFalse(camera2.close(a, true), "玩家A看的不是摄像头2，不应该能成功关闭（即使强制）");
		Assertions.assertFalse(camera2.close(a, false), "玩家B从摄像头2切换到了摄像头1，不应该能成功关闭");
		Assertions.assertFalse(camera2.close(a, true), "玩家B从摄像头2切换到了摄像头1，不应该能成功关闭（即使强制）");
		camera1.setClosable(false);
		Assertions.assertFalse(camera1.close(a, false), "在不强制的情况下，不应该能关闭不可关闭的摄像头");
		Assertions.assertTrue(camera1.close(a, true), "在强制的情况下，应该能关闭不可关闭的摄像头");
		camera1.setClosable(true);
		Assertions.assertTrue(camera1.closeCompletely(b, true), "应该能彻底关闭摄像头");
		Assertions.assertEquals(camera2, plugin.getCameraManager().getCamera(b), "玩家B应当看着摄像头2");
		Assertions.assertTrue(camera2.close(b), "应当能成功关闭摄像头2");
		Assertions.assertNull(plugin.getCameraManager().getViewing(b), "玩家B应当没有看着任何摄像头");
		Assertions.assertEquals(2, cui.getCameraCount(), "还未tick，摄像头还未销毁");
		camera2.setKeepAlive(true);
		tick();
		Assertions.assertEquals(2, cui.getCameraCount(), "已经tick，但由于KeepAlive，所以都不会销毁");
		camera2.setKeepAlive(false);
		camera1.setKeepAlive(true);
		tick();
		Assertions.assertEquals(1, cui.getCameraCount(), "摄像头2应当已经销毁");
		camera1.setKeepAlive(false);
		tick();
		Assertions.assertEquals(0, cui.getCameraCount(), "摄像头1应当已经销毁");
		Assertions.assertEquals(CUIInstance.State.READY, cui.getState(), "ChestUI应当还未销毁");
		cui.edit().keepAlive(false);
		tick();
		Assertions.assertEquals(CUIInstance.State.DESTROYED, cui.getState(), "ChestUI应当已经销毁");
	}

	public static class TestCUI implements ChestUI<TestCUI> {
		private CUIType<TestCUI> type;

		@Override
		public void onInitialize(CUIType<TestCUI> type) {
			this.type = type;
		}

		@Override
		public @Nullable <S> Camera<TestCUI> getDisplayedCamera(DisplayContext<S> context) {
			return type.createInstance(new InstanceHandler()).createCamera(camera -> {
			});
		}

		private static class InstanceHandler implements CUIInstanceHandler<TestCUI> {
			@Override
			public void onInitialize(CUIInstance<TestCUI> cui) {
				cui.edit().keepAlive(true)
						.layer(0,
								new Layer(1, 9).edit()
										.all((row, column) -> Button.builder()
												.material(Material.BLACK_STAINED_GLASS_PANE).displayName(" ").build())
										.slot(0, 0, () -> Button.builder().material(Material.RED_STAINED_GLASS_PANE)
												.displayName("Previous").click(event -> {
												}).build())
										.slot(0, 8, () -> Button.builder().material(Material.GREEN_STAINED_GLASS_PANE)
												.displayName("Next").click(event -> {
												}).build())
										.done())
						.done();
			}
		}
	}
}
