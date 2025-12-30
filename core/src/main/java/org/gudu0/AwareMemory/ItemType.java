package org.gudu0.AwareMemory;

public enum ItemType {
    ORE,
    DUST,
    INGOT,
    CRUSHED_ORE,
    GEAR,
    PLATE,
    ROD,
    MACHINE_PARTS;

    private static final ItemType[] VALUES = values();
    private static final int SIZE = VALUES.length;

    public ItemType getNext(){
        return VALUES[(this.ordinal() + 1) % SIZE];
    }
}
