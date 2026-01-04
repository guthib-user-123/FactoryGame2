package org.gudu0.AwareMemory;

import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.List;
import java.util.Locale;

/**
 * Generates "infinite" orders once milestones run out.
 *
 * New behavior:
 * - weighted random (not a fixed cycle)
 * - more PROCESS orders (crusher/smelter/press/roller)
 * - reduced chance of PLACE orders
 * - avoids generating duplicates that are already active
 */
public final class OrderGenerator {

    // ---------------- Identity / scaling ----------------

    private int nextAutoId = 1;

    /** How many orders have been completed/claimed total (used for scaling). */
    private int completedCount = 0;

    /** RNG for "random but repeatable within a run". Seed can be persisted later if you want. */
    private final RandomXS128 rng;

    public OrderGenerator() {
        // Web-safe seed source.
        long seed = TimeUtils.millis() * 0x9E3779B97F4A7C15L;
        this.rng = new RandomXS128(seed);
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = Math.max(0, completedCount);
    }

    /** Call when the player claims an order (so future orders scale up). */
    public void onOrderClaimed() {
        completedCount++;
    }

    // ---------------- Weights (tune these freely) ----------------

    // You asked: reduce PLACE orders; add more PROCESS orders.
    private float wSell   = 0.30f;
    private float wProcess= 0.52f;
    private float wMoney  = 0.13f;
    private float wPlace  = 0.05f;

    // ---------------- Templates ----------------

    private static final class ProcessSpec {
        final int machineTileId;
        final ItemType outputType; // can be null meaning "any output"

        final String title;
        final String descPrefix;

        ProcessSpec(int machineTileId, ItemType outputType, String title, String descPrefix) {
            this.machineTileId = machineTileId;
            this.outputType = outputType;
            this.title = title;
            this.descPrefix = descPrefix;
        }
    }

    /**
     * Add more machines here. These IDs MUST match your WorldGrid tile constants.
     * If any constant names differ in your project, change them here.
     */
    private static final ProcessSpec[] PROCESS_POOL = new ProcessSpec[] {
        // Crusher: ORE -> DUST
        new ProcessSpec(WorldGrid.TILE_CRUSHER, ItemType.DUST,  "Crush Ore",     "Make "),
        // Smelter: DUST -> INGOT
        new ProcessSpec(WorldGrid.TILE_SMELTER, ItemType.INGOT, "Smelt Ingots",  "Make "),
        // Press: INGOT -> PLATE
        new ProcessSpec(WorldGrid.TILE_PRESS,   ItemType.PLATE, "Press Plates",  "Make "),
        // Roller: PLATE -> ROD
        new ProcessSpec(WorldGrid.TILE_ROLLER,  ItemType.ROD,   "Roll Rods",     "Make "),
    };

    private static final ItemType[] SELL_TYPES = new ItemType[] {
        ItemType.ORE, ItemType.DUST, ItemType.INGOT, ItemType.PLATE, ItemType.ROD
    };

    // ---------------- Public API ----------------

    /**
     * Generate one new order.
     * Pass the current active order list so we can avoid duplicates.
     */
    public Order generateNext(long currentTick, float currentMoney, List<Order> existingActive) {
        int tier = completedCount / 4; // every 4 claims, difficulty steps up

        // Try a few times to avoid duplicates like "Sell 10 items" appearing twice.
        for (int attempt = 0; attempt < 12; attempt++) {
            Order candidate = generateOne(tier, currentMoney);

            if (!isDuplicate(candidate, existingActive)) {
                return candidate;
            }
        }

        // Fallback: just return something even if it duplicates (should be rare).
        return generateOne(tier, currentMoney);
    }

    // ---------------- Internals ----------------

    private Order generateOne(int tier, float currentMoney) {
        String id = String.format(Locale.ROOT, "auto_%04d", nextAutoId++);

        int pick = pickWeighted(wSell, wProcess, wMoney, wPlace);

        switch (pick) {
            case 0: return makeSellOrder(id, tier);
            case 1: return makeProcessOrder(id, tier);
            case 2: return makeMoneyOrder(id, tier, currentMoney);
            default: return makePlaceOrder(id, tier);
        }
    }

    /**
     * Returns 0..3 matching the weights in order: sell, process, money, place.
     */
    private int pickWeighted(float a, float b, float c, float d) {
        float sum = a + b + c + d;
        float r = rng.nextFloat() * sum;

        if ((r -= a) < 0f) return 0;
        if ((r -= b) < 0f) return 1;
        if ((r -= c) < 0f) return 2;
        return 3;
    }

    private Order makeSellOrder(String id, int tier) {
        // 60% chance: sell ANY items
        // 40% chance: sell a specific type (more variety)
        boolean specific = rng.nextFloat() < 0.40f;

        int base = 8 + tier * 8;
        int target = jitterCount(base);

        int reward = 70 + tier * 55 + (target * 6);

        if (!specific) {
            return Order.sellItems(
                id,
                "Ship Items",
                "Sell " + target + " items.",
                reward,
                -1,
                target
            );
        }

        ItemType t = SELL_TYPES[rng.nextInt(SELL_TYPES.length)];
        return Order.sellItems(
            id,
            "Ship " + t.name(),
            "Sell " + target + " " + t.name() + ".",
            reward,
            t.ordinal(),
            target
        );
    }

    private Order makeProcessOrder(String id, int tier) {
        ProcessSpec spec = PROCESS_POOL[rng.nextInt(PROCESS_POOL.length)];

        int base = 6 + tier * 6;
        int target = jitterCount(base);

        // Processing is harder than selling; pay more.
        int reward = 110 + tier * 80 + (target * 10);

        int outputFilter = (spec.outputType == null) ? -1 : spec.outputType.ordinal();

        return Order.processInMachine(
            id,
            spec.title,
            spec.descPrefix + target + " " + (spec.outputType == null ? "items" : spec.outputType.name()) + ".",
            reward,
            spec.machineTileId,
            outputFilter,
            target
        );
    }

    private Order makeMoneyOrder(String id, int tier, float currentMoney) {
        // "delta" increases with tier + has randomness so it doesn't feel repetitive.
        int baseDelta = 250 + tier * 350;
        int delta = baseDelta + rng.nextInt(250);

        int targetMoney = (int) currentMoney + delta;
        int reward = 140 + tier * 90;

        return Order.reachMoney(
            id,
            "Grow Cash",
            "Reach $" + targetMoney + ".",
            reward,
            targetMoney
        );
    }

    private Order makePlaceOrder(String id, int tier) {
        // Rare (by weights), but still useful as a "push expansion" objective.
        int base = 5 + tier * 4;
        int target = jitterCount(base);

        int reward = 60 + tier * 50 + (target * 6);

        return Order.placeTiles(
            id,
            "Expand Factory",
            "Place " + target + " machines/tiles.",
            reward,
            -1,
            target
        );
    }

    private int jitterCount(int base) {
        // +/- ~25% randomness, but never below 1.
        float mul = 0.85f + rng.nextFloat() * 0.30f; // 0.85 .. 1.15
        int v = Math.max(1, (int) (base * mul));
        return v;
    }

    private boolean isDuplicate(Order candidate, List<Order> existingActive) {
        if (existingActive == null) return false;

        // Simple "signature" rule:
        // same kind + same machine + same item filter => treat as duplicate.
        for (Order o : existingActive) {
            if (o.kind != candidate.kind) continue;
            if (o.requiredTileId != candidate.requiredTileId) continue;
            if (o.requiredItemTypeId != candidate.requiredItemTypeId) continue;
            return true;
        }
        return false;
    }
}
