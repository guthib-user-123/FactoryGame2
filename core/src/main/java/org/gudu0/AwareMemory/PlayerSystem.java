package org.gudu0.AwareMemory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class PlayerSystem {
    public Texture tex;
    public float x, y;

    public float speed = 300f;
    public float pushSpeed = 110f;

    public float centerStrength = 6.5f;
    public float snapEpsilon = 0.5f;

    // Feet placement box
    public float boxW = 22f, boxH = 14f;
    public float boxOffX = 5f, boxOffY = 0f;

    public PlayerSystem(Texture plrTex, WorldGrid world) {
        this.tex = plrTex;
        x = world.WORLD_W / 2f - tex.getWidth() / 2f;
        y = world.WORLD_H / 2f - tex.getHeight() / 2f;
    }

    public float boxX() { return x + boxOffX; }
    public float boxY() { return y + boxOffY; }

    public void updateMovement(float dt, WorldGrid world) {
        float mx = 0, my = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) mx += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))  mx -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))    my += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))  my -= 1f;

        if (mx != 0 || my != 0) {
            float len = (float)Math.sqrt(mx*mx + my*my);
            mx /= len; my /= len;
        }

        x += mx * speed * dt;
        y += my * speed * dt;

        clampToWorld(world);
    }

    private static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    public void updateMachinePush(float dt, WorldGrid world) {
        // Figure out which cells the player feet box overlaps
        float px = boxX();
        float py = boxY();
        float pw = boxW;
        float ph = boxH;

        int minX = (int)(px / WorldGrid.CELL);
        int maxX = (int)((px + pw) / WorldGrid.CELL);
        int minY = (int)(py / WorldGrid.CELL);
        int maxY = (int)((py + ph) / WorldGrid.CELL);

        float pushX = 0f;
        float pushY = 0f;

        for (int cx = minX; cx <= maxX; cx++) {
            for (int cy = minY; cy <= maxY; cy++) {
                if (cx < 0 || cy < 0 || cx >= world.wCells || cy >= world.hCells) continue;
                int id = world.grid[cx][cy];
                if (id != WorldGrid.TILE_CONVEYOR && id != WorldGrid.TILE_SMELTER) continue;

                int rot = world.rot[cx][cy];

                // 0 right, 1 down, 2 left, 3 up
                if (rot == 0) pushX += 1f;
                else if (rot == 1) pushY -= 1f;
                else if (rot == 2) pushX -= 1f;
                else if (rot == 3) pushY += 1f;
            }
        }

        if (pushX == 0f && pushY == 0f) return;

        // Normalize so multiple overlaps don't increase speed
        float len = (float)Math.sqrt(pushX * pushX + pushY * pushY);
        float dirX = pushX / len;
        float dirY = pushY / len;

        // Apply push
        x += dirX * pushSpeed * dt;
        y += dirY * pushSpeed * dt;

        // Clamp inside world
        float maxPX = world.WORLD_W - tex.getWidth();
        float maxPY = world.WORLD_H - tex.getHeight();
        x = clamp(x, 0f, maxPX);
        y = clamp(y, 0f, maxPY);

        // ===== Auto-centering (weighted by overlapped conveyor tiles) =====
        float feetCenterX = boxX() + boxW / 2f;
        float feetCenterY = boxY() + boxH / 2f;

        float sumWeights = 0f;
        float targetCenterX = 0f;
        float targetCenterY = 0f;

        for (int cx = minX; cx <= maxX; cx++) {
            for (int cy = minY; cy <= maxY; cy++) {
                if (cx < 0 || cy < 0 || cx >= world.wCells || cy >= world.hCells) continue;
                int id = world.grid[cx][cy];
                if (id != WorldGrid.TILE_CONVEYOR && id != WorldGrid.TILE_SMELTER) continue;

                // Weight by overlap area between feet box and the tile
                float tx = cx * WorldGrid.CELL;
                float ty = cy * WorldGrid.CELL;

                float ox = Math.max(0f, Math.min(px + pw, tx + WorldGrid.CELL) - Math.max(px, tx));
                float oy = Math.max(0f, Math.min(py + ph, ty + WorldGrid.CELL) - Math.max(py, ty));
                float w = ox * oy;

                if (w <= 0f) continue;

                sumWeights += w;
                targetCenterX += (tx + WorldGrid.CELL / 2f) * w;
                targetCenterY += (ty + WorldGrid.CELL / 2f) * w;
            }
        }

        if (sumWeights > 0f) {
            targetCenterX /= sumWeights;
            targetCenterY /= sumWeights;

            float alpha = 1f - (float)Math.exp( -centerStrength * dt);

            // If pushed horizontally, center Y toward blended lane center; if pushed vertically, center X
            if (Math.abs(dirX) >= Math.abs(dirY)) {
                float delta = (targetCenterY - feetCenterY);
                y += delta * alpha;

                if (Math.abs(delta) < snapEpsilon) {
                    y += (targetCenterY - (boxY() + boxH / 2f));
                }
            } else {
                float delta = (targetCenterX - feetCenterX);
                x += delta * alpha;

                if (Math.abs(delta) < snapEpsilon) {
                    x += (targetCenterX - (boxX() + boxW / 2f));
                }
            }

            // Clamp after centering adjustment
            x = clamp(x, 0f, maxPX);
            y = clamp(y, 0f, maxPY);
        }
    }

    public boolean blocksCell(WorldGrid world, int cx, int cy) {
        float px = boxX(), py = boxY(), pw = boxW, ph = boxH;
        float tx = cx * WorldGrid.CELL, ty = cy * WorldGrid.CELL, tw = WorldGrid.CELL, th = WorldGrid.CELL;
        return px < tx + tw && px + pw > tx && py < ty + th && py + ph > ty;
    }

    private void clampToWorld(WorldGrid world) {
        float maxX = world.WORLD_W - tex.getWidth();
        float maxY = world.WORLD_H - tex.getHeight();
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > maxX) x = maxX;
        if (y > maxY) y = maxY;
    }

    public void drawPlayerPlaceBoxDebug(ShapeRenderer shapes, OrthographicCamera camera) {
        float px = boxX();
        float py = boxY();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.setColor(1f, 0f, 0f, 0.35f);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.rect(px, py, boxW, boxH);
        shapes.end();
        shapes.setColor(1f, 1f, 1f, 1f);
    }

}
