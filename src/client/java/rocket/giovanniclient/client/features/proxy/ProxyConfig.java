package rocket.giovanniclient.client.features.proxy;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ProxyConfig {
    @Expose
    @ConfigOption(
            name = "Enable Hypixel Proxy packet",
            desc = "Overrides hostname packet in handshake so you can use proxies"
    )
    @ConfigEditorBoolean
    public boolean PROXY_TOGGLE = false;
}