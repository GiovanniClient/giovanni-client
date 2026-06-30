package rocket.giovanniclient.client.config;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.util.Utils;

@SuppressWarnings("unused")
public class AboutConfig extends Config {
    private long lastUpdateCheckClick = 0; // to cooldown update click checks

    @Expose
    @ConfigOption(name = "Check for Updates", desc = "Automatically checks for updates on each startup")
    @ConfigEditorBoolean
    public boolean AUTO_CHECK_FOR_UPDATES = true;

    @Expose
    @ConfigOption(name = "RatterScanner check", desc = "Checking .jar safety via RatterScanner exposes your IP with them.")
    @ConfigEditorBoolean
    public boolean RATTER_SCANNER_CHECK = true;

    @Expose
    @ConfigOption(name = "Auto Update", desc = "Automatically download new version when update is available")
    @ConfigEditorBoolean
    public boolean AUTO_DOWNLOAD_UPDATES = false;

    @ConfigOption(name = "Check for Updates", desc = "Manually check if an update is available.")
    @ConfigEditorButton(buttonText = "§lCheck!")
    public Runnable checkUpdateButton = () -> {
        final long currentTime = System.currentTimeMillis();
        final long CHECK_COOLDOWN_MILLIS = 5000;
        if (currentTime - lastUpdateCheckClick < CHECK_COOLDOWN_MILLIS) {
            long remainingSeconds = (CHECK_COOLDOWN_MILLIS - (currentTime - lastUpdateCheckClick)) / 1000 + 1;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.FIRE_EXTINGUISH, 1.0f));
            Utils.chat("Update check is on cooldown! Please wait " + remainingSeconds + " seconds.");
            return;
        }

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        GiovanniClientClient.UPDATE_MANAGER.runUpdateFlow();

        lastUpdateCheckClick = currentTime; // Reset button cooldown
    };







    @Expose
    @Accordion
    @ConfigOption(name = "Used Software, Libraries and Code", desc = "")
    public UsedSoftware us = new UsedSoftware();

    public static class UsedSoftware {
        @ConfigOption(name = "MoulConfig (LGPL 3.0)", desc = "")
        @ConfigEditorButton(buttonText = "Source")
        public Runnable moulconfig = () -> Util.getPlatform().openUri("https://github.com/NotEnoughUpdates/MoulConfig");

        @ConfigOption(name = "Fabric (Apache 2.0)", desc = "")
        @ConfigEditorButton(buttonText = "Source")
        public Runnable fabric = () -> Util.getPlatform().openUri("https://github.com/FabricMC/fabric");

        @ConfigOption(name = "Mixin (MIT)", desc = "")
        @ConfigEditorButton(buttonText = "Source")
        public Runnable mixin = () -> Util.getPlatform().openUri("https://github.com/SpongePowered/Mixin/");

        @ConfigOption(name = "LibAutoUpdate (BSD-2-Clause)", desc = "")
        @ConfigEditorButton(buttonText = "Source")
        public Runnable libautoupdate = () -> Util.getPlatform().openUri("https://github.com/nea89o/libautoupdate");

        @ConfigOption(name = "WI-Freecam (GPL 3.0)", desc = "rewritten with yarn mappings")
        @ConfigEditorButton(buttonText = "Source")
        public Runnable wi_freecam = () -> Util.getPlatform().openUri("https://github.com/Wurst-Imperium/WI-Freecam");

        @ConfigOption(name = "SkyHanni (LGPL 2.1)", desc = "used as inspiration for moulconfig implementation\n(moulconfig docs aren't the best)")
        @ConfigEditorButton(buttonText = "Source")
        public Runnable skyhanni = () -> Util.getPlatform().openUri("https://github.com/hannibal002/SkyHanni/");
    }

}