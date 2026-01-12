package sb.rocket.giovanniclient.client.features.inventorybuttons;

import java.util.ArrayList;
import java.util.List;

public final class UiButtonsConfig {
    public int version = 1;
    public boolean editMode = false; // togglabile via keybind
    public List<UiButtonDef> buttons = new ArrayList<>();
}
