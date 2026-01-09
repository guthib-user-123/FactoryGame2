package org.gudu0.AwareMemory.entities;

import org.gudu0.AwareMemory.*;

public final class PressEntity extends TileEntity {
    // Tune later; “passes per tick”
    public float subcellsPerSecond = 24f; // start here (≈ 12/5 = 2.4 tiles/sec)
    private float moveAcc = 0f;

    // Processing
    public float processTime = 1.75f; // tune
    private int processingItemId = EMPTY;
    private float remaining = 0f;
    private boolean done = false;

    public PressEntity(int cellX, int cellY, int rot) {
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
        if (item.type != ItemType.INGOT) return false;
        return super.canAccept(item, fromEdge);
    }

    @Override
    public void step(TileWorld world, int currentTick) {
        // Tick processing timer first (deterministic: finish then attempt movement this tick)
        tickProcessing(world);

        moveAcc += subcellsPerSecond * world.fixedDt() * world.getItemSpeedMul();

        int passes = (int) moveAcc;
        if (passes <= 0) return;
        moveAcc -= passes;

        for (int pass = 0; pass < passes; pass++) {
            // Process baseU from exit -> entry on row v=2 (rotated)
            for (int baseU = 4; baseU >= 0; baseU--) {
                int[] uv = rotUV(baseU, 2, rot);
                int u = uv[0], v = uv[1];

                int itemId = occ[u][v];
                if (itemId == EMPTY) continue;

                Item item = world.getItem(itemId);
                if (item == null) { occ[u][v] = EMPTY; continue; }

                if (item.enteredThisTick(currentTick)) continue;

                // Special: processing cell is baseU == 2 (P)
                if (baseU == 2) {
                    // If this is the processing item and not done, it cannot move.
                    if (itemId == processingItemId && !done) continue;

                    // If done, try to move into output buffer (baseU 3)
                    int[] nextUV = rotUV(3, 2, rot);
                    int nu = nextUV[0], nv = nextUV[1];
                    if (occ[nu][nv] == EMPTY) {
                        occ[nu][nv] = itemId;
                        occ[u][v] = EMPTY;

                        // Clear processing state once it leaves P
                        if (itemId == processingItemId) {
                            processingItemId = EMPTY;
                            remaining = 0f;
                            done = false;
                        }
                    }
                    continue;
                }

                // If not at exit cell, move forward internally
                if (baseU < 4) {
                    // But don't allow moving INTO processing cell if it’s occupied (normal occupancy already covers)
                    int[] nextUV = rotUV(baseU + 1, 2, rot);
                    int nu = nextUV[0], nv = nextUV[1];
                    if (occ[nu][nv] == EMPTY) {
                        occ[nu][nv] = itemId;
                        occ[u][v] = EMPTY;

                        // If it just entered processing cell, start processing (only if empty)
                        if (baseU + 1 == 2) {
                            tryStartProcessing(world, occ[nu][nv]);
                        }
                    }
                    continue;
                }

                // At exit cell: handoff forward to neighbor
                Dir fwd = Dir.fromRot(rot);
                int nx = cellX + fwd.dx;
                int ny = cellY + fwd.dy;

                TileEntity neighbor = world.getEntity(nx, ny);
                if (neighbor == null) continue;

                if (!neighbor.canAccept(item, fwd.opposite())) continue;

                occ[u][v] = EMPTY;
                neighbor.accept(item, fwd.opposite(), currentTick);
            }
        }
    }

    private void tryStartProcessing(TileWorld world, int itemIdInP) {
        if (processingItemId != EMPTY) return;
        Item it = world.getItem(itemIdInP);
        if (it == null) return;

        // Safety: only process correct type
        if (it.type != ItemType.INGOT) return;

        processingItemId = it.id;
        remaining = processTime;
        done = false;
    }

    private void tickProcessing(TileWorld world) {
        if (processingItemId == EMPTY) {
            // If P cell somehow has an item, but we aren’t tracking it, start it.
            int[] pUV = rotUV(2, 2, rot);
            int pid = occ[pUV[0]][pUV[1]];
            if (pid != EMPTY) tryStartProcessing(world, pid);
            return;
        }

        if (done) return;

        remaining -= world.fixedDt();

        if (remaining > 0f) return;

        Item it = world.getItem(processingItemId);
        if (it != null && it.type == ItemType.INGOT) {
            it.type = ItemType.PLATE;
            // value unchanged (tune later if desired)
        }
        done = true;
    }
    @Override
    public void writeSaveData(WorldGrid.TileSave out) {
        out.f0 = remaining;
        out.i0 = processingItemId;
        out.b0 = done;
    }

    @Override
    public void readSaveData(WorldGrid.TileSave in) {
        remaining = in.f0;
        processingItemId = in.i0;
        done = in.b0;
    }

}
