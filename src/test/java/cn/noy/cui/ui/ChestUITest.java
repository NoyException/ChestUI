package cn.noy.cui.ui;

import cn.noy.cui.CUIPlugin;
import cn.noy.cui.layer.Layer;
import org.bukkit.Material;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChestUITest {
	private static ServerMock server;
	private static CUIPlugin plugin;
	private static PlayerMock a, b;
	private static ChestUI<TestCUI> cui;
	private static Camera<TestCUI> camera;

	@BeforeAll
	public static void setup() {
		server = MockBukkit.mock();
		plugin = MockBukkit.load(CUIPlugin.class);
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

	@Test
	@Order(1)
	public void testCreate() {
		cui = plugin.getCUIManager().createCUI(TestCUI.class);
		Assertions.assertNotNull(cui);
	}

	@Test
	@Order(2)
	public void testOpen() {
		Assertions.assertEquals(1, cui.getCameraCount(), "应当只有一个默认相机");
		Assertions.assertTrue(cui.getDefaultCamera().open(a, false), "应当能成功使用默认摄像头");
		camera = cui.createCamera();
		Assertions.assertTrue(camera.open(b, false), "应当能成功使用新建摄像头");
		Assertions.assertEquals(2, cui.getCameraCount(), "添加相机后应该有两个相机");
		Assertions.assertNull(camera.getInventory(), "服务器还没有tick，当前玩家应该还未展示CUI");
		// 应当替换为server的tick
		server.getScheduler().performOneTick();
		Assertions.assertEquals(camera.getInventory(), b.getOpenInventory().getTopInventory(),
				"打开CUI后的下一tick，玩家应当已经看见CUI");
		camera.setClosable(false);
		Assertions.assertFalse(cui.getDefaultCamera().open(b, false), "不以子摄像头方式打开时，无法关闭原有摄像头");
		camera.setClosable(true);
		Assertions.assertTrue(cui.getDefaultCamera().open(b, true), "以子摄像头方式打开时，应当打开成功");
	}

	@Test
	@Order(3)
	public void testClose() {
		Assertions.assertFalse(camera.close(a, false), "玩家A看的不是新建摄像头，不应该能成功关闭");
		Assertions.assertFalse(camera.close(a, true), "玩家A看的不是新建摄像头，不应该能成功关闭（即使强制）");
		Assertions.assertFalse(camera.close(a, false), "玩家B从新建摄像头切换到了默认摄像头，不应该能成功关闭");
		Assertions.assertFalse(camera.close(a, true), "玩家B从新建摄像头切换到了默认摄像头，不应该能成功关闭（即使强制）");
		cui.getDefaultCamera().setClosable(false);
		Assertions.assertFalse(cui.getDefaultCamera().close(a, false), "在不强制的情况下，不应该能关闭不可关闭的摄像头");
		Assertions.assertTrue(cui.getDefaultCamera().close(a, true), "在强制的情况下，应该能关闭不可关闭的摄像头");
		cui.getDefaultCamera().setClosable(true);
		Assertions.assertTrue(camera.closeCascade(b, true), "应该能级联关闭摄像头");
		Assertions.assertEquals(2, cui.getCameraCount(), "还未tick，摄像头还未销毁");
		camera.setKeepAlive(true);
		server.getScheduler().performOneTick();
		Assertions.assertEquals(2, cui.getCameraCount(), "已经tick，但由于存在无法销毁的摄像头，所以默认摄像头也不会销毁");
		camera.setKeepAlive(false);
		cui.getDefaultCamera().setKeepAlive(true);
		server.getScheduler().performOneTick();
		Assertions.assertEquals(1, cui.getCameraCount(), "新建摄像头应当已经销毁");
		cui.getDefaultCamera().setKeepAlive(false);
		server.getScheduler().performOneTick();
		Assertions.assertEquals(0, cui.getCameraCount(), "默认摄像头应当已经销毁");
		Assertions.assertEquals(ChestUI.State.DESTROYED, cui.getState(), "ChestUI应当已经销毁");
	}

	public static class TestCUI implements CUIHandler<TestCUI> {
		@Override
		public void onInitialize(ChestUI<TestCUI> cui) {
			cui.edit().setKeepAlive(true).setLayer(0, new Layer(1, 9).edit()
					.editAll((slotHandler, row, column) -> slotHandler.button(
							builder -> builder.material(Material.BLACK_STAINED_GLASS_PANE).displayName(" ").build()))
					.editSlot(0, 0, slotHandler -> slotHandler.button(builder -> builder
							.material(Material.RED_STAINED_GLASS_PANE).displayName("Previous").clickHandler(event -> {
							}).build()))
					.editSlot(0, 8, slotHandler -> slotHandler.button(builder -> builder
							.material(Material.GREEN_STAINED_GLASS_PANE).displayName("Next").clickHandler(event -> {
							}).build()))
					.finish()).finish();
		}
	}
}
