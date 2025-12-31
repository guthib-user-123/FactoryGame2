package org.gudu0.AwareMemory;

public final class TestHarness {

    static WorldGrid worldGrid;
    static TileWorld tileWorld;

    public static String ANSI_RESET = "\u001b[0m";
    public static String ANSI_RED = "\u001b[31m";
    public static String ANSI_GREEN = "\u001b[32m";
    public static String ANSI_YELLOW = "\u001b[33m";
    public static String ANSI_BLUE = "\u001b[34m";


    public TestHarness() {
        worldGrid = new WorldGrid(5, 5);
        tileWorld = new TileWorld(worldGrid);
    }

//    public static void main(String[] args) {
//        new TestHarness();
//        place(1, 5, 5, 0);
////        assertTile(5, 5, 5, 0);
//        place(2, 6, 5, 0);
//        dump(true, true);
//    }

    public void place(int tileID, int cellX, int cellY, int rot) {
        if (worldGrid.grid[cellX][cellY] == WorldGrid.TILE_EMPTY) {

            worldGrid.grid[cellX][cellY] = tileID;
            worldGrid.rot[cellX][cellY] = rot;

            // AUTO-UPGRADE: if we placed a conveyor, maybe it should become a splitter
            if (tileID == WorldGrid.TILE_CONVEYOR) {
                int upgraded = tileWorld.decideAutoTileForConveyor(cellX, cellY, rot);
                worldGrid.grid[cellX][cellY] = upgraded;
            }

            tileWorld.rebuildEntityAt(cellX, cellY);
        }
    }

    public void assertTile(int expectedTileID, int cellX, int cellY, int expectedRot) {
        if (worldGrid.grid[cellX][cellY] != expectedTileID) {
            System.out.println("wrong tile");
            System.out.println("Expected " + worldGrid.getTileName(expectedTileID) + ", but got " + worldGrid.getTileName(worldGrid.grid[cellX][cellY]) + ".");
            dump(true, false);
            throw new AssertionError();
        }
        if (worldGrid.rot[cellX][cellY] != expectedRot) {
            System.out.println("wrong rot");
            System.out.println("Expected " + worldGrid.getRotName(expectedRot) + ", but got " + worldGrid.getRotName(worldGrid.rot[cellX][cellY]) + ".");
            dump(false, true);
            throw new AssertionError();
        }
    }

    private static void dump(boolean dumpGrid, boolean dumpRot) {
        int w = worldGrid.wCells;
        int h = worldGrid.hCells;

        if (dumpGrid) {
            System.out.println("=== GRID (tile ids) ===");
            for (int y = h - 1; y >= 0; y--) { // top -> bottom for nicer map viewing
                for (int x = 0; x < w; x++) {
                    int id = worldGrid.grid[x][y]; // IMPORTANT: grid[x][y]
                    if (id != 0) {
                        System.out.print(ANSI_RED + id + ANSI_RESET + "  ");
                    } else {
                        System.out.print(id + "  ");
                    }
                }
                System.out.println();
            }
        }

        if (dumpRot) {
            System.out.println("=== ROT (0=E,1=S,2=W,3=N) ===");
            for (int y = h - 1; y >= 0; y--) { // top -> bottom
                for (int x = 0; x < w; x++) {
                    int r = worldGrid.rot[x][y]; // IMPORTANT: rot[x][y]
                    System.out.print(r + "  ");
                }
                System.out.println();
            }
        }
    }

}
