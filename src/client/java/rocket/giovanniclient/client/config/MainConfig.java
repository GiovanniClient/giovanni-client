package rocket.giovanniclient.client.config;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import net.wimods.freecam.FreecamConfig;
import rocket.giovanniclient.client.features.autosolvers.AutoSolversConfig;
import rocket.giovanniclient.client.features.fun.FunConfig;
import rocket.giovanniclient.client.features.inventorybuttons.InventoryButtonsConfig;
import rocket.giovanniclient.client.features.misc.MiscConfig;
import rocket.giovanniclient.client.features.render.RenderConfig;
import rocket.giovanniclient.client.features.rift.RiftConfig;
import rocket.giovanniclient.client.features.slayers.SlayersConfig;

public class MainConfig extends Config {
    @Override
    public StructuredText getTitle() {
        return StructuredText.of("§bGiovanni Client");
    }

    @Expose
    @Category(name = "About", desc = "Information about GiovanniClient and updates.")
    public AboutConfig about = new AboutConfig();

    // solvers are stuff that tells you how to play, autosolvers are stuff that plays the game for you
    @Expose
    @Category(name = "Auto Solvers", desc = "Various auto solvers for GUIs")
    public AutoSolversConfig asc = new AutoSolversConfig();

    @Expose
    @Category(name = "Slayers", desc = "QOL mods for different slayers")
    public SlayersConfig sc = new SlayersConfig();

    @Expose
    @Category(name = "Rift", desc = "Easy rift for players who hate the only actually fun part of the game")
    public RiftConfig riftconfig = new RiftConfig();

    @Expose
    @Category(name = "Render", desc = "See or hide stuff you shouldn't :P")
    public RenderConfig rc = new RenderConfig();

    @Expose
    @Category(name = "Freecam", desc = "WI-Freecam rewritten for GiovanniClient")
    public FreecamConfig freecamConfig = new FreecamConfig();

    @Expose
    @Category(name = "Inventory Buttons", desc = "like NEU once did")
    public InventoryButtonsConfig ibc = new InventoryButtonsConfig();

    @Expose
    @Category(name = "Misc", desc = "Stuff I don't know where to put")
    public MiscConfig msc = new MiscConfig();

    @Expose
    @Category(name = "Fun", desc = "becuater bagu")
    public FunConfig fc = new FunConfig();

    @Expose
    @Category(name = "Debug", desc = "here be dragons")
    public DebugConfig dc = new DebugConfig();
}