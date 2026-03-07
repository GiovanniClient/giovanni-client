package sb.rocket.giovanniclient.client.features.inventorybuttons;

public class EditModeState {
    private static boolean editMode = false;

    public static boolean isEditMode() {
        return editMode;
    }

    public static void setEditMode(boolean mode) {
        editMode = mode;
    }
}