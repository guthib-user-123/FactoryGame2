package org.gudu0.AwareMemory.entities;

import org.gudu0.AwareMemory.*;

@SuppressWarnings({"EnhancedSwitchMigration", "DataFlowIssue"})
public final class FilterEntity extends TileEntity {

    public enum Variant { FL, FR, LR }

    private final Variant variant;
    public Variant getVariant() { return variant; }

    // Movement rate (matches SplitterEntity)
    public float subcellsPerSecond = 24f;
    private float moveAcc = 0f;

    // Alternates between outputs (same behavior as splitter)
    private boolean toggle = false;


    public static final int RULE_ANY  = -1;
    public static final int RULE_NONE = -2;

    // Store rules as ints: -1 Any, -2 None, >=0 ItemType ordinal
    private int ruleForward = RULE_ANY;
    private int ruleLeft    = RULE_ANY;
    private int ruleRight   = RULE_ANY;

    public enum Out { FORWARD, LEFT, RIGHT }

    public int getRule(Out o) {
        switch (o) {
            case FORWARD: return ruleForward;
            case LEFT:    return ruleLeft;
            case RIGHT:   return ruleRight;
            default:      return ruleForward;
        }
    }

    public void setRule(Out o, int v) {
        switch (o) {
            case FORWARD: ruleForward = v; break;
            case LEFT:    ruleLeft = v;    break;
            case RIGHT:   ruleRight = v;   break;
        }
    }


    public void cycleRule(Out o, int delta) {
        int cur = getRule(o);
        setRule(o, cycle(cur, delta));
    }

    private static int cycle(int cur, int delta) {
        // ring: 0=ANY, 1..N = ItemTypes, N+1=NONE
        int n = ItemType.values().length;
        int ring = n + 2;

        int idx;
        if (cur == RULE_ANY) idx = 0;
        else if (cur == RULE_NONE) idx = n + 1;
        else idx = 1 + cur;

        idx = (idx + delta) % ring;
        if (idx < 0) idx += ring;

        if (idx == 0) return RULE_ANY;
        if (idx == n + 1) return RULE_NONE;
        return idx - 1; // ordinal
    }


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

        ItemType t = item.type;

        boolean okF = branchExists(Branch.FORWARD) && ruleAllows(Branch.FORWARD, t);
        boolean okL = branchExists(Branch.LEFT)    && ruleAllows(Branch.LEFT, t);
        boolean okR = branchExists(Branch.RIGHT)   && ruleAllows(Branch.RIGHT, t);

        // No allowed outputs -> jam
        if (!okF && !okL && !okR) return;

        // Try to route. If 2+ options, use toggle as a simple round-robin.
        // Strategy: build an order list depending on toggle.
        // Build the alternating preference based on THIS filter variant.

        // FL alternates between FORWARD <-> LEFT
        // FR alternates between FORWARD <-> RIGHT
        // LR alternates between LEFT <-> RIGHT
        Branch first = null;
        Branch second = null;

        switch (variant) {
            case FL:
                first  = toggle ? Branch.LEFT    : Branch.FORWARD;
                second = toggle ? Branch.FORWARD : Branch.LEFT;
                break;
            case FR:
                first  = toggle ? Branch.RIGHT   : Branch.FORWARD;
                second = toggle ? Branch.FORWARD : Branch.RIGHT;
                break;
            case LR:
                first  = toggle ? Branch.RIGHT   : Branch.LEFT;
                second = toggle ? Branch.LEFT    : Branch.RIGHT;
                break;
        }

        if (first != null && branchExists(first) && ruleAllows(first, t)) {
            if (tryMoveFromDecisionToBranch(first)) { toggle = !toggle; return; }
        }
        if (second != null && branchExists(second) && ruleAllows(second, t)) {
            if (tryMoveFromDecisionToBranch(second)) { toggle = !toggle; return; }
        }
        // otherwise jam (either no allowed outputs or both blocked)

        // all candidate branches blocked -> jam
    }


    private boolean branchExists(Branch br) {
        // based purely on variant wiring (not the rule)
        switch (variant) {
            case FL:
                return br == Branch.FORWARD || br == Branch.LEFT;
            case FR:
                return br == Branch.FORWARD || br == Branch.RIGHT;
            case LR:
                return br == Branch.LEFT || br == Branch.RIGHT;
            default:
                return false; // defensive, should never happen if enum is complete
        }
    }


    private boolean ruleAllows(Branch br, ItemType t) {
        int rule;
        switch (br) {
            case FORWARD:
                rule = ruleForward;
                break;
            case LEFT:
                rule = ruleLeft;
                break;
            case RIGHT:
                rule = ruleRight;
                break;
            default:
                return false; // defensive, should never happen
        }
        if (rule == RULE_NONE) {return false;}
        if (rule == RULE_ANY) {return true;}
        return rule == t.ordinal();
    }


    @Override
    public boolean outputsTo(Dir outEdge) {
        Dir travel = Dir.fromRot(rot);

        boolean forward = (variant == Variant.FL || variant == Variant.FR);
        boolean left    = (variant == Variant.FL || variant == Variant.LR);
        boolean right   = (variant == Variant.FR || variant == Variant.LR);

        if (forward && outEdge == travel) return true;
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
        out.i0 = ruleForward;
        out.i1 = ruleLeft;
        out.i2 = ruleRight;
    }

    @Override
    public void readSaveData(WorldGrid.TileSave in) {
        toggle = in.b0;
        ruleForward = in.i0;
        ruleLeft    = in.i1;
        ruleRight   = in.i2;
    }

}
