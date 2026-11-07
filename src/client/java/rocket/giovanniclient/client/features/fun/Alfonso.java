package rocket.giovanniclient.client.features.fun;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ArmorStand;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.util.PlayerLocator;

public final class Alfonso {
    private static final Component NAME = Component.literal("Alfonso").withStyle(ChatFormatting.RED);

    // Position of the NPC's name armor stand from its entity NBT.
    private static final double NPC_X = -15.5;
    private static final double NPC_Y = 135.0;
    private static final double NPC_Z = 44.0;

    private Alfonso() {}

    public static Component getNameTag(ArmorStand stand, Component original) {
        if (original == null) return null;
        var config = ConfigManager.getConfig().fc;
        if (!config.TROLL_FEATURES || !config.ALFONSO) return original;
        if (!PlayerLocator.isPlayerIn("The Rift") || !stand.hasCustomName()) return original;
        if (Math.abs(stand.getX() - NPC_X) > 0.5 || Math.abs(stand.getZ() - NPC_Z) > 0.5) return original;
        // The hologram stands are vertically offset from the NPC's feet.
        if (Math.abs(stand.getY() - NPC_Y) > 3.0) return original;

        String name = ChatFormatting.stripFormatting(stand.getCustomName().getString()).trim();
        if (name.isEmpty() || name.equalsIgnoreCase("CLICK")) return original;

        return NAME;
    }
}
