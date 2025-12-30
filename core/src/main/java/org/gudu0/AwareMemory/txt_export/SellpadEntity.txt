package org.gudu0.AwareMemory.entities;

import org.gudu0.AwareMemory.*;

public final class SellpadEntity extends TileEntity {

    public SellpadEntity(int cellX, int cellY, int rot) {
        super(cellX, cellY, rot);
    }

    @Override
    public int[] entryCellFrom(Dir fromEdge) {
        // Accept from ANY side.
        // Use the edge-center local cell as the entry point:
        // West  -> (0,2)
        // East  -> (4,2)
        // South -> (2,0)
        // North -> (2,4)
        return switch (fromEdge) {
            case WEST  -> rotUV(0, 2, rot);
            case EAST  -> rotUV(4, 2, rot);
            case SOUTH -> rotUV(2, 0, rot);
            case NORTH -> rotUV(2, 4, rot);
        };
    }

    @Override
    public void accept(Item item, Dir fromEdge, int currentTick) {
        // Instant sell: do NOT occupy a cell.
        // Still markEntered for consistency (not strictly required here).
        item.markEntered(currentTick);

        // Add money and delete item immediately
        // (matches your “sell instantly” decision)
        // Note: no float text yet; you can add that later via a callback/queue.
        // TileWorld owns economy output.
        // We need TileWorld here, but accept() doesn’t have it, so we handle in step by storing IDs.
        //
        // Easiest solution: occupy for 1 tick then sell in step().
        // That keeps TileEntity interface unchanged.
        super.accept(item, fromEdge, currentTick);
    }

    @Override
    public void step(TileWorld world, int currentTick) {
        // Sell any item currently sitting on any entry cell(s) we might have used.
        // Since we accept from any side, easiest: scan entire 5x5 and consume.
        // This is cheap (25 cells) and deterministic.

        for (int u = 0; u < N; u++) {
            for (int v = 0; v < N; v++) {
                int id = occ[u][v];
                if (id == EMPTY) continue;

                Item it = world.getItem(id);
                occ[u][v] = EMPTY;

                if (it != null) {
                    world.addEarned(it.value);
                    world.deleteItem(it.id);
                }
            }
        }
    }
}
