package net.wimods.freecam;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class FreecamConfig {

    @ConfigOption(name = "Native Settings", desc = "Opens WI-Freecam's native settings screen.")
    @ConfigEditorButton(buttonText = "Open")
    public Runnable openNativeSettings = () -> WiFreecam.INSTANCE.getGui().open();
}
