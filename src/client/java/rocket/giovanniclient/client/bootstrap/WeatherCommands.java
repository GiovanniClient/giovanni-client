package rocket.giovanniclient.client.bootstrap;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import rocket.giovanniclient.client.features.render.WeatherOverride;

final class WeatherCommands {
    private WeatherCommands() {}

    static LiteralArgumentBuilder<FabricClientCommandSource> create() {
        LiteralArgumentBuilder<FabricClientCommandSource> command = ClientCommands.literal("weather")
                .executes(context -> {
                    context.getSource().sendFeedback(Component.literal(
                            "§eWeather: §f" + WeatherOverride.mode().commandName()
                                    + "§e. Usage: /gio weather <clear|rain|thunder|snow|off>"));
                    return 1;
                });

        for (WeatherOverride.Mode mode : WeatherOverride.Mode.values()) {
            command.then(ClientCommands.literal(mode.commandName()).executes(context -> {
                WeatherOverride.setMode(mode);
                context.getSource().sendFeedback(Component.literal(mode.isActive()
                        ? "§aClient weather forced to §f" + mode.commandName()
                                + "§a in all worlds and dimensions until you close the game."
                        : "§aWeather override disabled. Using server weather."));
                return 1;
            }));
        }
        return command;
    }
}
