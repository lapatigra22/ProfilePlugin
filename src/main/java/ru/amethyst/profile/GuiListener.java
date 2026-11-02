package ru.amethyst.profile;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GuiListener implements Listener {

    private final ProfilePlugin plugin;
    private final DataStore store;

    public GuiListener(ProfilePlugin plugin, DataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof ProfileGui.Holder holder)) return;
        e.setCancelled(true); // ничего нельзя забрать из меню
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        boolean self = player.getUniqueId().equals(holder.target());
        OfflinePlayer target = Bukkit.getOfflinePlayer(holder.target());

        if (e.getSlot() == ProfileGui.SLOT_LIKE && !self) {
            boolean added = store.toggleLike(holder.target(), player.getUniqueId());
            player.sendMessage(Component.text(added
                    ? "Вы поставили лайк игроку " + holder.targetName()
                    : "Вы убрали лайк у игрока " + holder.targetName(), NamedTextColor.GREEN));
            ProfileGui.refresh(player, e.getView().getTopInventory(), target, plugin);
        } else if (e.getSlot() == ProfileGui.SLOT_REVIEW && !self) {
            player.closeInventory();
            player.sendMessage(Component.text("Чтобы оставить отзыв, напишите: /profile review "
                    + holder.targetName() + " <текст>", NamedTextColor.YELLOW));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof ProfileGui.Holder) {
            e.setCancelled(true);
        }
    }
}
