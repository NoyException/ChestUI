package cn.noy.cui.slot;

import cn.noy.cui.event.CUIClickEvent;
import cn.noy.cui.util.ItemStacks;
import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class Button extends Slot {
    private Consumer<CUIClickEvent<?>> clickHandler;
    private ItemStack itemStack;
    private boolean dirty = true;

    private Button() {
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public ItemStack display(ItemStack legacy) {
        dirty = false;
        return itemStack;
    }

    @Override
    public void click(CUIClickEvent<?> event) {
        if (clickHandler != null) {
            event.setCancelled(true);
            clickHandler.accept(event);
        }
    }

    @Override
    public ItemStack place(ItemStack itemStack) {
        return itemStack;
    }

    @Override
    public ItemStack collect(ItemStack itemStack) {
        return itemStack;
    }

    @Override
    public Slot deepClone() {
        var button = new Button();
        button.clickHandler = clickHandler;
        button.itemStack = itemStack.clone();
        return button;
    }

    public static class Builder {
        private final Button button = new Button();

        Builder() {
        }

        public Builder itemStack(ItemStack itemStack) {
            button.itemStack = itemStack;
            return this;
        }

        public Builder skull(Player player) {
            PlayerProfile profile = Bukkit.getServer().createProfile(player.getUniqueId(), player.getName());
            PlayerTextures textures = profile.getTextures();
            profile.setTextures(textures);
            button.itemStack = new ItemStack(Material.PLAYER_HEAD);
            button.itemStack.editMeta(meta -> ((SkullMeta) meta).setPlayerProfile(profile));
            return this;
        }

        public Builder skull(URL skullTexture) {
            PlayerProfile profile = Bukkit.getServer().createProfileExact(UUID.randomUUID(), UUID.randomUUID().toString());
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(skullTexture);
            profile.setTextures(textures);
            button.itemStack = new ItemStack(Material.PLAYER_HEAD);
            button.itemStack.editMeta(meta -> ((SkullMeta) meta).setPlayerProfile(profile));
            return this;
        }

        public Builder material(Material material) {
            button.itemStack = new ItemStack(material);
            return this;
        }

        public Builder amount(int amount) {
            button.itemStack.setAmount(amount);
            return this;
        }

        public Builder displayName(Component displayName) {
            displayName = ItemStacks.cleanComponent(displayName);
            var meta = button.itemStack.getItemMeta();
            meta.displayName(displayName);
            button.itemStack.setItemMeta(meta);
            return this;
        }

        public Builder displayName(String displayName) {
            return displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName));
        }

        public Builder lore(List<Component> lore) {
            lore = lore.stream().map(ItemStacks::cleanComponent).toList();
            var meta = button.itemStack.getItemMeta();
            meta.lore(lore);
            button.itemStack.setItemMeta(meta);
            return this;
        }

        public Builder lore(Component... lore) {
            return lore(List.of(lore));
        }

        public Builder lore(String... lore) {
            var list = Arrays.stream(lore)
                    .map(s -> (Component) LegacyComponentSerializer.legacyAmpersand().deserialize(s))
                    .toList();
            return lore(list);
        }

        /**
         * 设置按钮的点击事件。触发时事件会被自动拦截取消，如果你想让点击事件继续传递给下一层，请使用{@link CUIClickEvent#setCancelled(boolean)}。<br>
         * Set the click event of the button. The event will be automatically intercepted and canceled when triggered.
         * If you want the click event to continue to be passed to the next layer, please use {@link CUIClickEvent#setCancelled(boolean)}.
         *
         * @param clickHandler 点击事件处理器<br>
         *                     Click event handler
         * @return 构建器<br>
         * Builder
         */
        public Builder clickHandler(Consumer<CUIClickEvent<?>> clickHandler) {
            button.clickHandler = clickHandler;
            return this;
        }

        public Button build() {
            return button;
        }
    }
}
