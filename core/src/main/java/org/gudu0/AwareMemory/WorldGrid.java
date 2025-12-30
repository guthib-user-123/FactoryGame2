package org.gudu0.AwareMemory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

import java.util.ArrayList;

public class WorldGrid {
    public static final int GRID_W = 30;
    public static final int GRID_H = 30;
    public static final int CELL = 32;

    public static final int TILE_EMPTY = 0;
    public static final int TILE_CONVEYOR = 1;
    public static final int TILE_SMELTER = 2;
    public static final int TILE_SELLPAD = 3;
    public static final int TILE_SPAWNER = 4;
    public static final int TILE_CRUSHER = 5;
    public static final int TILE_SPLITTER_FL = 6; // forward + left
    public static final int TILE_SPLITTER_FR = 7; // forward + right
    public static final int TILE_SPLITTER_LR = 8; // left + right
    public static final int TILE_MERGER = 9;


    public final float WORLD_W;
    public final float WORLD_H;

    public final int wCells;
    public final int hCells;

    public final int[][] grid;
    public final int[][] rot;

    // After loadWithTileWorld(), we stash loaded items until Main finishes rebuilding entities.
    private ItemSave[] pendingItems = null;
    private int pendingNextItemId = 0;


    public static class ItemSave {
        public int id;
        public String type;   // "ORE","DUST","INGOT" (string is safest for compatibility)
        public float value;

        public int cx, cy;    // tile coords
        public int u, v;      // subcell coords 0..4
    }


    private static class SaveData {
        int version = 2; // new
        int w, h;
        int[] tiles;
        int[] rots;

        // Optional (new). Old saves won't have these -> null.
        int nextItemId;
        ItemSave[] items;
    }


    public WorldGrid() {
        wCells = (GRID_W * 2);
        hCells = (GRID_H * 2);
        WORLD_W = wCells * CELL;
        WORLD_H = hCells * CELL;

        grid = new int[wCells][hCells];
        rot  = new int[wCells][hCells];
    }

    public boolean inBoundsCell(int cx, int cy) {
        return cx >= 0 && cy >= 0 && cx < wCells && cy < hCells;
    }

    public void saveWithTileWorld(String name, TileWorld tileWorld) {
        SaveData s = new SaveData();
        s.w = wCells;
        s.h = hCells;
        s.tiles = new int[s.w * s.h];
        s.rots  = new int[s.w * s.h];

        for (int y = 0; y < s.h; y++) {
            for (int x = 0; x < s.w; x++) {
                int i = x + y * s.w;
                s.tiles[i] = grid[x][y];
                s.rots[i]  = rot[x][y];
            }
        }

        // New (v2): items + next id
        s.nextItemId = tileWorld.exportNextItemId();
        ArrayList<ItemSave> list = tileWorld.exportItemSaves();
        s.items = list.toArray(new ItemSave[0]);

        Json json = new Json();
        FileHandle fh = Gdx.files.local(name);
        fh.writeString(json.prettyPrint(s), false);
    }

    public void loadWithTileWorld(String name, TileWorld tileWorld) {
        FileHandle fh = Gdx.files.local(name);
        if (!fh.exists()) return;

        Json json = new Json();
        SaveData s = json.fromJson(SaveData.class, fh);

        if (s.w != wCells || s.h != hCells) {
            throw new RuntimeException("Save dimensions mismatch. Save=" + s.w + "x" + s.h +
                " Current=" + wCells + "x" + hCells);
        }

        for (int y = 0; y < s.h; y++) {
            for (int x = 0; x < s.w; x++) {
                int i = x + y * s.w;
                grid[x][y] = s.tiles[i];
                rot[x][y]  = s.rots[i];
            }
        }

        // Stash items for later. Old saves: s.items == null, s.nextItemId == 0.
        pendingItems = s.items;
        pendingNextItemId = s.nextItemId;
    }

    public void applyLoadedItemsTo(TileWorld tileWorld) {
        tileWorld.importItemSaves(pendingItems, pendingNextItemId);
        pendingItems = null;
        pendingNextItemId = 0;
    }


    public void save(String name) {
        SaveData s = new SaveData();
        s.w = wCells;
        s.h = hCells;
        s.tiles = new int[s.w * s.h];
        s.rots  = new int[s.w * s.h];

        for (int y = 0; y < s.h; y++) {
            for (int x = 0; x < s.w; x++) {
                int i = x + y * s.w;
                s.tiles[i] = grid[x][y];
                s.rots[i]  = rot[x][y];
            }
        }

        Json json = new Json();
        FileHandle fh = Gdx.files.local(name);
        fh.writeString(json.prettyPrint(s), false);
    }

    public void load(String name) {
        FileHandle fh = Gdx.files.local(name);
        if (!fh.exists()) return;

        Json json = new Json();
        SaveData s = json.fromJson(SaveData.class, fh);

        if (s.w != wCells || s.h != hCells) {
            throw new RuntimeException("Save dimensions mismatch. Save=" + s.w + "x" + s.h +
                " Current=" + wCells + "x" + hCells);
        }

        for (int y = 0; y < s.h; y++) {
            for (int x = 0; x < s.w; x++) {
                int i = x + y * s.w;
                grid[x][y] = s.tiles[i];
                rot[x][y] = s.rots[i];
            }
        }
    }
}
