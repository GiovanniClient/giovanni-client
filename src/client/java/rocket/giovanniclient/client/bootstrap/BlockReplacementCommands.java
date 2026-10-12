package rocket.giovanniclient.client.bootstrap;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import rocket.giovanniclient.client.features.render.BlockReplacements;

import java.util.Map;
import java.util.Objects;

final class BlockReplacementCommands {
    private BlockReplacementCommands() {}

    static LiteralArgumentBuilder<FabricClientCommandSource> create() {
        return ClientCommands.literal("replaceblock")
                .executes(context -> {
                    context.getSource().sendFeedback(Component.literal(
                            "Usage: /gio replaceblock <oldblock> <newblock> | list | remove <oldblock> | clear"));
                    return 0;
                })
                .then(ClientCommands.literal("list").executes(context -> {
                    Map<String, String> entries = BlockReplacements.entries();
                    context.getSource().sendFeedback(Component.literal(entries.isEmpty()
                            ? "§eBlock replacement list is empty." : "§eBlock replacements:"));
                    entries.forEach((oldBlock, newBlock) -> context.getSource().sendFeedback(
                            Component.literal(" §f" + oldBlock + " §7-> §f" + newBlock)));
                    return 1;
                }))
                .then(ClientCommands.literal("clear").executes(context -> {
                    BlockReplacements.clear();
                    context.getSource().sendFeedback(Component.literal("§aBlock replacements cleared."));
                    return 1;
                }))
                .then(ClientCommands.literal("remove")
                        .then(ClientCommands.argument("oldblock", IdentifierArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        BlockReplacements.entries().keySet().stream()
                                                .filter(Objects::nonNull).map(Identifier::tryParse).filter(Objects::nonNull), builder))
                                .executes(context -> {
                                    Identifier oldBlock = context.getArgument("oldblock", Identifier.class);
                                    boolean removed = BlockReplacements.remove(oldBlock);
                                    context.getSource().sendFeedback(Component.literal(removed
                                            ? "§aRemoved block replacement: §f" + oldBlock
                                            : "§eNo block replacement for: §f" + oldBlock));
                                    return removed ? 1 : 0;
                                })))
                .then(ClientCommands.argument("oldblock", IdentifierArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                BuiltInRegistries.BLOCK.keySet(), builder))
                        .then(ClientCommands.argument("newblock", IdentifierArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        BuiltInRegistries.BLOCK.keySet(), builder))
                                .executes(BlockReplacementCommands::replace)));
    }

    private static int replace(CommandContext<FabricClientCommandSource> context) {
        Identifier oldBlock = context.getArgument("oldblock", Identifier.class);
        Identifier newBlock = context.getArgument("newblock", Identifier.class);
        for (Identifier id : new Identifier[]{oldBlock, newBlock}) {
            if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                context.getSource().sendError(Component.literal("Unknown block: " + id));
                return 0;
            }
        }
        BlockReplacements.put(oldBlock, newBlock);
        context.getSource().sendFeedback(Component.literal("§aBlock rendering: §f" + oldBlock + " §7-> §f" + newBlock));
        return 1;
    }
}
