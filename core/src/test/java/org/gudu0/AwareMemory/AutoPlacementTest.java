package org.gudu0.AwareMemory;

import org.junit.jupiter.api.Test;

public final class AutoPlacementTest {

    // Rotation convention: 0=EAST, 1=SOUTH, 2=WEST, 3=NORTH
    private static final int E = 0, S = 1, W = 2, N = 3;

    @Test
    public void placingConveyorInEmptySpace_staysConveyor() {
        TestHarness h = new TestHarness();

        int x = 3, y = 3, rot = E;
        h.place(WorldGrid.TILE_CONVEYOR, x, y, rot);

        h.assertTile(WorldGrid.TILE_CONVEYOR, x, y, E);
    }

    @Test
    public void conveyorUpgradesToSplitterLR_whenLeftAndRightOutputsAreValid() {
        TestHarness h = new TestHarness();

        int cx = 5, cy = 5;

        // Center outputs EAST, so:
        // left output is NORTH neighbor (must accept from SOUTH) => conveyor travel NORTH (rot=N)
        // right output is SOUTH neighbor (must accept from NORTH) => conveyor travel SOUTH (rot=S)
        h.place(WorldGrid.TILE_CONVEYOR, cx, cy + 1, N); // accepts from SOUTH
        h.place(WorldGrid.TILE_CONVEYOR, cx, cy - 1, S); // accepts from NORTH

        // Place center last so auto-upgrade sees neighbors/entities
        h.place(WorldGrid.TILE_CONVEYOR, cx, cy, E);

        h.assertTile(WorldGrid.TILE_SPLITTER, cx, cy, E);
    }

    @Test
    public void conveyorUpgradesToSplitterFL_whenForwardAndLeftOutputsAreValid() {
        TestHarness h = new TestHarness();

        int cx = 5, cy = 5;

        // Forward output for EAST is the EAST neighbor accepting from WEST => conveyor travel EAST (rot=E)
        h.place(WorldGrid.TILE_CONVEYOR, cx + 1, cy, E); // accepts from WEST

        // Left output for EAST is NORTH neighbor accepting from SOUTH => travel NORTH (rot=N)
        h.place(WorldGrid.TILE_CONVEYOR, cx, cy + 1, N); // accepts from SOUTH

        // Place center last
        h.place(WorldGrid.TILE_CONVEYOR, cx, cy, E);

        h.assertTile(WorldGrid.TILE_SPLITTER, cx, cy, E);
    }

    @Test
    public void conveyorUpgradesToMerger_whenForwardOutputOk_andAtLeastTwoInputsFeedIn() {
        TestHarness h = new TestHarness();

        int cx = 5, cy = 5;

        // Forward output OK: EAST neighbor accepts from WEST => travel EAST (rot=E)
        h.place(WorldGrid.TILE_CONVEYOR, cx + 1, cy, E);

        // Inputs for out=EAST are: back(WEST neighbor outputs EAST), left(NORTH neighbor outputs SOUTH), right(SOUTH neighbor outputs NORTH)
        // Provide two inputs: WEST outputs EAST, NORTH outputs SOUTH
        h.place(WorldGrid.TILE_CONVEYOR, cx - 1, cy, E); // outputs EAST into center
        h.place(WorldGrid.TILE_CONVEYOR, cx, cy + 1, S); // outputs SOUTH into center

        // Place center last
        h.place(WorldGrid.TILE_CONVEYOR, cx, cy, E);

        h.assertTile(WorldGrid.TILE_MERGER, cx, cy, E);
    }

    @Test
    public void mergerTakesPriorityOverSplitter_whenBothCouldApply() {
        TestHarness h = new TestHarness();

        int cx = 5, cy = 5;

        // Make splitter LR possible (valid left/right outputs)
        h.place(WorldGrid.TILE_CONVEYOR, cx, cy + 1, N); // accepts from SOUTH (left output)
        h.place(WorldGrid.TILE_CONVEYOR, cx, cy - 1, S); // accepts from NORTH (right output)

        // Make merger possible too (forward output OK + 2 inputs)
        h.place(WorldGrid.TILE_CONVEYOR, cx + 1, cy, E); // forward output OK (accepts from WEST)
        h.place(WorldGrid.TILE_CONVEYOR, cx - 1, cy, E); // input from back (outputs EAST into center)
        // second input: use NORTH neighbor outputting SOUTH into center (already used as left-output acceptor above)
        // For it to output SOUTH, it must travel SOUTH (rot=S), so overwrite it:
        h.place(WorldGrid.TILE_CONVEYOR, cx, cy + 1, S); // outputs SOUTH into center (now it's an input instead)

        // Place center last
        h.place(WorldGrid.TILE_CONVEYOR, cx, cy, E);

        // Merger check runs before splitter check => merger should win
        h.assertTile(WorldGrid.TILE_MERGER, cx, cy, E);
    }
}
