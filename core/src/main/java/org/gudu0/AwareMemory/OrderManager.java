package org.gudu0.AwareMemory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns milestone orders, updates progress via events, and handles claiming rewards.
 * <p>
 * Fixed milestone approach:
 * - You define an ordered list of milestone Orders.
 * - Manager keeps N active at a time (example: 3).
 * - When one is claimed, it activates the next milestone.
 */
public final class OrderManager {

    /** How many orders you want visible at once. */
    public int maxActiveOrders;

    /** Milestones in order. */
    private final ArrayList<Order> milestoneQueue = new ArrayList<>();

    /** Orders currently shown to the player. */
    private final ArrayList<Order> activeOrders = new ArrayList<>();

    /** Index into milestoneQueue for the next order to activate. */
    private int nextMilestoneIndex = 0;

    public OrderManager(int maxActiveOrders) {
        this.maxActiveOrders = maxActiveOrders;
    }

    private final OrderGenerator generator = new OrderGenerator();
    private boolean autoGenerate = true; // you can toggle this later if you want


    // -------------------------
    // Public accessors (UI reads these)
    // -------------------------

    public List<Order> getActiveOrdersReadOnly() {
        return Collections.unmodifiableList(activeOrders);
    }

    // -------------------------
    // Setup
    // -------------------------

    /**
     * Call once (game start / new save) to define your milestone list.
     * Keep IDs stable so you can save/load later if you want.
     */
    public void setMilestones(ArrayList<Order> milestones) {
        milestoneQueue.clear();
        milestoneQueue.addAll(milestones);

        activeOrders.clear();
        nextMilestoneIndex = 0;

        // Fill initial active orders.
        refillActiveOrders(0);
        generateOrdersIfNeeded(0, 0f);
    }

    /** Makes sure activeOrders has up to desiredActiveCount items. */
    private void refillActiveOrders(long currentTick) {
        while (activeOrders.size() < maxActiveOrders && nextMilestoneIndex < milestoneQueue.size()) {
            Order o = milestoneQueue.get(nextMilestoneIndex++);
            o.createdTick = currentTick;
            activeOrders.add(o);
        }
    }

    // -------------------------
    // Events (these are what your game calls)
    // -------------------------

    public void onItemSold(ItemType itemType, int amount, float currentMoney, @SuppressWarnings("unused") long currentTick) {
        int typeId = itemType.ordinal();
        for (Order o : activeOrders) {
            if (o.claimed) continue;
            if (o.kind != Order.Kind.SELL_ITEMS) continue;

            // If this order requires a specific item type, enforce it.
            if (o.requiredItemTypeId != -1 && o.requiredItemTypeId != typeId) continue;

            o.currentCount += amount;
            if (o.currentCount >= o.targetCount) o.completed = true;
        }

        // Money might complete REACH_MONEY orders too.
        updateMoneyMilestones(currentMoney);
    }

    public void onItemProcessed(int machineTileId, ItemType outputType, int amount, float currentMoney) {
        int outputId = outputType.ordinal();

        for (Order o : activeOrders) {
            if (o.claimed) continue;
            if (o.kind != Order.Kind.PROCESS_IN_MACHINE) continue;

            // Must be the correct machine
            if (o.requiredTileId != machineTileId) continue;

            // Optional output filter
            if (o.requiredItemTypeId != -1 && o.requiredItemTypeId != outputId) continue;

            o.currentCount += amount;
            if (o.currentCount >= o.targetCount) o.completed = true;
        }

        updateMoneyMilestones(currentMoney);
    }

    public void onTilePlaced(int tileId, float currentMoney, @SuppressWarnings("unused") long currentTick) {
        for (Order o : activeOrders) {
            if (o.claimed) continue;
            if (o.kind != Order.Kind.PLACE_TILES) continue;

            // Optional tile filter
            if (o.requiredTileId != -1 && o.requiredTileId != tileId) continue;

            o.currentCount += 1;
            if (o.currentCount >= o.targetCount) o.completed = true;
        }

        updateMoneyMilestones(currentMoney);
    }

    public void onMoneyChanged(float newMoney) {
        updateMoneyMilestones(newMoney);
    }

    private void updateMoneyMilestones(float money) {
        for (Order o : activeOrders) {
            if (o.claimed) continue;
            if (o.kind != Order.Kind.REACH_MONEY) continue;

            if (money >= o.targetMoney) o.completed = true;
        }
    }

    // -------------------------
    // Claiming
    // -------------------------

    /**
     * Attempt to claim an order by index in the active list.
     * Returns reward money if claimed, else 0.
     * <p>
     * UI should call this when player clicks "Claim".
     */
    public int tryClaimActiveIndex(int activeIndex, long currentTick, float currentMoney) {
        if (activeIndex < 0 || activeIndex >= activeOrders.size()) return 0;

        Order o = activeOrders.get(activeIndex);

        if (!o.completed) return 0;
        if (o.claimed) return 0;

        o.claimed = true;

        int reward = o.rewardMoney;

        generator.onOrderClaimed();
        // NEW behavior: remove it immediately and replace with the next milestone(s)
        cleanupClaimedAndRefill(currentTick);      // your existing function
        generateOrdersIfNeeded(currentTick, currentMoney);
        return reward;
    }


    /**
     * Remove any claimed orders from the active list, then pull in new milestones
     * until we have desiredActiveCount active again.
     * <p>
     * Call this after a claim, or occasionally if you want it to self-heal.
     */
    private void cleanupClaimedAndRefill(long currentTick) {
        // Remove claimed orders (iterate backwards to avoid index shifting)
        for (int i = activeOrders.size() - 1; i >= 0; i--) {
            if (activeOrders.get(i).claimed) {
                activeOrders.remove(i);
            }
        }

        // Add new milestones to get back to desired count
        refillActiveOrders(currentTick);
    }

    private void generateOrdersIfNeeded(long currentTick, float currentMoney) {
        if (!autoGenerate) return;

        while (activeOrders.size() < maxActiveOrders) {
            Order o = generator.generateNext(currentTick, currentMoney, activeOrders);

            o.createdTick = currentTick;
            activeOrders.add(o);
        }
    }


    public void setMaxActiveOrders(int v) {
        this.maxActiveOrders = v;
    }
}
