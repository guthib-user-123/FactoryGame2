package org.gudu0.AwareMemory.entities;

import org.gudu0.AwareMemory.Dir;
import org.gudu0.AwareMemory.Item;
import org.gudu0.AwareMemory.TileEntity;
import org.gudu0.AwareMemory.TileWorld;

public final class ConveyorEntity extends TileEntity {
    // Tune later; “passes per tick”
    public float subcellsPerSecond = 100f; // start here (≈ 12/5 = 2.4 tiles/sec)
    private float moveAcc = 0f;


    public enum Shape { STRAIGHT, TURN_LEFT, TURN_RIGHT }

    private Shape shape = Shape.STRAIGHT;

    public void setShape(Shape s) { this.shape = s; }
    public Shape getShape() { return shape; }

    public ConveyorEntity(int cellX, int cellY, int rot) {
        super(cellX, cellY, rot);
    }

    @Override
    public int[] entryCellFrom(Dir fromEdge) {
        Dir out = Dir.fromRot(rot);

        Dir in = out.opposite(); // default straight.
        switch (shape) {
            case STRAIGHT: {
                in = out.opposite(); // from behind
                break;
            }
            case TURN_LEFT: {
                in = out.left();    // from left side
                break;
            }
            case TURN_RIGHT: {
                in = out.right();  // from right side
                break;
            }
        }

        if (fromEdge != in) return null;

        // Base rot0 (output EAST) entry cells:
        // STRAIGHT: from WEST edge -> (0,2)
        // LEFT-IN:  from NORTH edge -> (2,4)
        // RIGHT-IN: from SOUTH edge -> (2,0)
        int[] base = new int[]{0, 2}; // default to straight
        switch (shape) {
            case STRAIGHT: {
                base = new int[]{0, 2};
                break;
            }
            case TURN_LEFT: {
                base = new int[]{2, 4};
                break;
            }
            case TURN_RIGHT: {
                base = new int[]{2, 0};
                break;
            }
        }

        return rotUV(base[0], base[1], rot);
    }


    @Override
    public void step(TileWorld world, int currentTick) {
        int[][] base = pathBase();

        moveAcc += subcellsPerSecond * world.fixedDt();
        int passes = (int) moveAcc;
        if (passes <= 0) return;
        moveAcc -= passes;

        for (int pass = 0; pass < passes; pass++) {
            // move exit->entry along the path: indices last -> first
            for (int i = base.length - 1; i >= 0; i--) {
                int[] uv = rotUV(base[i][0], base[i][1], rot);
                int u = uv[0], v = uv[1];
                int itemId = occ[u][v];
                if (itemId == EMPTY) continue;

                Item item = world.getItem(itemId);
                if (item == null) { occ[u][v] = EMPTY; continue; }
                if (item.enteredThisTick(currentTick)) continue;

                // if not last cell in path, move to next cell in path
                if (i < base.length - 1) {
                    int[] nextUV = rotUV(base[i+1][0], base[i+1][1], rot);
                    int nu = nextUV[0], nv = nextUV[1];
                    if (occ[nu][nv] == EMPTY) {
                        occ[nu][nv] = itemId;
                        occ[u][v] = EMPTY;
                    }
                    continue;
                }

                // last cell: try handoff to neighbor in outDir()
                Dir out = Dir.fromRot(rot);
                int nx = cellX + out.dx;
                int ny = cellY + out.dy;

                TileEntity neighbor = world.getEntity(nx, ny);
                if (neighbor == null) continue;
                if (!neighbor.canAccept(item, out.opposite())) continue;

                occ[u][v] = EMPTY;
                neighbor.accept(item, out.opposite(), currentTick);
            }
        }
    }

    private static final int[][] PATH_STRAIGHT = {
        {0,2},{1,2},{2,2},{3,2},{4,2}
    };
    // input from NORTH -> output EAST
    private static final int[][] PATH_LEFT_IN = {
        {2,4},{2,3},{2,2},{3,2},{4,2}
    };
    // input from SOUTH -> output EAST
    private static final int[][] PATH_RIGHT_IN = {
        {2,0},{2,1},{2,2},{3,2},{4,2}
    };

    private int[][] pathBase() {
        int[][] returnShape = PATH_STRAIGHT; // Default to straight
        switch (shape) {
            case STRAIGHT: {
                //noinspection DataFlowIssue
                returnShape = PATH_STRAIGHT;
                break;
            }
            case TURN_LEFT: {
                returnShape = PATH_LEFT_IN;
                break;
            }
            case TURN_RIGHT: {
                returnShape = PATH_RIGHT_IN;
                break;
            }
        }
        return returnShape;
    }
}
