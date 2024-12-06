package cn.noy.cui.prebuilt.cui;

import cn.noy.cui.layer.Layer;
import cn.noy.cui.slot.SlotHandler;
import cn.noy.cui.ui.*;
import org.bukkit.Material;

import java.util.List;

@ChestSize(maxRow = 6, maxDepth = 6)
@ChestTitle("CUI Monitor")
public class CUIMonitor implements CUIHandler {
    private ChestUI<CUIMonitor> cui;
    private Layer displayCUIs;
    private int page;

    @Override
    public void onInitialize(ChestUI<?> cui) {
        this.displayCUIs = new Layer(5, 9);
        this.cui = cui.edit().
                setLayer(1, displayCUIs).
                finish();
    }

    @Override
    public void onTick() {
        List<ChestUI<?>> cuis = CUIManager.getInstance().getCUIs();
        int size = cuis.size();
        int maxPage = (size - 1) / 45 + 1;
        if (page >= maxPage) {
            page = maxPage - 1;
        }
        for (int row = 0; row < 5; row++) {
            for (int column = 0; column < 9; column++) {
                int index = page * 45 + row * 9 + column;
                if (index >= size) {
                    displayCUIs.edit().editSlot(row, column, SlotHandler::empty);
                    continue;
                }

                ChestUI<?> target = cuis.get(index);
                if (target.getHandlerClass() == CUIMonitor.class) {
                    displayCUIs.edit().editSlot(row, column, slotHandler -> slotHandler.button(builder -> builder.
                            material(Material.BARRIER).
                            displayName(target.getTitle()).
                            lore("&7A CUI Monitor like this", "&cCannot be monitored").
                            build()));
                } else {
                    displayCUIs.edit().editSlot(row, column, slotHandler -> slotHandler.button(builder -> builder.
                            material(Material.CHEST).
                            displayName(target.getTitle()).
                            lore(String.format("&b%d&r viewer(s)", target.getViewers().size())).
                            clickHandler(event -> cui.switchTo(event.getPlayer(), target)).
                            build()));
                }
            }
        }
    }
}
