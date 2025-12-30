package org.gudu0.AwareMemory.entities;

import org.gudu0.AwareMemory.*;

public final class MergerEntity extends TileEntity {

    public float subcellsPerSecond = 24f;
    private float moveAcc = 0f;

    // Round-robin input selection when both sides can feed the merge
    private boolean toggle = false;

    public MergerEntity(int cellX, int cellY, int rot) {
        super(cellX, cellY, rot);
    }

    @Override
    public int[] entryCellFrom(Dir fromEdge) {
        Dir out = Dir.fromRot(rot);

        // Allow inputs from back, left, right (relative to output)
        Dir back  = out.opposite();
        Dir left  = out.left();
        Dir right = out.right();

        // Pick distinct entry cells for each edge
        // (base rot0/out=EAST coordinates)
        if (fromEdge == back)  return rotUV(0, 2, rot); // from WEST edge
        if (fromEdge == left)  return rotUV(2, 4, rot); // from NORTH edge
        if (fromEdge == right) return rotUV(2, 0, rot); // from SOUTH edge

        return null;
    }


    @Override
    public void step(TileWorld world, int currentTick) {
        moveAcc += subcellsPerSecond * world.fixedDt();
        int passes = (int) moveAcc;
        if (passes <= 0) return;
        moveAcc -= passes;

        for (int pass = 0; pass < passes; pass++) {
            stepExit(world, currentTick);

            // output lane
            moveIfPossible(world, currentTick, 3, 2, 4, 2);
            moveIfPossible(world, currentTick, 2, 2, 3, 2);

            // NEW: advance back lane toward decision: (0,2)->(1,2)->(2,2)
            moveIfPossible(world, currentTick, 1, 2, 2, 2);
            moveIfPossible(world, currentTick, 0, 2, 1, 2);

            // feed decision from *three* inputs instead of two
            feedDecisionFromInputs(world, currentTick);

            // existing side lanes
            moveIfPossible(world, currentTick, 2, 4, 2, 3);
            moveIfPossible(world, currentTick, 2, 0, 2, 1);
        }
    }


    // ---------- Helpers ----------

    private void stepExit(TileWorld world, int currentTick) {
        // Exit cell in base is (4,2)
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

    private int rr = 0; // 0=left,1=right,2=back

    private void feedDecisionFromInputs(TileWorld world, int currentTick) {
        int[] dUV = rotUV(2, 2, rot);
        int du = dUV[0], dv = dUV[1];
        if (occ[du][dv] != EMPTY) return;

        int[][] sourcesBase = {
            {2, 3}, // left lane near
            {2, 1}, // right lane near
            {1, 2}  // back lane near
        };

        // try up to 3 attempts starting from rr
        for (int k = 0; k < 3; k++) {
            int i = (rr + k) % 3;
            int[] sUV = rotUV(sourcesBase[i][0], sourcesBase[i][1], rot);
            if (tryMove(world, currentTick, sUV[0], sUV[1], du, dv)) {
                rr = (i + 1) % 3; // next time start after the one we used
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

        // No double-move: if it entered this tile this tick, it can't advance within it
        if (item.enteredThisTick(currentTick)) return false;

        occ[tu][tv] = itemId;
        occ[fu][fv] = EMPTY;
        return true;
    }
}
