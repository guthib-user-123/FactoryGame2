package org.gudu0.AwareMemory.entities;

import org.gudu0.AwareMemory.*;

public final class SmelterEntity extends TileEntity {
    // Tune later; “passes per tick”
    public float subcellsPerSecond = 24f; // start here (≈ 12/5 = 2.4 tiles/sec)
    private float moveAcc = 0f;

    // Processing
    public float processTime = 1.75f; // tune
    public float valueMultiplier = 2.0f;

    private int processingItemId = EMPTY;
    private float remaining = 0f;
    private boolean done = false;

    public SmelterEntity(int cellX, int cellY, int rot) {
        super(cellX, cellY, rot);
    }

    @Override
    public int[] entryCellFrom(Dir fromEdge) {
        Dir fwd = Dir.fromRot(rot);
        Dir back = fwd.opposite();
        if (fromEdge != back) return null;
        return rotUV(0, 2, rot);
    }

    @Override
    public boolean canAccept(Item item, Dir fromEdge) {
        if (item.type != ItemType.DUST) return false;
        return super.canAccept(item, fromEdge);
    }

    @Override
    public void step(TileWorld world, int currentTick) {
        tickProcessing(world);
        moveAcc += subcellsPerSecond * world.fixedDt();
        int passes = (int) moveAcc;
        if (passes <= 0) return;
        moveAcc -= passes;

        for (int pass = 0; pass < passes; pass++) {
            for (int baseU = 4; baseU >= 0; baseU--) {
                int[] uv = rotUV(baseU, 2, rot);
                int u = uv[0], v = uv[1];

                int itemId = occ[u][v];
                if (itemId == EMPTY) continue;

                Item item = world.getItem(itemId);
                if (item == null) { occ[u][v] = EMPTY; continue; }

                if (item.enteredThisTick(currentTick)) continue;

                if (baseU == 2) {
                    if (itemId == processingItemId && !done) continue;

                    int[] nextUV = rotUV(3, 2, rot);
                    int nu = nextUV[0], nv = nextUV[1];
                    if (occ[nu][nv] == EMPTY) {
                        occ[nu][nv] = itemId;
                        occ[u][v] = EMPTY;

                        if (itemId == processingItemId) {
                            processingItemId = EMPTY;
                            remaining = 0f;
                            done = false;
                        }
                    }
                    continue;
                }

                if (baseU < 4) {
                    int[] nextUV = rotUV(baseU + 1, 2, rot);
                    int nu = nextUV[0], nv = nextUV[1];
                    if (occ[nu][nv] == EMPTY) {
                        occ[nu][nv] = itemId;
                        occ[u][v] = EMPTY;

                        if (baseU + 1 == 2) {
                            tryStartProcessing(world, occ[nu][nv]);
                        }
                    }
                    continue;
                }

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
        if (it.type != ItemType.DUST) return;

        processingItemId = it.id;
        remaining = processTime;
        done = false;
    }

    private void tickProcessing(TileWorld world) {
        if (processingItemId == EMPTY) {
            int[] pUV = rotUV(2, 2, rot);
            int pid = occ[pUV[0]][pUV[1]];
            if (pid != EMPTY) tryStartProcessing(world, pid);
            return;
        }

        if (done) return;

        remaining -= world.fixedDt();

        if (remaining > 0f) return;

        Item it = world.getItem(processingItemId);
        if (it != null && it.type == ItemType.DUST) {
            it.type = ItemType.INGOT;
            it.value *= valueMultiplier; // apply once
        }
        done = true;
    }
}
