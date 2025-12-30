package org.gudu0.AwareMemory;

public enum Dir {
    EAST(1, 0), SOUTH(0, -1), WEST(-1, 0), NORTH(0, 1);

    public final int dx, dy;
    Dir(int dx, int dy) { this.dx = dx; this.dy = dy; }

    public static Dir fromRot(int rot) {
        return switch (rot & 3) {
            case 0 -> EAST;
            case 1 -> SOUTH;
            case 2 -> WEST;
            default -> NORTH;
        };
    }

    public Dir opposite() {
        return switch (this) {
            case EAST -> WEST;
            case WEST -> EAST;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
        };
    }
    public Dir left() {
        return switch (this) {
            case EAST -> NORTH;
            case NORTH -> WEST;
            case WEST -> SOUTH;
            case SOUTH -> EAST;
        };
    }

    public Dir right() {
        return switch (this) {
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
            case NORTH -> EAST;
        };
    }

}
