package sb.rocket.giovanniclient.client.features.inventorybuttons;

public final class UiButtonsEditorState {
    private static boolean editMode = false;
    private static String selectedSlot = InventoryButtonLayout.DEFAULT_ID;

    private UiButtonsEditorState() {}

    public static boolean isEditMode() { return editMode; }
    public static void setEditMode(boolean on) { editMode = on; }

    public static String getSelectedSlot() { return selectedSlot; }

    public static void setSelectedSlot(String slotId) {
        InventoryButtonLayout s = InventoryButtonLayout.fromIdOrDefault(slotId);
        selectedSlot = (s == null ? InventoryButtonLayout.DEFAULT_ID : s.id());
    }
}
