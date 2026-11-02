package ru.amethyst.profile;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Хранилище профилей в plugins/ProfilePlugin/data.yml */
public class DataStore {

    public record Review(UUID author, String authorName, String text, long time) {}

    private final ProfilePlugin plugin;
    private final File file;
    private YamlConfiguration cfg = new YamlConfiguration();

    public DataStore(ProfilePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        plugin.getDataFolder().mkdirs();
        if (file.exists()) {
            cfg = YamlConfiguration.loadConfiguration(file);
        }
    }

    private void save() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить data.yml: " + e.getMessage());
        }
    }

    private String path(UUID id, String key) {
        return "players." + id + "." + key;
    }

    // ---- описание и достижения ----

    public String getDescription(UUID id) {
        return cfg.getString(path(id, "description"), "");
    }

    public void setDescription(UUID id, String text) {
        cfg.set(path(id, "description"), text == null || text.isBlank() ? null : text);
        save();
    }

    public String getAchievements(UUID id) {
        return cfg.getString(path(id, "achievements"), "");
    }

    public void setAchievements(UUID id, String text) {
        cfg.set(path(id, "achievements"), text == null || text.isBlank() ? null : text);
        save();
    }

    // ---- лайки ----

    public int likeCount(UUID target) {
        return cfg.getStringList(path(target, "likes")).size();
    }

    public boolean hasLiked(UUID target, UUID liker) {
        return cfg.getStringList(path(target, "likes")).contains(liker.toString());
    }

    /** @return true если лайк поставлен, false если снят */
    public boolean toggleLike(UUID target, UUID liker) {
        List<String> likes = new ArrayList<>(cfg.getStringList(path(target, "likes")));
        boolean added;
        if (likes.remove(liker.toString())) {
            added = false;
        } else {
            likes.add(liker.toString());
            added = true;
        }
        cfg.set(path(target, "likes"), likes.isEmpty() ? null : likes);
        save();
        return added;
    }

    // ---- отзывы (один отзыв от одного автора, повторный перезаписывает) ----

    public void setReview(UUID target, UUID author, String authorName, String text) {
        String base = path(target, "reviews." + author);
        cfg.set(base + ".name", authorName);
        cfg.set(base + ".text", text);
        cfg.set(base + ".time", System.currentTimeMillis());
        save();
    }

    public boolean removeReview(UUID target, UUID author) {
        String base = path(target, "reviews." + author);
        if (!cfg.contains(base)) return false;
        cfg.set(base, null);
        save();
        return true;
    }

    /** Отзывы, новые сверху */
    public List<Review> getReviews(UUID target) {
        List<Review> result = new ArrayList<>();
        ConfigurationSection sec = cfg.getConfigurationSection(path(target, "reviews"));
        if (sec == null) return result;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection r = sec.getConfigurationSection(key);
            if (r == null) continue;
            try {
                result.add(new Review(UUID.fromString(key), r.getString("name", "???"),
                        r.getString("text", ""), r.getLong("time")));
            } catch (IllegalArgumentException ignored) {
            }
        }
        result.sort(Comparator.comparingLong(Review::time).reversed());
        return result;
    }
}
