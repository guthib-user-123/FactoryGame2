package org.gudu0.AwareMemory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.gudu0.AwareMemory.entities.FilterEntity;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;


import java.util.ArrayList;



import java.util.List;

@SuppressWarnings({"EnhancedSwitchMigration", "FieldCanBeLocal", "UnnecessaryLocalVariable"})
public class Hud {
    public final OrthographicCamera cam;
    public final BitmapFont uiFont;
    public final BitmapFont smallFont;

    // --------------- Hotbar ----------
    final float slotSize = 64f;
    final float pad = 8f;
    final float hotbarWidth = (slotSize * 10f) + (pad * 11f);

    final float hotbarX = (1920f - hotbarWidth) / 2f;
    final float hotbarY = 20f;

    // --------------- Filter Panel -------------------
    // Filter panel layout (HUD coords)
    final float filterPanelWidth = 520f;
    final float filterPanelHeight = 220f;
    final float filterPanelX = (1920f - filterPanelWidth) / 2f;
    final float filterPanelY = hotbarY + slotSize + pad * 2f + 20f; // just above hotbar

    // Value box layout
    final float filterLabelX = filterPanelX + 20f;
    final float valueX = filterPanelX + 220f;
    final float valueW = 260f;
    final float valueH = 34f;

    // Row vertical layout (edit THESE to move rows)
    final float rowsTopPad = 100f;   // distance from panel top to first row's box bottom
    final float rowStep = 44f;      // distance between row box bottoms (can be != valueH)

    // ---------------- Orders panel (HUD coords) ----------------
    private boolean ordersOpen = false;
    @SuppressWarnings("FieldMayBeFinal")
    private boolean ordersClipDebug = false;
    private final Vector3 tmpVec3 = new Vector3();
    private final Vector3 tmpVec3b = new Vector3();

    private final float ordersPanelW = 560f;
    private final float ordersPanelH = 640f;
    private final float ordersPanelX = 1920f - ordersPanelW - 20f; // right side
    private final float ordersPanelY = 1080f - ordersPanelH - 20f; // top padding
    // Orders list scrolling (pixels). 0 = top.
    private float ordersScrollPx = 0f;

    // ---------------- Options panel (HUD coords) ----------------
    private boolean optionsOpen = false;

    private static final float PADDING = 20f;

    private float optionsPanelW;
    private float optionsPanelH;
    private float optionsPanelX;
    private float optionsPanelY;

    // Toggle layout inside options panel
    private final float optRowH = 44f;
    private final float optCheckSize = 26f;
    private final float optLeftPad = 20f;
    private final float optTopPad = 110f;  // space for title/subtitle
    private final float optRowGap = 6f;

    private final float optInputW = 120f;
    private final float optInputH = 28f;
    private final float optInputPadRight = 18f;




    // ---------- General Hud Methods ---------
    public boolean isMouseOverBlockingUi(float hudX, float hudY, boolean filterOpen) {
        if (isOverHotbar(hudX, hudY)) return true;
        if (filterOpen && isOverFilterPanel(hudX, hudY)) return true;
        if (ordersOpen && isOverOrdersPanel(hudX, hudY)) return true;

        // NEW
        if (optionsOpen && isOverOptionsPanel(hudX, hudY)) return true;

        return false;
    }
    private static boolean inBox(float x, float y, float bx, float by, float bw, float bh) {
        return x >= bx && x <= bx + bw && y >= by && y <= by + bh;
    }
    public Hud() {
        cam = new OrthographicCamera();
        cam.setToOrtho(false, 1920, 1080);
        cam.update();

        uiFont = new BitmapFont(Gdx.files.internal("fonts/UI_large.fnt"));
        uiFont.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);

        smallFont = new BitmapFont(Gdx.files.internal("fonts/UI_small.fnt"));
        smallFont.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);

        recalcOptionsPanel();
    }
    private static String tileName(int tileId) {
        switch (tileId) {
            case WorldGrid.TILE_CONVEYOR: return "Conveyor";
            case WorldGrid.TILE_SMELTER:  return "Smelter";
            case WorldGrid.TILE_CRUSHER:  return "Crusher";
            case WorldGrid.TILE_SPAWNER:  return "Spawner";
            case WorldGrid.TILE_SELLPAD:  return "Sellpad";
            case WorldGrid.TILE_PRESS:    return "Press";
            case WorldGrid.TILE_ROLLER:   return "Roller";
            case WorldGrid.TILE_FILTER_FL:return "Filter (F+L)";
            case WorldGrid.TILE_FILTER_FR:return "Filter (F+R)";
            case WorldGrid.TILE_FILTER_LR:return "Filter (L+R)";
            case WorldGrid.TILE_SPLITTER: return "Splitter";
            case WorldGrid.TILE_MERGER:   return "Merger";
            default: return "Tile " + tileId;
        }
    }
    public void dispose() {
        uiFont.dispose();
        smallFont.dispose();
    }
    public void draw(SpriteBatch batch, float money, int itemCount, int page, int slot, int hoverSlot, int[] pageTiles, TextureRegion[] iconByTileId, TextureRegion white, float[] costByTile) {
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        uiFont.draw(batch, "Money: $" + (Math.round(money * 10f) / 10f), 20, 1060);
        uiFont.draw(batch, "Items: " + itemCount, 20, 1020);

        // background strip
        batch.setColor(0f, 0f, 0f, 0.5f);
        batch.draw(white, hotbarX, hotbarY, hotbarWidth, slotSize + pad * 2f);
        batch.setColor(1f, 1f, 1f, 1f);

        // slots
        for (int i = 0; i < 10; i++) {
            float x = hotbarX + pad + i * (slotSize + pad);
            float y = hotbarY + pad;

            // slot background
            batch.setColor(0.15f, 0.15f, 0.15f, 0.9f);
            batch.draw(white, x, y, slotSize, slotSize);

            // highlight selected
            if (i == slot) {
                batch.setColor(1f, 1f, 1f, 0.25f);
                batch.draw(white, x - 3f, y - 3f, slotSize + 6f, slotSize + 6f);
            }

            // icon
            int tileId = pageTiles[i];
            if (tileId != 0 && tileId < iconByTileId.length) {
                TextureRegion icon = iconByTileId[tileId];
                if (icon != null) {
                    batch.setColor(1f, 1f, 1f, 1f);

                    // Fit icon into slot (keep a margin)
                    float m = 6f;
                    batch.draw(icon, x + m, y + m, slotSize - m * 2f, slotSize - m * 2f);
                }
            }

            batch.setColor(1f, 1f, 1f, 1f);
        }

        // optional: small page indicator (tiny text is fine)
        smallFont.draw(batch, "Page " + (page + 1), hotbarX, hotbarY + slotSize + pad * 2f + 18f);

        // --- Hover tooltip ---
        if (hoverSlot >= 0 && hoverSlot < 10) {
            int tileId = pageTiles[hoverSlot];
            if (tileId != 0) {
                String name = tileName(tileId);

                float cost = (tileId >= 0 && tileId < costByTile.length) ? costByTile[tileId] : 0f;

                String line1 = name;
                String line2 = "$" + (int) ((int) (cost * 10f) / 10f);

                // position tooltip above the hovered slot
                float sx = hotbarX + pad + hoverSlot * (slotSize + pad);
                float sy = hotbarY + pad;

                float descPopupWidth = 180f;
                float gapBetweenNameAndPrice = 50f;
                float tx = sx + slotSize * 0.5f - descPopupWidth * 0.5f;
                float ty = sy + slotSize + 16f;

                // clamp inside screen
                if (tx < 10f) tx = 10f;
                if (tx + descPopupWidth > 1910f) tx = 1910f - descPopupWidth;

                batch.setColor(0f, 0f, 0f, 0.75f);
                batch.draw(white, tx, ty, descPopupWidth, gapBetweenNameAndPrice);
                batch.setColor(1f, 1f, 1f, 1f);

                smallFont.draw(batch, line1, tx + 10f, ty + gapBetweenNameAndPrice - 8f);
                smallFont.draw(batch, line2, tx + 10f, ty + 23f);
            }
        }

        batch.end();
    }
    public void drawMergervariant (SpriteBatch batch, int variant, float drawX, float drawY){
        smallFont.draw(batch, Integer.toString(variant), drawX + 4f, drawY + WorldGrid.CELL - 4f);
    }
    // Returns -1 if not over any slot, else 0..9
    public int slotAt(float hudX, float hudY) {
        float barH = slotSize + pad * 2f;
        if (hudX < hotbarX || hudX > hotbarX + hotbarWidth) return -1;
        if (hudY < hotbarY || hudY > hotbarY + barH) return -1;

        float slotsY = hotbarY + pad;
        if (hudY < slotsY || hudY > slotsY + slotSize) return -1;

        for (int i = 0; i < 10; i++) {
            float sx = hotbarX + pad + i * (slotSize + pad);
            float sy = slotsY;
            //noinspection ConstantValue
            if (hudX >= sx && hudX <= sx + slotSize && hudY >= sy && hudY <= sy + slotSize) {
                return i;
            }
        }
        return -1;
    }
    public boolean isOverHotbar(float hudX, float hudY) {
        float barH = slotSize + pad * 2f;
        return hudX >= hotbarX && hudX <= hotbarX + hotbarWidth && hudY >= hotbarY && hudY <= hotbarY + barH;
    }

    // ---------- Filter Methods ---------
    // Returns the Y (bottom) of the first row's value box
    private float firstRowY() {
        return filterPanelY + filterPanelHeight - rowsTopPad;
    }
    // Computes row Y positions in the exact same order you draw/click
    private float rowYForward() { return firstRowY(); }
    private float rowYLeft(boolean hasForward) { return firstRowY() - (hasForward ? rowStep : 0f); }
    private float rowYRight(boolean hasForward, boolean hasLeft) {
        return firstRowY() - (hasForward ? rowStep : 0f) - (hasLeft ? rowStep : 0f);
    }
    // Returns: -1 none, 0 forward, 1 left, 2 right, 99 close
    public int filterButtonAt(float hudX, float hudY, org.gudu0.AwareMemory.entities.FilterEntity.Variant variant) {
        if (!isOverFilterPanel(hudX, hudY)) return -1;

        // close button (top-right)
        float cx = filterPanelX + filterPanelWidth - 34f;
        float cy = filterPanelY + filterPanelHeight - 34f;
        if (hudX >= cx && hudX <= cx + 24f && hudY >= cy && hudY <= cy + 24f) return 99;

        // rows present depends on variant
        boolean hasForward = (variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.FL ||
            variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.FR);
        boolean hasLeft    = (variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.FL ||
            variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.LR);
        boolean hasRight   = (variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.FR ||
            variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.LR);

        float yF = rowYForward();
        float yL = rowYLeft(hasForward);
        float yR = rowYRight(hasForward, hasLeft);

        if (hasForward && inBox(hudX, hudY, valueX, yF, valueW, valueH)) return 0;
        if (hasLeft    && inBox(hudX, hudY, valueX, yL, valueW, valueH)) return 1;
        if (hasRight   && inBox(hudX, hudY, valueX, yR, valueW, valueH)) return 2;

        return -1;
    }
    public boolean isOverFilterPanel(float hudX, float hudY) {
        return hudX >= filterPanelX && hudX <= filterPanelX + filterPanelWidth && hudY >= filterPanelY && hudY <= filterPanelY + filterPanelHeight;
    }
    public void drawFilterPanel(SpriteBatch batch, FilterEntity filter, TextureRegion white) {
        FilterEntity.Variant variant = filter.getVariant();

        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        // panel bg
        batch.setColor(0f, 0f, 0f, 0.75f);
        batch.draw(white, filterPanelX, filterPanelY, filterPanelWidth, filterPanelHeight);
        batch.setColor(1f, 1f, 1f, 1f);

        uiFont.draw(batch, "Filter", filterPanelX + 16f, filterPanelY + filterPanelHeight - 16f);
        smallFont.draw(batch, "Shift+Click to open - LMB next - RMB prev - Esc close",
            filterPanelX + 16f, filterPanelY + 20f);

        // close button
        float cx = filterPanelX + filterPanelWidth - 34f;
        float cy = filterPanelY + filterPanelHeight - 34f;
        batch.setColor(0.2f, 0.2f, 0.2f, 1f);
        batch.draw(white, cx, cy, 24f, 24f);
        batch.setColor(1f, 1f, 1f, 1f);
        smallFont.draw(batch, "X", cx + 7f, cy + 18f);

        boolean hasForward = (variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.FL ||
            variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.FR);
        boolean hasLeft    = (variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.FL ||
            variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.LR);
        boolean hasRight   = (variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.FR ||
            variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.LR);

        float yF = rowYForward();
        float yL = rowYLeft(hasForward);
        float yR = rowYRight(hasForward, hasLeft);

        if (hasForward) drawRow(batch, white, "Forward", ruleText(filter.getRule(FilterEntity.Out.FORWARD)), yF);
        if (hasLeft)    drawRow(batch, white, "Left",    ruleText(filter.getRule(FilterEntity.Out.LEFT)),    yL);
        if (hasRight)   drawRow(batch, white, "Right",   ruleText(filter.getRule(FilterEntity.Out.RIGHT)),   yR);

        batch.end();
    }
    private void drawRow(SpriteBatch batch, TextureRegion white, String label, String value, float y) {
        smallFont.draw(batch, label + ":", filterLabelX, y + 24f);

        batch.setColor(0.15f, 0.15f, 0.15f, 1f);
        batch.draw(white, valueX, y, valueW, valueH);
        batch.setColor(1f, 1f, 1f, 1f);

        smallFont.draw(batch, value, valueX + 10f, y + 24f);
    }
    private static String ruleText(int rule) {
        if (rule == org.gudu0.AwareMemory.entities.FilterEntity.RULE_ANY) return "Any";
        if (rule == org.gudu0.AwareMemory.entities.FilterEntity.RULE_NONE) return "None";
        // ordinal -> name
        return org.gudu0.AwareMemory.ItemType.values()[rule].name();
    }

    // ------------- Orders Methods -----------
    public boolean isOverOrdersPanel(float hudX, float hudY) {
        return hudX >= ordersPanelX && hudX <= ordersPanelX + ordersPanelW
            && hudY >= ordersPanelY && hudY <= ordersPanelY + ordersPanelH;
    }
    public void toggleOrdersPanel() {
        ordersOpen = !ordersOpen;
        if (ordersOpen) ordersScrollPx = 0f;
    }
    public boolean isOrdersPanelOpen() {
        return ordersOpen;
    }
    public void drawOrdersPanel(SpriteBatch batch, OrderManager orders, float currentMoney, TextureRegion white, Viewport hudViewport) {
        if (!ordersOpen) return;

        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        // Panel background
        batch.setColor(0f, 0f, 0f, 0.78f);
        batch.draw(white, ordersPanelX, ordersPanelY, ordersPanelW, ordersPanelH);
        batch.setColor(1f, 1f, 1f, 1f);

        uiFont.draw(batch, "Orders", ordersPanelX + 16f, ordersPanelY + ordersPanelH - 16f);
        smallFont.draw(batch, "Press O to close", ordersPanelX + 16f, ordersPanelY + ordersPanelH - 52f);

        // List layout
        float contentX = ordersPanelX + 16f;
        float contentW = ordersPanelW - 32f;

        float listTop = ordersListTopY();
        float listBottom = ordersListBottomY();

        float y = listTop + ordersScrollPx; // <-- scroll moves content up as scroll increases
        float cardH = 110f;
        float gap = 10f;

        // --- Clip the scrolling list so cards never render outside the panel ---
        // This is the real fix for "spilling past the bottom".
        batch.flush();

        Rectangle clipBounds = new Rectangle(
            contentX,
            listBottom,
            contentW,
            listTop - listBottom
        );
        tmpVec3.set(clipBounds.x, clipBounds.y, 0f);
        tmpVec3b.set(clipBounds.x + clipBounds.width, clipBounds.y + clipBounds.height, 0f);
        hudViewport.project(tmpVec3);
        hudViewport.project(tmpVec3b);

        Rectangle scissors = getRectangle();
        ScissorStack.pushScissors(scissors);



        List<Order> list = orders.getActiveOrdersReadOnly(); // active milestone orders
        if (list.isEmpty()) {
            smallFont.draw(batch, "No active orders.", contentX, y);
            batch.end();
            return;
        }

        // Order has progressText/progress01 helpers
        for (Order o : list) {
            float cardTop = y;
            float cardBottom = y - cardH;

            // Skip drawing if this card is fully above the visible list area
            if (cardBottom > listTop) {
                y -= (cardH + gap);
                continue;
            }

            // Stop if we've gone below the visible list area (everything else will be lower too)
            if (cardTop < listBottom) break;


            // Card background
            batch.setColor(0.12f, 0.12f, 0.12f, 0.95f);
            batch.draw(white, contentX, y - cardH, contentW, cardH);
            batch.setColor(1f, 1f, 1f, 1f);

            // Title + description
            smallFont.draw(batch, o.title, contentX + 10f, y - 12f);
            smallFont.draw(batch, o.desc, contentX + 10f, y - 34f);

            // Progress text
            String prog = o.progressText(currentMoney);
            smallFont.draw(batch, prog, contentX + 10f, y - 58f);

            // Progress bar
            float barX = contentX + 10f;
            float barY = y - 92f;
            float barW = contentW - 20f;
            float barHh = 16f;

            // bar bg
            batch.setColor(0.05f, 0.05f, 0.05f, 1f);
            batch.draw(white, barX, barY, barW, barHh);

            // bar fill
            float fill = barW * o.progress01(currentMoney);
            batch.setColor(o.completed ? 0.3f : 0.2f, o.completed ? 0.8f : 0.6f, 0.2f, 1f);
            batch.draw(white, barX, barY, fill, barHh);
            batch.setColor(1f, 1f, 1f, 1f);

            // --- Claim button (top-right of the card) ---
            // Keep these numbers in ONE place so draw + hit-test match.
            float btnW = 120f;
            float btnH = 30f;
            float btnPad = 10f;

            // Card top-right corner area:
            float btnX = contentX + contentW - btnW - btnPad;
            float btnY = (y - btnPad) - btnH; // measured down from the card top (y)

            boolean canClaim = o.completed && !o.claimed;

            if (canClaim) {
                // Brighter when clickable
                batch.setColor(0.20f, 0.70f, 0.25f, 1f);
            } else {
                // Dim when not claimable
                batch.setColor(0.20f, 0.20f, 0.20f, 1f);
            }

            batch.draw(white, btnX, btnY, btnW, btnH);
            batch.setColor(1f, 1f, 1f, 1f);

            // Button label
            String label;
            if (o.claimed) label = "CLAIMED";
            else if (o.completed) label = "CLAIM +$" + o.rewardMoney;
            else label = "+$" + o.rewardMoney; // show reward even before completion

            smallFont.draw(batch, label, btnX + 10f, btnY + 21f);


            y -= (cardH + gap);

            // Stop drawing if we run off the panel (scroll later)
            if (y < ordersPanelY + 20f) break;
        }
        batch.flush();
        ScissorStack.popScissors();
        if (ordersClipDebug) {
            float t = 2f;
            batch.setColor(1f, 0.15f, 0.15f, 0.9f);
            batch.draw(white, clipBounds.x, clipBounds.y, clipBounds.width, t);
            batch.draw(white, clipBounds.x, clipBounds.y + clipBounds.height - t, clipBounds.width, t);
            batch.draw(white, clipBounds.x, clipBounds.y, t, clipBounds.height);
            batch.draw(white, clipBounds.x + clipBounds.width - t, clipBounds.y, t, clipBounds.height);
            batch.setColor(1f, 1f, 1f, 1f);
        }

        batch.end();
    }

    private Rectangle getRectangle() {
        float scX = Math.min(tmpVec3.x, tmpVec3b.x);
        float scY = Math.min(tmpVec3.y, tmpVec3b.y);
        float scW = Math.abs(tmpVec3b.x - tmpVec3.x);
        float scH = Math.abs(tmpVec3b.y - tmpVec3.y);

        float scaleX = (float) Gdx.graphics.getBackBufferWidth() / (float) Gdx.graphics.getWidth();
        float scaleY = (float) Gdx.graphics.getBackBufferHeight() / (float) Gdx.graphics.getHeight();
        if (scaleX != 1f || scaleY != 1f) {
            scX *= scaleX;
            scY *= scaleY;
            scW *= scaleX;
            scH *= scaleY;
        }

        return new Rectangle(scX, scY, scW, scH);
    }

    /**
     * Returns the index of the active order whose CLAIM button is under the mouse.
     * Returns -1 if none.
     * <p>
     * IMPORTANT: This uses the SAME layout math as drawOrdersPanel().
     */
    public int ordersClaimButtonAt(float hudX, float hudY, OrderManager orders) {
        if (!ordersOpen) return -1;
        if (!isOverOrdersPanel(hudX, hudY)) return -1;
        // Only allow clicking within the scrollable list area (not header/footer).
        if (hudY > ordersListTopY() || hudY < ordersListBottomY()) return -1;


        float contentX = ordersPanelX + 16f;
        float contentW = ordersPanelW - 32f;

        float y = ordersListTopY() + ordersScrollPx;
        float cardH = 110f;
        float gap = 10f;

        // Button layout (must match draw)
        float btnW = 120f;
        float btnH = 30f;
        float btnPad = 10f;

        List<Order> list = orders.getActiveOrdersReadOnly();

        for (int i = 0; i < list.size(); i++) {
            Order o = list.get(i);

            float btnX = contentX + contentW - btnW - btnPad;
            float btnY = (y - btnPad) - btnH;

            boolean canClaim = o.completed && !o.claimed;
            if (canClaim && inBox(hudX, hudY, btnX, btnY, btnW, btnH)) {
                return i;
            }

            y -= (cardH + gap);
            if (y < ordersListBottomY()) break;
        }

        return -1;
    }
    // Top of the list area (below title)
    private float ordersListTopY() { return ordersPanelY + ordersPanelH - 90f; }
    // Bottom padding inside the panel
    private float ordersListBottomY() { return ordersPanelY + 20f; }
    // Visible height for the scrolling list
    private float ordersListViewportH() { return ordersListTopY() - ordersListBottomY(); }
    private float ordersContentHeight(int orderCount) {
        float cardH = 110f;
        float gap = 10f;
        if (orderCount <= 0) return 0f;
        return orderCount * cardH + (orderCount - 1) * gap;
    }
    private float ordersMaxScroll(OrderManager orders) {
        int n = orders.getActiveOrdersReadOnly().size();
        float content = ordersContentHeight(n);
        float view = ordersListViewportH();
        return Math.max(0f, content - view);
    }
    /**
     * Call this when mouse wheel scroll happens while hovering the orders panel.
     * wheelAmountY is the LibGDX scroll amount (typically +/- 1 per notch).
     */
    public void scrollOrders(float wheelAmountY, OrderManager orders) {
        float pixelsPerNotch = 40f;

        // wheelAmountY > 0 usually means "scroll down" (show later items)
        ordersScrollPx += wheelAmountY * pixelsPerNotch;

        ordersScrollPx = clamp(ordersScrollPx, 0f, ordersMaxScroll(orders));
    }
    @SuppressWarnings("SameParameterValue")
    private static float clamp(float v, float lo, float hi) {
        if (v < lo) return lo;
        return Math.min(v, hi);
    }

    // ------------ Options Methods --------------------------------------------------------------------------------------------------------
    private final GlyphLayout glyph = new GlyphLayout();
    private final float optRowBgInset = 14f; // shift row background right a bit


    public void toggleOptionsMenu() {
        optionsOpen = !optionsOpen;
    }
    @SuppressWarnings("unused")
    public boolean isOptionsMenuOpen() {
        return optionsOpen;
    }
    public boolean isOverOptionsPanel(float hudX, float hudY) {
        return hudX >= optionsPanelX && hudX <= optionsPanelX + optionsPanelW
            && hudY >= optionsPanelY && hudY <= optionsPanelY + optionsPanelH;
    }
    public void drawOptionsMenu(SpriteBatch batch, TextureRegion white) {
        if (!optionsOpen) return;

        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        // Panel background
        batch.setColor(0f, 0f, 0f, 0.78f);
        batch.draw(white, optionsPanelX, optionsPanelY, optionsPanelW, optionsPanelH);
        batch.setColor(1f, 1f, 1f, 1f);

        uiFont.draw(batch, "Options", optionsPanelX + 16f, optionsPanelY + optionsPanelH - 16f);
        smallFont.draw(batch, "Press P to close", optionsPanelX + 16f, optionsPanelY + optionsPanelH - 52f);

        // ---- Toggles ----
        float startY = optionsPanelY + optionsPanelH - optTopPad;
        float checkX = optionsPanelX + optLeftPad + 10;
        float labelX = checkX + optCheckSize + 14f;

        for (int i = 0; i < optionToggles.size(); i++) {
            ToggleEntry t = optionToggles.get(i);
            boolean on = t.getter.get();

            float rowTop = startY - i * (optRowH + optRowGap);
            float rowBottom = rowTop - optRowH;

            // row bg
            batch.setColor(0.12f, 0.12f, 0.12f, 0.85f);
            batch.draw(white, optionsPanelX + optLeftPad, rowBottom, optionsPanelW - optLeftPad * 2f, optRowH);

            // checkbox bg
            float cy = rowBottom + (optRowH - optCheckSize) * 0.5f;
            batch.setColor(0.18f, 0.18f, 0.18f, 1f);
            batch.draw(white, checkX, cy, optCheckSize, optCheckSize);

            // checkbox fill if on
            if (on) {
                batch.setColor(0.25f, 0.85f, 0.25f, 1f);
                batch.draw(white, checkX + 4f, cy + 4f, optCheckSize - 8f, optCheckSize - 8f);
            }

            batch.setColor(1f, 1f, 1f, 1f);
            smallFont.draw(batch, t.label, labelX, rowBottom + 28f);
        }

        batch.setColor(1f, 1f, 1f, 1f);
        // ---- Int inputs ----
        float intStartY = startY - optionToggles.size() * (optRowH + optRowGap) - 16f;

        for (int i = 0; i < optionInts.size(); i++) {
            IntEntry e = optionInts.get(i);

            float rowTop = intStartY - i * (optRowH + optRowGap);
            float rowBottom = rowTop - optRowH;

            // row bg
            batch.setColor(0.12f, 0.12f, 0.12f, 0.85f);
            batch.draw(white, optionsPanelX + optLeftPad, rowBottom, optionsPanelW - optLeftPad * 2f, optRowH);

            // label
            batch.setColor(1f, 1f, 1f, 1f);
            smallFont.draw(batch, e.label, optionsPanelX + optLeftPad + 12f, rowBottom + 28f);

            // input box on right
            float boxX = optionsPanelX + optionsPanelW - optLeftPad - optInputPadRight - optInputW;
            float boxY = rowBottom + (optRowH - optInputH) * 0.5f;

            // box bg (brighter when focused)
            if (e.focused) batch.setColor(0.22f, 0.22f, 0.22f, 1f);
            else           batch.setColor(0.16f, 0.16f, 0.16f, 1f);
            batch.draw(white, boxX, boxY, optInputW, optInputH);

            // text (what user is typing if focused, else current value)
            batch.setColor(1f, 1f, 1f, 1f);
            String shown = e.focused ? e.editText : Integer.toString(e.getter.get());
            smallFont.draw(batch, shown, boxX + 8f, boxY + 20f);

            // blinking cursor when focused (positioned after text)
            if (e.focused && ((System.currentTimeMillis() / 350) % 2 == 0)) {
                glyph.setText(smallFont, shown);
                float textW = glyph.width;

                float textX = boxX + 8f;
                float cursorX = textX + textW + 2f;

                // clamp so it never draws outside the box
                float cursorMaxX = boxX + optInputW - 6f;
                if (cursorX > cursorMaxX) cursorX = cursorMaxX;

                batch.setColor(1f, 1f, 1f, 1f);
                batch.draw(white, cursorX, boxY + 6f, 2f, optInputH - 12f);
            }


            // min/max hint (optional)
            // smallFont.draw(batch, "[" + e.min + ".." + e.max + "]", boxX - 92f, boxY + 20f);
        }


        batch.end();
    }

    protected void recalcOptionsPanel() {
        float screenW = cam.viewportWidth;   // 1920
        float screenH = cam.viewportHeight;  // 1080

        optionsPanelW = screenW * 0.85f;
        optionsPanelH = screenH * 0.80f;

        optionsPanelX = (screenW - optionsPanelW) * 0.5f;
        optionsPanelY = (screenH - optionsPanelH) * 0.5f;

        optionsPanelX = Math.max(PADDING, optionsPanelX);
        optionsPanelY = Math.max(PADDING, optionsPanelY);
    }
    // ---------- Options: toggle model ----------
    public interface BoolGetter { boolean get(); }
    public interface BoolSetter { void set(boolean v); }

    private static final class ToggleEntry {
        final String label;
        final BoolGetter getter;
        final BoolSetter setter;

        ToggleEntry(String label, BoolGetter getter, BoolSetter setter) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
        }
    }

    private final ArrayList<ToggleEntry> optionToggles = new ArrayList<>();

    public void addToggle(String label, BoolGetter getter, BoolSetter setter) {
        optionToggles.add(new ToggleEntry(label, getter, setter));
    }

    // Returns toggle index if clicking a checkbox row; else -1
    public int optionsToggleAt(float hudX, float hudY) {
        if (!optionsOpen) return -1;
        if (!isOverOptionsPanel(hudX, hudY)) return -1;

        float startY = optionsPanelY + optionsPanelH - optTopPad; // top row y (row top)
        float checkX = optionsPanelX + optLeftPad;
        float rowW = optionsPanelW - optLeftPad * 2f;

        for (int i = 0; i < optionToggles.size(); i++) {
            float rowTop = startY - i * (optRowH + optRowGap);
            float rowBottom = rowTop - optRowH;

            // click anywhere in row toggles it (feels good)
            if (inBox(hudX, hudY, optionsPanelX + optLeftPad, rowBottom, rowW, optRowH)) {
                return i;
            }

            // (optional) restrict only to checkbox area:
            // if (inBox(hudX, hudY, checkX, rowBottom + (optRowH - optCheckSize)/2f, optCheckSize, optCheckSize)) return i;
        }

        return -1;
    }

    public void toggleOption(int index) {
        if (index < 0 || index >= optionToggles.size()) return;
        ToggleEntry t = optionToggles.get(index);
        boolean cur = t.getter.get();
        t.setter.set(!cur);
    }

    // --------- Options Int Input --------
    public interface IntGetter { int get(); }
    public interface IntSetter { void set(int v); }

    private static final class IntEntry {
        final String label;
        final IntGetter getter;
        final IntSetter setter;
        final int min;
        final int max;

        String editText;      // what user is typing
        boolean focused = false;

        IntEntry(String label, IntGetter getter, IntSetter setter, int min, int max) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
            this.editText = Integer.toString(getter.get());
        }
    }

    private final ArrayList<IntEntry> optionInts = new ArrayList<>();

    public void addIntOption(String label, int min, int max, IntGetter getter, IntSetter setter) {
        optionInts.add(new IntEntry(label, getter, setter, min, max));
    }

    private int focusedIntIndex = -1;

    public boolean clickOptionsInt(float hudX, float hudY) {
        if (!optionsOpen) return false;

        // if click outside panel, commit + unfocus
        if (!isOverOptionsPanel(hudX, hudY)) {
            commitFocusedInt();
            unfocusAllInts();
            return false;
        }

        // compute where int rows start (must match draw math)
        float startY = optionsPanelY + optionsPanelH - optTopPad;
        float intStartY = startY - optionToggles.size() * (optRowH + optRowGap) - 16f;

        float boxX = optionsPanelX + optionsPanelW - optLeftPad - optInputPadRight - optInputW;

        for (int i = 0; i < optionInts.size(); i++) {
            float rowTop = intStartY - i * (optRowH + optRowGap);
            float rowBottom = rowTop - optRowH;

            float boxY = rowBottom + (optRowH - optInputH) * 0.5f;

            // click inside the input box focuses it
            if (inBox(hudX, hudY, boxX, boxY, optInputW, optInputH)) {
                focusInt(i);
                return true;
            }

            // (optional) click anywhere on row focuses too:
            // if (inBox(hudX, hudY, optionsPanelX + optLeftPad, rowBottom, optionsPanelW - optLeftPad*2f, optRowH)) { focusInt(i); return true; }
        }

        // clicked inside panel but not on an int box => commit/unfocus
        commitFocusedInt();
        unfocusAllInts();
        return false;
    }

    private void focusInt(int idx) {
        if (idx < 0 || idx >= optionInts.size()) return;

        // commit old focused first
        commitFocusedInt();

        unfocusAllInts();
        focusedIntIndex = idx;
        IntEntry e = optionInts.get(idx);
        e.focused = true;
        e.editText = Integer.toString(e.getter.get()); // refresh
    }

    private void unfocusAllInts() {
        for (IntEntry e : optionInts) e.focused = false;
        focusedIntIndex = -1;
    }

    private void commitFocusedInt() {
        if (focusedIntIndex < 0 || focusedIntIndex >= optionInts.size()) return;

        IntEntry e = optionInts.get(focusedIntIndex);
        int parsed;
        try {
            // allow empty / "-" to mean "no commit"
            if (e.editText == null || e.editText.isEmpty() || e.editText.equals("-")) return;

            parsed = Integer.parseInt(e.editText);
        } catch (Exception ex) {
            // revert to current value on invalid
            e.editText = Integer.toString(e.getter.get());
            return;
        }

        // clamp
        if (parsed < e.min) parsed = e.min;
        if (parsed > e.max) parsed = e.max;

        e.setter.set(parsed);
        e.editText = Integer.toString(parsed);
    }

    public boolean optionsKeyTyped(char character) {
        if (!optionsOpen) return false;
        if (focusedIntIndex < 0 || focusedIntIndex >= optionInts.size()) return false;

        IntEntry e = optionInts.get(focusedIntIndex);

        // digits
        if (character >= '0' && character <= '9') {
            e.editText += character;
            return true;
        }

        // allow minus only at start (optional)
        if (character == '-' && (e.editText.isEmpty())) {
            e.editText = "-";
            return true;
        }

        return false;
    }

    public boolean optionsKeyDown(int keycode) {
        if (!optionsOpen) return false;
        if (focusedIntIndex < 0 || focusedIntIndex >= optionInts.size()) return false;

        IntEntry e = optionInts.get(focusedIntIndex);

        if (keycode == com.badlogic.gdx.Input.Keys.BACKSPACE) {
            if (!e.editText.isEmpty()) e.editText = e.editText.substring(0, e.editText.length() - 1);
            return true;
        }

        if (keycode == com.badlogic.gdx.Input.Keys.ENTER) {
            commitFocusedInt();
            unfocusAllInts();
            return true;
        }

        if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
            // cancel = revert and unfocus
            e.editText = Integer.toString(e.getter.get());
            unfocusAllInts();
            return true;
        }

        return false;
    }


}
