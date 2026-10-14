package org.blood.pl3xMapMarker;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import net.pl3x.map.core.Pl3xMap;
import net.pl3x.map.core.event.EventHandler;
import net.pl3x.map.core.event.EventListener;
import net.pl3x.map.core.event.server.ServerLoadedEvent;
import net.pl3x.map.core.event.world.WorldLoadedEvent;
import net.pl3x.map.core.image.IconImage;
import net.pl3x.map.core.markers.Point;
import net.pl3x.map.core.markers.layer.SimpleLayer;
import net.pl3x.map.core.markers.marker.Marker;
import net.pl3x.map.core.markers.option.Options;
import net.pl3x.map.core.world.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class MarkerManager implements EventListener {

    public static final String LAYER_KEY = "mapmark";
    public static final String ICON_KEY = "mapmark_pin";

    private final JavaPlugin plugin;
    private final MarkerStorage storage;

    public MarkerManager(JavaPlugin plugin, MarkerStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void init() {
        registerIcon();
        for (MarkedObject obj : storage.all()) {
            addToMap(obj);
        }
    }

    @EventHandler
    public void onServerLoaded(ServerLoadedEvent event) {
        init();
    }

    @EventHandler
    public void onWorldLoaded(WorldLoadedEvent event) {
        registerIcon();
        for (MarkedObject obj : storage.all()) {
            if (obj.getWorld().equals(event.getWorld().getName())) {
                addToMap(obj);
            }
        }
    }

    public void shutdown() {
        try {
            if (Pl3xMap.api() != null && Pl3xMap.api().getWorldRegistry() != null) {
                for (World world : Pl3xMap.api().getWorldRegistry()) {
                    if (world.getLayerRegistry().has(LAYER_KEY)) {
                        world.getLayerRegistry().unregister(LAYER_KEY);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void registerIcon() {
        if (Pl3xMap.api().getIconRegistry().has(ICON_KEY)) {
            return;
        }
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xE6, 0x3C, 0x3C));
        g.fillOval(6, 2, 20, 20);
        int[] xs = {9, 23, 16};
        int[] ys = {17, 17, 30};
        g.fillPolygon(xs, ys, 3);
        g.setColor(Color.WHITE);
        g.fillOval(12, 8, 8, 8);
        g.setColor(new Color(0x99, 0x1F, 0x1F));
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(6, 2, 20, 20);
        g.dispose();
        Pl3xMap.api().getIconRegistry().register(ICON_KEY, new IconImage(ICON_KEY, image, "png"));
    }

    private SimpleLayer getLayer(String worldName) {
        World world = Pl3xMap.api().getWorldRegistry().get(worldName);
        if (world == null) {
            return null;
        }
        if (world.getLayerRegistry().has(LAYER_KEY)) {
            return (SimpleLayer) world.getLayerRegistry().get(LAYER_KEY);
        }
        SimpleLayer layer = new SimpleLayer(LAYER_KEY, () -> "Метки");
        layer.setUpdateInterval(15);
        layer.setShowControls(true);
        layer.setDefaultHidden(false);
        layer.setPriority(10);
        layer.setZIndex(500);
        world.getLayerRegistry().register(LAYER_KEY, layer);
        return layer;
    }

    public boolean addToMap(MarkedObject obj) {
        SimpleLayer layer = getLayer(obj.getWorld());
        if (layer == null) {
            plugin.getLogger().warning("Pl3xMap world not found: " + obj.getWorld());
            return false;
        }
        String tooltip = "<b>" + escape(obj.getLabel()) + "</b><br>ID: " + obj.getId()
                + "<br>Автор: " + escape(obj.getAuthor());
        Marker<?> marker;
        if (obj.getType() == MarkedObject.Type.MARKER) {
            marker = Marker.icon(obj.getMarkerKey(), Point.of(obj.getX1(), obj.getZ1()), ICON_KEY)
                    .setOptions(Options.builder()
                            .tooltipContent(tooltip)
                            .popupContent(tooltip)
                            .build());
        } else {
            int stroke = obj.getColor() | 0xFF000000;
            int fill = (obj.getColor() & 0x00FFFFFF) | 0x4D000000;
            marker = Marker.rectangle(obj.getMarkerKey(),
                            Point.of(obj.getX1(), obj.getZ1()),
                            Point.of(obj.getX2(), obj.getZ2()))
                    .setOptions(Options.builder()
                            .strokeWeight(3)
                            .strokeColor(stroke)
                            .fillColor(fill)
                            .tooltipContent(tooltip)
                            .popupContent(tooltip)
                            .build());
        }
        layer.addMarker(marker);
        return true;
    }

    public void removeFromMap(MarkedObject obj) {
        SimpleLayer layer = getLayer(obj.getWorld());
        if (layer != null) {
            layer.removeMarker(obj.getMarkerKey());
        }
    }

    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
