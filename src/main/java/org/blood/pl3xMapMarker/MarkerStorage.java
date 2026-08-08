package org.blood.pl3xMapMarker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class MarkerStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<Integer, MarkedObject> objects = new LinkedHashMap<>();
    private int nextId = 1;

    public MarkerStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "markers.yml");
    }

    public void load() {
        objects.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        nextId = config.getInt("next-id", 1);
        ConfigurationSection section = config.getConfigurationSection("objects");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection obj = section.getConfigurationSection(key);
            if (obj == null) {
                continue;
            }
            try {
                int id = Integer.parseInt(key);
                MarkedObject.Type type = MarkedObject.Type.valueOf(obj.getString("type", "MARKER"));
                MarkedObject marked = new MarkedObject(
                        id,
                        type,
                        obj.getString("world", "world"),
                        obj.getString("label", ""),
                        obj.getString("author", "?"),
                        (int) obj.getLong("color", 0xFFFF0000L),
                        obj.getDouble("x1"),
                        obj.getDouble("z1"),
                        obj.getDouble("x2"),
                        obj.getDouble("z2")
                );
                objects.put(id, marked);
                if (id >= nextId) {
                    nextId = id + 1;
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Skipping invalid object entry: " + key);
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("next-id", nextId);
        for (MarkedObject obj : objects.values()) {
            String path = "objects." + obj.getId();
            config.set(path + ".type", obj.getType().name());
            config.set(path + ".world", obj.getWorld());
            config.set(path + ".label", obj.getLabel());
            config.set(path + ".author", obj.getAuthor());
            config.set(path + ".color", obj.getColor());
            config.set(path + ".x1", obj.getX1());
            config.set(path + ".z1", obj.getZ1());
            config.set(path + ".x2", obj.getX2());
            config.set(path + ".z2", obj.getZ2());
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create data folder");
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save markers.yml: " + e.getMessage());
        }
    }

    public int nextId() {
        return nextId++;
    }

    public void add(MarkedObject obj) {
        objects.put(obj.getId(), obj);
        save();
    }

    public MarkedObject get(int id) {
        return objects.get(id);
    }

    public MarkedObject remove(int id) {
        MarkedObject removed = objects.remove(id);
        if (removed != null) {
            save();
        }
        return removed;
    }

    public List<MarkedObject> all() {
        return new ArrayList<>(objects.values());
    }
}
