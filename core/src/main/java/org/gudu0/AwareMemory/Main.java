package org.gudu0.AwareMemory;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.gudu0.AwareMemory.entities.ConveyorEntity;

import java.util.ArrayList;
import java.util.Arrays;


public class Main extends ApplicationAdapter {
    private WorldGrid world;
    private PlayerSystem player;
//    private ItemsSystem itemsSys;
    private TileWorld tileWorld;
    private Hud hud;

    private ShapeRenderer shapes;
    private final Vector3 mouse = new Vector3();
    private int hoverCellX = -1;
    private int hoverCellY = -1;
    private boolean hoverValid = false;
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private float money = 1000000.0f;

    private static final float COST_CONVEYOR = 1f;
    private static final float COST_SMELTER  = 15f;
    private static final float COST_SELLPAD  = 10f;
    private static final float COST_SPAWNER  = 25f;
    private static final float COST_CRUSHER  = 20f;
    private static final float COST_SPLIT_FL = 12f;
    private static final float COST_SPLIT_FR = 12f;
    private static final float COST_SPLIT_LR = 12f;
    private static final float COST_MERGER = 12f;

    private static final int HOTBAR_SLOTS = 10;
    // Each page is an array of tile IDs.
    // 0 means “empty slot”
    private final int[][] hotbarPages = {
        { // Page 0
            WorldGrid.TILE_CONVEYOR,
            WorldGrid.TILE_SMELTER,
            WorldGrid.TILE_SELLPAD,
            WorldGrid.TILE_SPAWNER,
            WorldGrid.TILE_CRUSHER,
            0,
            0,
            0,
            0,
            0
        },
        { // Page 1 (example future)
            0,0,0,0,0,0,0,0,0,0
        }
    };

    private int hotbarPage = 0;
    private int hotbarSlot = 0; // 0..9
    private int scrollDelta = 0;



    private static final float REFUND_RATE = 0.75f;

    private int selectedTile = WorldGrid.TILE_CONVEYOR; // default

    private int selectedRot = 0;
    private float stateTime = 0f;

    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] conveyorAnim = new Animation[4];
    private final ArrayList<Texture> conveyorTextures = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] smelterAnim = new Animation[4];
    private final ArrayList<Texture> smelterTextures = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] sellPadAnim = new Animation[4];
    private final ArrayList<Texture> sellPadTextures = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] spawnerAnim = new Animation[4];
    private final ArrayList<Texture> spawnerTextures = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] crusherAnim = new Animation[4];
    private final ArrayList<Texture> crusherTextures = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] splitterLRAnim = new Animation[4];
    private final ArrayList<Texture> splitterLRTextures = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] splitterFRAnim = new Animation[4];
    private final ArrayList<Texture> splitterFRTextures = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] splitterFLAnim = new Animation[4];
    private final ArrayList<Texture> splitterFLTextures = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private TextureRegion[][] mergerSprite = new TextureRegion[4][3]; // [outRot][variant]
    private final ArrayList<Texture> mergerSpriteTextures = new ArrayList<>();

    private TextureRegion[][] conveyorTurn = new TextureRegion[4][2]; // [rot][0=left,1=right]
    private final ArrayList<Texture> conveyorTurnTextures = new ArrayList<>();

    private boolean debugOverlay = false;

    private Texture whiteTex;
    private TextureRegion whiteRegion;
    private TextureRegion[] iconByTileId;


    Texture playerTex;
    private Texture oreTex;
    private Texture dustTex;
    private Texture ingotTex;
    private Texture crushedOreTex;
    private Texture gearTex;
    private Texture plateTex;
    private Texture rodTex;
    private Texture machinePartsTex;


    // --- METHODS --- \\
    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public void create() {
        world = new WorldGrid();
        playerTex = new Texture(Gdx.files.internal("player.png"));
        player = new PlayerSystem(playerTex, world);
//        itemsSys = new ItemsSystem();
        tileWorld = new TileWorld(world);
        hud = new Hud();

        camera = new OrthographicCamera();
        viewport = new FitViewport(1920, 1080, camera);
        viewport.apply(true);

        camera.position.set(world.WORLD_W / 2f, world.WORLD_H / 2f, 0f);
        camera.update();

        shapes = new ShapeRenderer();
        batch = new SpriteBatch();

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        whiteTex = new Texture(pm);
        pm.dispose();
        whiteRegion = new TextureRegion(whiteTex);

        // Scrollwheel
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                scrollDelta += (int) Math.signum(amountY);
                return true;
            }
        });


        // Enable alpha
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Machine Animations and Sprites
        {
            // 32 frames: 0.05f = ~20fps, 0.10f = 10fps
            conveyorAnim[0] = makeAnim("Conveyor/right/conveyor_right", 32, false);  //right
            conveyorAnim[1] = makeAnim("Conveyor/down/conveyor_down", 32, false);        //down
            conveyorAnim[2] = makeAnim("Conveyor/left/conveyor_left", 32, true);     //left
            conveyorAnim[3] = makeAnim("Conveyor/up/conveyor_up", 32, false);    //up

            smelterAnim[0] = makeAnim("smelter/right/smelter_right", 2, false);
            smelterAnim[1] = makeAnim("smelter/down/smelter_down", 2, false);
            smelterAnim[2] = makeAnim("smelter/left/smelter_left", 2, false);
            smelterAnim[3] = makeAnim("smelter/up/smelter_up", 2, false);

            sellPadAnim[0] = makeAnim("seller/sellpad", 1, false);
            sellPadAnim[1] = makeAnim("seller/sellpad", 1, false);
            sellPadAnim[2] = makeAnim("seller/sellpad", 1, false);
            sellPadAnim[3] = makeAnim("seller/sellpad", 1, false);

            spawnerAnim[0] = makeAnim("spawner/right/spawner_right", 1, false);
            spawnerAnim[1] = makeAnim("spawner/down/spawner_down", 1, false);
            spawnerAnim[2] = makeAnim("spawner/left/spawner_left", 1, false);
            spawnerAnim[3] = makeAnim("spawner/up/spawner_up", 1, false);

            crusherAnim[0] = makeAnim("crusher/right/crusher_right", 1, false);
            crusherAnim[1] = makeAnim("crusher/down/crusher_down", 1, false);
            crusherAnim[2] = makeAnim("crusher/left/crusher_left", 1, false);
            crusherAnim[3] = makeAnim("crusher/up/crusher_up", 1, false);

            splitterLRAnim[0] = makeAnim("splitterLR/right/splitter_LR_right", 1, false);
            splitterLRAnim[1] = makeAnim("splitterLR/down/splitter_LR_down", 1, false);
            splitterLRAnim[2] = makeAnim("splitterLR/left/splitter_LR_left", 1, false);
            splitterLRAnim[3] = makeAnim("splitterLR/up/splitter_LR_up", 1, false);

            splitterFRAnim[0] = makeAnim("splitterFR/right/splitter_FR_right", 1, false);
            splitterFRAnim[1] = makeAnim("splitterFR/down/splitter_FR_down", 1, false);
            splitterFRAnim[2] = makeAnim("splitterFR/left/splitter_FR_left", 1, false);
            splitterFRAnim[3] = makeAnim("splitterFR/up/splitter_FR_up", 1, false);

            splitterFLAnim[0] = makeAnim("splitterFL/right/splitter_FL_right", 1, false);
            splitterFLAnim[1] = makeAnim("splitterFL/down/splitter_FL_down", 1, false);
            splitterFLAnim[2] = makeAnim("splitterFL/left/splitter_FL_left", 1, false);
            splitterFLAnim[3] = makeAnim("splitterFL/up/splitter_FL_up", 1, false);

            // outRot: 0=E,1=S,2=W,3=N
            // Output NORTH (rot 3)
            mergerSprite[3][0] = loadMerger("Conveyor/mergeUp/conveyor_merge_EWN.png");
            mergerSprite[3][1] = loadMerger("Conveyor/mergeUp/conveyor_merge_ESN.png");
            mergerSprite[3][2] = loadMerger("Conveyor/mergeUp/conveyor_merge_WSN.png");

            // Output SOUTH (rot 1)
            mergerSprite[1][0] = loadMerger("Conveyor/mergeDown/conveyor_merge_EWS.png");
            mergerSprite[1][1] = loadMerger("Conveyor/mergeDown/conveyor_merge_WNS.png");
            mergerSprite[1][2] = loadMerger("Conveyor/mergeDown/conveyor_merge_ENS.png");

            // Output EAST (rot 0)
            mergerSprite[0][0] = loadMerger("Conveyor/mergeRight/conveyor_merge_NSE.png");
            mergerSprite[0][1] = loadMerger("Conveyor/mergeRight/conveyor_merge_SWE.png");
            mergerSprite[0][2] = loadMerger("Conveyor/mergeRight/conveyor_merge_NWE.png");

            // Output WEST (rot 2)
            mergerSprite[2][0] = loadMerger("Conveyor/mergeLeft/conveyor_merge_NSW.png");
            mergerSprite[2][1] = loadMerger("Conveyor/mergeLeft/conveyor_merge_NEW.png");
            mergerSprite[2][2] = loadMerger("Conveyor/mergeLeft/conveyor_merge_SEW.png");



            // conveyorTurn[rot][variant]
            // rot: OUTPUT direction (0=E,1=S,2=W,3=N)
            // variant 0/1: the two possible corner shapes for that output dir (as used by draw logic)

            conveyorTurn[0][0] = loadTurn("Conveyor/rotateUp/conveyor_rotate_up_left.png"); // correct
            conveyorTurn[0][1] = loadTurn("Conveyor/rotateDown/conveyor_rotate_down_left.png"); // correct

            conveyorTurn[1][0] = loadTurn("Conveyor/rotateRight/conveyor_rotate_right_up.png"); // correct
            conveyorTurn[1][1] = loadTurn("Conveyor/rotateLeft/conveyor_rotate_left_up.png"); // correct

            conveyorTurn[2][0] = loadTurn("Conveyor/rotateDown/conveyor_rotate_down_right.png"); // correct
            conveyorTurn[2][1] = loadTurn("Conveyor/rotateUp/conveyor_rotate_up_right.png"); // correct

            conveyorTurn[3][0] = loadTurn("Conveyor/rotateLeft/conveyor_rotate_left_down.png"); // correct
            conveyorTurn[3][1] = loadTurn("Conveyor/rotateRight/conveyor_rotate_right_down.png"); // correct

        }
        selectedTile = hotbarPages[hotbarPage][hotbarSlot];

        // Item Textures
        oreTex   = new Texture(Gdx.files.internal("item/gold/ore.png"));
        dustTex  = new Texture(Gdx.files.internal("item/gold/dust.png"));
        ingotTex = new Texture(Gdx.files.internal("item/gold/ingot.png"));
        crushedOreTex = new Texture(Gdx.files.internal("item/crushedOre.png"));
        gearTex = new Texture(Gdx.files.internal("item/gear.png"));
        plateTex = new Texture(Gdx.files.internal("item/plate.png"));
        rodTex = new Texture(Gdx.files.internal("item/rod.png"));
        machinePartsTex = new Texture(Gdx.files.internal("item/machineParts.png"));

        // Hotbar Icons
        {
            int maxId = 0;
            maxId = Math.max(maxId, WorldGrid.TILE_CONVEYOR);
            maxId = Math.max(maxId, WorldGrid.TILE_SMELTER);
            maxId = Math.max(maxId, WorldGrid.TILE_SELLPAD);
            maxId = Math.max(maxId, WorldGrid.TILE_SPAWNER);
            maxId = Math.max(maxId, WorldGrid.TILE_CRUSHER);
            maxId = Math.max(maxId, WorldGrid.TILE_SPLITTER_FL);
            maxId = Math.max(maxId, WorldGrid.TILE_SPLITTER_FR);
            maxId = Math.max(maxId, WorldGrid.TILE_SPLITTER_LR);
            maxId = Math.max(maxId, WorldGrid.TILE_MERGER);

            iconByTileId = new TextureRegion[maxId + 1];

            // Pick representative frames.
            // Use rot=0 and time=0 for animations.
            iconByTileId[WorldGrid.TILE_CONVEYOR] = conveyorAnim[0].getKeyFrame(0f, true);
            iconByTileId[WorldGrid.TILE_SMELTER] = smelterAnim[0].getKeyFrame(0f, true);
            iconByTileId[WorldGrid.TILE_CRUSHER] = crusherAnim[0].getKeyFrame(0f, true);
            iconByTileId[WorldGrid.TILE_SPAWNER] = spawnerAnim[0].getKeyFrame(0f, true);
            iconByTileId[WorldGrid.TILE_SELLPAD] = sellPadAnim[0].getKeyFrame(0f, false); // if this is a TextureRegion already; otherwise new TextureRegion(sellpadTex)
            iconByTileId[WorldGrid.TILE_MERGER] = mergerSprite[0][0];


            iconByTileId[WorldGrid.TILE_SPLITTER_FL] = splitterFLAnim[0].getKeyFrame(0f, false); // whatever your arrays are
            iconByTileId[WorldGrid.TILE_SPLITTER_FR] = splitterFRAnim[0].getKeyFrame(0f, false);
            iconByTileId[WorldGrid.TILE_SPLITTER_LR] = splitterLRAnim[0].getKeyFrame(0f, false);
        }
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.08f, 0.08f, 0.10f, 1f);
        float dt = Gdx.graphics.getDeltaTime();
        stateTime += dt;

        player.updateMovement(dt, world);
        player.updateMachinePush(dt, world);
        controls();
        doCameraStuff(dt);
        doGetMouseHover();

        doSelectionInput();
        doRotationInput();
        doGetPlacement();
        tileWorld.update(dt)    ;
        money += tileWorld.consumeEarnedThisFrame();

        doGridDraw();
        doPlacedTilesDraw();
        doDrawItems();

        doDrawPlayer();
        doHoverDraw();
        if (debugOverlay) {
            drawDebugOverlay();
        }
        hud.draw(batch, money, tileWorld.itemCount(), hotbarPage, hotbarSlot, hotbarPages[hotbarPage], iconByTileId, whiteRegion);


    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        playerTex.dispose();
        hud.dispose();
        oreTex.dispose();
        dustTex.dispose();
        ingotTex.dispose();
        crushedOreTex.dispose();
        gearTex.dispose();
        plateTex.dispose();
        rodTex.dispose();
        machinePartsTex.dispose();
        whiteTex.dispose();


        for (Texture t : conveyorTextures){
            t.dispose();
        }
        for (Texture t : smelterTextures){
            t.dispose();
        }
        for (Texture t : sellPadTextures){
            t.dispose();
        }
        for (Texture t : spawnerTextures){
            t.dispose();
        }
        for (Texture t : crusherTextures){
            t.dispose();
        }
        for (Texture t : splitterFLTextures){
            t.dispose();
        }
        for (Texture t : splitterFRTextures){
            t.dispose();
        }
        for (Texture t : splitterLRTextures){
            t.dispose();
        }
        for (Texture t : conveyorTurnTextures){
            t.dispose();
        }
        for (Texture t : mergerSpriteTextures){
            t.dispose();
        }

        conveyorTextures.clear();
        smelterTextures.clear();
        sellPadTextures.clear();
        spawnerTextures.clear();
        crusherTextures.clear();
        conveyorTurnTextures.clear();
        mergerSpriteTextures.clear();

    }

    private TextureRegion loadTurn(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        conveyorTurnTextures.add(t);
        return new TextureRegion(t);
    }
    private TextureRegion loadMerger(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        mergerSpriteTextures.add(t);
        return new TextureRegion(t);
    }


    private void drawDebugOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);

        float sub = WorldGrid.CELL / 5f;

        // 1) Draw 5x5 subcell grid lines ONLY for built tiles
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.5f, 0.5f, 0.5f, 0.5f);
        for (int y = 0; y < world.hCells; y++) {
            for (int x = 0; x < world.wCells; x++) {
                if (world.grid[x][y] == WorldGrid.TILE_EMPTY) continue;

                float x0 = x * WorldGrid.CELL;
                float y0 = y * WorldGrid.CELL;

                // vertical sub-lines
                for (int i = 1; i < 5; i++) {
                    float lx = x0 + i * sub;
                    shapes.line(lx, y0, lx, y0 + WorldGrid.CELL);
                }

                // horizontal sub-lines
                for (int i = 1; i < 5; i++) {
                    float ly = y0 + i * sub;
                    shapes.line(x0, ly, x0 + WorldGrid.CELL, ly);
                }
            }
        }

        shapes.end();
        shapes.setColor(1f, 1f, 1f, 1f);
        // 2) Draw occupied subcells as filled boxes
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        for (int y = 0; y < world.hCells; y++) {
            for (int x = 0; x < world.wCells; x++) {
                TileEntity te = tileWorld.getEntity(x, y);
                if (te == null) continue;

                float x0 = x * WorldGrid.CELL;
                float y0 = y * WorldGrid.CELL;

                for (int u = 0; u < 5; u++) {
                    for (int v = 0; v < 5; v++) {
                        int id = te.getItemIdAt(u, v);
                        if (id == TileEntity.EMPTY) continue;

                        float rx = x0 + u * sub;
                        float ry = y0 + v * sub;
                        shapes.rect(rx + 1f, ry + 1f, sub - 2f, sub - 2f);
                    }
                }
            }
        }

        shapes.end();
    }

    public void controls(){
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) Gdx.app.exit();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            if (Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setWindowedMode(1280, 720);
            } else {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) saveGame("save2");
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
            loadGame("save2");
            // Second pass: now that all entities exist, refresh conveyor shapes everywhere
            tileWorld.refreshAllConveyorShapes();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            // Spawn onto the hovered tile (if it has an entity)
            if (hoverValid) {
                int cx = hoverCellX, cy = hoverCellY;
                int rot = world.rot[cx][cy];
                Dir fwd = Dir.fromRot(rot);

                // Conveyor accepts from BACK side, so spawn "coming from back"
                Dir fromEdge = fwd.opposite();

                tileWorld.spawnOnTile(cx, cy, ItemType.ORE, 1f, fromEdge);
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            debugOverlay = !debugOverlay;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)){
            camera.zoom -= 0.05f;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)){
            camera.zoom += 0.05f;
        }
    }

    private static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    private int worldCellsX() { return world.wCells; }
    private int worldCellsY() { return world.hCells; }

    private void saveGame(String name) {
        world.saveWithTileWorld(name, tileWorld);
    }

    private void loadGame(String name) {
        world.loadWithTileWorld(name, tileWorld);

        // rebuild entities from tiles/rots
        for (int x = 0; x < world.wCells; x++) {
            for (int y = 0; y < world.hCells; y++) {
                if (world.grid[x][y] == WorldGrid.TILE_EMPTY) tileWorld.clearEntityAt(x, y);
                else tileWorld.rebuildEntityAt(x, y);
            }
        }
        tileWorld.refreshAllConveyorShapes();

        // import items into rebuilt entities
        world.applyLoadedItemsTo(tileWorld);
    }

    @SuppressWarnings("SpellCheckingInspection")
    public void doCameraStuff(float dt){
        // Camera follow player (smooth) + clamp to world bounds
        float targetX = player.x + player.tex.getWidth() / 2f;
        float targetY = player.y + player.tex.getHeight() / 2f;

        // Smooth follow using exponential-ish lerp
        // higher = snappier, lower = floaty
        float camLerp = 10f;
        float alpha = 1f - (float)Math.exp(-camLerp * dt);
        camera.position.x += (targetX - camera.position.x) * alpha;
        camera.position.y += (targetY - camera.position.y) * alpha;

        // Clamp camera so we don't see outside the world
        float halfW = viewport.getWorldWidth() / 2f;
        float halfH = viewport.getWorldHeight() / 2f;

        camera.position.x = clamp(camera.position.x, halfW, world.WORLD_W - halfW);
        camera.position.y = clamp(camera.position.y, halfH, world.WORLD_H - halfH);

        camera.update();
    }

    public void doGridDraw(){
        // Grid
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.setProjectionMatrix(camera.combined);
        shapes.setColor(0.35f, 0.40f, 0.50f, 0.35f); // dim bluish-grey

        shapes.begin(ShapeRenderer.ShapeType.Line);

        float inset = 0.5f;

        // Vertical lines
        for (int x = 0; x <= worldCellsX(); x++) {
            float px = x * WorldGrid.CELL;

            // push the outer borders inward so they don't get clipped
            if (x == 0) px += inset;
            else if (x == worldCellsX()) px -= inset;
            else px += inset; // keeps inner lines crisp too (optional)

            shapes.line(px, inset, px, world.WORLD_H - inset);
        }

        // Horizontal lines
        for (int y = 0; y <= worldCellsY(); y++) {
            float py = y * WorldGrid.CELL;

            if (y == 0) py += inset;
            else if (y == worldCellsY()) py -= inset;
            else py += inset;

            shapes.line(inset, py, world.WORLD_W - inset, py);
        }

        shapes.end();
        shapes.setColor(1f, 1f, 1f, 1f);
    }

    public void doGetMouseHover() {
        // Convert screen mouse position into world coords
        mouse.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(mouse); // IMPORTANT: uses your FitViewport + camera

        int cx = (int)(mouse.x / WorldGrid.CELL);
        int cy = (int)(mouse.y / WorldGrid.CELL);

        hoverValid = (cx >= 0 && cy >= 0 && cx < worldCellsX() && cy < worldCellsY());
        if (hoverValid) {
            hoverCellX = cx;
            hoverCellY = cy;
        } else {
            hoverCellX = -1;
            hoverCellY = -1;
        }
    }

    public void doHoverDraw() {
        if (!hoverValid) return;

        float x = hoverCellX * WorldGrid.CELL;
        float y = hoverCellY * WorldGrid.CELL;

        boolean ok = world.grid[hoverCellX][hoverCellY] == WorldGrid.TILE_EMPTY &&
                !player.blocksCell(world, hoverCellX, hoverCellY) &&
                money >= getTileCost(selectedTile);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (ok) batch.setColor(0.4f, 1f, 0.4f, 0.55f);
        else    batch.setColor(1f, 0.3f, 0.3f, 0.55f);

        switch (selectedTile){
            case WorldGrid.TILE_CONVEYOR: {
                TextureRegion frame = conveyorAnim[selectedRot].getKeyFrame(stateTime, true);
                batch.draw(frame, x, y, WorldGrid.CELL, WorldGrid.CELL);
                break;
            }
            case WorldGrid.TILE_SMELTER: {
                TextureRegion frame = smelterAnim[selectedRot].getKeyFrame(stateTime, false);
                batch.draw(frame, x, y, WorldGrid.CELL, WorldGrid.CELL);
                break;
            }
            case WorldGrid.TILE_SELLPAD: {
                TextureRegion frame = sellPadAnim[selectedRot].getKeyFrame(stateTime, false);
                batch.draw(frame, x, y, WorldGrid.CELL, WorldGrid.CELL);
                break;
            }
            case WorldGrid.TILE_SPAWNER: {
                TextureRegion frame = spawnerAnim[selectedRot].getKeyFrame(0f);
                batch.draw(frame, x, y, WorldGrid.CELL, WorldGrid.CELL);
                break;

            }
            case WorldGrid.TILE_CRUSHER: {
                TextureRegion frame = crusherAnim[selectedRot].getKeyFrame(0f);
                batch.draw(frame, x, y, WorldGrid.CELL, WorldGrid.CELL);
                break;
            }
        }

        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }
    private float getTileCost(int tile) {
        float costToPlace = 0f;
        switch (tile) {
            case WorldGrid.TILE_CONVEYOR: {
                costToPlace = COST_CONVEYOR;
                break;
            }
            case WorldGrid.TILE_SMELTER : {
                costToPlace = COST_SMELTER;
                break;
            }
            case WorldGrid.TILE_SELLPAD : {
                costToPlace = COST_SELLPAD;
                break;
            }
            case WorldGrid.TILE_SPAWNER : {
                costToPlace = COST_SPAWNER;
                break;
            }
            case WorldGrid.TILE_CRUSHER: {
                costToPlace = COST_CRUSHER;
                break;
            }
            case WorldGrid.TILE_SPLITTER_FL: {
                costToPlace = COST_SPLIT_FL;
                break;
            }
            case WorldGrid.TILE_SPLITTER_FR: {
                costToPlace = COST_SPLIT_FR;
                break;
            }
            case WorldGrid.TILE_SPLITTER_LR: {
                costToPlace = COST_SPLIT_LR;
                break;
            }
            case WorldGrid.TILE_MERGER: {
                costToPlace = COST_MERGER;
                break;

            }
            default: {
               costToPlace = 0f;
                break;
            }
        };
        return costToPlace;
    }
    public void doSelectionInput() {
        // number keys -> slot
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) selectSlot(0);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) selectSlot(1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) selectSlot(2);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) selectSlot(3);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) selectSlot(4);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) selectSlot(5);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7)) selectSlot(6);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_8)) selectSlot(7);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_9)) selectSlot(8);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0)) selectSlot(9);

        // page cycling
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET) || Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)) {
            hotbarPage = (hotbarPage + 1) % hotbarPages.length;
            applyHotbarSelection(); // keep same slot on new page
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET) || Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)) {
            hotbarPage = (hotbarPage - 1 + hotbarPages.length) % hotbarPages.length;
            applyHotbarSelection();
        }

        // optional: mouse wheel cycles slots
        if (scrollDelta != 0) {
            hotbarSlot = (hotbarSlot + scrollDelta + HOTBAR_SLOTS) % HOTBAR_SLOTS;
            applyHotbarSelection();
            scrollDelta = 0; // consume event
        }

        if (scrollDelta != 0) {
            hotbarSlot = (hotbarSlot + scrollDelta + HOTBAR_SLOTS) % HOTBAR_SLOTS;
            applyHotbarSelection();
        }
    }

    private void selectSlot(int slot) {
        hotbarSlot = slot;
        applyHotbarSelection();
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    private boolean isManuallyPlaceable(int tile) {
        boolean placeableTile = false;
        switch (tile) {
            case WorldGrid.TILE_CONVEYOR: {
                placeableTile = true;
                break;
            }
            case WorldGrid.TILE_SMELTER: {
                placeableTile = true;
                break;
            }
            case WorldGrid.TILE_SELLPAD: {
                placeableTile = true;
                break;
            }
            case WorldGrid.TILE_SPAWNER: {
                placeableTile = true;
                break;
            }
            case WorldGrid.TILE_CRUSHER: {
                placeableTile = true;
                break;
            }
            default: {
                placeableTile = false;
                break;
            }
        };
        return placeableTile;
    }

    private void applyHotbarSelection() {
        int tile = hotbarPages[hotbarPage][hotbarSlot];
        if (tile != 0 && isManuallyPlaceable(tile)) selectedTile = tile;
    }

    private static int dirToRot(Dir d) {
        int rotValueForDir = 0;
        switch (d) {
            case EAST: {
                rotValueForDir = 0;
                break;
            }
            case SOUTH: {
                rotValueForDir = 1;
                break;
            }
            case WEST: {
                rotValueForDir = 2;
                break;
            }
            case NORTH: {
                rotValueForDir = 3;
                break;
            }
        };
        return rotValueForDir;
    }

    private Animation<TextureRegion> makeAnim(String base, int last, boolean reverseFrames) {
        Texture[] frames = loadFrames(base, last);
        String b = base.toLowerCase();
        if (b.contains("conveyor")){ conveyorTextures.addAll(Arrays.asList(frames)); }
        if (b.contains("smelter")){ smelterTextures.addAll(Arrays.asList(frames)); }
        if (b.contains("sellpad")){ sellPadTextures.addAll(Arrays.asList(frames)); }
        if (b.contains("spawner")){ spawnerTextures.addAll(Arrays.asList(frames)); }
        if (b.contains("crusher")){ crusherTextures.addAll(Arrays.asList(frames)); }
        if (b.contains("lr")){ splitterLRTextures.addAll(Arrays.asList(frames)); }
        if (b.contains("fr")){ splitterFRTextures.addAll(Arrays.asList(frames)); }
        if (b.contains("fl")){ splitterFLTextures.addAll(Arrays.asList(frames)); }

        TextureRegion[] regs = new TextureRegion[frames.length];
        for (int i = 0; i < frames.length; i++) {
            regs[i] = new TextureRegion(frames[i]);
        }

        if (reverseFrames) {
            regs = reverse(regs);
        }

        Animation<TextureRegion> anim = new Animation<>(0.03f, regs);
        anim.setPlayMode(Animation.PlayMode.LOOP);
        return anim;
    }
    public void doPlacedTilesDraw() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (int x = 0; x < worldCellsX(); x++) {
            for (int y = 0; y < worldCellsY(); y++) {
                int id = world.grid[x][y];
                if (id == WorldGrid.TILE_EMPTY) continue;

                float drawX = x * WorldGrid.CELL;
                float drawY = y * WorldGrid.CELL;
                int outRot = world.rot[x][y];


                if (id == WorldGrid.TILE_CONVEYOR){
                    if (tileWorld.getEntity(x, y) instanceof ConveyorEntity) {
                        ConveyorEntity c = (ConveyorEntity) tileWorld.getEntity(x, y);

                        // your world rot = output direction
                        Dir out = Dir.fromRot(outRot);

                        ConveyorEntity.Shape conveyorShape = c.getShape();
                        switch (conveyorShape) {
                            case STRAIGHT: {
                                TextureRegion frame = conveyorAnim[outRot].getKeyFrame(stateTime, true);
                                batch.draw(frame, drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);
                                break;
                            }
                            case TURN_LEFT: {
                                // input side relative to output
                                Dir in = (c.getShape() == ConveyorEntity.Shape.TURN_LEFT) ? out.left() : out.right();
                                int inRot = dirToRot(in);
                                // which way do we turn relative to the IN direction?
                                // idx 0 = left-turn (CCW), idx 1 = right-turn (CW)
                                int idx = (out == in.left()) ? 0 : 1;

                                // ---- FIX: your corner sprites are 90° off ----
                                // If it's a left turn, rotate sprite clockwise; if it's a right turn, rotate sprite counterclockwise.
                                // (This matches your screenshot: C for one type, K for the other.)
                                int correctedRot = (idx == 0) ? ((inRot + 1) & 3) : ((inRot + 3) & 3);

                                batch.draw(conveyorTurn[correctedRot][1 - idx], drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);
                            }
                            case TURN_RIGHT: {
                                // input side relative to output
                                Dir in = (c.getShape() == ConveyorEntity.Shape.TURN_LEFT) ? out.left() : out.right();

                                int inRot = dirToRot(in);

                                // which way do we turn relative to the IN direction?
                                // idx 0 = left-turn (CCW), idx 1 = right-turn (CW)
                                int idx = (out == in.left()) ? 0 : 1;

                                // ---- FIX: your corner sprites are 90° off ----
                                // If it's a left turn, rotate sprite clockwise; if it's a right turn, rotate sprite counterclockwise.
                                // (This matches your screenshot: C for one type, K for the other.)
                                int correctedRot = (idx == 0) ? ((inRot + 1) & 3) : ((inRot + 3) & 3);

                                batch.draw(conveyorTurn[correctedRot][1 - idx], drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);
                            }
                        }
                    } else {
                        TextureRegion frame = conveyorAnim[outRot].getKeyFrame(stateTime, true);
                        batch.draw(frame, drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);
                    }
                }
                switch (id) {
                    case WorldGrid.TILE_CONVEYOR: {
                        break;
                    }
                    case WorldGrid.TILE_SMELTER: {
                        batch.draw(smelterAnim[outRot].getKeyFrame(0f), drawX, drawY, WorldGrid.CELL, WorldGrid.CELL); // idle frame
                        break;
                    }
                    case WorldGrid.TILE_SELLPAD: {
                        TextureRegion frame = sellPadAnim[outRot].getKeyFrame(0f);
                        batch.draw(frame, drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);
                        break;
                    }
                    case WorldGrid.TILE_SPAWNER: {
                        TextureRegion frame = spawnerAnim[outRot].getKeyFrame(0f);
                        batch.draw(frame, drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);
                        break;
                    }
                    case WorldGrid.TILE_CRUSHER: {
                        TextureRegion frame = crusherAnim[outRot].getKeyFrame(0f);
                        batch.draw(frame, drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);
                        break;
                    }
                    case WorldGrid.TILE_SPLITTER_LR: {
                        TextureRegion frame = splitterLRAnim[outRot].getKeyFrame(0f);
                        batch.draw(frame, drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);
                        break;
                    }
                    case WorldGrid.TILE_SPLITTER_FL: {
                        TextureRegion frame = splitterFLAnim[outRot].getKeyFrame(0f);
                        batch.draw(frame, drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);
                        break;
                    }
                    case WorldGrid.TILE_SPLITTER_FR: {
                        TextureRegion frame = splitterFRAnim[outRot].getKeyFrame(0f);
                        batch.draw(frame, drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);
                        break;
                    }
                    case WorldGrid.TILE_MERGER: {
                        int variant = mergerVariantAt(x, y, outRot);

                        batch.draw(mergerSprite[outRot][variant], drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);
                        if (debugOverlay) {
                            batch.end();

                            batch.setProjectionMatrix(camera.combined);
                            batch.begin();

                            hud.drawMergervariant(batch, variant, drawX, drawY);

                            batch.end();
                            batch.begin();
                        }
                        break;
                    }
                }
            }
        }

        batch.end();
    }

    private int mergerVariantAt(int x, int y, int outRot) {
        Dir out = Dir.fromRot(outRot);

        boolean inBack  = feedsFrom(x, y, out.opposite());
        boolean inLeft  = feedsFrom(x, y, out.left());
        boolean inRight = feedsFrom(x, y, out.right());

        // Variant meaning (must match your sprite table):
        // 0 = left + right
        // 1 = back + right
        // 2 = back + left
        if (inLeft && inRight) return 0;
        if (inBack && inRight) return 1;
        if (inBack && inLeft)  return 2;

        return 0; // fallback (should rarely happen)
    }

    private boolean feedsFrom(int cx, int cy, Dir fromEdgeIntoThis) {
        int nx = cx + fromEdgeIntoThis.dx;
        int ny = cy + fromEdgeIntoThis.dy;

        TileEntity n = tileWorld.getEntity(nx, ny);
        if (n == null) return false;

        Dir dirFromNeighborToThis = fromEdgeIntoThis.opposite();
        return n.outputsTo(dirFromNeighborToThis);
    }

    public void doGetPlacement() {
        if (!hoverValid) return;
        if (player.blocksCell(world, hoverCellX, hoverCellY)) { return; }

        // Left click = place
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            if (world.grid[hoverCellX][hoverCellY] == WorldGrid.TILE_EMPTY) {
                if (!isManuallyPlaceable(selectedTile)) return;

                float cost = getTileCost(selectedTile);
                if (money < cost) return; // can't afford
                money -= cost;

                world.grid[hoverCellX][hoverCellY] = selectedTile;
                world.rot[hoverCellX][hoverCellY] = selectedRot;

                // AUTO-UPGRADE: if we placed a conveyor, maybe it should become a splitter
                if (selectedTile == WorldGrid.TILE_CONVEYOR) {
                    int upgraded = tileWorld.decideAutoTileForConveyor(hoverCellX, hoverCellY, selectedRot);
                    world.grid[hoverCellX][hoverCellY] = upgraded;
                }

                tileWorld.rebuildEntityAt(hoverCellX, hoverCellY);
            }
        }

        // Right click = delete
        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            int old = world.grid[hoverCellX][hoverCellY];
            if (old != WorldGrid.TILE_EMPTY) {
                money += getTileCost(old) * REFUND_RATE;
            }
            world.grid[hoverCellX][hoverCellY] = WorldGrid.TILE_EMPTY;
            tileWorld.clearEntityAt(hoverCellX, hoverCellY);
        }

    }

    public void doDrawPlayer(){
        // Player sprite
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(player.tex, player.x, player.y);
        batch.end();
    }

    public void doDrawItems() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (TileWorld.ItemRenderInfo info : tileWorld.renderInfos()) {
            Texture tex = oreTex;
            switch (info.item().type) {
                case ORE: {
                    tex = oreTex;
                    break;
                }
                case DUST: {
                    tex = dustTex;
                    break;
                }
                case INGOT: {
                    tex = ingotTex;
                }
                case CRUSHED_ORE: {
                    tex = crushedOreTex;
                }
                case GEAR: {
                    tex = gearTex;
                }
                case PLATE: {
                    tex = plateTex;
                }
                case ROD: {
                    tex = rodTex;
                }
                case MACHINE_PARTS: {
                    tex = machinePartsTex;
                }
            };

            // Center the 32x32 item on the subcell center
            float x = info.x() - 16f;
            float y = info.y() - 16f;
            batch.draw(tex, x, y, 32f, 32f);
        }

        batch.end();
    }

    private static Texture[] loadFrames(String basePath, int lastInclusive) {
        int count = lastInclusive - 1 + 1;
        Texture[] out = new Texture[count];
        for (int i = 0; i < count; i++) {
            int frameNum = 1 + i;
            String path = basePath + frameNum + ".png";

            if (!Gdx.files.internal(path).exists()) {
                throw new RuntimeException("Missing frame: " + path);
            }

            out[i] = new Texture(Gdx.files.internal(path));
        }
        return out;
    }

    public void doRotationInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            selectedRot = (selectedRot + 1) & 3; // 0..3 wrap
        }
    }

    private static TextureRegion[] reverse(TextureRegion[] src) {
        TextureRegion[] out = new TextureRegion[src.length];
        for (int i = 0; i < src.length; i++) {
            out[i] = src[src.length - 1 - i];
        }
        return out;
    }




}
