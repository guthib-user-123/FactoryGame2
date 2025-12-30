package org.gudu0.AwareMemory.entities;

import org.gudu0.AwareMemory.*;

public final class SpawnerEntity extends TileEntity {
    // Tune later; “passes per tick”
    public float subcellsPerSecond = 24f; // start here (≈ 12/5 = 2.4 tiles/sec)
    private float moveAcc = 0f;

    // Spawner timing
    private float timer = 0f;
    public float interval = 0.75f;  // tune
    public ItemType spawnType = ItemType.ORE;
    public float spawnValue = 1f;

    public SpawnerEntity(int cellX, int cellY, int rot) {
        super(cellX, cellY, rot);
    }

    @Override
    public int[] entryCellFrom(Dir fromEdge) {
        // Spawner does not accept items
        return null;
    }

    @Override
    public void step(TileWorld world, int currentTick) {
        // 1) Advance internal movement (like a short belt from (2,2) -> (4,2))

        moveAcc += subcellsPerSecond * world.fixedDt();
        int passes = (int) moveAcc;
        if (passes <= 0) return;
        moveAcc -= passes;

        for (int pass = 0; pass < passes; pass++) {
            // base rot0 order: u=4..2 on row v=2
            for (int baseU = 4; baseU >= 2; baseU--) {
                int[] uv = rotUV(baseU, 2, rot);
                int u = uv[0], v = uv[1];
                int itemId = occ[u][v];
                if (itemId == EMPTY) continue;

                Item item = world.getItem(itemId);
                if (item == null) { occ[u][v] = EMPTY; continue; }

                if (item.enteredThisTick(currentTick)) continue;

                // internal move forward if not at exit
                if (baseU < 4) {
                    int[] nextUV = rotUV(baseU + 1, 2, rot);
                    int nu = nextUV[0], nv = nextUV[1];
                    if (occ[nu][nv] == EMPTY) {
                        occ[nu][nv] = itemId;
                        occ[u][v] = EMPTY;
                    }
                    continue;
                }

                // at exit: try handoff forward
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

        // 2) Spawn new item into the spawn cell (base rot0: (2,2))
        // Use FIXED_TICK timing: TileWorld ticks at 60Hz, so just add 1/60 per tick.
//        timer += (1f / 60f);
        timer += world.fixedDt();

        if (timer < interval) return;

        int[] spawnUV = rotUV(2, 2, rot);
        int su = spawnUV[0], sv = spawnUV[1];

        if (occ[su][sv] != EMPTY) {
            // blocked: don’t accumulate backlog
            timer = interval;
            return;
        }

        Item it = world.createItem(spawnType, spawnValue);
        occ[su][sv] = it.id;
        it.markEntered(currentTick);

        timer = 0f;
    }
}
