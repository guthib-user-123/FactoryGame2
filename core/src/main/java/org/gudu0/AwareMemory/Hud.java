package org.gudu0.AwareMemory;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Hud {
    public final OrthographicCamera cam;
    public final BitmapFont uiFont;
    public final BitmapFont smallFont;
    final float slotSize = 64f;
    final float pad = 8f;
    final float barW = (slotSize * 10f) + (pad * 11f);

    final float x0 = (1920f - barW) / 2f;
    final float y0 = 20f;


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
