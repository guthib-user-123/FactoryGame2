package org.gudu0.AwareMemory;

import org.gudu0.AwareMemory.entities.ConveyorEntity;
import org.gudu0.AwareMemory.entities.SplitterEntity;

import static org.junit.jupiter.api.Assertions.*;

public final class TestHarness {

    public final WorldGrid world;
    public final TileWorld tileWorld;

    public TestHarness() {
        this.world = new WorldGrid(5, 5);
        this.tileWorld = new TileWorld(world);
    }

    public void delete(int cx, int cy) {
        world.grid[cx][cy] = WorldGrid.TILE_EMPTY;
        tileWorld.clearEntityAt(cx, cy);
        tileWorld.refreshAutoTilesNear(cx, cy);

    }

    public void place(int tileID, int cx, int cy, int rot) {
        if (world.grid[cx][cy] != WorldGrid.TILE_EMPTY) return;

        world.grid[cx][cy] = tileID;
        world.rot[cx][cy] = rot;

        if (tileID == WorldGrid.TILE_CONVEYOR) {
            int upgraded = tileWorld.decideAutoTileForConveyor(cx, cy, rot);
            world.grid[cx][cy] = upgraded;
        }

        tileWorld.rebuildEntityAt(cx, cy);
        tileWorld.refreshAutoTilesNear(cx, cy);
    }

    // ---- Assertions ----

    public void assertTileId(int expectedTileID, int cx, int cy) {
        int got = world.grid[cx][cy];
        assertEquals(
            expectedTileID, got,
            () -> "TileID mismatch @(" + cx + "," + cy + ")\n"
                + "Expected: " + world.getTileName(expectedTileID) + "\n"
                + "Got:      " + world.getTileName(got) + "\n\n"
                + dump(true, false)
        );
//        System.out.println(dump(true, false));

    }

    public void assertRot(int expectedRot, int cx, int cy) {
        int got = world.rot[cx][cy];
        assertEquals(
            expectedRot, got,
            () -> "Rot mismatch @(" + cx + "," + cy + ")\n"
                + "Expected: " + world.getRotName(expectedRot) + "\n"
                + "Got:      " + world.getRotName(got) + "\n\n"
                + dump(false, true)
        );
//        System.out.println(dump(false, true));

    }

    public void assertTile(int expectedTileID, int cx, int cy, int expectedRot) {
        assertTileId(expectedTileID, cx, cy);
        assertRot(expectedRot, cx, cy);
    }

    public void assertSplitterVariant(SplitterEntity.Variant expected, int cx, int cy) {
        TileEntity te = tileWorld.getEntity(cx, cy);
        //noinspection SimplifiableAssertion
        assertTrue(te instanceof SplitterEntity,
            () -> "Expected SplitterEntity @(" + cx + "," + cy + "), got: " + (te == null ? "null" : te.getClass().getSimpleName())
                + "\n\n" + dump(true, true));

        SplitterEntity s = (SplitterEntity) te;
        assertEquals(expected, s.getVariant(),
            () -> "Splitter variant mismatch @(" + cx + "," + cy + ")\n"
                + "Expected: " + expected + "\n"
                + "Got:      " + s.getVariant() + "\n\n" + dump(true, true));
//        System.out.println(dump(true, true));
    }

    public void assertConveyorShape(ConveyorEntity.Shape expected, int cx, int cy) {
        TileEntity te = tileWorld.getEntity(cx, cy);
        //noinspection SimplifiableAssertion
        assertTrue(te instanceof ConveyorEntity,
            () -> "Expected ConveyorEntity @(" + cx + "," + cy + "), got: " + (te == null ? "null" : te.getClass().getSimpleName())
                + "\n\n" + dump(true, true));

        ConveyorEntity c = (ConveyorEntity) te;
        assertEquals(expected, c.getShape(),
            () -> "Conveyor shape mismatch @(" + cx + "," + cy + ")\n"
                + "Expected: " + expected + "\n"
                + "Got:      " + c.getShape() + "\n\n" + dump(true, true));
//        System.out.println(dump(true, true));

    }

    public void spawnOnTile(int cx, int cy, ItemType type, float value, Dir fromEdge) {
        tileWorld.spawnOnTile(cx, cy, type, value, fromEdge);
    }

    public void assertItemCount(int expected) {
        assertEquals(expected, tileWorld.itemCount(),
            () -> "Item count mismatch\nExpected: " + expected + "\nGot: " + tileWorld.itemCount() + "\n\n" + dump(true, true));
//        System.out.println(dump(true, true));

    }

    // ---- Dump ----

    private String dump(boolean dumpGrid, boolean dumpRot) {
        int w = world.wCells;
        int h = world.hCells;

        StringBuilder sb = new StringBuilder();

        if (dumpGrid) {
            sb.append("=== GRID (tile ids) ===\n");
            for (int y = h - 1; y >= 0; y--) {
                for (int x = 0; x < w; x++) {
                    sb.append(world.grid[x][y]).append("  ");
                }
                sb.append("\n");
            }
        }

        if (dumpRot) {
            sb.append("=== ROT (0=E,1=S,2=W,3=N) ===\n");
            for (int y = h - 1; y >= 0; y--) {
                for (int x = 0; x < w; x++) {
                    sb.append(world.rot[x][y]).append("  ");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
