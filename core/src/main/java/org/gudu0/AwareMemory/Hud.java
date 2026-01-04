package org.gudu0.AwareMemory;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.gudu0.AwareMemory.entities.FilterEntity;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;


import java.util.List;

@SuppressWarnings("EnhancedSwitchMigration")
public class Hud {
    public final OrthographicCamera cam;
    public final BitmapFont uiFont;
    public final BitmapFont smallFont;
    final float slotSize = 64f;
    final float pad = 8f;
    final float hotbarWidth = (slotSize * 10f) + (pad * 11f);

    final float hotbarX = (1920f - hotbarWidth) / 2f;
    final float hotbarY = 20f;

    // Filter panel layout (HUD coords)
    final float filterPanelWidth = 520f;
    final float filterPanelHeight = 220f;
    final float filterPanelX = (1920f - filterPanelWidth) / 2f;
    final float filterPanelY = hotbarY + slotSize + pad * 2f + 20f; // just above hotbar

    // Value box layout
    final float rowH = 44f;
    final float filterLabelX = filterPanelX + 20f;
    final float valueX = filterPanelX + 220f;
    final float valueW = 260f;
    final float valueH = 34f;

    // Row vertical layout (edit THESE to move rows)
    final float rowsTopPad = 100f;   // distance from panel top to first row's box bottom
    final float rowStep = 44f;      // distance between row box bottoms (can be != valueH)

    // ---------------- Orders panel (HUD coords) ----------------
    private boolean ordersOpen = false;

    private final float ordersPanelW = 560f;
    private final float ordersPanelH = 640f;
    private final float ordersPanelX = 1920f - ordersPanelW - 20f; // right side
    private final float ordersPanelY = 1080f - ordersPanelH - 20f; // top padding
    // Orders list scrolling (pixels). 0 = top.
    private float ordersScrollPx = 0f;



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


    public boolean isOverFilterPanel(float hudX, float hudY) {
        return hudX >= filterPanelX && hudX <= filterPanelX + filterPanelWidth && hudY >= filterPanelY && hudY <= filterPanelY + filterPanelHeight;
    }
    public boolean isMouseOverBlockingUi(float hudX, float hudY, boolean filterOpen) {
        // Hotbar always blocks placement clicks
        if (isOverHotbar(hudX, hudY)) return true;

        // Filter panel blocks if open
        if (filterOpen && isOverFilterPanel(hudX, hudY)) return true;

        // Orders panel blocks if open (we add this helper below)
        if (ordersOpen && isOverOrdersPanel(hudX, hudY)) return true;

        return false;
    }

    public boolean isOverOrdersPanel(float hudX, float hudY) {
        return hudX >= ordersPanelX && hudX <= ordersPanelX + ordersPanelW
            && hudY >= ordersPanelY && hudY <= ordersPanelY + ordersPanelH;
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

    private static boolean inBox(float x, float y, float bx, float by, float bw, float bh) {
        return x >= bx && x <= bx + bw && y >= by && y <= by + bh;
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

    public Hud() {
        cam = new OrthographicCamera();
        cam.setToOrtho(false, 1920, 1080);
        cam.update();

        uiFont = new BitmapFont();
        uiFont.getData().setScale(2.5f);

        smallFont = new BitmapFont();
        smallFont.getData().setScale(1.0f);
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

    public void toggleOrdersPanel() {
        ordersOpen = !ordersOpen;
        if (ordersOpen) ordersScrollPx = 0f;
    }


    public boolean isOrdersPanelOpen() {
        return ordersOpen;
    }

    public void closeOrdersPanel() {
        ordersOpen = false;
    }

    public void drawOrdersPanel(SpriteBatch batch, OrderManager orders, float currentMoney, TextureRegion white) {
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

        Rectangle scissors = new Rectangle();
        ScissorStack.calculateScissors(cam, batch.getTransformMatrix(), clipBounds, scissors);
        ScissorStack.pushScissors(scissors);



        List<Order> list = orders.getActiveOrdersReadOnly(); // active milestone orders
        if (list.isEmpty()) {
            smallFont.draw(batch, "No active orders.", contentX, y);
            batch.end();
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            Order o = list.get(i); // Order has progressText/progress01 helpers
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
            smallFont.draw(batch, o.desc,  contentX + 10f, y - 34f);

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

        batch.end();
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
            if (hudX >= sx && hudX <= sx + slotSize && hudY >= sy && hudY <= sy + slotSize) {
                return i;
            }
        }
        return -1;
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

    public boolean isOverHotbar(float hudX, float hudY) {
        float barH = slotSize + pad * 2f;
        return hudX >= hotbarX && hudX <= hotbarX + hotbarWidth && hudY >= hotbarY && hudY <= hotbarY + barH;
    }

    public void dispose() {
        uiFont.dispose();
    }

    // Top of the list area (below title)
    private float ordersListTopY() { return ordersPanelY + ordersPanelH - 90f; }
    // Bottom padding inside the panel
    private float ordersListBottomY() { return ordersPanelY + 20f; }
    // Visible height for the scrolling list
    private float ordersListViewportH() { return ordersListTopY() - ordersListBottomY(); }

    private static float clamp(float v, float lo, float hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

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

}
