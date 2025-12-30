package org.gudu0.AwareMemory;

public final class Item {
    public final int id;
    public ItemType type;
    public float value;

    private int enteredTick = -1;

    public Item(int id, ItemType type, float value) {
        this.id = id;
        this.type = type;
        this.value = value;
    }

    public boolean enteredThisTick(int tick) {
        return enteredTick == tick;
    }

    public void markEntered(int tick) {
        enteredTick = tick;
    }
}
