package ru.amethyst.profile;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class ProfilePlugin extends JavaPlugin {

    private DataStore store;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new DataStore(this);
        store.load();

        ProfileCommand handler = new ProfileCommand(this, store);
        PluginCommand cmd = getCommand("profile");
        if (cmd != null) {
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }
        getServer().getPluginManager().registerEvents(new GuiListener(this, store), this);
    }

    public DataStore store() {
        return store;
    }
}
