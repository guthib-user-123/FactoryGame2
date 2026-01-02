package org.gudu0.AwareMemory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.utils.Json;


@SuppressWarnings("EnhancedSwitchMigration")
public class WorldGrid {
    public static int GRID_W = 30;
    public static int GRID_H = 30;
    public static final int CELL = 32;

    public static final int TILE_EMPTY = 0;
    public static final int TILE_CONVEYOR = 1;
    public static final int TILE_SMELTER = 2;
    public static final int TILE_SELLPAD = 3;
    public static final int TILE_SPAWNER = 4;
    public static final int TILE_CRUSHER = 5;
    public static final int TILE_SPLITTER = 6;
    public static final int TILE_MERGER = 7;
    public static final int TILE_PRESS = 8;
    public static final int TILE_ROLLER = 9;
    public static final int TILE_FILTER_LR = 10;
    public static final int TILE_FILTER_FR = 11;
    public static final int TILE_FILTER_FL = 12;

    public final float WORLD_W;
    public final float WORLD_H;

    public final int wCells;
    public final int hCells;

    public final int[][] grid;
    public final int[][] rot;

    // After loadWithTileWorld(), we stash loaded items until Main finishes rebuilding entities.
    private ItemSave[] pendingItems = null;
    private TileSave[] pendingTileSaves = null;
    private int pendingNextItemId = 0;


    public static class ItemSave {
        public int id;
        public byte typeId;   // ItemType.ordinal()
        public float value;

        public int cx, cy;    // tile coords
        public int u, v;      // subcell coords 0..4
    }


    @SuppressWarnings("unused")
    public static class SaveData {
        public int w, h;
        public int[] tiles;
        public int[] rots;

        public TileSave[] tileSaves;
        public ItemSave[] items;
        public int nextItemId;
    }
    @SuppressWarnings("unused")
    public static class TileSave {
        public int cx, cy;

        // machine-specific fields (sparse; only some used per tile)
        public float f0, f1;
        public int i0, i1, i2;
        public boolean b0;
    }

    public WorldGrid() {
        wCells = (GRID_W * 2);
        hCells = (GRID_H * 2);
        WORLD_W = wCells * CELL;
        WORLD_H = hCells * CELL;


        grid = new int[wCells][hCells];
        rot  = new int[wCells][hCells];
    }
    public WorldGrid(int width, int height) {
        GRID_W = width;
        GRID_H = height;
        wCells = (GRID_W * 2);
        hCells = (GRID_H * 2);
        WORLD_W = wCells * CELL;
        WORLD_H = hCells * CELL;

        grid = new int[wCells][hCells];
        rot  = new int[wCells][hCells];
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean inBoundsCell(int cx, int cy) {
        return cx >= 0 && cy >= 0 && cx < wCells && cy < hCells;
    }

    public String getTileName(int id){
        String idToReturn = "TILE_EMPTY";
        switch (id){
            case 0: idToReturn = "TILE_EMPTY"; break;
            case 1: idToReturn = "TILE_CONVEYOR"; break;
            case 2: idToReturn = "TILE_SMELTER"; break;
            case 3: idToReturn = "TILE_SELLPAD"; break;
            case 4: idToReturn = "TILE_SPAWNER"; break;
            case 5: idToReturn = "TILE_CRUSHER"; break;
            case 6: idToReturn = "TILE_SPLITTER"; break;
            case 7: idToReturn = "TILE_MERGER"; break;
            case 8: idToReturn = "TILE_PRESS"; break;
            case 9: idToReturn = "TILE_ROLLER"; break;
            case 10: idToReturn = "TILE_FILTER_LR"; break;
            case 11: idToReturn = "TILE_FILTER_FR"; break;
            case 12: idToReturn = "TILE_FILTER_FL"; break;

        }
        return idToReturn;
    }

    public String getRotName(int direction){
        String dirToReturn = "ERROR";
        switch (direction){
            case 0: dirToReturn = "RIGHT"; break;
            case 1: dirToReturn = "DOWN"; break;
            case 2: dirToReturn = "LEFT"; break;
            case 3: dirToReturn = "UP"; break;
        }
        return dirToReturn;
    }

    public void saveWithTileWorld(String name, TileWorld tileWorld) {
        SaveIO.save(name, this, tileWorld);
    }


    public void loadWithTileWorld(String name) {
        SaveIO.LoadedData s = SaveIO.load(name);
        if (s == null) return;

        if (s.w != wCells || s.h != hCells) {
            throw new RuntimeException("Save dimensions mismatch. Save=" + s.w + "x" + s.h +
                " Current=" + wCells + "x" + hCells);
        }

        // Fill grid/rot from linear arrays
        for (int y = 0; y < s.h; y++) {
            for (int x = 0; x < s.w; x++) {
                int i = x + y * s.w;
                grid[x][y] = s.tiles[i];
                rot[x][y]  = s.rots[i];
            }
        }

        // Stash for later (same pattern you already use)
        pendingItems = s.items;
        pendingNextItemId = s.nextItemId;
        pendingTileSaves = s.tileSaves;
    }


    public void applyLoadedItemsTo(TileWorld tileWorld) {
        tileWorld.importItemSaves(pendingItems, pendingNextItemId);
        tileWorld.importTileSaves(pendingTileSaves);

        pendingItems = null;
        pendingNextItemId = 0;
        pendingTileSaves = null;
    }

    public int getTileID(int x, int y) {
        return grid[x][y];
    }

    private boolean isWeb() {
        return Gdx.app != null && Gdx.app.getType() == ApplicationType.WebGL;
    }

    private Json createJson() {
        Json json = new Json();
        json.setTypeName(null);
        return json;
    }
}
