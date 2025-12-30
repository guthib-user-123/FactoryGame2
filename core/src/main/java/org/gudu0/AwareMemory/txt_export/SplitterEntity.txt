package org.gudu0.AwareMemory.entities;

import org.gudu0.AwareMemory.*;

public final class SplitterEntity extends TileEntity {

    public enum Variant { FL, FR, LR }

    private Variant variant;
    public void setVariant(Variant v) { this.variant = v; }
    public Variant getVariant() { return variant; }


    // Movement rate (matches your conveyor approach)
    public float subcellsPerSecond = 24f;
    private float moveAcc = 0f;

    // Alternates between outputs
    private boolean toggle = false;

    public SplitterEntity(int cellX, int cellY, int rot, Variant variant) {
        super(cellX, cellY, rot);
        this.variant = variant;
    }

    @Override
    public int[] entryCellFrom(Dir fromEdge) {
        // Accept only from BACK side (like conveyors/machines)
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
            // 1) Branch exits try to handoff (exit-first)
            stepExit(world, currentTick, Branch.FORWARD);
            if (hasLeft())  stepExit(world, currentTick, Branch.LEFT);
            if (hasRight()) stepExit(world, currentTick, Branch.RIGHT);

            // 2) Move within branches toward exits
            stepBranchInternal(Branch.FORWARD);
            if (hasLeft())  stepBranchInternal(Branch.LEFT);
            if (hasRight()) stepBranchInternal(Branch.RIGHT);

            // 3) Decision cell routes into one of the branch first-cells
            stepDecision(world, currentTick);

            // 4) Move input lane toward decision (back-to-front)
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
        // For each branch, there are exactly 2 cells from decision-side to exit-side:
        // forward: (3,2)->(4,2)
        // left:    (2,3)->(2,4)
        // right:   (2,1)->(2,0)

        int[] near = branchNearDecisionBase(b);
        int[] far  = exitCellBase(b);

        int[] nuv = rotUV(near[0], near[1], rot);
        int[] fuv = rotUV(far[0], far[1], rot);

        int nu = nuv[0], nv = nuv[1];
        int fu = fuv[0], fv = fuv[1];

        if (occ[nu][nv] == EMPTY) return;
        if (occ[fu][fv] != EMPTY) return;

        // Move item from near -> far
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

        // Decide output order based on toggle:
        Branch a = firstChoice();
        Branch b = secondChoice();

        if (tryMoveFromDecisionToBranch(a)) {
            toggle = !toggle; // only flip when move succeeds
            return;
        }
        if (tryMoveFromDecisionToBranch(b)) {
            toggle = !toggle;
        }
        // else jam: stay at decision cell
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
        // Input lane: (0,2)->(1,2)->(2,2)
        int[] c0 = rotUV(0, 2, rot);
        int[] c1 = rotUV(1, 2, rot);
        int[] d  = rotUV(2, 2, rot);

        // (1,2) -> decision
        if (occ[c1[0]][c1[1]] != EMPTY && occ[d[0]][d[1]] == EMPTY) {
            occ[d[0]][d[1]] = occ[c1[0]][c1[1]];
            occ[c1[0]][c1[1]] = EMPTY;
        }

        // (0,2) -> (1,2)
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

    // ----------------- Variant + geometry helpers -----------------

    private boolean hasLeft() {
        return variant == Variant.FL || variant == Variant.LR;
    }

    private boolean hasRight() {
        return variant == Variant.FR || variant == Variant.LR;
    }

    private Branch firstChoice() {
        // Alternate outputs; “toggle=false” chooses the first branch of the variant.
        return switch (variant) {
            case FL -> (toggle ? Branch.LEFT : Branch.FORWARD);
            case FR -> (toggle ? Branch.RIGHT : Branch.FORWARD);
            case LR -> (toggle ? Branch.RIGHT : Branch.LEFT);
        };
    }

    private Branch secondChoice() {
        // The other branch
        return switch (variant) {
            case FL -> (toggle ? Branch.FORWARD : Branch.LEFT);
            case FR -> (toggle ? Branch.FORWARD : Branch.RIGHT);
            case LR -> (toggle ? Branch.LEFT : Branch.RIGHT);
        };
    }

    private Dir outDir(Branch b) {
        Dir travel = Dir.fromRot(rot);
        return switch (b) {
            case FORWARD -> travel;
            case LEFT -> travel.left();
            case RIGHT -> travel.right();
        };
    }

    private int[] branchNearDecisionBase(Branch b) {
        // The first cell after decision (2,2) in base rot0
        return switch (b) {
            case FORWARD -> new int[]{3, 2};
            case LEFT -> new int[]{2, 3};
            case RIGHT -> new int[]{2, 1};
        };
    }

    private int[] exitCellBase(Branch b) {
        // Edge exit cells in base rot0
        return switch (b) {
            case FORWARD -> new int[]{4, 2}; // east edge center
            case LEFT -> new int[]{2, 4};    // north edge center
            case RIGHT -> new int[]{2, 0};   // south edge center
        };
    }
}
