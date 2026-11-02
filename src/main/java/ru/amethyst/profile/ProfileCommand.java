package ru.amethyst.profile;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("desc", "achievements", "like", "review", "reviews", "help");

    private final ProfilePlugin plugin;
    private final DataStore store;

    public ProfileCommand(ProfilePlugin plugin, DataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только игрокам.");
            return true;
        }

        if (args.length == 0) {
            ProfileGui.open(player, player, plugin);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "desc", "description", "about" -> setText(player, args, true);
            case "achievements", "ach" -> setText(player, args, false);
            case "like" -> like(player, args);
            case "review" -> review(player, args);
            case "reviews" -> reviews(player, args);
            case "help" -> help(player);
            default -> {
                OfflinePlayer target = find(args[0]);
                if (target == null) {
                    msg(player, "Игрок \"" + args[0] + "\" не найден. /profile help — список команд.", NamedTextColor.RED);
                } else {
                    ProfileGui.open(player, target, plugin);
                }
            }
        }
        return true;
    }

    private void setText(Player p, String[] args, boolean description) {
        String what = description ? "Описание" : "Достижения";
        String sub = description ? "desc" : "achievements";
        if (args.length < 2) {
            msg(p, "Использование: /profile " + sub + " <текст>  (или \"clear\" чтобы очистить)", NamedTextColor.YELLOW);
            return;
        }
        String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        if (text.equalsIgnoreCase("clear")) {
            if (description) store.setDescription(p.getUniqueId(), null);
            else store.setAchievements(p.getUniqueId(), null);
            msg(p, what + " очищено.", NamedTextColor.GREEN);
            return;
        }
        int max = plugin.getConfig().getInt(description ? "max-description-length" : "max-achievements-length",
                description ? 120 : 200);
        if (text.length() > max) {
            msg(p, "Слишком длинный текст: максимум " + max + " символов (у вас " + text.length() + ").", NamedTextColor.RED);
            return;
        }
        if (description) store.setDescription(p.getUniqueId(), text);
        else store.setAchievements(p.getUniqueId(), text);
        msg(p, what + " сохранено. Посмотреть: /profile", NamedTextColor.GREEN);
    }

    private void like(Player p, String[] args) {
        if (args.length < 2) {
            msg(p, "Использование: /profile like <ник>", NamedTextColor.YELLOW);
            return;
        }
        OfflinePlayer target = find(args[1]);
        if (target == null) {
            msg(p, "Игрок не найден.", NamedTextColor.RED);
            return;
        }
        if (target.getUniqueId().equals(p.getUniqueId())) {
            msg(p, "Нельзя лайкнуть себя.", NamedTextColor.RED);
            return;
        }
        boolean added = store.toggleLike(target.getUniqueId(), p.getUniqueId());
        msg(p, (added ? "Вы поставили лайк игроку " : "Вы убрали лайк у игрока ") + target.getName(), NamedTextColor.GREEN);
    }

    private void review(Player p, String[] args) {
        if (args.length < 3) {
            msg(p, "Использование: /profile review <ник> <текст>  (\"clear\" вместо текста удалит ваш отзыв)", NamedTextColor.YELLOW);
            return;
        }
        OfflinePlayer target = find(args[1]);
        if (target == null) {
            msg(p, "Игрок не найден.", NamedTextColor.RED);
            return;
        }
        if (target.getUniqueId().equals(p.getUniqueId())) {
            msg(p, "Нельзя оставить отзыв самому себе.", NamedTextColor.RED);
            return;
        }
        String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();
        if (text.equalsIgnoreCase("clear")) {
            boolean removed = store.removeReview(target.getUniqueId(), p.getUniqueId());
            msg(p, removed ? "Ваш отзыв удалён." : "У вас нет отзыва об этом игроке.",
                    removed ? NamedTextColor.GREEN : NamedTextColor.RED);
            return;
        }
        int max = plugin.getConfig().getInt("max-review-length", 100);
        if (text.length() > max) {
            msg(p, "Слишком длинный отзыв: максимум " + max + " символов (у вас " + text.length() + ").", NamedTextColor.RED);
            return;
        }
        store.setReview(target.getUniqueId(), p.getUniqueId(), p.getName(), text);
        msg(p, "Отзыв о " + target.getName() + " сохранён. Повторный отзыв заменит предыдущий.", NamedTextColor.GREEN);

        Player online = target.getPlayer();
        if (online != null) {
            msg(online, p.getName() + " оставил(а) вам отзыв. Смотрите: /profile", NamedTextColor.GOLD);
        }
    }

    private void reviews(Player p, String[] args) {
        OfflinePlayer target = args.length >= 2 ? find(args[1]) : p;
        if (target == null) {
            msg(p, "Игрок не найден.", NamedTextColor.RED);
            return;
        }
        List<DataStore.Review> list = store.getReviews(target.getUniqueId());
        if (list.isEmpty()) {
            msg(p, "У игрока " + target.getName() + " пока нет отзывов.", NamedTextColor.GRAY);
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy");
        msg(p, "Отзывы о " + target.getName() + " (" + list.size() + "):", NamedTextColor.GOLD);
        for (int i = 0; i < Math.min(10, list.size()); i++) {
            DataStore.Review r = list.get(i);
            p.sendMessage(Component.text("• " + r.authorName() + " (" + fmt.format(new Date(r.time())) + "): ",
                    NamedTextColor.YELLOW).append(Component.text(r.text(), NamedTextColor.WHITE)));
        }
        if (list.size() > 10) msg(p, "...и ещё " + (list.size() - 10), NamedTextColor.GRAY);
    }

    private void help(Player p) {
        msg(p, "/profile — ваш профиль", NamedTextColor.YELLOW);
        msg(p, "/profile <ник> — профиль игрока", NamedTextColor.YELLOW);
        msg(p, "/profile desc <текст> — своё описание", NamedTextColor.YELLOW);
        msg(p, "/profile achievements <текст> — свои достижения", NamedTextColor.YELLOW);
        msg(p, "/profile like <ник> — поставить/убрать лайк", NamedTextColor.YELLOW);
        msg(p, "/profile review <ник> <текст> — оставить отзыв", NamedTextColor.YELLOW);
        msg(p, "/profile reviews [ник] — все отзывы", NamedTextColor.YELLOW);
    }

    /** Ищет онлайн-игрока, затем игрока, уже заходившего на сервер. */
    private OfflinePlayer find(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    private void msg(Player p, String text, NamedTextColor color) {
        p.sendMessage(Component.text(text, color));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            SUBS.stream().filter(s -> s.startsWith(prefix)).forEach(out::add);
            addPlayers(out, prefix);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("like") || sub.equals("review") || sub.equals("reviews")) {
                addPlayers(out, args[1].toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    private void addPlayers(List<String> out, String prefix) {
        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (pl.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(pl.getName());
        }
    }
}
