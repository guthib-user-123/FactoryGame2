package org.gudu0.AwareMemory.entities;

import org.gudu0.AwareMemory.*;

public final class FilterEntity extends TileEntity {

    public enum Variant { FL, FR, LR }

    private final Variant variant;
    public Variant getVariant() { return variant; }

    // Movement rate (matches SplitterEntity)
    public float subcellsPerSecond = 24f;
    private float moveAcc = 0f;

    // Alternates between outputs (same behavior as splitter)
    private boolean toggle = false;

    // null = Any, ItemType = that type only, special sentinel = None
    private ItemType filterForward = null;
    private ItemType filterLeft = null;
    private ItemType filterRight = null;
    private boolean noneForward = false;
    private boolean noneLeft = false;
    private boolean noneRight = false;


    public FilterEntity(int cellX, int cellY, int rot, Variant variant) {
        super(cellX, cellY, rot);
        this.variant = variant;
    }

    @Override
    public int[] entryCellFrom(Dir fromEdge) {
        // Accept only from BACK side
        Dir travel = Dir.fromRot(rot);
        Dir back = travel.opposite();
        if (fromEdge != back) return null;

        // Base entry cell is (0,2) in rot0
        return rotUV(0, 2, rot);
    }

    @Override
    public void step(TileWorld world, int currentTick) {
        moveAcc += subcellsPerSecond * world.fixedDt();
        int passes = (int) moveAcc;
        if (passes <= 0) return;
        moveAcc -= passes;

        for (int pass = 0; pass < passes; pass++) {
            // Exit-first handoff attempts
            stepExit(world, currentTick, Branch.FORWARD);
            stepExit(world, currentTick, Branch.LEFT);
            stepExit(world, currentTick, Branch.RIGHT);

            // Move within branches toward exits (always run all 3 so nothing “freezes”)
            stepBranchInternal(Branch.FORWARD);
            stepBranchInternal(Branch.LEFT);
            stepBranchInternal(Branch.RIGHT);

            // Decision cell routes into one of the branch first-cells
            stepDecision(world, currentTick);

            // Move input lane toward decision (back-to-front)
            stepInputTowardDecision();
        }
    }

    // ----------------- Movement pieces -----------------

    private enum Branch { FORWARD, LEFT, RIGHT }

    private void stepExit(TileWorld world, int currentTick, Branch b) {
        int[] exitBase = exitCellBase(b);
        int[] exitUV = rotUV(exitBase[0], exitBase[1], rot);
        int eu = exitUV[0], ev = exitUV[1];

        int itemId = occ[eu][ev];
        if (itemId == EMPTY) return;

        Item item = world.getItem(itemId);
        if (item == null) { occ[eu][ev] = EMPTY; return; }

        if (item.enteredThisTick(currentTick)) return;

        Dir out = outDir(b);
        int nx = cellX + out.dx;
        int ny = cellY + out.dy;

        TileEntity neighbor = world.getEntity(nx, ny);
        if (neighbor == null) return;

        if (!neighbor.canAccept(item, out.opposite())) return;

        occ[eu][ev] = EMPTY;
        neighbor.accept(item, out.opposite(), currentTick);
    }

    private void stepBranchInternal(Branch b) {
        int[] near = branchNearDecisionBase(b);
        int[] far  = exitCellBase(b);

        int[] nuv = rotUV(near[0], near[1], rot);
        int[] fuv = rotUV(far[0], far[1], rot);

        int nu = nuv[0], nv = nuv[1];
        int fu = fuv[0], fv = fuv[1];

        if (occ[nu][nv] == EMPTY) return;
        if (occ[fu][fv] != EMPTY) return;

        occ[fu][fv] = occ[nu][nv];
        occ[nu][nv] = EMPTY;
    }

    private void stepDecision(TileWorld world, int currentTick) {
        int[] dUV = rotUV(2, 2, rot);
        int du = dUV[0], dv = dUV[1];

        int itemId = occ[du][dv];
        if (itemId == EMPTY) return;

        Item item = world.getItem(itemId);
        if (item == null) { occ[du][dv] = EMPTY; return; }

        if (item.enteredThisTick(currentTick)) return;

        // (Later you’ll add filtering logic here.)
        Branch a = firstChoice();
        Branch b = secondChoice();

        if (tryMoveFromDecisionToBranch(a)) {
            toggle = !toggle;
            return;
        }
        if (tryMoveFromDecisionToBranch(b)) {
            toggle = !toggle;
        }
        // else jam
    }

    @Override
    public boolean outputsTo(Dir outEdge) {
        Dir travel = Dir.fromRot(rot);

        if (outEdge == travel) return true; // forward always exists

        boolean left  = (variant == Variant.FL || variant == Variant.LR);
        boolean right = (variant == Variant.FR || variant == Variant.LR);

        if (left && outEdge == travel.left()) return true;
        if (right && outEdge == travel.right()) return true;

        return false;
    }

    private void stepInputTowardDecision() {
        int[] c0 = rotUV(0, 2, rot);
        int[] c1 = rotUV(1, 2, rot);
        int[] d  = rotUV(2, 2, rot);

        if (occ[c1[0]][c1[1]] != EMPTY && occ[d[0]][d[1]] == EMPTY) {
            occ[d[0]][d[1]] = occ[c1[0]][c1[1]];
            occ[c1[0]][c1[1]] = EMPTY;
        }

        if (occ[c0[0]][c0[1]] != EMPTY && occ[c1[0]][c1[1]] == EMPTY) {
            occ[c1[0]][c1[1]] = occ[c0[0]][c0[1]];
            occ[c0[0]][c0[1]] = EMPTY;
        }
    }

    private boolean tryMoveFromDecisionToBranch(Branch b) {
        if (b == null) return false;

        int[] dUV = rotUV(2, 2, rot);
        int du = dUV[0], dv = dUV[1];

        int[] nearBase = branchNearDecisionBase(b);
        int[] nearUV = rotUV(nearBase[0], nearBase[1], rot);
        int nu = nearUV[0], nv = nearUV[1];

        if (occ[nu][nv] != EMPTY) return false;

        occ[nu][nv] = occ[du][dv];
        occ[du][dv] = EMPTY;
        return true;
    }

    // ----------------- Variant helpers -----------------

    private Branch firstChoice() {
        switch (variant) {
            case FL: return (toggle ? Branch.LEFT : Branch.FORWARD);
            case FR: return (toggle ? Branch.RIGHT : Branch.FORWARD);
            case LR: return (toggle ? Branch.RIGHT : Branch.LEFT);
        }
        return Branch.FORWARD;
    }

    private Branch secondChoice() {
        switch (variant) {
            case FL: return (toggle ? Branch.FORWARD : Branch.LEFT);
            case FR: return (toggle ? Branch.FORWARD : Branch.RIGHT);
            case LR: return (toggle ? Branch.LEFT : Branch.RIGHT);
        }
        return Branch.LEFT;
    }

    private Dir outDir(Branch b) {
        Dir travel = Dir.fromRot(rot);
        switch (b) {
            case FORWARD: return travel;
            case LEFT:    return travel.left();
            case RIGHT:   return travel.right();
        }
        return travel;
    }

    private int[] branchNearDecisionBase(Branch b) {
        switch (b) {
            case FORWARD: return new int[]{3, 2};
            case LEFT:    return new int[]{2, 3};
            case RIGHT:   return new int[]{2, 1};
        }
        return new int[]{3, 2};
    }

    private int[] exitCellBase(Branch b) {
        switch (b) {
            case FORWARD: return new int[]{4, 2};
            case LEFT:    return new int[]{2, 4};
            case RIGHT:   return new int[]{2, 0};
        }
        return new int[]{4, 2};
    }

    @Override
    public void writeSaveData(WorldGrid.TileSave out) {
        out.b0 = toggle;
    }

    @Override
    public void readSaveData(WorldGrid.TileSave in) {
        toggle = in.b0;
    }
}
