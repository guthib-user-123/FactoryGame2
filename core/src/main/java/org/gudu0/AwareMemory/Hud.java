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

    public void dispose() {
        uiFont.dispose();
    }
}
