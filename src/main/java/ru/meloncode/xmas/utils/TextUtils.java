package ru.meloncode.xmas.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ru.meloncode.xmas.LocaleManager;
import ru.meloncode.xmas.MagicTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TextUtils {
    public static final String DISPLAY_NAME = "XMas Tree";
    private static final String PREFIX = "<dark_red>[<dark_green>" + DISPLAY_NAME + "<dark_red>] <reset>";
    private static final String CONSOLE_PREFIX = "<dark_red>[<dark_green>" + DISPLAY_NAME + "<dark_red>] <dark_green>";

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    public static List<Component> generateChatReqList(MagicTree tree) {
        Objects.requireNonNull(tree, "tree");
        List<Component> list = new ArrayList<>();
        Component title = Component.text(LocaleManager.GROW_REQ_LIST_TITLE, NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD);
        if (LocaleManager.GROW_REQ_LIST_HINT != null && !LocaleManager.GROW_REQ_LIST_HINT.isBlank()) {
            title = title.hoverEvent(HoverEvent.showText(parse(LocaleManager.GROW_REQ_LIST_HINT)));
        }
        list.add(title);
        if (tree.getLevel().getLevelupRequirements() != null && tree.getLevel().getLevelupRequirements().size() > 0)
            for (Material cMaterial : tree.getLevel().getLevelupRequirements().keySet()) {
                int levelReq = tree.getLevel().getLevelupRequirements().get(cMaterial);
                int treeReq = 0;
                if (tree.getLevelupRequirements().containsKey(cMaterial))
                    treeReq = tree.getLevelupRequirements().get(cMaterial);

                NamedTextColor color = treeReq == 0 ? NamedTextColor.GREEN : NamedTextColor.RED;
                Component line = Component.translatable(cMaterial.getItemTranslationKey())
                        .color(color)
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text(" : " + (levelReq - treeReq) + " / " + levelReq, color).decorate(TextDecoration.BOLD));
                if (treeReq == 0) {
                    line = line.decorate(TextDecoration.STRIKETHROUGH);
                }
                list.add(line);
            }
        return list;
    }

    public static void sendMessage(Player player, String message) {
        sendMessage((CommandSender) player, message);
    }

    public static void sendMessage(Player player, Component message) {
        sendMessage((CommandSender) player, message);
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null && message != null) {
            sender.sendMessage(parse(PREFIX + message));
        }
    }

    public static void sendMessage(CommandSender sender, Component message) {
        if (sender != null && message != null) {
            sender.sendMessage(parse(PREFIX).append(message));
        }
    }

    public static void sendRawMessage(CommandSender sender, String message) {
        if (sender != null && message != null) {
            sender.sendMessage(parse(message));
        }
    }

    public static void sendConsoleMessage(String message) {
        if (message != null) {
            Bukkit.getConsoleSender().sendMessage(parse(CONSOLE_PREFIX + message));
        }
    }

    public static Component parse(String message) {
        if (message == null) {
            return Component.empty();
        }
        if (message.indexOf('§') >= 0) {
            return LEGACY_SECTION.deserialize(message);
        }
        if (message.indexOf('&') >= 0 && message.indexOf('<') < 0) {
            return LEGACY_AMPERSAND.deserialize(message);
        }
        return MINI_MESSAGE.deserialize(message);
    }

    public static List<Component> parseList(List<String> messages) {
        List<Component> components = new ArrayList<>();
        if (messages != null) {
            for (String message : messages) {
                components.add(parse(message));
            }
        }
        return components;
    }
}
