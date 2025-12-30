package org.gudu0.AwareMemory;

public abstract class TileEntity {
    public static final int N = 5;
    public static final int EMPTY = -1;

    public final int cellX, cellY;   // build-grid cell position
    public int rot;                  // 0..3

    // Occupancy: item id per subcell, or EMPTY
    protected final int[][] occ = new int[N][N];

    protected TileEntity(int cellX, int cellY, int rot) {
        this.cellX = cellX;
        this.cellY = cellY;
        this.rot = rot;
        for (int u = 0; u < N; u++) for (int v = 0; v < N; v++) occ[u][v] = EMPTY;
    }

    // Entry cell for an incoming edge, in LOCAL coords.
    // Return null if this tile does not accept from that edge.
    public abstract int[] entryCellFrom(Dir fromEdge);

    // Can this tile accept this item coming from that edge right now?
    public boolean canAccept(Item item, Dir fromEdge) {
        int[] uv = entryCellFrom(fromEdge);
        return uv != null && occ[uv[0]][uv[1]] == EMPTY;
    }
    // Can this tile output items to this edge direction?
    // Default: "forward" only.
    public boolean outputsTo(Dir outEdge) {
        return Dir.fromRot(rot) == outEdge;
    }

    // Actually place item into entry cell (TileWorld calls this after canAccept).
    public void accept(Item item, Dir fromEdge, int currentTick) {
        int[] uv = entryCellFrom(fromEdge);
        if (uv == null) throw new IllegalStateException("No entry for " + fromEdge);
        if (occ[uv[0]][uv[1]] != EMPTY) throw new IllegalStateException("Entry occupied");
        occ[uv[0]][uv[1]] = item.id;
        item.markEntered(currentTick);
    }

    // Called by TileWorld: run movement inside this tile.
    // "passes" lets you do stepsPerTick by calling multiple times.
    public abstract void step(TileWorld world, int currentTick);

    // When tile is removed: delete any contained items (rule you chose).
    public void destroyContainedItems(TileWorld world) {
        for (int u = 0; u < N; u++) {
            for (int v = 0; v < N; v++) {
                int id = occ[u][v];
                if (id != EMPTY) {
                    world.deleteItem(id);
                    occ[u][v] = EMPTY;
                }
            }
        }
    }
    public int getItemIdAt(int u, int v) {
        return occ[u][v];
    }

    public boolean acceptsFrom(Dir fromEdge) {
        return entryCellFrom(fromEdge) != null;
    }


    // Rotate a LOCAL coordinate from base-rot(0) space into current rot space.
    // rot = 0 right, 1 down, 2 left, 3 up
    public static int[] rotUV(int u, int v, int rot) {
        rot &= 3;
        switch (rot) {
            case 0:
                return new int[]{u, v};
            case 1:
                return new int[]{v, N - 1 - u};
            case 2:
                return new int[]{N - 1 - u, N - 1 - v};
            default:
                return new int[]{N - 1 - v, u};
        }
    }

    // Called during save
    public void writeSaveData(WorldGrid.TileSave out) {
        // default: nothing
    }

    // Called after entity is created on load
    public void readSaveData(WorldGrid.TileSave in) {
        // default: nothing
    }

}
