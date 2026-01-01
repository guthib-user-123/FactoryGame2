package org.gudu0.AwareMemory;

import org.gudu0.AwareMemory.entities.ConveyorEntity;
import org.gudu0.AwareMemory.entities.MergerEntity;
import org.gudu0.AwareMemory.entities.SplitterEntity;
import org.junit.jupiter.api.Test;

import static org.gudu0.AwareMemory.WorldGrid.TILE_CONVEYOR;

@SuppressWarnings("GrazieInspection")
public final class AutoPlacementTest {

    // Rotation convention: 0=EAST, 1=SOUTH, 2=WEST, 3=NORTH
    private static final int E = 0, S = 1, W = 2, N = 3;

    @Test
    public void placingConveyorInEmptySpace_staysConveyor() {
        TestHarness h = new TestHarness();

        int x = 2, y = 2;
        h.place(TILE_CONVEYOR, x, y, E);

        h.assertTile(TILE_CONVEYOR, x, y, E);
        // Confirmed Visally
    }

    @Test
    public void deletingTileWorks() {
        TestHarness h = new TestHarness();

        int x = 2, y = 2;
        h.place(TILE_CONVEYOR, x, y, E);
        h.assertTile(TILE_CONVEYOR, x, y, E);

        h.delete(x, y);

        h.assertTileId(WorldGrid.TILE_EMPTY, x, y);
        // Confirmed Visually
    }

    @Test
    public void upgradesToSplitter_whenLeftAndRightOutputsAvailable_setsVariantLR() {
        TestHarness h = new TestHarness();
        int cx = 2, cy = 2;

        // For center out=E:
        // lOk -> neighbor at NORTH must accept from SOUTH => conveyor rot=N
        // rOk -> neighbor at SOUTH must accept from NORTH => conveyor rot=S
        h.place(TILE_CONVEYOR, cx, cy + 1, N);
        h.place(TILE_CONVEYOR, cx, cy - 1, S);

        h.place(TILE_CONVEYOR, cx, cy, E);

        h.assertTile(WorldGrid.TILE_SPLITTER, cx, cy, E);
        h.assertSplitterVariant(SplitterEntity.Variant.LR, cx, cy);
        // Confirmed visually
    }

    @Test
    public void upgradesToSplitter_whenForwardAndLeftAvailable_setsVariantFL() {
        TestHarness h = new TestHarness();
        int cx = 2, cy = 2;

        // fOk -> neighbor at EAST must accept from WEST => conveyor rot=E
        // lOk -> neighbor at NORTH must accept from SOUTH => conveyor rot=N
        h.place(TILE_CONVEYOR, cx + 1, cy, E);
        h.place(TILE_CONVEYOR, cx, cy + 1, N);

        h.place(TILE_CONVEYOR, cx, cy, E);

        h.assertTile(WorldGrid.TILE_SPLITTER, cx, cy, E);
        h.assertSplitterVariant(SplitterEntity.Variant.FL, cx, cy);
        // Confirmed Visually
    }

    @Test void becomesTurnCorrectly(){
        TestHarness h = new TestHarness();
        int cx = 2, cy = 2;

        h.place(TILE_CONVEYOR, cx, cy, E);
        h.place(TILE_CONVEYOR, cx + 1, cy, E);
        // 2 long line of conveyors

        h.place(TILE_CONVEYOR, cx + 1, cy - 1, S);
        // Down facing conveyor below 2nd top conveyor.

        h.assertConveyorShape(ConveyorEntity.Shape.TURN_RIGHT, cx + 1, cy);
        // last conveyor should now be a turn.


    }

    @Test
    void cornerEastToDown_isRotSouth_andTurnRightInputFromWest() {
        TestHarness h = new TestHarness();
        int cx = 2, cy = 2;

        // feeder from the west
        h.place(TILE_CONVEYOR, cx, cy, E);

        // corner outputs DOWN, so rot = S (not E)
        h.place(TILE_CONVEYOR, cx + 1, cy, S);

        // next belt continues downward
        h.place(TILE_CONVEYOR, cx + 1, cy - 1, S);

        // Corner should accept from WEST (which is out.right when out=S)
        h.assertTile(WorldGrid.TILE_CONVEYOR, cx + 1, cy, S);
        h.assertConveyorShape(ConveyorEntity.Shape.TURN_RIGHT, cx + 1, cy);
    }


    @Test
    public void turnConvertsToMergeCorrectly(){
        TestHarness h = new TestHarness();
        int cx = 2, cy = 2;
        h.place(TILE_CONVEYOR, cx, cy, E);
        // Place East facing Conveyor

        h.place(TILE_CONVEYOR, cx, cy + 1, S);
        // Plase South Facing Conveyor above (facing into it)

        h.assertConveyorShape(ConveyorEntity.Shape.TURN_LEFT, cx, cy);
        // Conveyor should have converted to a left turn conveyor

        h.place(TILE_CONVEYOR, cx, cy - 1, N);
        // Place North Facing Conveyor below the now turn conveyor, it should convert to a merge because of input top, and input botttom.


        //test passes up till here.

        h.assertTile(WorldGrid.TILE_MERGER, cx, cy, E);
        // Should have converted to merger.
    }

    @Test
    public void conveyorUpgradesToMergerWhenPossible(){
        TestHarness h = new TestHarness();
        int cx = 2, cy = 2;

        h.place(TILE_CONVEYOR, cx, cy, E);
        h.place(TILE_CONVEYOR, cx + 1, cy, E);
        h.place(TILE_CONVEYOR, cx + 2, cy, E);
        h.place(TILE_CONVEYOR, cx + 3, cy, E);
        // 4 long conveyor line facing east.
        h.assertTile(TILE_CONVEYOR, cx, cy, E);
        h.assertTile(TILE_CONVEYOR, cx + 1, cy, E);
        h.assertTile(TILE_CONVEYOR, cx + 2, cy, E);
        h.assertTile(TILE_CONVEYOR, cx + 3, cy, E);
        // All conveyors should be east facing and conveyors, no changes.
        h.assertConveyorShape(ConveyorEntity.Shape.STRAIGHT, cx, cy);
        h.assertConveyorShape(ConveyorEntity.Shape.STRAIGHT, cx + 1, cy);
        h.assertConveyorShape(ConveyorEntity.Shape.STRAIGHT, cx + 2, cy);
        h.assertConveyorShape(ConveyorEntity.Shape.STRAIGHT, cx + 3, cy);
        // All conveyors should be straight.

        h.place(TILE_CONVEYOR, cx + 1, cy + 1, S);
        // Place a down facing conveyor above the second conveyor.

        h.assertTile(WorldGrid.TILE_MERGER, cx + 1, cy, E);
        // The conveyor on 3,2 should have become a merger because: input from side (back, W), input from side (left, N), output to side (front, E).
    }

    @Test
    public void deletingConveyorMakesITgoBackToConveyorCorrectly(){
        TestHarness h = new TestHarness();
        int cx = 2, cy = 2;

        h.place(TILE_CONVEYOR, cx, cy, E);
        h.place(TILE_CONVEYOR, cx + 1, cy, E);
        h.place(TILE_CONVEYOR, cx + 2, cy, E);
        h.place(TILE_CONVEYOR, cx + 3, cy, E);
        // 4 long conveyor line facing east.
        h.assertTile(TILE_CONVEYOR, cx, cy, E);
        h.assertTile(TILE_CONVEYOR, cx + 1, cy, E);
        h.assertTile(TILE_CONVEYOR, cx + 2, cy, E);
        h.assertTile(TILE_CONVEYOR, cx + 3, cy, E);
        // All conveyors should be east facing and conveyors, no changes.
        h.assertConveyorShape(ConveyorEntity.Shape.STRAIGHT, cx, cy);
        h.assertConveyorShape(ConveyorEntity.Shape.STRAIGHT, cx + 1, cy);
        h.assertConveyorShape(ConveyorEntity.Shape.STRAIGHT, cx + 2, cy);
        h.assertConveyorShape(ConveyorEntity.Shape.STRAIGHT, cx + 3, cy);
        // All conveyors should be straight.

        h.place(TILE_CONVEYOR, cx + 1, cy + 1, S);
        // Place a down facing conveyor above the second conveyor.

        h.assertTile(WorldGrid.TILE_MERGER, cx + 1, cy, E);
        // The conveyor on 3,2 should have become a merger because: input from side (back, W), input from side (left, N), output to side (front, E).
        // Should pass

        h.delete(cx + 1, cy + 1);
        //delete the down facing conveyor that made it a merger, should bake the conveyor below convert to regular straign conveyor.
        h.assertTile(TILE_CONVEYOR, cx + 1, cy, E);
        //sould be east conveyor
        h.assertConveyorShape(ConveyorEntity.Shape.STRAIGHT, cx + 1, cy);
        // Should have converted back to a straight conveyor, because there is no extra input now.
    }

    @Test
    public void mergerDowngradesToConveyorWhenSecondInputRemoved() {
        TestHarness h = new TestHarness();
        int cx = 2, cy = 2;

        h.place(TILE_CONVEYOR, cx, cy, E);
        h.place(TILE_CONVEYOR, cx + 1, cy, E);
        h.place(TILE_CONVEYOR, cx + 2, cy, E);

        // Add second input from above to make (cx+1,cy) a merger
        h.place(TILE_CONVEYOR, cx + 1, cy + 1, S);
        h.assertTile(WorldGrid.TILE_MERGER, cx + 1, cy, E);

        // Remove that input -> should revert
        h.delete(cx + 1, cy + 1);
        h.assertTile(TILE_CONVEYOR, cx + 1, cy, E);
    }

    @Test
    public void upgradesToSplitter_whenForwardAndRightAvailable_setsVariantFR() {
        TestHarness h = new TestHarness();
        int cx = 2, cy = 2;

        // fOk -> EAST neighbor rot=E
        // rOk -> SOUTH neighbor must accept from NORTH => rot=S
        h.place(TILE_CONVEYOR, cx + 1, cy, E);
        h.place(TILE_CONVEYOR, cx, cy - 1, S);

        h.place(TILE_CONVEYOR, cx, cy, E);

        h.assertTile(WorldGrid.TILE_SPLITTER, cx, cy, E);
        h.assertSplitterVariant(SplitterEntity.Variant.FR, cx, cy);
    }

    @Test
    public void upgradesToMerger_whenTwoInputsAndForwardOk() {
        TestHarness h = new TestHarness();
        int cx = 2, cy = 2;

        // For center out=E:
        // fOk: EAST neighbor accepts from WEST => rot=E
        h.place(TILE_CONVEYOR, cx + 1, cy, E);

        // Inputs into center:
        // back input = WEST neighbor outputs EAST => rot=E
        // right input = SOUTH neighbor outputs NORTH => rot=N
        h.place(TILE_CONVEYOR, cx - 1, cy, E);
        h.place(TILE_CONVEYOR, cx, cy - 1, N);

        h.place(TILE_CONVEYOR, cx, cy, E);

        h.assertTile(WorldGrid.TILE_MERGER, cx, cy, E);
    }

    @Test
    public void mergerWinsPriority_overSplitter_whenBothArePossible() {
        TestHarness h = new TestHarness();
        int cx = 2, cy = 2;

        // Make splitter possible: fOk + lOk
        h.place(TILE_CONVEYOR, cx + 1, cy, E); // fOk
        h.place(TILE_CONVEYOR, cx, cy + 1, N); // lOk

        // Also make merger possible: 2 inputs (back + right)
        h.place(TILE_CONVEYOR, cx - 1, cy, E); // inBack
        h.place(TILE_CONVEYOR, cx, cy - 1, N); // inRight

        h.place(TILE_CONVEYOR, cx, cy, E);

        // decideAutoTileForConveyor checks merger first, so it should be merger.
        h.assertTile(WorldGrid.TILE_MERGER, cx, cy, E);
    }

    @Test
    public void deletingFeeder_updatesConveyorShapeBackToStraight() {
        TestHarness h = new TestHarness();
        int cx = 2, cy = 2;

        // Center out=E. To force TURN_LEFT: feed from NORTH into center.
        // neighborOutputsInto(from NORTH) requires NORTH neighbor outputs SOUTH => rot=S
        h.place(TILE_CONVEYOR, cx, cy + 1, S);
        h.place(TILE_CONVEYOR, cx, cy, E);

        h.assertConveyorShape(ConveyorEntity.Shape.TURN_LEFT, cx, cy);

        h.delete(cx, cy + 1);

        // With no incoming, TileWorld falls back to STRAIGHT
        h.assertConveyorShape(ConveyorEntity.Shape.STRAIGHT, cx, cy);
    }

    @Test
    public void deletingTile_deletesContainedItems() {
        TestHarness h = new TestHarness();
        int cx = 2, cy = 2;

        h.place(TILE_CONVEYOR, cx, cy, E);

        // Spawn entering from the conveyor's back edge
        Dir from = Dir.fromRot(E).opposite();
        h.spawnOnTile(cx, cy, ItemType.ORE, 1f, from);

        h.assertItemCount(1);

        h.delete(cx, cy);

        // clearEntityAt destroys contained items
        h.assertItemCount(0);
        h.assertTileId(WorldGrid.TILE_EMPTY, cx, cy);
    }

    @Test
    public void placementTest(){
        TestHarness h = new TestHarness();
        int cx = 2, cy = 2;

        h.place(TILE_CONVEYOR, cx, cy, N);
        h.place(TILE_CONVEYOR, cx, cy - 1, S);

        h.assertRot(S, cx, cy-1);
    }

    @Test
    public void conveyorCornerShapePicksTURN_LEFTvsTURN_RIGHT(){
        TestHarness h = new TestHarness();
        int cx = 2, cy = 2;


    }

    @Test
    public void mergerVariantPicksLRWhenFedFromVothSidesNoBack(){
        TestHarness h = new TestHarness();
        int cx = 2, cy = 2;

        h.place(WorldGrid.TILE_MERGER, cx, cy, E);
        h.place(TILE_CONVEYOR, cx, cy + 1, S);
        h.place(TILE_CONVEYOR, cx, cy - 1, N);
        h.place(TILE_CONVEYOR, cx + 1, cy, E);

        h.assertMergerVariant(MergerEntity.Variant.LR, cx, cy);
    }

}
