package org.gudu0.AwareMemory;

public enum ItemType {
    ORE(0),
    DUST(1),
    INGOT(2),
    PLATE(3),
    ROD(4),
    GEAR(5),
    MACHINE_PARTS(6);

    public final int saveId;
    ItemType(int saveId) { this.saveId = saveId; }

    private static final ItemType[] BY_ID = new ItemType[256];
    static {
        for (ItemType t : values()) BY_ID[t.saveId & 0xFF] = t;
    }

    public static ItemType fromSaveId(int id) {
        ItemType t = BY_ID[id & 0xFF];
        return (t != null) ? t : ORE; // safe fallback
    }
}
