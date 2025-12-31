package org.gudu0.AwareMemory.entities;

import org.gudu0.AwareMemory.*;

public final class SmelterEntity extends TileEntity {
    // Subcell conveyor-style movement along the internal lane.
    // Accumulates fractional movement based on fixedDt(), then executes integer subcell steps.
    public float subcellsPerSecond = 24f; // start here (≈ 12/5 = 2.4 tiles/sec)
    private float subcellMoveAccumulator = 0f;

    // Processing state:
    // - activeProcessItemID tracks the item currently in the process cell (u=2,v=2)
    // - processTimeLeft counts down while item stays in that cell
    // - hasCompletedSmelt means conversion has been applied; item may still be sitting in the cell until it can move forward

    public float processTime = 1.75f; // tune
    public float outputValueMultiplier = 2.0f;

    private int activeProcessItemID = EMPTY;
    private float processTimeLeft = 0f;
    private boolean hasCompletedSmelt = false;

    public SmelterEntity(int cellX, int cellY, int rot) {
        super(cellX, cellY, rot);
    }

    @Override
    public int[] entryCellFrom(Dir fromEdge) {
        // Accept only from BACK side, into (0,2) in base rot0.
        Dir fwd = Dir.fromRot(rot);
        Dir back = fwd.opposite();
        if (fromEdge != back) return null;
        return rotUV(0, 2, rot);
    }

    @Override
    public boolean canAccept(Item item, Dir fromEdge) {
        // Type gate at the portal
        if (item.type != ItemType.DUST) return false;
        return super.canAccept(item, fromEdge);
    }

    @Override
    public void step(TileWorld world, int currentTick) {
        tickProcessing(world);
        subcellMoveAccumulator += subcellsPerSecond * world.fixedDt();
        int subcellSteps = (int) subcellMoveAccumulator;
        if (subcellSteps <= 0) return;
        subcellMoveAccumulator -= subcellSteps;

        // Internal lane is base coords (u, v) with v=2:
        // [0,2] entry -> [1,2] -> [2,2] PROCESS CELL -> [3,2] -> [4,2] exit to neighbor tile.
        for (int step = 0; step < subcellSteps; step++) {
            // Process laneU from exit -> entry on row v=2 (rotated)
            for (int laneU = 4; laneU >= 0; laneU--) {
                int[] laneUV = rotUV(laneU, 2, rot);
                int u = laneUV[0];
                int v = laneUV[1];

                int itemId = occ[u][v];
                if (itemId == EMPTY) continue;

                Item item = world.getItem(itemId);
                if (item == null) { occ[u][v] = EMPTY; continue; }

                if (item.enteredThisTick(currentTick)) continue;

                // Special: processing cell is laneU == 2 (P)
                if (laneU == 2) {
                    // Hold the processing item in the process cell until smelt completes.
                    if (itemId == activeProcessItemID && !hasCompletedSmelt) continue;

                    int[] nextLaneUV = rotUV(3, 2, rot);
                    int nextU = nextLaneUV[0];
                    int nextV = nextLaneUV[1];
                    if (occ[nextU][nextV] == EMPTY) {
                        occ[nextU][nextV] = itemId;
                        occ[u][v] = EMPTY;

                        // Clear processing state once it leaves P
                        if (itemId == activeProcessItemID) {
                            activeProcessItemID = EMPTY;
                            processTimeLeft = 0f;
                            hasCompletedSmelt = false;
                        }
                    }
                    continue;
                }

                // If not at exit cell, move forward internally
                if (laneU < 4) {
                    // But don't allow moving INTO processing cell if it’s occupied (normal occupancy already covers)
                    int[] nextUV = rotUV(laneU + 1, 2, rot);
                    int nextU = nextUV[0];
                    int nextV = nextUV[1];
                    if (occ[nextU][nextV] == EMPTY) {
                        occ[nextU][nextV] = itemId;
                        occ[u][v] = EMPTY;

                        // If it just entered processing cell, start processing (only if empty)
                        if (laneU + 1 == 2) {
                            tryStartProcessing(world, occ[nextU][nextV]);
                        }
                    }
                    continue;
                }
                // At exit cell: handoff forward to neighbor
                Dir fwd = Dir.fromRot(rot);
                int outCellX = cellX + fwd.dx;
                int outCellY = cellY + fwd.dy;

                TileEntity outCell = world.getEntity(outCellX, outCellY);
                if (outCell == null) continue;

                if (!outCell.canAccept(item, fwd.opposite())) continue;

                occ[u][v] = EMPTY;
                outCell.accept(item, fwd.opposite(), currentTick);
            }
        }
    }

    private void tryStartProcessing(TileWorld world, int candidateItemId) {
        if (activeProcessItemID != EMPTY) return;
        Item item = world.getItem(candidateItemId);
        if (item == null) return;

        // Safety: only process correct type
        if (item.type != ItemType.DUST) return;

        activeProcessItemID = item.id;
        processTimeLeft = processTime;
        hasCompletedSmelt = false;
    }

    private void tickProcessing(TileWorld world) {
        // Ensures processing starts as soon as an item enters the process cell, and advances the timer each tick.
        if (activeProcessItemID == EMPTY) {
            int[] processorUV = rotUV(2, 2, rot);
            int processorCellItemID = occ[processorUV[0]][processorUV[1]];
            if (processorCellItemID != EMPTY) tryStartProcessing(world, processorCellItemID);
            return;
        }

        if (hasCompletedSmelt) return;

        processTimeLeft -= world.fixedDt();

        if (processTimeLeft > 0f) return;

        Item item = world.getItem(activeProcessItemID);
        if (item != null && item.type == ItemType.DUST) {
            item.type = ItemType.INGOT;
            item.value *= outputValueMultiplier;
        }
        hasCompletedSmelt = true;
    }

    @Override
    public void writeSaveData(WorldGrid.TileSave out) {
        out.f0 = processTimeLeft;
        out.i0 = activeProcessItemID;
        out.b0 = hasCompletedSmelt;
    }

    @Override
    public void readSaveData(WorldGrid.TileSave in) {
        processTimeLeft = in.f0;
        activeProcessItemID = in.i0;
        hasCompletedSmelt = in.b0;
    }
}
