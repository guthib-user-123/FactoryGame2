package org.gudu0.AwareMemory;

/**
 * A single milestone order.
 * <p>
 * Design goals:
 * - Deterministic
 * - Easy to add new order types later
 * - No world scanning (we update via explicit events)
 */
public final class Order {

    /** What kind of progress this order listens for. */
    public enum Kind {
        SELL_ITEMS,            // sell N items (optionally specific type)
        PROCESS_IN_MACHINE,    // process N items in a specific machine (optionally specific output type)
        PLACE_TILES,           // place N tiles (optionally specific tile id)
        REACH_MONEY            // reach at least $X
    }

    // ---------- Identity / UI ----------
    public final String id;      // stable identifier (useful later for save/load or debugging)
    public final String title;
    public final String desc;

    // ---------- Reward ----------
    public final int rewardMoney;

    // ---------- Type + requirements ----------
    public final Kind kind;

    /**
     * Optional constraints (meaning depends on Kind).
     * Use -1 to mean "any".
     * <p>
     * SELL_ITEMS:
     *   requiredItemTypeId: only count this type, else any
     * <p>
     * PROCESS_IN_MACHINE:
     *   requiredTileId: machine tile id (ex: WorldGrid.TILE_ROLLER)
     *   requiredItemTypeId: optional output type filter (ex: ItemType.ROD ordinal)
     * <p>
     * PLACE_TILES:
     *   requiredTileId: only count placements of this tile id, else any
     * <p>
     * REACH_MONEY:
     *   required* not used
     */
    public final int requiredTileId;
    public final int requiredItemTypeId;

    // ---------- Progress ----------
    public final int targetCount;     // for count-based orders
    public int currentCount;          // progresses via events

    public final int targetMoney;     // for money milestone orders

    // ---------- Completion state ----------
    public boolean completed;
    public boolean claimed;

    public long createdTick;          // optional: can help sorting later

    private Order(
        String id,
        String title,
        String desc,
        int rewardMoney,
        Kind kind,
        int requiredTileId,
        int requiredItemTypeId,
        int targetCount,
        int targetMoney
    ) {
        this.id = id;
        this.title = title;
        this.desc = desc;
        this.rewardMoney = rewardMoney;
        this.kind = kind;

        this.requiredTileId = requiredTileId;
        this.requiredItemTypeId = requiredItemTypeId;

        this.targetCount = targetCount;
        this.targetMoney = targetMoney;

        this.currentCount = 0;
        this.completed = false;
        this.claimed = false;
        this.createdTick = 0;
    }

    // ---------- Factory helpers (easy to read when building milestones) ----------

    public static Order sellItems(String id, String title, String desc,
                                  int rewardMoney, int itemTypeIdOrAny,
                                  int targetCount) {
        return new Order(id, title, desc, rewardMoney, Kind.SELL_ITEMS,
            -1, itemTypeIdOrAny, targetCount, 0);
    }

    public static Order processInMachine(String id, String title, String desc,
                                         int rewardMoney,
                                         int machineTileId,
                                         int outputItemTypeIdOrAny,
                                         int targetCount) {
        return new Order(id, title, desc, rewardMoney, Kind.PROCESS_IN_MACHINE,
            machineTileId, outputItemTypeIdOrAny, targetCount, 0);
    }

    @SuppressWarnings("unused")
    public static Order placeTiles(String id, String title, String desc,
                                   int rewardMoney,
                                   int tileIdOrAny,
                                   int targetCount) {
        return new Order(id, title, desc, rewardMoney, Kind.PLACE_TILES,
            tileIdOrAny, -1, targetCount, 0);
    }

    public static Order reachMoney(String id, String title, String desc,
                                   int rewardMoney,
                                   int targetMoney) {
        return new Order(id, title, desc, rewardMoney, Kind.REACH_MONEY,
            -1, -1, 0, targetMoney);
    }

    // ---------- UI helpers ----------

    /** 0..1 progress for a bar, given currentMoney. */
    public float progress01(float currentMoney) {
        if (kind == Kind.REACH_MONEY) {
            if (targetMoney <= 0) return 1f;
            return clamp01(currentMoney / (float) targetMoney);
        }
        if (targetCount <= 0) return 1f;
        return clamp01(currentCount / (float) targetCount);
    }

    public String progressText(float currentMoney) {
        if (kind == Kind.REACH_MONEY) {
            return ((int) currentMoney) + " / " + targetMoney;
        }
        return currentCount + " / " + targetCount;
    }

    private static float clamp01(float x) {
        if (x < 0f) return 0f;
        //noinspection ManualMinMaxCalculation
        if (x > 1f) return 1f;
        return x;
    }
}
