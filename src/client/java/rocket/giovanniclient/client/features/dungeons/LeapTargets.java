package rocket.giovanniclient.client.features.dungeons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import rocket.giovanniclient.client.util.PlayerLocator;
import rocket.giovanniclient.client.util.ScoreboardUtils;
import rocket.giovanniclient.client.util.TabListUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LeapTargets {
    private static final int FIRST_TARGET_SLOT = 11;
    private static final int LAST_TARGET_SLOT = 15;
    private static final int MAX_TARGETS = 4;
    private static final String RANKS = "(?:\\[[^\\]\\r\\n]+]\\s+)*";
    private static final String USERNAME = "([A-Za-z0-9_]{1,16})";
    private static final Pattern HEAD_NAME = Pattern.compile("^" + RANKS + USERNAME + "$");
    private static final Pattern TAB_MEMBER = Pattern.compile(
            "^\\[\\d+]\\s+" + RANKS + USERNAME
                    + "\\s+.*?\\((Mage|Berserk(?:er)?|Healer|Tank|Archer|Dead)(?:\\s+(?:[IVXLCDM]+|\\d+))?\\)$",
            Pattern.CASE_INSENSITIVE);

    private LeapTargets() {}

    static List<LeapTarget> read(ChestMenu menu, LocalPlayer player) {
        if (menu.getContainer().getContainerSize() <= LAST_TARGET_SLOT) return List.of();

        Map<String, TabMember> members = readRoster();
        Set<String> seen = new HashSet<>();
        seen.add(nameKey(player.getGameProfile().name()));
        List<LeapTarget> targets = new ArrayList<>(MAX_TARGETS);
        // These server-owned slots can change as the server fills or updates the menu.
        for (int slot = FIRST_TARGET_SLOT; slot <= LAST_TARGET_SLOT && targets.size() < MAX_TARGETS; slot++) {
            ItemStack head = menu.getSlot(slot).getItem();
            if (!head.is(Items.PLAYER_HEAD)) continue;

            Matcher matcher = HEAD_NAME.matcher(ScoreboardUtils.stripMinecraftFormatting(head.getHoverName().getString()).strip());
            if (!matcher.matches()) continue;
            String name = matcher.group(1);
            String key = nameKey(name);
            if (!seen.add(key)) continue;

            TabMember member = members.get(key);
            DungeonClass dungeonClass = member == null ? null : member.dungeonClass();
            targets.add(new LeapTarget(slot, name, dungeonClass, member != null && member.dead(), head));
        }
        return List.copyOf(targets);
    }

    static boolean isInDungeon(Minecraft client) {
        if (client.level == null || client.player == null) return false;

        String area = PlayerLocator.getPokedexArea(client);
        return PlayerLocator.isCatacombsPokedexArea(area) || "Catacombs".equalsIgnoreCase(area)
                || readRoster().containsKey(nameKey(client.player.getGameProfile().name()));
    }

    private static Map<String, TabMember> readRoster() {
        Map<String, TabMember> members = new HashMap<>();
        for (String line : TabListUtils.getCleanedLines(false, true)) {
            Matcher matcher = TAB_MEMBER.matcher(PlayerLocator.stripLeadingSymbols(line).stripTrailing());
            if (!matcher.matches()) continue;

            String name = nameKey(matcher.group(1));
            String status = matcher.group(2).toUpperCase(Locale.ROOT);
            DungeonClass dungeonClass = switch (status) {
                case "MAGE" -> DungeonClass.MAGE;
                case "BERSERK", "BERSERKER" -> DungeonClass.BERSERKER;
                case "HEALER" -> DungeonClass.HEALER;
                case "TANK" -> DungeonClass.TANK;
                case "ARCHER" -> DungeonClass.ARCHER;
                default -> null;
            };
            members.putIfAbsent(name, new TabMember(dungeonClass, status.equals("DEAD")));
        }
        return members;
    }

    private static String nameKey(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private record TabMember(DungeonClass dungeonClass, boolean dead) {}

    record LeapTarget(int slot, String name, DungeonClass dungeonClass, boolean dead, ItemStack head) {}
}
