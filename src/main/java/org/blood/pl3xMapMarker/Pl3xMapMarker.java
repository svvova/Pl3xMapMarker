package org.blood.pl3xMapMarker;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Pl3xMapMarker extends JavaPlugin {

    private MarkerStorage storage;
    private MarkerManager manager;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("Pl3xMap") == null) {
            getLogger().severe("Pl3xMap не найден! Отключаю плагин.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        storage = new MarkerStorage(this);
        storage.load();

        manager = new MarkerManager(this, storage);
        try {
            net.pl3x.map.core.Pl3xMap.api().getEventRegistry().register(manager);
        } catch (Throwable t) {
            getLogger().warning("Could not register Pl3xMap event listener: " + t.getMessage());
        }
        // Delay init so Pl3xMap has its worlds registered
        Bukkit.getScheduler().runTaskLater(this, () -> manager.init(), 20L);

        MapMarkCommand executor = new MapMarkCommand(this, storage, manager);
        PluginCommand command = getCommand("mapmark");
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.shutdown();
        }
        if (storage != null) {
            storage.save();
        }
    }
}
