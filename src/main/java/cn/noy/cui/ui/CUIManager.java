package cn.noy.cui.ui;

import cn.noy.cui.CUIPlugin;
import cn.noy.cui.util.ItemStacks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CUIManager implements Listener {
    private static CUIManager instance;

    public static CUIManager getInstance() {
        if (instance == null) {
            instance = new CUIManager();
        }
        return instance;
    }

    private CUIManager() {
    }

    private BukkitTask task;
    private final HashMap<UUID, ChestUI<?>> viewing = new HashMap<>();
    private final HashSet<ChestUI<?>> cuis = new HashSet<>();
    private final Queue<Runnable> pending = new LinkedList<>();
    private boolean updating;

    public <T extends CUIHandler> ChestUI<T> createCUI(@NotNull Class<T> handlerClass) {
        T handler;
        try {
            Constructor<T> constructor = handlerClass.getConstructor();
            handler = constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException("CUI Handler `" + handlerClass.getCanonicalName() + "` must have a public no-args constructor");
        }
        ChestUI<T> cui = new ChestUI<>(handler);
        handler.onInitialize(cui);
        cuis.add(cui);
        return cui;
    }

    public ChestUI<?> getViewingCUI(@NotNull Player player) {
        return viewing.get(player.getUniqueId());
    }

    public List<ChestUI<?>> getCUIs() {
        return new ArrayList<>(cuis);
    }

    private void tick() {
        while (true) {
            var task = pending.poll();
            if (task == null) {
                break;
            }
            task.run();
        }

        cuis.forEach(cui -> cui.getTrigger().tick());
        updating = true;
        viewing.values().stream().distinct().forEach(cui -> cui.getTrigger().update());
        updating = false;
    }

    public void initialize() {
        if (task != null) {
            throw new IllegalStateException("CUIManager has already been initialized");
        }
        task = Bukkit.getScheduler().runTaskTimer(
                CUIPlugin.getInstance(),
                this::tick,
                0, 1
        );
        Bukkit.getPluginManager().registerEvents(this, CUIPlugin.getInstance());
    }

    public void uninitialize() {
        if (task == null) {
            throw new IllegalStateException("CUIManager has not been initialized");
        }
        task.cancel();
        task = null;
        new ArrayList<>(cuis).forEach(ChestUI::destroy);
    }

    void notifyOpen(@NotNull Player viewer, @NotNull ChestUI<?> chestUI) {
        pending.add(() -> viewing.put(viewer.getUniqueId(), chestUI));
    }

    void notifyClose(@NotNull Player player, @NotNull ChestUI<?> chestUI) {
        pending.add(() -> viewing.remove(player.getUniqueId(), chestUI));
    }

    void notifyDestroy(ChestUI<?> cui) {
        pending.add(() -> cuis.remove(cui));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClose(InventoryCloseEvent event) {
        if (updating)
            return;
        if (!(event.getPlayer() instanceof Player player))
            return;

        ChestUI<?> cui = viewing.get(player.getUniqueId());
        if (cui != null) {
            cui.close(player, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        ChestUI<?> cui = viewing.get(player.getUniqueId());
        if (cui == null)
            return;

        if (event.getClickedInventory() != player.getInventory()) {
            var rawSlot = event.getRawSlot();
            cui.getTrigger().click(player, event.getClick(), event.getAction(),
                    rawSlot / 9, rawSlot % 9, event.getCursor());
            event.setCancelled(true);
        } else {
            switch (event.getAction()) {
                case InventoryAction.MOVE_TO_OTHER_INVENTORY -> {
                    var itemStack = event.getCurrentItem();
                    var remaining = cui.getTrigger().addItem(player, itemStack);
                    event.setCurrentItem(remaining);
                    event.setCancelled(true);
                }
                case COLLECT_TO_CURSOR -> {
                    cui.getTrigger().collect(player, event.getCursor(), true);
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        ChestUI<?> cui = viewing.get(player.getUniqueId());
        if (cui == null)
            return;

        var view = event.getView();
        var cursor = event.getOldCursor().clone();
        var amount = new AtomicInteger();
        if (!ItemStacks.isEmpty(event.getCursor())) {
            amount.set(event.getCursor().getAmount());
        }

        event.getNewItems().forEach((rawSlot, itemStack) -> {
            if (view.convertSlot(rawSlot) != rawSlot)
                return;

            var remaining = cui.getTrigger().
                    place(player, itemStack, rawSlot / 9, rawSlot % 9);
            if (!ItemStacks.isEmpty(remaining)) {
                amount.addAndGet(remaining.getAmount());
            }
        });
        cursor.setAmount(amount.get());
        event.setCursor(cursor);
    }
}
