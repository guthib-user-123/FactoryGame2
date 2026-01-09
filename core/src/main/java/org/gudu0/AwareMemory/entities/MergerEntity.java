package org.gudu0.AwareMemory.entities;

import org.gudu0.AwareMemory.*;

public final class MergerEntity extends TileEntity {

    /**
     * Variant controls which TWO input edges are allowed (relative to output direction).
     * Output is always forward (rot direction).
     *
     * BL = Back + Left
     * BR = Back + Right
     * LR = Left + Right
     */
    public enum Variant { BL, BR, LR }

    private Variant variant = Variant.BL;

    public Variant getVariant() { return variant; }
    public void setVariant(Variant v) {
        if (v != null) this.variant = v;
    }

    public float subcellsPerSecond = 24f;
    private float moveAcc = 0f;

    // Round-robin lane selector (0=left, 1=right, 2=back)
    private int rr = 0;

    public MergerEntity(int cellX, int cellY, int rot, Variant variant) {
        super(cellX, cellY, rot);
        setVariant(variant);
    }

    @Override
    public int[] entryCellFrom(Dir fromEdge) {
        Dir out = Dir.fromRot(rot);

        Dir back  = out.opposite();
        Dir left  = out.left();
        Dir right = out.right();

        // Variant gates which edges can ACCEPT NEW ITEMS.
        boolean allowBack  = (variant != Variant.LR);
        boolean allowLeft  = (variant == Variant.BL || variant == Variant.LR);
        boolean allowRight = (variant == Variant.BR || variant == Variant.LR);

        // Base rot0/out=EAST entry cells:
        // back (WEST)  -> (0,2)
        // left (NORTH) -> (2,4)
        // right(SOUTH) -> (2,0)
        if (fromEdge == back  && allowBack)  return rotUV(0, 2, rot);
        if (fromEdge == left  && allowLeft)  return rotUV(2, 4, rot);
        if (fromEdge == right && allowRight) return rotUV(2, 0, rot);

        return null;
    }

    @Override
    public void step(TileWorld world, int currentTick) {
        moveAcc += subcellsPerSecond * world.fixedDt() * world.getItemSpeedMul();

        int passes = (int) moveAcc;
        if (passes <= 0) return;
        moveAcc -= passes;

        for (int pass = 0; pass < passes; pass++) {
            stepExit(world, currentTick);

            // output lane: (2,2)->(3,2)->(4,2)
            moveIfPossible(world, currentTick, 3, 2, 4, 2);
            moveIfPossible(world, currentTick, 2, 2, 3, 2);

            // back lane: (0,2)->(1,2)->(2,2)
            moveIfPossible(world, currentTick, 1, 2, 2, 2);
            moveIfPossible(world, currentTick, 0, 2, 1, 2);

            // merge decision: choose which lane feeds into (2,2)
            feedDecisionFromInputs(world, currentTick);

            // side lanes: left (2,4)->(2,3) and right (2,0)->(2,1)
            moveIfPossible(world, currentTick, 2, 4, 2, 3);
            moveIfPossible(world, currentTick, 2, 0, 2, 1);
        }
    }

    // ---------- Helpers ----------

    private void stepExit(TileWorld world, int currentTick) {
        int[] exitUV = rotUV(4, 2, rot);
        int eu = exitUV[0], ev = exitUV[1];

        int itemId = occ[eu][ev];
        if (itemId == EMPTY) return;

        Item item = world.getItem(itemId);
        if (item == null) { occ[eu][ev] = EMPTY; return; }
        if (item.enteredThisTick(currentTick)) return;

        Dir out = Dir.fromRot(rot);
        int nx = cellX + out.dx;
        int ny = cellY + out.dy;

        TileEntity neighbor = world.getEntity(nx, ny);
        if (neighbor == null) return;
        if (!neighbor.canAccept(item, out.opposite())) return;

        occ[eu][ev] = EMPTY;
        neighbor.accept(item, out.opposite(), currentTick);
    }

    /**
     * Feeds the decision cell (2,2) from up to 3 sources.
     *
     * IMPORTANT: Even if the current variant "disables" a lane, we still allow draining
     * that lane IF it already contains an item near the decision. This prevents trapping
     * items when the variant changes due to smart placement.
     */
    private void feedDecisionFromInputs(TileWorld world, int currentTick) {
        int[] dUV = rotUV(2, 2, rot);
        int du = dUV[0], dv = dUV[1];
        if (occ[du][dv] != EMPTY) return;

        // Base coords near decision:
        // left  lane near = (2,3)
        // right lane near = (2,1)
        // back  lane near = (1,2)
        final int LEFT  = 0;
        final int RIGHT = 1;
        final int BACK  = 2;

        int[][] sourcesBase = {
            {2, 3}, // LEFT
            {2, 1}, // RIGHT
            {1, 2}  // BACK
        };

        boolean allowBack  = (variant != Variant.LR);
        boolean allowLeft  = (variant == Variant.BL || variant == Variant.LR);
        boolean allowRight = (variant == Variant.BR || variant == Variant.LR);

        for (int k = 0; k < 3; k++) {
            int lane = (rr + k) % 3;

            boolean laneAllowed =
                (lane == LEFT  && allowLeft) ||
                    (lane == RIGHT && allowRight) ||
                    (lane == BACK  && allowBack);

            int[] sUV = rotUV(sourcesBase[lane][0], sourcesBase[lane][1], rot);

            // If lane is disabled, only try it if it already has an item (drain behavior).
            boolean laneHasItem = (occ[sUV[0]][sUV[1]] != EMPTY);
            if (!laneAllowed && !laneHasItem) continue;

            if (tryMove(world, currentTick, sUV[0], sUV[1], du, dv)) {
                rr = (lane + 1) % 3;
                return;
            }
        }
    }

    private void moveIfPossible(TileWorld world, int currentTick, int buFrom, int bvFrom, int buTo, int bvTo) {
        int[] fromUV = rotUV(buFrom, bvFrom, rot);
        int[] toUV   = rotUV(buTo,   bvTo,   rot);
        tryMove(world, currentTick, fromUV[0], fromUV[1], toUV[0], toUV[1]);
    }

    private boolean tryMove(TileWorld world, int currentTick, int fu, int fv, int tu, int tv) {
        int itemId = occ[fu][fv];
        if (itemId == EMPTY) return false;
        if (occ[tu][tv] != EMPTY) return false;

        Item item = world.getItem(itemId);
        if (item == null) { occ[fu][fv] = EMPTY; return false; }

        if (item.enteredThisTick(currentTick)) return false;

        occ[tu][tv] = itemId;
        occ[fu][fv] = EMPTY;
        return true;
    }

    @Override
    public void writeSaveData(WorldGrid.TileSave out) {
        out.i0 = rr;
    }

    @Override
    public void readSaveData(WorldGrid.TileSave in) {
        rr = in.i0;
    }
}
