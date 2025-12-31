package org.gudu0.AwareMemory;

@SuppressWarnings({"EnhancedSwitchMigration", "DuplicateBranchesInSwitch"})
public enum Dir {
    EAST(1, 0), SOUTH(0, -1), WEST(-1, 0), NORTH(0, 1);

    public final int dx, dy;
    Dir(int dx, int dy) { this.dx = dx; this.dy = dy; }

    public static Dir fromRot(int rot) {
        switch (rot & 3) {
            case 0:
                return EAST;
            case 1:
                return SOUTH;
            case 2:
                return WEST;
            default:
                return NORTH;
        }
    }

    public Dir opposite() {
        switch (this) {
            case EAST:
                return WEST;
            case WEST:
                return EAST;
            case NORTH:
                return SOUTH;
            case SOUTH:
                return NORTH;
            default:
                return NORTH;
        }
    }
    public Dir left() {
        switch (this) {
            case EAST:
                return NORTH;
            case NORTH:
                return WEST;
            case WEST:
                return SOUTH;
            case SOUTH:
                return EAST;
            default:
                return NORTH;
        }
    }

    public Dir right() {
        switch (this) {
            case EAST:
                return SOUTH;
            case SOUTH:
                return WEST;
            case WEST:
                return NORTH;
            case NORTH:
                return EAST;
            default:
                return EAST;
        }
    }

}
