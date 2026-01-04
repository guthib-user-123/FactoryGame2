package org.gudu0.AwareMemory;

import org.gudu0.AwareMemory.entities.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"PatternVariableCanBeUsed", "EnhancedSwitchMigration"})
public final class TileWorld {
    private static final float FIXED_TICK = 1f / 60f;

    private final WorldGrid world;
    private final TileEntity[][] entities;

    public float fixedDt() {
        return FIXED_TICK;
    }

    private float acc = 0f;
    private int tick = 0;

    private int nextItemId = 1;
    private final Map<Integer, Item> items = new HashMap<>();

    public TileWorld(WorldGrid world) {
        this.world = world;
        this.entities = new TileEntity[world.wCells][world.hCells];
    }

    public TileEntity getEntity(int cx, int cy) {
        if (!world.inBoundsCell(cx, cy)) return null;
        return entities[cx][cy];
    }

    public void importTileSaves(WorldGrid.TileSave[] saves) {
        if (saves == null) return;

        for (WorldGrid.TileSave ts : saves) {
            if (!world.inBoundsCell(ts.cx, ts.cy)) continue;

            TileEntity te = entities[ts.cx][ts.cy];
            if (te != null) {
                te.readSaveData(ts);
            }
        }
    }

    // Called by placement code (normal path)
    public void rebuildEntityAt(int cx, int cy) {
        rebuildEntityAtInternal(cx, cy, true);
    }

    // Called by SmartPlacement only (no recursion)
    void rebuildEntityAtFromSmartPlacement(int cx, int cy) {
        rebuildEntityAtInternal(cx, cy, false);
    }

    private void rebuildEntityAtInternal(int cx, int cy, boolean runSmartPlacement) {
        // Destroy old entity + its items (your current rule)
        TileEntity old = entities[cx][cy];
        if (old != null) old.destroyContainedItems(this);

        int id = world.grid[cx][cy];
        int rot = world.rot[cx][cy];

        TileEntity created;
        switch (id) {
            case WorldGrid.TILE_CONVEYOR:
                created = new ConveyorEntity(cx, cy, rot);
                break;
            case WorldGrid.TILE_SPLITTER:
                created = new SplitterEntity(cx, cy, rot, SplitterEntity.Variant.FL);
                break; // temp default
            case WorldGrid.TILE_MERGER:
                created = new MergerEntity(cx, cy, rot, MergerEntity.Variant.BL);
                break;     // temp default
            case WorldGrid.TILE_SELLPAD:
                created = new SellpadEntity(cx, cy, rot);
                break;
            case WorldGrid.TILE_SPAWNER:
                created = new SpawnerEntity(cx, cy, rot);
                break;
            case WorldGrid.TILE_CRUSHER:
                created = new CrusherEntity(cx, cy, rot);
                break;
            case WorldGrid.TILE_SMELTER:
                created = new SmelterEntity(cx, cy, rot);
                break;
            case WorldGrid.TILE_PRESS:
                created = new PressEntity(cx, cy, rot);
                break;
            case WorldGrid.TILE_ROLLER:
                created = new RollerEntity(cx, cy, rot);
                break;
            case WorldGrid.TILE_FILTER_FL:
                created = new FilterEntity(cx, cy, rot, FilterEntity.Variant.FL);
                break;
            case WorldGrid.TILE_FILTER_FR:
                created = new FilterEntity(cx, cy, rot, FilterEntity.Variant.FR);
                break;
            case WorldGrid.TILE_FILTER_LR:
                created = new FilterEntity(cx, cy, rot, FilterEntity.Variant.LR);
                break;
            default:
                created = null;
                break;
        }

        entities[cx][cy] = created;

        if (runSmartPlacement) {
            SmartPlacement.refreshAll(this);
        }
    }

    private void clearEntityAtInternal(int cx, int cy, boolean runSmartPlacement){
        TileEntity old = entities[cx][cy];
        if (old != null) old.destroyContainedItems(this);
        entities[cx][cy] = null;

        if (runSmartPlacement) SmartPlacement.refreshAll(this);
    }

    public void clearEntityAt(int cx, int cy){
        clearEntityAtInternal(cx, cy, true);
    }

    public void clearEntityAtFromSmartPlacement(int cx, int cy){
        clearEntityAtInternal(cx, cy, false);
    }

    public void refreshAllConveyorShapes() {
        for (int y = 0; y < world.hCells; y++) {
            for (int x = 0; x < world.wCells; x++) {
                refreshConveyorShapeAt(x, y);
            }
        }
    }

    public void update(float dt) {
        acc += dt;
        while (acc >= FIXED_TICK) {
            tickOnce();
            acc -= FIXED_TICK;
        }
    }

    private void tickOnce() {
        tick++;

        // Tile-centric update: deterministic grid scan (x-major or y-major; pick one)
        for (int y = 0; y < world.hCells; y++) {
            for (int x = 0; x < world.wCells; x++) {
                TileEntity te = entities[x][y];
                if (te != null) te.step(this, tick);
            }
        }
    }

    // --- Rendering helper: subcell -> world coords ---
    public float subcellCenterX(int cellX, int u) {
        float sub = WorldGrid.CELL / 5f;
        return cellX * WorldGrid.CELL + (u + 0.5f) * sub;
    }
    public float subcellCenterY(int cellY, int v) {
        float sub = WorldGrid.CELL / 5f;
        return cellY * WorldGrid.CELL + (v + 0.5f) * sub;
    }

    private void refreshConveyorShapeAt(int cx, int cy) {
        if (!(getEntity(cx, cy) instanceof ConveyorEntity)) return;
        ConveyorEntity c = (ConveyorEntity) getEntity(cx, cy);

        assert c != null;
        Dir out = Dir.fromRot(c.rot);

        // Candidate input edges for this conveyor (based on rot/output)
        Dir inStraight = out.opposite();
        Dir inLeft     = out.left();
        Dir inRight    = out.right();

        boolean straightIn = neighborOutputsInto(cx, cy, inStraight);
        boolean leftIn     = neighborOutputsInto(cx, cy, inLeft);
        boolean rightIn    = neighborOutputsInto(cx, cy, inRight);

        // Prefer keeping current shape if it's still fed (stability)
        ConveyorEntity.Shape cur = c.getShape();
        boolean curOk;
        switch (cur) {
            case STRAIGHT:
                curOk = straightIn;
                break;
            case TURN_LEFT:
                curOk = leftIn;
                break;
            case TURN_RIGHT:
                curOk = rightIn;
                break;
            default:
                curOk = false;
                break;
        }
        if (curOk) return;

        // Otherwise choose any valid incoming connection.
        // Priority is arbitrary; you said you don't care.
        if (straightIn) c.setShape( ConveyorEntity.Shape.STRAIGHT);
        else if (leftIn) c.setShape( ConveyorEntity.Shape.TURN_LEFT);
        else if (rightIn) c.setShape( ConveyorEntity.Shape.TURN_RIGHT);
        else {
            // No incoming neighbor pointing into us:
            // fall back to output-connectivity (optional) or just default straight.
            c.setShape( ConveyorEntity.Shape.STRAIGHT);
        }
    }

    private boolean neighborOutputsInto(int cx, int cy, Dir fromEdgeIntoThis) {
        int nx = cx + fromEdgeIntoThis.dx;
        int ny = cy + fromEdgeIntoThis.dy;

        TileEntity n = getEntity(nx, ny);
        if (n == null) return false;

        Dir dirFromNeighborToThis = fromEdgeIntoThis.opposite();

        // For now: treat "output direction" as forward rot for these tiles.
        // (We'll extend for splitters/mergers later.)
        if (n instanceof SellpadEntity) return false;

        return n.outputsTo(dirFromNeighborToThis);
    }

    private boolean canOutputTo(int cx, int cy, Dir out) {
        int nx = cx + out.dx;
        int ny = cy + out.dy;
        TileEntity n = getEntity(nx, ny);
        if (n == null) return false;

        // Can neighbor accept something entering from our side?
        return n.acceptsFrom(out.opposite());
    }

    // Decide if a conveyor should be upgraded into a splitter based on available outputs.
    public int decideAutoTileForConveyor(int cx, int cy, int outRot) {
        Dir out = Dir.fromRot(outRot);

        // --- MERGER CHECK (inputs) ---
        boolean inBack  = neighborOutputsInto(cx, cy, out.opposite());
        boolean inLeft  = neighborOutputsInto(cx, cy, out.left());
        boolean inRight = neighborOutputsInto(cx, cy, out.right());

        boolean fOk = canOutputTo(cx, cy, out);

        // Any 2 of the 3 inputs -> merger
        int inCount = (inBack ? 1 : 0) + (inLeft ? 1 : 0) + (inRight ? 1 : 0);

        if (inCount >= 2) {
            return WorldGrid.TILE_MERGER;
        }


        // --- SPLITTER CHECK (outputs) ---
        boolean lOk = canOutputTo(cx, cy, out.left());
        boolean rOk = canOutputTo(cx, cy, out.right());

        if ((lOk && rOk) || (fOk && lOk) || (fOk && rOk)) return WorldGrid.TILE_SPLITTER;


        return WorldGrid.TILE_CONVEYOR;
    }

    public WorldGrid worldGrid() {
        return world;
    }

    private final OrderManager orders = new OrderManager(5); // keep 3 active

    public OrderManager getOrders() {
        return orders;
    }

    public int getTick() {
        return tick;
    }

    public float getMoney() {
        return money;
    }

    // Money is owned by TileWorld (single source of truth).
    private float money = 0f;

    // Optional: still track "earned this frame" if you like the concept (HUD effects).
    private float earnedThisFrame = 0f;

    /**
     * Add money to the player total.
     * IMPORTANT: this is the only place that should mutate money.
     */
    public void addMoney(float amount) {
        money += amount;
        earnedThisFrame += amount;

        // Let orders react to money milestones.
        orders.onMoneyChanged(money);
    }

    /**
     * Spend money for placement.
     * Returns true if the player could afford it.
     */
    public boolean trySpendMoney(float amount) {
        if (money < amount) return false;
        money -= amount;

        // Money changed, so money-milestones might (un)block; you only care about >= thresholds,
        // but calling this keeps logic centralized.
        orders.onMoneyChanged(money);
        return true;
    }

    public void clearItems(){
        items.clear();
    }
    public int exportNextItemId() { return nextItemId; }
    public Item getItem(int id) { return items.get(id); }
    public void deleteItem(int id) { items.remove(id); }
    public int itemCount() { return items.size(); }
    public void importItemSaves(WorldGrid.ItemSave[] itemsFromSave, int nextIdFromSave) {
        // Clear runtime items + occupancy
        items.clear();
        for (int y = 0; y < world.hCells; y++) {
            for (int x = 0; x < world.wCells; x++) {
                TileEntity te = entities[x][y];
                if (te == null) continue;
                for (int u = 0; u < TileEntity.N; u++) {
                    for (int v = 0; v < TileEntity.N; v++) {
                        te.occ[u][v] = TileEntity.EMPTY;
                    }
                }
            }
        }

        int maxId = 0;

        if (itemsFromSave != null) {
            for (WorldGrid.ItemSave s : itemsFromSave) {
                if (s == null) continue;
                if (!world.inBoundsCell(s.cx, s.cy)) continue;
                if (s.u < 0 || s.u >= TileEntity.N || s.v < 0 || s.v >= TileEntity.N) continue;

                TileEntity te = entities[s.cx][s.cy];
                if (te == null) continue;
                if (te.occ[s.u][s.v] != TileEntity.EMPTY) continue;

                ItemType[] types = ItemType.values();
                int tid = s.typeId & 0xFF;
                //noinspection ConstantValue
                if (tid < 0 || tid >= types.length) continue;
                ItemType type = types[tid];


                Item it = new Item(s.id, type, s.value);
                items.put(it.id, it);
                te.occ[s.u][s.v] = it.id;

                if (it.id > maxId) maxId = it.id;
            }
        }

        int candidate = nextIdFromSave;
        if (candidate <= 0 || candidate <= maxId) candidate = maxId + 1;
        nextItemId = candidate;
    }
    public Item createItem(ItemType type, float value) {
        Item it = new Item(nextItemId++, type, value);
        items.put(it.id, it);
        return it;
    }
    // Spawn into a specific tile’s entry, if valid
    public void spawnOnTile(int cx, int cy, ItemType type, float value, Dir fromEdge) {
        TileEntity te = getEntity(cx, cy);
        if (te == null) return;
        Item it = new Item(nextItemId++, type, value);
        if (!te.canAccept(it, fromEdge)) return;
        items.put(it.id, it);
        te.accept(it, fromEdge, tick);
    }
    public WorldGrid.TileSave[] exportTileSaves() {
        ArrayList<WorldGrid.TileSave> out = new ArrayList<>();

        for (int y = 0; y < world.hCells; y++) {
            for (int x = 0; x < world.wCells; x++) {
                TileEntity te = entities[x][y];
                if (te == null) continue;

                WorldGrid.TileSave ts = new WorldGrid.TileSave();
                ts.cx = x;
                ts.cy = y;

                te.writeSaveData(ts);
                out.add(ts);
            }
        }
        return out.toArray(new WorldGrid.TileSave[0]);
    }
    // Expose occupancy for drawing:
    public Iterable<ItemRenderInfo> renderInfos() {
        // Simple: walk entities, emit each occupied cell. Not optimized, but fine to start.
        java.util.ArrayList<ItemRenderInfo> out = new java.util.ArrayList<>();
        for (int y = 0; y < world.hCells; y++) {
            for (int x = 0; x < world.wCells; x++) {
                TileEntity te = entities[x][y];
                if (te == null) continue;
                for (int u = 0; u < 5; u++) {
                    for (int v = 0; v < 5; v++) {
                        int id = te.occ[u][v];
                        if (id == TileEntity.EMPTY) continue;
                        Item it = items.get (id);
                        if (it == null) continue;
                        out.add(new ItemRenderInfo(it, subcellCenterX(x, u), subcellCenterY(y, v)));
                    }
                }
            }
        }
        return out;
    }
    public ArrayList<WorldGrid.ItemSave> exportItemSaves() {
        ArrayList<WorldGrid.ItemSave> out = new ArrayList<>();

        for (int y = 0; y < world.hCells; y++) {
            for (int x = 0; x < world.wCells; x++) {
                TileEntity te = entities[x][y];
                if (te == null) continue;

                for (int u = 0; u < TileEntity.N; u++) {
                    for (int v = 0; v < TileEntity.N; v++) {
                        int id = te.occ[u][v];
                        if (id == TileEntity.EMPTY) continue;

                        Item it = items.get(id);
                        if (it == null) continue;

                        WorldGrid.ItemSave s = new WorldGrid.ItemSave();
                        s.id = it.id;
                        s.typeId = (byte) it.type.ordinal();
                        s.value = it.value;
                        s.cx = x; s.cy = y;
                        s.u = u; s.v = v;
                        out.add(s);
                    }
                }
            }
        }
        return out;
    }
    @SuppressWarnings("ClassCanBeRecord")
    public static final class ItemRenderInfo {
        private final Item item;
        private final float x;
        private final float y;

        public ItemRenderInfo(Item item, float x, float y) {
            this.item = item;
            this.x = x;
            this.y = y;
        }

        public Item item() { return item; }
        public float x() { return x; }
        public float y() { return y; }
    }
}
