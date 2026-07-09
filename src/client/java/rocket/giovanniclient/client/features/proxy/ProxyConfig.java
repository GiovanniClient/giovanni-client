package rocket.giovanniclient.client.features.proxy;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

public class ProxyConfig {
    @Expose
    @ConfigOption(
            name = "Enable Hypixel Proxy packet",
            desc = "Overrides hostname packet in handshake so you can use proxies"
    )
    @ConfigEditorBoolean
    public Property<Boolean> PROXY_TOGGLE = Property.of(false);
}
