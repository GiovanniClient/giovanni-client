package net.wimods.freecam;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class FreecamConfig {

    @Expose
    @ConfigOption(name = "Native Settings", desc = "Opens WI-Freecam's native settings screen.")
    @ConfigEditorButton(buttonText = "Open")
    public Runnable openNativeSettings = () -> WiFreecam.INSTANCE.getGui().open();
}
