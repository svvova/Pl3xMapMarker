package org.blood.pl3xMapMarker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MapMarkCommand implements TabExecutor {

    public static final String PERM_MARKER = "mapmark.marker";
    public static final String PERM_TERRITORY = "mapmark.territory";
    public static final String PERM_ADMIN = "mapmark.admin";

    private static final int DEFAULT_COLOR = 0x3388FF;

    private final MarkerStorage storage;
    private final MarkerManager manager;
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    public MapMarkCommand(MarkerStorage storage, MarkerManager manager) {
        this.storage = storage;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "marker" -> handleMarker(sender, args);
            case "pos1" -> handlePos(sender, 1);
            case "pos2" -> handlePos(sender, 2);
            case "territory" -> handleTerritory(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "list" -> handleList(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("--- MapMark ---", NamedTextColor.GOLD));
        if (sender.hasPermission(PERM_MARKER)) {
            sender.sendMessage(Component.text("/mapmark marker <текст> — поставить маркер", NamedTextColor.YELLOW));
        }
        if (sender.hasPermission(PERM_TERRITORY)) {
            sender.sendMessage(Component.text("/mapmark pos1 | pos2 — выбрать точки территории", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/mapmark territory <текст> [#цвет] — создать территорию", NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text("/mapmark list — список объектов", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mapmark delete <id> — удалить объект", NamedTextColor.YELLOW));
    }

    private void handleMarker(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Только для игроков.", NamedTextColor.RED));
            return;
        }
        if (!player.hasPermission(PERM_MARKER)) {
            player.sendMessage(Component.text("Нет прав.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Использование: /mapmark marker <текст>", NamedTextColor.RED));
            return;
        }
        String text = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Location loc = player.getLocation();
        MarkedObject obj = new MarkedObject(
                storage.nextId(),
                MarkedObject.Type.MARKER,
                player.getWorld().getName(),
                text,
                player.getName(),
                0,
                loc.getX(), loc.getZ(), 0, 0
        );
        storage.add(obj);
        manager.addToMap(obj);
        player.sendMessage(Component.text("Маркер #" + obj.getId() + " «" + text + "» создан.", NamedTextColor.GREEN));
    }

    private void handlePos(CommandSender sender, int index) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Только для игроков.", NamedTextColor.RED));
            return;
        }
        if (!player.hasPermission(PERM_TERRITORY)) {
            player.sendMessage(Component.text("Нет прав.", NamedTextColor.RED));
            return;
        }
        Location loc = player.getLocation();
        if (index == 1) {
            pos1.put(player.getUniqueId(), loc);
        } else {
            pos2.put(player.getUniqueId(), loc);
        }
        player.sendMessage(Component.text("Точка " + index + " установлена: "
                + loc.getBlockX() + ", " + loc.getBlockZ(), NamedTextColor.GREEN));
    }

    private void handleTerritory(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Только для игроков.", NamedTextColor.RED));
            return;
        }
        if (!player.hasPermission(PERM_TERRITORY)) {
            player.sendMessage(Component.text("Нет прав.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Использование: /mapmark territory <текст> [#цвет]", NamedTextColor.RED));
            return;
        }
        Location p1 = pos1.get(player.getUniqueId());
        Location p2 = pos2.get(player.getUniqueId());
        if (p1 == null || p2 == null) {
            player.sendMessage(Component.text("Сначала выберите точки: /mapmark pos1 и /mapmark pos2", NamedTextColor.RED));
            return;
        }
        if (!p1.getWorld().equals(p2.getWorld()) || !p1.getWorld().equals(player.getWorld())) {
            player.sendMessage(Component.text("Обе точки должны быть в одном (текущем) мире.", NamedTextColor.RED));
            return;
        }

        int color = DEFAULT_COLOR;
        int textEnd = args.length;
        String last = args[args.length - 1];
        if (last.startsWith("#")) {
            Integer parsed = parseColor(last);
            if (parsed == null) {
                player.sendMessage(Component.text("Неверный цвет: " + last + " (пример: #FF8800)", NamedTextColor.RED));
                return;
            }
            color = parsed;
            textEnd = args.length - 1;
        }
        if (textEnd < 2) {
            player.sendMessage(Component.text("Использование: /mapmark territory <текст> [#цвет]", NamedTextColor.RED));
            return;
        }
        String text = String.join(" ", java.util.Arrays.copyOfRange(args, 1, textEnd));

        MarkedObject obj = new MarkedObject(
                storage.nextId(),
                MarkedObject.Type.TERRITORY,
                player.getWorld().getName(),
                text,
                player.getName(),
                color,
                p1.getX(), p1.getZ(), p2.getX(), p2.getZ()
        );
        storage.add(obj);
        manager.addToMap(obj);
        pos1.remove(player.getUniqueId());
        pos2.remove(player.getUniqueId());
        player.sendMessage(Component.text("Территория #" + obj.getId() + " «" + text + "» создана.", NamedTextColor.GREEN));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Использование: /mapmark delete <id>", NamedTextColor.RED));
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("ID должен быть числом.", NamedTextColor.RED));
            return;
        }
        MarkedObject obj = storage.get(id);
        if (obj == null) {
            sender.sendMessage(Component.text("Объект #" + id + " не найден.", NamedTextColor.RED));
            return;
        }
        if (!canDelete(sender, obj)) {
            sender.sendMessage(Component.text("Вы можете удалять только свои объекты.", NamedTextColor.RED));
            return;
        }
        manager.removeFromMap(obj);
        storage.remove(id);
        sender.sendMessage(Component.text("Объект #" + id + " удалён.", NamedTextColor.GREEN));
    }

    private boolean canDelete(CommandSender sender, MarkedObject obj) {
        if (sender.hasPermission(PERM_ADMIN)) {
            return true;
        }
        if (!sender.getName().equals(obj.getAuthor())) {
            return false;
        }
        return obj.getType() == MarkedObject.Type.MARKER
                ? sender.hasPermission(PERM_MARKER)
                : sender.hasPermission(PERM_TERRITORY);
    }

    private void handleList(CommandSender sender) {
        List<MarkedObject> all = storage.all();
        if (all.isEmpty()) {
            sender.sendMessage(Component.text("Объектов нет.", NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text("--- Объекты на карте (" + all.size() + ") ---", NamedTextColor.GOLD));
        for (MarkedObject obj : all) {
            String typeName = obj.getType() == MarkedObject.Type.MARKER ? "Маркер" : "Территория";
            Component line = Component.text("#" + obj.getId() + " ", NamedTextColor.AQUA)
                    .append(Component.text("[" + typeName + "] ", NamedTextColor.GRAY))
                    .append(Component.text(obj.getLabel() + " ", NamedTextColor.WHITE))
                    .append(Component.text("(" + obj.getAuthor() + ", " + obj.getWorld() + ")", NamedTextColor.DARK_GRAY))
                    .hoverEvent(HoverEvent.showText(
                            Component.text("Нажмите, чтобы подставить команду удаления", NamedTextColor.YELLOW)))
                    .clickEvent(ClickEvent.suggestCommand("/mapmark delete " + obj.getId()));
            sender.sendMessage(line);
        }
    }

    private Integer parseColor(String input) {
        String hex = input.substring(1);
        if (hex.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, String @NotNull [] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission(PERM_MARKER)) {
                result.add("marker");
            }
            if (sender.hasPermission(PERM_TERRITORY)) {
                result.add("pos1");
                result.add("pos2");
                result.add("territory");
            }
            result.add("delete");
            result.add("list");
            result.removeIf(s -> !s.startsWith(args[0].toLowerCase()));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            for (MarkedObject obj : storage.all()) {
                String id = String.valueOf(obj.getId());
                if (id.startsWith(args[1])) {
                    result.add(id);
                }
            }
        }
        return result;
    }
}
