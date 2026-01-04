package org.gudu0.AwareMemory;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import org.gudu0.AwareMemory.entities.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;


@SuppressWarnings({"FieldMayBeFinal", "UnusedAssignment", "PatternVariableCanBeUsed", "EnhancedSwitchMigration"})
public class Main extends ApplicationAdapter {
    private WorldGrid world;
    private PlayerSystem player;
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

    private Viewport hudViewport;
    private final Vector2 tmpHud = new Vector2();

    private boolean filterUiOpen = false;
    private int filterCx = -1, filterCy = -1;
    private FilterEntity editingFilter = null;

    private static final float COST_CONVEYOR = 1f;
    private static final float COST_SMELTER  = 15f;
    private static final float COST_SELLPAD  = 10f;
    private static final float COST_SPAWNER  = 25f;
    private static final float COST_CRUSHER  = 20f;
    private static final float COST_SPLITTER = 12f;
    private static final float COST_MERGER = 12f;
    private static final float COST_PRESS = 25f;
    private static final float COST_ROLLER = 50f;
    private static final float COST_FILTER = 50f;

    private static final int HOTBAR_SLOTS = 10;
    // Each page is an array of tile IDs.
    // 0 means “empty slot”
    private final int[][] hotbarPages = {
        { // Page 0
            WorldGrid.TILE_CONVEYOR,
            WorldGrid.TILE_CRUSHER,
            WorldGrid.TILE_SELLPAD,
            WorldGrid.TILE_SPAWNER,
            WorldGrid.TILE_SMELTER,
            WorldGrid.TILE_PRESS,
            WorldGrid.TILE_ROLLER,
            WorldGrid.TILE_FILTER_LR,
            WorldGrid.TILE_FILTER_FR,
            WorldGrid.TILE_FILTER_FL
        },
        { // Page 1 (example future)
            0,0,0,0,0,0,0,0,0,0
        }
    };

    private int hotbarPage = 0;
    private int hotbarSlot = 0; // 0..9
    private int scrollDelta = 0;
    @SuppressWarnings("FieldCanBeLocal")
    private int hotbarHoverSlot = -1;

    private static final int MAX_TILE_ID = 256;

    // simple registries (indexed by tile id)
    private final float[] costByTile = new float[MAX_TILE_ID];
    private final boolean[] placeableByTile = new boolean[MAX_TILE_ID];

    // [tileId][rot] -> animation (null means “no generic animation draw”)
    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[][] animByTileRot = new Animation[MAX_TILE_ID][4];


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

    private TextureRegion[][] splitterSprite = new TextureRegion[4][3]; // 0=FL,1=FR,2=LR
    private final ArrayList<Texture> splitterSpriteTextures = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] pressAnim = new Animation[4];
    private final ArrayList<Texture> pressTextures = new ArrayList<>();

    private TextureRegion[][] mergerSprite = new TextureRegion[4][3]; // [outRot][variant]
    private final ArrayList<Texture> mergerSpriteTextures = new ArrayList<>();

    private final TextureRegion[][] conveyorTurn = new TextureRegion[4][2]; // [rot][0=left,1=right]
    private final ArrayList<Texture> conveyorTurnTextures = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] rollerAnim = new Animation[4];
    private final ArrayList<Texture> rollerTextures = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] filter_LR_Anim = new Animation[4];
    private final ArrayList<Texture> filter_LR_Textures = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] filter_FR_Anim = new Animation[4];
    private final ArrayList<Texture> filter_FR_Textures = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] filter_FL_Anim = new Animation[4];
    private final ArrayList<Texture> filter_FL_Textures = new ArrayList<>();

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

    private InputMultiplexer inputMux;


    // TO ADD A NEW MACHINE
    {
        /*
        After this, adding a new machine becomes:
        In Main.java:
        Add/load its newMachineAnim[4] + newMachineTextures
        Call registerTile(TILE_NEW, COST_NEW, true, newMachineAnim, false)
        Put it in hotbarPages[...] if you want it selectable

        Still required elsewhere (unavoidable):
        WorldGrid: add TILE_NEW constant
        TileWorld.rebuildEntityAt: create new NewMachineEntity(...)
        */
    }

    // --- METHODS --- \\
    @SuppressWarnings({"SpellCheckingInspection"})
    @Override
    public void create() {
        Gdx.app.setLogLevel(com.badlogic.gdx.Application.LOG_DEBUG);
        world = new WorldGrid();
        playerTex = new Texture(Gdx.files.internal("player.png"));
        player = new PlayerSystem(playerTex, world);
        tileWorld = new TileWorld(world);
        hud = new Hud();
        tileWorld.addMoney(999.0f); // temp test starting money

        ArrayList<Order> milestones = new ArrayList<>();
        milestones.add(Order.sellItems("sell_10_any", "First Sales", "Sell 10 items.", 100, -1, 10));
        milestones.add(Order.processInMachine("roll_20", "Rods", "Make 20 rods in a Roller.", 250, WorldGrid.TILE_ROLLER, ItemType.ROD.ordinal(), 20));
        int targetMoney = (int) tileWorld.getMoney() + 100;
        milestones.add(Order.reachMoney("money_1000", "Savings", "Reach $" + targetMoney + "!", 400, targetMoney));

        tileWorld.getOrders().setMilestones(milestones);



        camera = new OrthographicCamera();
        viewport = new FitViewport(1920, 1080, camera);
        viewport.apply(true);

        hudViewport = new FitViewport(1920f, 1080f, hud.cam);
        hudViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);


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
        inputMux = new InputMultiplexer();

        // Orders scroll adapter (priority)
        inputMux.addProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (!hud.isOrdersPanelOpen()) return false;

                Vector2 hm = getHudMouse();
                if (!hud.isOverOrdersPanel(hm.x, hm.y)) return false;

                hud.scrollOrders(amountY, tileWorld.getOrders());
                return true; // consume wheel
            }
        });

        // Hotbar scroll adapter (fallback)
        inputMux.addProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                scrollDelta += (int) Math.signum(amountY);
                return true; // keep current behavior
            }
        });

        Gdx.input.setInputProcessor(inputMux);


        Gdx.input.setCatchKey(Input.Keys.F3, true);
        Gdx.input.setCatchKey(Input.Keys.F5, true);
        Gdx.input.setCatchKey(Input.Keys.F9, true);


        // Enable alpha
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Machine Animations and Sprites
        {
            // 32 frames: 0.05f = ~20fps, 0.10f = 10fps
            conveyorAnim[0] = makeAnim("Conveyor/right/conveyor_right", 32, false, conveyorTextures);  //right
            conveyorAnim[1] = makeAnim("Conveyor/down/conveyor_down", 32, false, conveyorTextures);        //down
            conveyorAnim[2] = makeAnim("Conveyor/left/conveyor_left", 32, true, conveyorTextures);     //left
            conveyorAnim[3] = makeAnim("Conveyor/up/conveyor_up", 32, false, conveyorTextures);    //up

            smelterAnim[0] = makeAnim("smelter/right/smelter_right", 2, false, smelterTextures);
            smelterAnim[1] = makeAnim("smelter/down/smelter_down", 2, false, smelterTextures);
            smelterAnim[2] = makeAnim("smelter/left/smelter_left", 2, false, smelterTextures);
            smelterAnim[3] = makeAnim("smelter/up/smelter_up", 2, false, smelterTextures);

            sellPadAnim[0] = makeAnim("seller/sellpad", 1, false, sellPadTextures);
            sellPadAnim[1] = makeAnim("seller/sellpad", 1, false, sellPadTextures);
            sellPadAnim[2] = makeAnim("seller/sellpad", 1, false, sellPadTextures);
            sellPadAnim[3] = makeAnim("seller/sellpad", 1, false, sellPadTextures);

            spawnerAnim[0] = makeAnim("spawner/right/spawner_right", 1, false, spawnerTextures);
            spawnerAnim[1] = makeAnim("spawner/down/spawner_down", 1, false, spawnerTextures);
            spawnerAnim[2] = makeAnim("spawner/left/spawner_left", 1, false, spawnerTextures);
            spawnerAnim[3] = makeAnim("spawner/up/spawner_up", 1, false, spawnerTextures);

            crusherAnim[0] = makeAnim("crusher/right/crusher_right", 1, false, crusherTextures);
            crusherAnim[1] = makeAnim("crusher/down/crusher_down", 1, false, crusherTextures);
            crusherAnim[2] = makeAnim("crusher/left/crusher_left", 1, false, crusherTextures);
            crusherAnim[3] = makeAnim("crusher/up/crusher_up", 1, false, crusherTextures);

            rollerAnim[0] = makeAnim("roller/roller", 1, false, rollerTextures);
            rollerAnim[1] = makeAnim("roller/roller", 1, false, rollerTextures);
            rollerAnim[2] = makeAnim("roller/roller", 1, false, rollerTextures);
            rollerAnim[3] = makeAnim("roller/roller", 1, false, rollerTextures);

            filter_FL_Anim[0] = makeAnim("filter/filterFL/right/filterFL_right", 1, false, filter_FL_Textures);
            filter_FL_Anim[1] = makeAnim("filter/filterFL/down/filterFL_down", 1, false, filter_FL_Textures);
            filter_FL_Anim[2] = makeAnim("filter/filterFL/left/filterFL_left", 1, false, filter_FL_Textures);
            filter_FL_Anim[3] = makeAnim("filter/filterFL/up/filterFL_up", 1, false, filter_FL_Textures);

            filter_FR_Anim[0] = makeAnim("filter/filterFR/right/filterFR_right", 1, false, filter_FR_Textures);
            filter_FR_Anim[1] = makeAnim("filter/filterFR/down/filterFR_down", 1, false, filter_FR_Textures);
            filter_FR_Anim[2] = makeAnim("filter/filterFR/left/filterFR_left", 1, false, filter_FR_Textures);
            filter_FR_Anim[3] = makeAnim("filter/filterFR/up/filterFR_up", 1, false, filter_FR_Textures);

            filter_LR_Anim[0] = makeAnim("filter/filterLR/right/filterLR_right", 1, false, filter_LR_Textures);
            filter_LR_Anim[1] = makeAnim("filter/filterLR/down/filterLR_down", 1, false, filter_LR_Textures);
            filter_LR_Anim[2] = makeAnim("filter/filterLR/left/filterLR_left", 1, false, filter_LR_Textures);
            filter_LR_Anim[3] = makeAnim("filter/filterLR/up/filterLR_up", 1, false, filter_LR_Textures);


            // Splitter sprites: [outRot][variant] where variant: 0=FL,1=FR,2=LR
            // outRot: 0=E,1=S,2=W,3=N

            // Output EAST (rot 0)
            splitterSprite[0][0] = loadSplitter("splitter/splitterFL/right/splitter_FL_right1.png");
            splitterSprite[0][1] = loadSplitter("splitter/splitterFR/right/splitter_FR_right1.png");
            splitterSprite[0][2] = loadSplitter("splitter/splitterLR/right/splitter_LR_right1.png");

            // Output SOUTH (rot 1)
            splitterSprite[1][0] = loadSplitter("splitter/splitterFL/down/splitter_FL_down1.png");
            splitterSprite[1][1] = loadSplitter("splitter/splitterFR/down/splitter_FR_down1.png");
            splitterSprite[1][2] = loadSplitter("splitter/splitterLR/down/splitter_LR_down1.png");

            // Output WEST (rot 2)
            splitterSprite[2][0] = loadSplitter("splitter/splitterFL/left/splitter_FL_left1.png");
            splitterSprite[2][1] = loadSplitter("splitter/splitterFR/left/splitter_FR_left1.png");
            splitterSprite[2][2] = loadSplitter("splitter/splitterLR/left/splitter_LR_left1.png");

            // Output NORTH (rot 3)
            splitterSprite[3][0] = loadSplitter("splitter/splitterFL/up/splitter_FL_up1.png");
            splitterSprite[3][1] = loadSplitter("splitter/splitterFR/up/splitter_FR_up1.png");
            splitterSprite[3][2] = loadSplitter("splitter/splitterLR/up/splitter_LR_up1.png");


            pressAnim[0] = makeAnim("press/press", 1, false, pressTextures);
            pressAnim[1] = makeAnim("press/press", 1, false, pressTextures);
            pressAnim[2] = makeAnim("press/press", 1, false, pressTextures);
            pressAnim[3] = makeAnim("press/press", 1, false, pressTextures);

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
            iconByTileId = new TextureRegion[MAX_TILE_ID];

            // Register all tiles (this also sets their default icon from anim rot=0)
            registerTile(WorldGrid.TILE_CONVEYOR, COST_CONVEYOR, true, conveyorAnim, true);
            registerTile(WorldGrid.TILE_SMELTER,  COST_SMELTER,  true, smelterAnim,  false);
            registerTile(WorldGrid.TILE_CRUSHER,  COST_CRUSHER,  true, crusherAnim,  false);
            registerTile(WorldGrid.TILE_SPAWNER,  COST_SPAWNER,  true, spawnerAnim,  false);
            registerTile(WorldGrid.TILE_SELLPAD,  COST_SELLPAD,  true, sellPadAnim,  false);
            registerTile(WorldGrid.TILE_PRESS,    COST_PRESS,    true, pressAnim,    false);
            registerTile(WorldGrid.TILE_ROLLER,   COST_ROLLER,   true, rollerAnim,   false);
            registerTile(WorldGrid.TILE_FILTER_FL, COST_FILTER, true, filter_FL_Anim, false);
            registerTile(WorldGrid.TILE_FILTER_FR, COST_FILTER, true, filter_FR_Anim, false);
            registerTile(WorldGrid.TILE_FILTER_LR, COST_FILTER, true, filter_LR_Anim, false);

            // not manually placeable (auto-upgrade)
            // Splitter is one tile id now; variant chosen at runtime.
            // Register cost/placeability, and set icon from sprites.
            costByTile[WorldGrid.TILE_SPLITTER] = COST_SPLITTER;   // pick one cost constant (or reuse LR cost)
            placeableByTile[WorldGrid.TILE_SPLITTER] = false;      // auto-upgrade only
            iconByTileId[WorldGrid.TILE_SPLITTER] = splitterSprite[0][2]; // show LR east as the icon (or pick any)


            // merger uses special sprite, so just set icon directly (and cost/placeable)
            costByTile[WorldGrid.TILE_MERGER] = COST_MERGER;
            placeableByTile[WorldGrid.TILE_MERGER] = false;
            iconByTileId[WorldGrid.TILE_MERGER] = mergerSprite[0][0];

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

        Vector2 hm = getHudMouse();
        boolean uiBlocking = hud.isMouseOverBlockingUi(hm.x, hm.y, filterUiOpen);

        // Hotbar click handling (select slots) happens regardless
        boolean clickedHud = (!filterUiOpen) && doHotbarMouseClick();

        if (filterUiOpen) {
            handleFilterUiInput();
        } else if (!clickedHud) {
            // "Try open filter UI" should still work (shift+click), even if UI isn't blocking.
            if (!tryOpenFilterUI()) {
                // Final gate: only allow placement when not over *any* blocking UI
                if (!uiBlocking) {
                    doGetPlacement();
                }
            }
        }


        tileWorld.update(dt)    ;

        doGridDraw();
        doPlacedTilesDraw();
        doDrawItems();

        doDrawPlayer();
        doHoverDraw();
        if (debugOverlay) {
            drawDebugOverlay();
        }
        hudViewport.apply();
        hotbarHoverSlot = hud.slotAt(hm.x, hm.y);
        hud.draw(batch,
            tileWorld.getMoney(),
            tileWorld.itemCount(),
            hotbarPage,
            hotbarSlot,
            hotbarHoverSlot,
            hotbarPages[hotbarPage],
            iconByTileId,
            whiteRegion,
            costByTile  // or getTileCost via callback; easiest is pass array
        );

        if (filterUiOpen && editingFilter != null) {
            hud.drawFilterPanel(batch, editingFilter, whiteRegion);
        }

        hud.drawOrdersPanel(batch, tileWorld.getOrders(), tileWorld.getMoney(), whiteRegion, hudViewport);


    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);      // world
        hudViewport.update(width, height, true);   // HUD
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
        for (Texture t : splitterSpriteTextures) {
            t.dispose();
        }
        for (Texture t : conveyorTurnTextures){
            t.dispose();
        }
        for (Texture t : mergerSpriteTextures){
            t.dispose();
        }
        for (Texture t : pressTextures){
            t.dispose();
        }
        for (Texture t : rollerTextures){
            t.dispose();
        }
        for (Texture t : filter_FL_Textures){
            t.dispose();
        }
        for (Texture t : filter_FR_Textures){
            t.dispose();
        }
        for (Texture t : filter_LR_Textures){
            t.dispose();
        }


        conveyorTextures.clear();
        smelterTextures.clear();
        sellPadTextures.clear();
        spawnerTextures.clear();
        crusherTextures.clear();
        conveyorTurnTextures.clear();
        mergerSpriteTextures.clear();
        pressTextures.clear();
        splitterSpriteTextures.clear();
        rollerTextures.clear();
        filter_FL_Textures.clear();
        filter_FR_Textures.clear();
        filter_LR_Textures.clear();
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
    private TextureRegion loadSplitter(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        splitterSpriteTextures.add(t);
        return new TextureRegion(t);
    }

    private boolean tryOpenFilterUI() {
        if (!hoverValid) return false; // <-- add this
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return false;
        if (!Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && !Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) return false;

        // get hovered tile cell (use whatever you already use in placement)
        int cx = hoverCellX;
        int cy = hoverCellY;

        int id = world.getTileID(cx, cy);
        if (id != WorldGrid.TILE_FILTER_FL && id != WorldGrid.TILE_FILTER_FR && id != WorldGrid.TILE_FILTER_LR) return false;

        TileEntity te = tileWorld.getEntity(cx, cy);
        if (!(te instanceof FilterEntity)) return false;

        editingFilter = (FilterEntity) te;
        filterCx = cx;
        filterCy = cy;
        filterUiOpen = true;
        return true; // consume click
    }

    private Vector2 getHudMouse() {
        tmpHud.set(Gdx.input.getX(), Gdx.input.getY());
        hudViewport.unproject(tmpHud); // handles letterboxing + y flip correctly
        return tmpHud;
    }

    private void closeFilterUI() {
        filterUiOpen = false;
        editingFilter = null;
        filterCx = filterCy = -1;
    }

    private void handleFilterUiInput() {
        if (editingFilter == null) { closeFilterUI(); return; }

        // if tile got deleted / replaced, close
        TileEntity teNow = tileWorld.getEntity(filterCx, filterCy);
        if (teNow != editingFilter) { closeFilterUI(); return; }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            closeFilterUI();
            return;
        }

        boolean l = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        boolean r = Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT);
        if (!l && !r) return;

        Vector2 hm = getHudMouse();

        // click outside panel closes (optional, but feels good)
        if (!hud.isOverFilterPanel(hm.x, hm.y)) {
            closeFilterUI();
            return;
        }

        int hit = hud.filterButtonAt(hm.x, hm.y, editingFilter.getVariant());
        if (hit == 99) { // close button
            closeFilterUI();
            return;
        }
        if (hit < 0) return;

        int delta = r ? -1 : 1;

        if (hit == 0) editingFilter.cycleRule(FilterEntity.Out.FORWARD, delta);
        if (hit == 1) editingFilter.cycleRule(FilterEntity.Out.LEFT, delta);
        if (hit == 2) editingFilter.cycleRule(FilterEntity.Out.RIGHT, delta);
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
        // Claim order reward (mouse click on HUD button)
        if (hud.isOrdersPanelOpen() && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Vector2 hm = getHudMouse();

            int claimIndex = hud.ordersClaimButtonAt(hm.x, hm.y, tileWorld.getOrders());
            if (claimIndex != -1) {
                int reward = tileWorld.getOrders().tryClaimActiveIndex(claimIndex, tileWorld.getTick(), tileWorld.getMoney());
                if (reward > 0) {
                    tileWorld.addMoney(reward);
                }
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) tileWorld.clearItems();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)){
            saveGame();
            Gdx.app.exit();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            if (Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setWindowedMode(1280, 720);
            } else {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) saveGame();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
            loadGame();
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            hud.toggleOrdersPanel();
        }
    }

    private static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    private int worldCellsX() { return world.wCells; }
    private int worldCellsY() { return world.hCells; }

    private void saveGame() {
        world.saveWithTileWorld("save2", tileWorld);
    }

    private void loadGame() {
        long t0 = TimeUtils.millis();
        world.loadWithTileWorld("save2");
        long t1 = TimeUtils.millis();

        for (int x = 0; x < world.wCells; x++) {
            for (int y = 0; y < world.hCells; y++) {
                if (world.grid[x][y] == WorldGrid.TILE_EMPTY)
                    tileWorld.clearEntityAtFromSmartPlacement(x, y);
                else
                    tileWorld.rebuildEntityAtFromSmartPlacement(x, y);
            }
        }
        long t2 = TimeUtils.millis();

        SmartPlacement.refreshAll(tileWorld);
        long t3 = TimeUtils.millis();

        world.applyLoadedItemsTo(tileWorld);
        long t4 = TimeUtils.millis();

        Gdx.app.log("LoadPerf", "loadWithTileWorld: " + (t1 - t0) + "ms");
        Gdx.app.log("LoadPerf", "rebuildEntities:   " + (t2 - t1) + "ms");
        Gdx.app.log("LoadPerf", "smartPlacement:    " + (t3 - t2) + "ms");
        Gdx.app.log("LoadPerf", "applyItems:        " + (t4 - t3) + "ms");
        Gdx.app.log("LoadPerf", "TOTAL:             " + (t4 - t0) + "ms");

//        System.out.println("loadWithTileWorld: " + (t1 - t0) + "ms");
//        System.out.println("rebuildEntities:   " + (t2 - t1) + "ms");
//        System.out.println("smartPlacement:    " + (t3 - t2) + "ms");
//        System.out.println("extraShapes:       " + (t4 - t3) + "ms");
//        System.out.println("applyItems:        " + (t5 - t4) + "ms");
//        System.out.println("TOTAL:             " + (t5 - t0) + "ms");
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

        boolean ok = world.grid[hoverCellX][hoverCellY] == WorldGrid.TILE_EMPTY && !player.blocksCell(world, hoverCellX, hoverCellY) && tileWorld.getMoney() >= getTileCost(selectedTile);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (ok) batch.setColor(0.4f, 1f, 0.4f, 0.55f);
        else    batch.setColor(1f, 0.3f, 0.3f, 0.55f);

        // conveyor preview: animated
        if (selectedTile == WorldGrid.TILE_CONVEYOR) {
            TextureRegion frame = conveyorAnim[selectedRot].getKeyFrame(stateTime, true);
            batch.draw(frame, x, y, WorldGrid.CELL, WorldGrid.CELL);
        } else {
            Animation<TextureRegion> a = animByTileRot[selectedTile][selectedRot];
            if (a != null) {
                TextureRegion frame = a.getKeyFrame(0f, false); // idle preview
                batch.draw(frame, x, y, WorldGrid.CELL, WorldGrid.CELL);
            }
        }

        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }

    private float getTileCost(int tile) {
        if (tile < 0 || tile >= MAX_TILE_ID) return 0f;
        return costByTile[tile];
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
    }

    private boolean doHotbarMouseClick() {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return false;

        Vector2 hm = getHudMouse();
        int slot = hud.slotAt(hm.x, hm.y);

        if (slot < 0) return false;

        hotbarSlot = slot;
        applyHotbarSelection(); // your existing method :contentReference[oaicite:5]{index=5}
        return true;
    }

    private void selectSlot(int slot) {
        hotbarSlot = slot;
        applyHotbarSelection();
    }

    private boolean isManuallyPlaceable(int tile) {
        return tile >= 0 && tile < MAX_TILE_ID && placeableByTile[tile];
    }

    private void applyHotbarSelection() {
        int tile = hotbarPages[hotbarPage][hotbarSlot];
        if (tile != 0 && isManuallyPlaceable(tile)) selectedTile = tile;
    }

    private static int dirToRot(Dir d) {
        int rotValueForDir = 0;
        switch (d) {
            case EAST: {
                //noinspection DataFlowIssue
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
        }
        return rotValueForDir;
    }

    private void registerTile(int tileId, float cost, @SuppressWarnings("SameParameterValue") boolean placeable, Animation<TextureRegion>[] anim4, boolean iconLoop) {
        if (tileId < 0 || tileId >= MAX_TILE_ID) {
            throw new RuntimeException("tileId out of range: " + tileId);
        }
        costByTile[tileId] = cost;
        placeableByTile[tileId] = placeable;

        if (anim4 != null) {
            System.arraycopy(anim4, 0, animByTileRot[tileId], 0, 4);

            // default icon: rot 0, t=0
            if (iconByTileId[tileId] == null && anim4[0] != null) {
                iconByTileId[tileId] = anim4[0].getKeyFrame(0f, iconLoop);
            }
        }
    }

    private Animation<TextureRegion> makeAnim(String base, int last, boolean reverseFrames, ArrayList<Texture> bucket) {
        Texture[] frames = loadFrames(base, last);
        bucket.addAll(Arrays.asList(frames));


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
    @SuppressWarnings({"DataFlowIssue", "DuplicateBranchesInSwitch"})
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

                        assert c != null;
                        ConveyorEntity.Shape conveyorShape = c.getShape();
                        switch (conveyorShape) {
                            case STRAIGHT: {
                                TextureRegion frame = conveyorAnim[outRot].getKeyFrame(stateTime, true);
                                batch.draw(frame, drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);
                                break;
                            }
                            case TURN_LEFT: {
                                // input side relative to output
                                @SuppressWarnings("ConstantValue") Dir in = (c.getShape() == ConveyorEntity.Shape.TURN_LEFT) ? out.left() : out.right();
                                int inRot = dirToRot(in);
                                // which way do we turn relative to the IN direction?
                                // idx 0 = left-turn (CCW), idx 1 = right-turn (CW)
                                int idx = (out == in.left()) ? 0 : 1;

                                // ---- FIX: your corner sprites are 90° off ----
                                // If it's a left turn, rotate sprite clockwise; if it's a right turn, rotate sprite counterclockwise.
                                // (This matches your screenshot: C for one type, K for the other.)
                                int correctedRot = (idx == 0) ? ((inRot + 1) & 3) : ((inRot + 3) & 3);

                                batch.draw(conveyorTurn[correctedRot][1 - idx], drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);
                                break;
                            }
                            case TURN_RIGHT: {
                                // input side relative to output
                                @SuppressWarnings("ConstantValue") Dir in = (c.getShape() == ConveyorEntity.Shape.TURN_LEFT) ? out.left() : out.right();

                                int inRot = dirToRot(in);

                                // which way do we turn relative to the IN direction?
                                // idx 0 = left-turn (CCW), idx 1 = right-turn (CW)
                                int idx = (out == in.left()) ? 0 : 1;

                                // ---- FIX: your corner sprites are 90° off ----
                                // If it's a left turn, rotate sprite clockwise; if it's a right turn, rotate sprite counterclockwise.
                                // (This matches your screenshot: C for one type, K for the other.)
                                int correctedRot = (idx == 0) ? ((inRot + 1) & 3) : ((inRot + 3) & 3);

                                batch.draw(conveyorTurn[correctedRot][1 - idx], drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);
                                break;
                            }
                        }
                    } else {
                        TextureRegion frame = conveyorAnim[outRot].getKeyFrame(stateTime, true);
                        batch.draw(frame, drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);
                    }
                } else if (id == WorldGrid.TILE_MERGER) {
                    int variantIdx = 0;

                    TileEntity te = tileWorld.getEntity(x, y);
                    if (te instanceof MergerEntity) {
                        MergerEntity m = (MergerEntity) te;
                        switch (m.getVariant()) {
                            case LR: variantIdx = 0; break;
                            case BR: variantIdx = 1; break;
                            case BL: variantIdx = 2; break;
                        }
                    } else {
                        // fallback if something ever renders without an entity
                        variantIdx = mergerVariantAt(x, y, outRot);
                    }

                    batch.draw(mergerSprite[outRot][variantIdx], drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);

                    if (debugOverlay) {
                        batch.end();

                        batch.setProjectionMatrix(camera.combined);
                        batch.begin();

                        hud.drawMergervariant(batch, variantIdx, drawX, drawY);

                        batch.end();
                        batch.begin();
                    }
                } else if (id == WorldGrid.TILE_SPLITTER) {
                    SplitterEntity s = (SplitterEntity) tileWorld.getEntity(x, y);
                    int v = 0;
                    if (s != null) {
                        switch (s.getVariant()) {
                            case FL: v = 0; break;
                            case FR: v = 1; break;
                            case LR: v = 2; break;
                        }
                    }
                    batch.draw(splitterSprite[outRot][v], drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);
                } else {
                    Animation<TextureRegion> a = animByTileRot[id][outRot];
                    if (a != null) {
                        batch.draw(a.getKeyFrame(0f, false), drawX, drawY, WorldGrid.CELL, WorldGrid.CELL);
                    }
                }
            }
        }

        batch.end();
        drawPortsOverlayForAllPlacedTiles();
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
                if (!tileWorld.trySpendMoney(cost)) return;


                world.grid[hoverCellX][hoverCellY] = selectedTile;
                world.rot[hoverCellX][hoverCellY] = selectedRot;

                // AUTO-UPGRADE: if we placed a conveyor, maybe it should become a splitter
                if (selectedTile == WorldGrid.TILE_CONVEYOR) {
                    int upgraded = tileWorld.decideAutoTileForConveyor(hoverCellX, hoverCellY, selectedRot);
                    world.grid[hoverCellX][hoverCellY] = upgraded;
                }
                tileWorld.rebuildEntityAt(hoverCellX, hoverCellY);
                int placedId = world.grid[hoverCellX][hoverCellY]; // includes auto-upgrade result
                tileWorld.getOrders().onTilePlaced(placedId, tileWorld.getMoney(), tileWorld.getTick());

            }
        }


        // Right click = delete
        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            int old = world.grid[hoverCellX][hoverCellY];
            if (old != WorldGrid.TILE_EMPTY) {
                tileWorld.addMoney(getTileCost(old) * REFUND_RATE);
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
                    //noinspection DataFlowIssue
                    tex = oreTex;
                    break;
                }
                case DUST: {
                    tex = dustTex;
                    break;
                }
                case INGOT: {
                    tex = ingotTex;
                    break;
                }
//                case CRUSHED_ORE: {
//                    tex = crushedOreTex;
//                    break;
//                }
                case GEAR: {
                    tex = gearTex;
                    break;
                }
                case PLATE: {
                    tex = plateTex;
                    break;
                }
                case ROD: {
                    tex = rodTex;
                    break;
                }
                case MACHINE_PARTS: {
                    tex = machinePartsTex;
                    break;
                }
                default: {
                    throw new RuntimeException("Unhandled item type: " + info.item().type);
                }
            }

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

    private static final float PORT_ALPHA = 0.55f;

    // Draw one little square on the 5×5 grid along an edge.
    // (east marker is the subcell at (4,2), west at (0,2), north at (2,4), south at (2,0))
    private void drawPortMarker(float drawX, float drawY, Dir side, boolean isInput) {
        float sub = WorldGrid.CELL / 5f;     // 5×5 subcells
        float size = sub;                   // marker size (1 subcell)

        float px = drawX;
        float py = drawY;

        switch (side) {
            case EAST:  px += 4f * sub; py += 2f * sub; break;
            case WEST:  px += 0f * sub; py += 2f * sub; break;
            case NORTH: px += 2f * sub; py += 4f * sub; break;
            case SOUTH: px += 2f * sub; py += 0f * sub; break;
        }

        if (isInput) {
            shapes.setColor(0.2f, 1.0f, 0.2f, PORT_ALPHA); // green-ish
        } else {
            shapes.setColor(1.0f, 0.2f, 0.2f, PORT_ALPHA); // red-ish
        }

        shapes.rect(px, py, size, size);
    }

    private void computePortsForTile(int x, int y, EnumSet<Dir> inputs, EnumSet<Dir> outputs) {
        inputs.clear();
        outputs.clear();

        int id = world.grid[x][y];
        if (id == WorldGrid.TILE_EMPTY) return;

        int outRot = world.rot[x][y];
        Dir out = Dir.fromRot(outRot);

        TileEntity te = tileWorld.getEntity(x, y);

        // --- Logistics tiles (exact) ---
        if (te instanceof ConveyorEntity) {
            ConveyorEntity c = (ConveyorEntity) te;

            outputs.add(out);

            switch (c.getShape()) {
                case STRAIGHT:
                    inputs.add(out.opposite());
                    break;
                case TURN_LEFT:
                    inputs.add(out.left());
                    break;
                case TURN_RIGHT:
                    inputs.add(out.right());
                    break;
            }
            return;
        }

        if (te instanceof MergerEntity) {
            MergerEntity m = (MergerEntity) te;

            outputs.add(out);

            switch (m.getVariant()) {
                case LR:
                    inputs.add(out.left());
                    inputs.add(out.right());
                    break;
                case BL:
                    inputs.add(out.opposite());
                    inputs.add(out.left());
                    break;
                case BR:
                    inputs.add(out.opposite());
                    inputs.add(out.right());
                    break;
            }
            return;
        }

        if (te instanceof SplitterEntity) {
            SplitterEntity s = (SplitterEntity) te;

            inputs.add(out.opposite());

            switch (s.getVariant()) {
                case FL:
                    outputs.add(out);
                    outputs.add(out.left());
                    break;
                case FR:
                    outputs.add(out);
                    outputs.add(out.right());
                    break;
                case LR:
                    outputs.add(out.left());
                    outputs.add(out.right());
                    break;
            }
            return;
        }

        if (te instanceof FilterEntity) {
            // Your filter tiles are directional too; treat like splitter-style ports.
            // Input is "back" relative to rot; outputs depend on variant.
            FilterEntity f = (FilterEntity) te;

            inputs.add(out.opposite());

            switch (f.getVariant()) {
                case FL:
                    outputs.add(out);
                    outputs.add(out.left());
                    break;
                case FR:
                    outputs.add(out);
                    outputs.add(out.right());
                    break;
                case LR:
                    outputs.add(out.left());
                    outputs.add(out.right());
                    break;
            }
            return;
        }

        // --- Machines (reasonable defaults) ---
        // Spawner: output only
        if (te instanceof SpawnerEntity) {
            outputs.add(out);
            return;
        }

        // Sellpad: input only
        if (te instanceof SellpadEntity) {
            inputs.add(out.left());
            inputs.add(out.right());
            inputs.add(out.opposite());
            inputs.add(out);
            return;
        }

        // Most processors: input back, output forward
        if (te != null) {
            inputs.add(out.opposite());
            outputs.add(out);
        }
    }

    private void drawPortsOverlayForAllPlacedTiles() {
        if (!debugOverlay) return;

        EnumSet<Dir> inputs = EnumSet.noneOf(Dir.class);
        EnumSet<Dir> outputs = EnumSet.noneOf(Dir.class);

        shapes.setProjectionMatrix(camera.combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        for (int x = 0; x < worldCellsX(); x++) {
            for (int y = 0; y < worldCellsY(); y++) {
                if (world.grid[x][y] == WorldGrid.TILE_EMPTY) continue;

                computePortsForTile(x, y, inputs, outputs);

                float drawX = x * WorldGrid.CELL;
                float drawY = y * WorldGrid.CELL;

                for (Dir d : inputs)  drawPortMarker(drawX, drawY, d, true);
                for (Dir d : outputs) drawPortMarker(drawX, drawY, d, false);
            }
        }

        shapes.end();
    }
}
