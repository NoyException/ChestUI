package cn.noy.cui.ui;

import org.bukkit.entity.Player;

public interface CUIHandler {
    void onInitialize(ChestUI<?> cui);
    default void onTick() {}
    default void onDestroy() {}

    default boolean onOpen(Player viewer) {return true;}
    default boolean onClose(Player viewer) {return true;}
    default boolean onSwitchTo(Player viewer, ChestUI<?> to) {return true;}
    default boolean onSwitchBack(Player viewer, ChestUI<?> from) {return true;}
}
