package org.gudu0.AwareMemory;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.gudu0.AwareMemory.entities.FilterEntity;

public class Hud {
    public final OrthographicCamera cam;
    public final BitmapFont uiFont;
    public final BitmapFont smallFont;
    final float slotSize = 64f;
    final float pad = 8f;
    final float barW = (slotSize * 10f) + (pad * 11f);

    final float x0 = (1920f - barW) / 2f;
    final float y0 = 20f;

    // Filter panel layout (HUD coords)
    final float fpW = 520f;
    final float fpH = 220f;
    final float fpX = (1920f - fpW) / 2f;
    final float fpY = y0 + slotSize + pad * 2f + 20f; // just above hotbar

    // Value box layout
    final float rowH = 44f;
    final float labelX = fpX + 20f;
    final float valueX = fpX + 220f;
    final float valueW = 260f;
    final float valueH = 34f;

    public boolean isOverFilterPanel(float hudX, float hudY) {
        return hudX >= fpX && hudX <= fpX + fpW && hudY >= fpY && hudY <= fpY + fpH;
    }

    // Returns: -1 none, 0 forward, 1 left, 2 right, 99 close
    public int filterButtonAt(float hudX, float hudY, org.gudu0.AwareMemory.entities.FilterEntity.Variant variant) {
        if (!isOverFilterPanel(hudX, hudY)) return -1;

        // close button (top-right)
        float cx = fpX + fpW - 34f;
        float cy = fpY + fpH - 34f;
        if (hudX >= cx && hudX <= cx + 24f && hudY >= cy && hudY <= cy + 24f) return 99;

        // rows present depends on variant
        boolean hasForward = (variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.FL ||
            variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.FR);
        boolean hasLeft    = (variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.FL ||
            variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.LR);
        boolean hasRight   = (variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.FR ||
            variant == org.gudu0.AwareMemory.entities.FilterEntity.Variant.LR);

        float y = fpY + fpH - 70f;

        if (hasForward && inBox(hudX, hudY, valueX, y, valueW, valueH)) return 0;
        if (hasForward) y -= rowH;

        if (hasLeft && inBox(hudX, hudY, valueX, y, valueW, valueH)) return 1;
        if (hasLeft) y -= rowH;

        if (hasRight && inBox(hudX, hudY, valueX, y, valueW, valueH)) return 2;

        return -1;
    }

    private static boolean inBox(float x, float y, float bx, float by, float bw, float bh) {
        return x >= bx && x <= bx + bw && y >= by && y <= by + bh;
    }

    public void drawFilterPanel(SpriteBatch batch, FilterEntity filter, TextureRegion white) {
        var variant = filter.getVariant();

        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        // panel bg
        batch.setColor(0f, 0f, 0f, 0.75f);
        batch.draw(white, fpX, fpY, fpW, fpH);
        batch.setColor(1f, 1f, 1f, 1f);

        uiFont.draw(batch, "Filter", fpX + 16f, fpY + fpH - 16f);
        smallFont.draw(batch, "Shift+Click to open - LMB next - RMB prev - Esc close",
            fpX + 16f, fpY + 20f);

        // close button
        float cx = fpX + fpW - 34f;
        float cy = fpY + fpH - 34f;
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

        float y = fpY + fpH - 70f;

        if (hasForward) { drawRow(batch, white, "Forward", ruleText(filter.getRule(org.gudu0.AwareMemory.entities.FilterEntity.Out.FORWARD)), y); y -= rowH; }
        if (hasLeft)    { drawRow(batch, white, "Left",    ruleText(filter.getRule(org.gudu0.AwareMemory.entities.FilterEntity.Out.LEFT)), y);    y -= rowH; }
        if (hasRight)   { drawRow(batch, white, "Right",   ruleText(filter.getRule(org.gudu0.AwareMemory.entities.FilterEntity.Out.RIGHT)), y);   y -= rowH; }

        batch.end();
    }

    private void drawRow(SpriteBatch batch, TextureRegion white, String label, String value, float y) {
        smallFont.draw(batch, label + ":", labelX, y + 24f);

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

    public void draw(SpriteBatch batch, float money, int itemCount, int page, int slot, int[] pageTiles, TextureRegion[] iconByTileId, TextureRegion white) {

        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        uiFont.draw(batch, "Money: $" + (Math.round(money * 10f) / 10f), 20, 1060);
        uiFont.draw(batch, "Items: " + itemCount, 20, 1020);

        // background strip
        batch.setColor(0f, 0f, 0f, 0.5f);
        batch.draw(white, x0, y0, barW, slotSize + pad * 2f);
        batch.setColor(1f, 1f, 1f, 1f);

        // slots
        for (int i = 0; i < 10; i++) {
            float x = x0 + pad + i * (slotSize + pad);
            float y = y0 + pad;

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
        smallFont.draw(batch, "Page " + (page + 1), x0, y0 + slotSize + pad * 2f + 18f);

        batch.end();
    }

    public void drawMergervariant (SpriteBatch batch, int variant, float drawX, float drawY){
        smallFont.draw(batch, Integer.toString(variant), drawX + 4f, drawY + WorldGrid.CELL - 4f);
    }

    // Returns -1 if not over any slot, else 0..9
    public int slotAt(float hudX, float hudY) {
        float barH = slotSize + pad * 2f;
        if (hudX < x0 || hudX > x0 + barW) return -1;
        if (hudY < y0 || hudY > y0 + barH) return -1;

        float slotsY = y0 + pad;
        if (hudY < slotsY || hudY > slotsY + slotSize) return -1;

        for (int i = 0; i < 10; i++) {
            float sx = x0 + pad + i * (slotSize + pad);
            float sy = slotsY;
            if (hudX >= sx && hudX <= sx + slotSize && hudY >= sy && hudY <= sy + slotSize) {
                return i;
            }
        }
        return -1;
    }

    public boolean isOverHotbar(float hudX, float hudY) {
        float barH = slotSize + pad * 2f;
        return hudX >= x0 && hudX <= x0 + barW && hudY >= y0 && hudY <= y0 + barH;
    }

    public void dispose() {
        uiFont.dispose();
    }
}
