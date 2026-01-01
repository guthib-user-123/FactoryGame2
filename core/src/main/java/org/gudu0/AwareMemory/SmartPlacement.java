package org.gudu0.AwareMemory;

import org.gudu0.AwareMemory.entities.ConveyorEntity;
import org.gudu0.AwareMemory.entities.SplitterEntity;
import org.gudu0.AwareMemory.entities.MergerEntity;


/**
 * SmartPlacement is EDIT-TIME logic only:
 * - Called after place/delete (not during simulation tick)
 * - Never changes rot (player-owned)
 * - Phase 1: only updates Conveyor shape + Splitter variant
 */
public final class SmartPlacement {

    private SmartPlacement() {}

    // Keep iterations small; this converges fast.
    private static final int MAX_PASSES = 4;

    public static void refreshAll(TileWorld world) {
        WorldGrid grid = world.worldGrid();

        // Repeat because conveyors depend on splitter variants and vice versa.
        for (int pass = 0; pass < MAX_PASSES; pass++) {
            boolean changed = false;

            changed |= refreshAllAutoTiles(world, grid);       // NEW: id upgrades/downgrades
            changed |= refreshAllMergerVariants(world, grid);
            changed |= refreshAllSplitterVariants(world, grid);
            changed |= refreshAllConveyorShapes(world, grid);


            if (!changed) return; // stable
        }
    }

    // -------------------------
    // Merger variant refresh
    // -------------------------

    private static boolean refreshAllMergerVariants(TileWorld world, WorldGrid grid) {
        boolean changedAny = false;

        MergerEntity.Variant[][] desired = new MergerEntity.Variant[grid.wCells][grid.hCells];

        // Snapshot pass: compute desired variants without mutating anything yet.
        for (int y = 0; y < grid.hCells; y++) {
            for (int x = 0; x < grid.wCells; x++) {
                if (!(world.getEntity(x, y) instanceof MergerEntity m)) continue;

                Dir outDir = Dir.fromRot(m.rot);

                // Merger output is forward; the 3 possible inputs are:
                Dir inputBack  = outDir.opposite();
                Dir inputLeft  = outDir.left();
                Dir inputRight = outDir.right();

                boolean fedBack  = neighborOutputsInto(world, grid, x, y, inputBack);
                boolean fedLeft  = neighborOutputsInto(world, grid, x, y, inputLeft);
                boolean fedRight = neighborOutputsInto(world, grid, x, y, inputRight);

                MergerEntity.Variant cur = m.getVariant();
                MergerEntity.Variant next = cur;

                // Stability rule: if current variant is still validly fed, keep it.
                if (!isMergerVariantFed(cur, fedBack, fedLeft, fedRight)) {
                    // Otherwise choose a variant that matches what is fed.
                    // Priority:
                    // 1) If back is fed, prefer a variant that includes back (BL / BR)
                    // 2) If back isn't fed but left+right are fed, use LR
                    // 3) If fewer than 2 inputs are fed, keep current (don’t thrash)
                    if (fedBack) {
                        if (fedLeft) next = MergerEntity.Variant.BL;
                        else if (fedRight) next = MergerEntity.Variant.BR;
                        // else: only back fed => keep current
                    } else {
                        if (fedLeft && fedRight) next = MergerEntity.Variant.LR;
                        // else: only one side fed => keep current
                    }
                }

                desired[x][y] = next;
            }
        }

        // Apply pass
        for (int y = 0; y < grid.hCells; y++) {
            for (int x = 0; x < grid.wCells; x++) {
                if (!(world.getEntity(x, y) instanceof MergerEntity m)) continue;

                MergerEntity.Variant next = desired[x][y];
                if (next == null) continue;

                if (m.getVariant() != next) {
                    m.setVariant(next);
                    changedAny = true;
                }
            }
        }

        return changedAny;
    }

    private static boolean isMergerVariantFed(
        MergerEntity.Variant variant,
        boolean fedBack,
        boolean fedLeft,
        boolean fedRight
    ) {
        return switch (variant) {
            case BL -> fedBack && fedLeft;
            case BR -> fedBack && fedRight;
            case LR -> fedLeft && fedRight;
        };
    }


    // -------------------------
    // Splitter variant refresh
    // -------------------------

    private static boolean refreshAllSplitterVariants(TileWorld world, WorldGrid grid) {
        boolean changedAny = false;

        // Compute desired variants (snapshot style) then apply.
        SplitterEntity.Variant[][] desired = new SplitterEntity.Variant[grid.wCells][grid.hCells];

        for (int y = 0; y < grid.hCells; y++) {
            for (int x = 0; x < grid.wCells; x++) {
                if (!(world.getEntity(x, y) instanceof SplitterEntity s)) continue;

                Dir travel = Dir.fromRot(s.rot);

                boolean canForward = canOutputTo(world, grid, x, y, travel);
                boolean canLeft    = canOutputTo(world, grid, x, y, travel.left());
                boolean canRight   = canOutputTo(world, grid, x, y, travel.right());

                // Your stated rules (rot never changes):
                // - if left+right valid => LR
                // - else if forward+left valid => FL
                // - else if forward+right valid => FR
                // - else keep current (don’t thrash)
                SplitterEntity.Variant cur = s.getVariant();
                SplitterEntity.Variant next = cur;

                if (canLeft && canRight) next = SplitterEntity.Variant.LR;
                else if (canForward && canLeft) next = SplitterEntity.Variant.FL;
                else if (canForward && canRight) next = SplitterEntity.Variant.FR;

                desired[x][y] = next;
            }
        }

        for (int y = 0; y < grid.hCells; y++) {
            for (int x = 0; x < grid.wCells; x++) {
                if (!(world.getEntity(x, y) instanceof SplitterEntity s)) continue;

                SplitterEntity.Variant next = desired[x][y];
                if (next == null) continue;

                if (s.getVariant() != next) {
                    s.setVariant(next);
                    changedAny = true;
                }
            }
        }

        return changedAny;
    }

    // -------------------------
    // Conveyor shape refresh
    // -------------------------

    private static boolean refreshAllConveyorShapes(TileWorld world, WorldGrid grid) {
        boolean changedAny = false;

        ConveyorEntity.Shape[][] desired = new ConveyorEntity.Shape[grid.wCells][grid.hCells];

        for (int y = 0; y < grid.hCells; y++) {
            for (int x = 0; x < grid.wCells; x++) {
                if (!(world.getEntity(x, y) instanceof ConveyorEntity belt)) continue;

                Dir out = Dir.fromRot(belt.rot);

                // These are the only 3 possible input sides for a conveyor with fixed rot.
                Dir inputStraight = out.opposite(); // from behind
                Dir inputLeft     = out.left();     // from left side
                Dir inputRight    = out.right();    // from right side

                boolean fedStraight = neighborOutputsInto(world, grid, x, y, inputStraight);
                boolean fedLeft     = neighborOutputsInto(world, grid, x, y, inputLeft);
                boolean fedRight    = neighborOutputsInto(world, grid, x, y, inputRight);

                ConveyorEntity.Shape cur = belt.getShape();

                // Stability: if current shape is still fed, keep it.
                if (isShapeFed(cur, fedStraight, fedLeft, fedRight)) {
                    desired[x][y] = cur;
                    continue;
                }

                // Otherwise choose a fed input. Priority is deterministic.
                ConveyorEntity.Shape next;
                if (fedStraight) next = ConveyorEntity.Shape.STRAIGHT;
                else if (fedLeft) next = ConveyorEntity.Shape.TURN_LEFT;
                else if (fedRight) next = ConveyorEntity.Shape.TURN_RIGHT;
                else next = ConveyorEntity.Shape.STRAIGHT; // default when isolated

                desired[x][y] = next;
            }
        }

        for (int y = 0; y < grid.hCells; y++) {
            for (int x = 0; x < grid.wCells; x++) {
                if (!(world.getEntity(x, y) instanceof ConveyorEntity belt)) continue;

                ConveyorEntity.Shape next = desired[x][y];
                if (next == null) continue;

                if (belt.getShape() != next) {
                    belt.setShape(next);
                    changedAny = true;
                }
            }
        }

        return changedAny;
    }

    private static boolean isShapeFed(
        ConveyorEntity.Shape shape,
        boolean fedStraight,
        boolean fedLeft,
        boolean fedRight
    ) {
        return switch (shape) {
            case STRAIGHT -> fedStraight;
            case TURN_LEFT -> fedLeft;
            case TURN_RIGHT -> fedRight;
        };
    }

    // -------------------------
    // Connectivity helpers
    // -------------------------

    /**
     * True if the neighbor on side (fromEdgeIntoThis) outputs towards this cell.
     * Example: fromEdgeIntoThis = WEST means neighbor is at (x-1,y) and must output EAST.
     */
    private static boolean neighborOutputsInto(TileWorld world, WorldGrid grid, int x, int y, Dir fromEdgeIntoThis) {
        int nx = x + fromEdgeIntoThis.dx;
        int ny = y + fromEdgeIntoThis.dy;

        if (!grid.inBoundsCell(nx, ny)) return false;

        var neighbor = world.getEntity(nx, ny);
        if (neighbor == null) return false;

        // Direction from neighbor to this cell.
        Dir outFromNeighbor = fromEdgeIntoThis.opposite();

        // Sellpad should never be treated as an output source.
        if (neighbor instanceof org.gudu0.AwareMemory.entities.SellpadEntity) return false;

        return neighbor.outputsTo(outFromNeighbor);
    }

    /**
     * SmartPlacement output check:
     * Returns true if we COULD output into that side after the player extends the line.
     *
     * - Empty cell => true (potential outlet)
     * - Existing entity => must accept from our side
     * - Out of bounds => false
     *
     * This is intentionally more permissive than "actually connected right now".
     */
    private static boolean canOutputTo(TileWorld world, WorldGrid grid, int x, int y, Dir outDir) {
        int nx = x + outDir.dx;
        int ny = y + outDir.dy;

        if (!grid.inBoundsCell(nx, ny)) return false;

        TileEntity neighbor = world.getEntity(nx, ny);

        // IMPORTANT: empty space is still a valid *potential* output for SmartPlacement
        if (neighbor == null) return true;

        return neighbor.acceptsFrom(outDir.opposite());
    }

    /**
     * For MERGER upgrade only:
     * Forward outlet is considered "ok" if:
     * - in bounds, and
     * - either the forward cell is empty (player can extend later),
     *   OR there is a neighbor that currently accepts from us.
     *
     * We intentionally do NOT use this relaxed rule for splitter outputs,
     * otherwise empty space makes splitters appear possible everywhere.
     */
    private static boolean mergerForwardOutletOk(TileWorld world, WorldGrid grid, int x, int y, Dir forwardOut) {
        int nx = x + forwardOut.dx;
        int ny = y + forwardOut.dy;

        if (!grid.inBoundsCell(nx, ny)) return false;

        TileEntity neighbor = world.getEntity(nx, ny);
        if (neighbor == null) return true; // empty space is a potential outlet

        return neighbor.acceptsFrom(forwardOut.opposite());
    }


    private static boolean refreshAllAutoTiles(TileWorld world, WorldGrid grid) {
        boolean changedAny = false;

        int[][] desiredId = new int[grid.wCells][grid.hCells];

        // Snapshot compute
        for (int y = 0; y < grid.hCells; y++) {
            for (int x = 0; x < grid.wCells; x++) {
                int curId = grid.grid[x][y];
                if (curId != WorldGrid.TILE_CONVEYOR &&
                    curId != WorldGrid.TILE_SPLITTER &&
                    curId != WorldGrid.TILE_MERGER) {
                    continue;
                }

                int rot = grid.rot[x][y];
                Dir out = Dir.fromRot(rot);

                // Potential INPUT edges for this cell (relative to out)
                Dir back  = out.opposite();
                Dir left  = out.left();
                Dir right = out.right();

                boolean fedBack  = neighborOutputsInto(world, grid, x, y, back);
                boolean fedLeft  = neighborOutputsInto(world, grid, x, y, left);
                boolean fedRight = neighborOutputsInto(world, grid, x, y, right);

                // Potential OUTPUT edges a splitter/merger could use
                boolean canForward = canOutputTo(world, grid, x, y, out);
                boolean canLeft    = canOutputTo(world, grid, x, y, out.left());
                boolean canRight   = canOutputTo(world, grid, x, y, out.right());

                boolean mergerHasTwoInputs =
                    (fedBack && fedLeft) || (fedBack && fedRight) || (fedLeft && fedRight);

                boolean splitterHasTwoOutputs =
                    (canLeft && canRight) || (canForward && canLeft) || (canForward && canRight);

                int nextId = WorldGrid.TILE_CONVEYOR;

                if (canForward && mergerHasTwoInputs) {
                    nextId = WorldGrid.TILE_MERGER;
                } else if (fedBack && splitterHasTwoOutputs) {
                    nextId = WorldGrid.TILE_SPLITTER;
                }

                desiredId[x][y] = nextId;
            }
        }

        // Apply
        for (int y = 0; y < grid.hCells; y++) {
            for (int x = 0; x < grid.wCells; x++) {
                int nextId = desiredId[x][y];
                if (nextId == 0) continue; // untouched cells
                if (grid.grid[x][y] == nextId) continue;

                grid.grid[x][y] = nextId;

                // IMPORTANT: rebuild without calling SmartPlacement again
                world.rebuildEntityAtFromSmartPlacement(x, y);

                changedAny = true;
            }
        }

        return changedAny;
    }

}

