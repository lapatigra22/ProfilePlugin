package ru.amethyst.profile;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class ProfileGui {

    public static final int SLOT_LIKE = 25;
    public static final int SLOT_REVIEW = 49;

    /** Холдер, по которому слушатель узнаёт наше меню. */
    public static final class Holder implements InventoryHolder {
        private final UUID target;
        private final String targetName;
        private Inventory inventory;

        Holder(UUID target, String targetName) {
            this.target = target;
            this.targetName = targetName;
        }

        public UUID target() { return target; }
        public String targetName() { return targetName; }

        @Override
        public Inventory getInventory() { return inventory; }
    }

    private ProfileGui() {}

    public static void open(Player viewer, OfflinePlayer target, ProfilePlugin plugin) {
        String name = target.getName() != null ? target.getName() : "Неизвестно";
        Holder holder = new Holder(target.getUniqueId(), name);
        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("Профиль: " + name, NamedTextColor.DARK_PURPLE));
        holder.inventory = inv;
        fill(inv, viewer, target, name, plugin);
        viewer.openInventory(inv);
    }

    /** Перерисовать уже открытое меню (например, после лайка). */
    public static void refresh(Player viewer, Inventory inv, OfflinePlayer target, ProfilePlugin plugin) {
        String name = target.getName() != null ? target.getName() : "Неизвестно";
        fill(inv, viewer, target, name, plugin);
    }

    private static void fill(Inventory inv, Player viewer, OfflinePlayer target, String name, ProfilePlugin plugin) {
        DataStore store = plugin.store();
        UUID id = target.getUniqueId();
        inv.clear();

        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);

        // Голова игрока
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        sm.setOwningPlayer(target);
        sm.displayName(plain(name, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        List<Component> headLore = new ArrayList<>();
        headLore.add(plain(target.isOnline() ? "● Онлайн" : "● Оффлайн",
                target.isOnline() ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        headLore.add(plain("Лайков: " + store.likeCount(id), NamedTextColor.RED));
        sm.lore(headLore);
        head.setItemMeta(sm);
        inv.setItem(4, head);

        // Описание
        String desc = store.getDescription(id);
        List<Component> descLore = desc.isEmpty()
                ? List.of(plain("Описание не задано.", NamedTextColor.GRAY))
                : wrap(desc, 32, NamedTextColor.WHITE);
        inv.setItem(19, item(Material.BOOK, plain("Описание", NamedTextColor.YELLOW), descLore));

        // Достижения
        String ach = store.getAchievements(id);
        List<Component> achLore = ach.isEmpty()
                ? List.of(plain("Достижения не указаны.", NamedTextColor.GRAY))
                : wrap(ach, 32, NamedTextColor.WHITE);
        inv.setItem(21, item(Material.NETHER_STAR, plain("Достижения", NamedTextColor.AQUA), achLore));

        // Статистика
        List<Component> stats = new ArrayList<>();
        stats.add(plain("Убийств игроков: " + stat(target, Statistic.PLAYER_KILLS), NamedTextColor.WHITE));
        stats.add(plain("Убито мобов: " + stat(target, Statistic.MOB_KILLS), NamedTextColor.WHITE));
        stats.add(plain("Смертей: " + stat(target, Statistic.DEATHS), NamedTextColor.WHITE));
        stats.add(plain("Наиграно: " + playtime(target), NamedTextColor.WHITE));
        inv.setItem(23, item(Material.DIAMOND_SWORD, plain("Статистика", NamedTextColor.GREEN), stats));

        // Лайк
        boolean self = viewer.getUniqueId().equals(id);
        boolean liked = store.hasLiked(id, viewer.getUniqueId());
        List<Component> likeLore = new ArrayList<>();
        likeLore.add(plain("Всего лайков: " + store.likeCount(id), NamedTextColor.WHITE));
        if (self) {
            likeLore.add(plain("Нельзя лайкнуть себя", NamedTextColor.GRAY));
        } else {
            likeLore.add(plain(liked ? "Нажмите, чтобы убрать лайк" : "Нажмите, чтобы поставить лайк",
                    NamedTextColor.GRAY));
        }
        inv.setItem(SLOT_LIKE, item(liked ? Material.RED_DYE : Material.GRAY_DYE,
                plain(liked ? "❤ Вы лайкнули" : "♡ Лайк", NamedTextColor.RED), likeLore));

        // Отзывы
        int max = Math.max(1, Math.min(7, plugin.getConfig().getInt("reviews-in-gui", 7)));
        List<DataStore.Review> reviews = store.getReviews(id);
        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy");
        for (int i = 0; i < Math.min(max, reviews.size()); i++) {
            DataStore.Review r = reviews.get(i);
            List<Component> lore = new ArrayList<>(wrap(r.text(), 32, NamedTextColor.WHITE));
            lore.add(plain(fmt.format(new Date(r.time())), NamedTextColor.DARK_GRAY));
            inv.setItem(37 + i, item(Material.WRITABLE_BOOK,
                    plain("Отзыв от " + r.authorName(), NamedTextColor.GOLD), lore));
        }
        if (reviews.isEmpty()) {
            inv.setItem(40, item(Material.BARRIER, plain("Отзывов пока нет", NamedTextColor.GRAY), List.of()));
        } else if (reviews.size() > max) {
            inv.setItem(36, item(Material.PAPER,
                    plain("Всего отзывов: " + reviews.size(), NamedTextColor.GRAY),
                    List.of(plain("Все: /profile reviews " + name, NamedTextColor.DARK_GRAY))));
        }

        // Кнопка отзыва
        if (!self) {
            inv.setItem(SLOT_REVIEW, item(Material.FEATHER, plain("Оставить отзыв", NamedTextColor.GOLD),
                    List.of(plain("Нажмите, чтобы узнать как", NamedTextColor.GRAY))));
        }
    }

    // ---------- утилиты ----------

    public static int stat(OfflinePlayer p, Statistic s) {
        try {
            return p.getStatistic(s);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String playtime(OfflinePlayer p) {
        Statistic st = null;
        // В разных версиях статистика называется по-разному
        for (String n : new String[]{"PLAY_ONE_MINUTE", "PLAY_TIME"}) {
            try {
                st = Statistic.valueOf(n);
                break;
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (st == null) return "?";
        long ticks = stat(p, st);
        long minutes = ticks / 20 / 60;
        return (minutes / 60) + " ч " + (minutes % 60) + " мин";
    }

    private static Component plain(String text, TextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack item(Material m, Component name, List<Component> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(name);
        if (!lore.isEmpty()) meta.lore(lore);
        it.setItemMeta(meta);
        return it;
    }

    public static List<Component> wrap(String text, int width, TextColor color) {
        List<Component> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            while (word.length() > width) {
                if (line.length() > 0) {
                    lines.add(plain(line.toString(), color));
                    line.setLength(0);
                }
                lines.add(plain(word.substring(0, width), color));
                word = word.substring(width);
            }
            if (line.length() + word.length() + 1 > width && line.length() > 0) {
                lines.add(plain(line.toString(), color));
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) lines.add(plain(line.toString(), color));
        return lines;
    }
}
