package sb.rocket.giovanniclient.client.config;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import net.minecraft.util.Util;
import sb.rocket.giovanniclient.client.GiovanniClientClient;
import sb.rocket.giovanniclient.client.util.Utils;

public class AboutConfig extends Config {

    @Expose
    @ConfigOption(name = "Check for Updates", desc = "Automatically checks for updates on each startup")
    @ConfigEditorBoolean
    public boolean AUTO_CHECK_FOR_UPDATES = true;

    @Expose
    @ConfigOption(name = "Auto Update", desc = "Automatically download new version on each startup")
    @ConfigEditorBoolean
    public boolean AUTO_UPDATE = false;

    @Expose
    @Accordion
    @ConfigOption(name = "Update Manually", desc = "")
    public ManualUpdates mu = new ManualUpdates();

    @Expose
    @Accordion
    @ConfigOption(name = "Used Software, Libraries and Code", desc = "")
    public UsedSoftware us = new UsedSoftware();

    public static class ManualUpdates {
        // Cooldown for check button
        private long lastCheckButtonClickTime = 0;
        private final long CHECK_COOLDOWN_MILLIS = 5000; // 5 seconds

        // Cooldown for download button
        private long lastDownloadButtonClickTime = 0;
        private final long DOWNLOAD_COOLDOWN_MILLIS = 5000; // 5 seconds

        @ConfigOption(name = "Check for Updates", desc = "Manually check if an update is available.")
        @ConfigEditorButton(buttonText = "Check Now")
        public Runnable checkUpdateButton = () -> {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastCheckButtonClickTime < CHECK_COOLDOWN_MILLIS) {
                long remainingSeconds = (CHECK_COOLDOWN_MILLIS - (currentTime - lastCheckButtonClickTime)) / 1000 + 1;
                Utils.chat("Update check is on cooldown! Please wait " + remainingSeconds + " seconds.");
                return;
            }

            // Delegate the check to the global UpdateManager instance
            GiovanniClientClient.UPDATE_MANAGER.checkForUpdate();

            lastCheckButtonClickTime = currentTime; // Reset cooldown
        };

        @ConfigOption(name = "Download Update", desc = "Download and install the available update (only if a check found one).")
        @ConfigEditorButton(buttonText = "Download Now")
        public Runnable downloadUpdateButton = () -> {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastDownloadButtonClickTime < DOWNLOAD_COOLDOWN_MILLIS) {
                long remainingSeconds = (DOWNLOAD_COOLDOWN_MILLIS - (currentTime - lastDownloadButtonClickTime)) / 1000 + 1;
                Utils.chat("Download is on cooldown! Please wait " + remainingSeconds + " seconds.");
                return;
            }

            // Get the pending update from the global UpdateManager
            if (GiovanniClientClient.UPDATE_MANAGER.getPendingUpdate() == null) {
                Utils.chat("No update available to download. Please check for updates first.");
                return;
            }

            // Delegate the download to the global UpdateManager
            GiovanniClientClient.UPDATE_MANAGER.launchUpdate(GiovanniClientClient.UPDATE_MANAGER.getPendingUpdate());

            lastDownloadButtonClickTime = currentTime; // Reset cooldown
        };
    }

    public static class UsedSoftware {
        @ConfigOption(name = "MoulConfig (LGPL 3.0)", desc = "")
        @ConfigEditorButton(buttonText = "Source")
        public Runnable moulconfig = () -> {
            Util.getOperatingSystem().open("https://github.com/NotEnoughUpdates/MoulConfig");
        };

        @ConfigOption(name = "Fabric (Apache 2.0)", desc = "")
        @ConfigEditorButton(buttonText = "Source")
        public Runnable fabric = () -> {
            Util.getOperatingSystem().open("https://github.com/FabricMC/fabric");
        };

        @ConfigOption(name = "Mixin (MIT)", desc = "")
        @ConfigEditorButton(buttonText = "Source")
        public Runnable mixin = () -> {
            Util.getOperatingSystem().open("https://github.com/SpongePowered/Mixin/");
        };

        @ConfigOption(name = "LibAutoUpdate (BSD-2-Clause)", desc = "")
        @ConfigEditorButton(buttonText = "Source")
        public Runnable libautoupdate = () -> {
            Util.getOperatingSystem().open("https://github.com/nea89o/libautoupdate");
        };

        @ConfigOption(name = "WI-Freecam (GPL 3.0)", desc = "rewritten with yarn mappings")
        @ConfigEditorButton(buttonText = "Source")
        public Runnable wi_freecam = () -> {
            Util.getOperatingSystem().open("https://github.com/Wurst-Imperium/WI-Freecam");
        };

        @ConfigOption(name = "SkyHanni (LGPL 2.1)", desc = "used as inspiration for moulconfig implementation and update logic")
        @ConfigEditorButton(buttonText = "Source")
        public Runnable skyhanni = () -> {
            Util.getOperatingSystem().open("https://github.com/hannibal002/SkyHanni/");
        };
    }

}