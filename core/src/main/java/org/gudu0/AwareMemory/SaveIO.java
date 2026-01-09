package org.gudu0.AwareMemory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Base64Coder;

import java.util.ArrayList;

/**
 * Binary (packed) save/load that works on Desktop + WebGL.
 * <p>
 * Stored as a Base64 string:
 * - WebGL: Preferences "saves"
 * - Desktop: local file (Gdx.files.local)
 * <p>
 * Format v1 (magic + version):
 *   u32 MAGIC = 'FGS1'
 *   u8  VERSION = 1
 *   varint w, varint h
 *   RLE-packed grid:
 *     packed = (tileId << 2) | rot (rot 0..3)
 *     repeat until w*h cells decoded:
 *       varint runLen
 *       varint packedValue
 *   varint nextItemId
 *   varint tileSaveCount
 *     for each:
 *       varint cx, varint cy
 *       float f0, float f1
 *       svarint i0, svarint i1, svarint i2
 *       u8 b0 (0/1)
 *   varint itemCount
 *     for each:
 *       varint id
 *       u8 typeId
 *       float value
 *       varint cx, varint cy
 *       u8 u, u8 v
 */
public final class SaveIO {
    private SaveIO() {}

    private static final int MAGIC = 0x46475331; // 'F' 'G' 'S' '1'
    private static final int VERSION = 2;

    public static final class LoadedData {
        public int w, h;

        // These are already the final arrays you want to stash into WorldGrid
        public int[] tiles; // length w*h
        public int[] rots;  // length w*h

        public WorldGrid.TileSave[] tileSaves;
        public WorldGrid.ItemSave[] items;
        public int nextItemId;
    }

    // ---------------- Public API ----------------

    public static void save(String name, WorldGrid world, TileWorld tileWorld) {
        // Export runtime state
        int nextItemId = tileWorld.exportNextItemId();
        WorldGrid.TileSave[] tileSaves = tileWorld.exportTileSaves();
        ArrayList<WorldGrid.ItemSave> itemList = tileWorld.exportItemSaves();
        WorldGrid.ItemSave[] items = itemList.toArray(new WorldGrid.ItemSave[0]);

        // Encode to bytes -> Base64
        byte[] bytes = encode(world, nextItemId, tileSaves, items);
        String b64 = new String(Base64Coder.encode(bytes));

        // Persist
        if (isWeb()) {
            Preferences prefs = Gdx.app.getPreferences("saves");
            prefs.putString(name, b64);
            prefs.flush();
        } else {
            FileHandle fh = Gdx.files.local(name);
            fh.writeString(b64, false);
        }
    }

    /** Returns null if missing/unreadable. */
    public static LoadedData load(String name) {
        String b64;

        if (isWeb()) {
            Preferences prefs = Gdx.app.getPreferences("saves");
            if (!prefs.contains(name)) return null;
            b64 = prefs.getString(name, "");
        } else {
            FileHandle fh = Gdx.files.local(name);
            if (!fh.exists()) return null;
            b64 = fh.readString();
        }

        if (b64 == null || b64.isEmpty()) return null;

        // If you still have old JSON saves lying around, detect and fail cleanly.
        if (b64.charAt(0) == '{') {
            // Old format: JSON. You said breaking saves is OK, so we just refuse.
            return null;
        }

        try {
            byte[] bytes = Base64Coder.decode(b64);
            return decode(bytes);
        } catch (Throwable t) {
            Gdx.app.error("SaveIO", "Failed to load/decode save: " + name, t);
            System.out.println("SaveIO: " + "Failed to load/decode save" + name + t);
            return null;
        }

    }

    // ---------------- Encoding ----------------

    private static byte[] encode(WorldGrid world, int nextItemId,
                                 WorldGrid.TileSave[] tileSaves,
                                 WorldGrid.ItemSave[] items) {
        Writer w = new Writer(32_768);

        w.writeInt(MAGIC);
        w.writeByte(VERSION);

        w.writeVarInt(world.wCells);
        w.writeVarInt(world.hCells);

        // RLE of packed cells, scanning same order you already use (y outer, x inner)
        final int W = world.wCells;
        final int H = world.hCells;
        final int total = W * H;

        int lastPacked = -1;
        int runLen = 0;

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int tile = world.grid[x][y];
                int rot  = world.rot[x][y] & 3;
                int packed = (tile << 2) | rot;

                if (packed == lastPacked && runLen < Integer.MAX_VALUE) {
                    runLen++;
                } else {
                    if (runLen > 0) {
                        w.writeVarInt(runLen);
                        w.writeVarInt(lastPacked);
                    }
                    lastPacked = packed;
                    runLen = 1;
                }
            }
        }
        // flush last run
        if (runLen > 0) {
            w.writeVarInt(runLen);
            w.writeVarInt(lastPacked);
        }

        // Next item id
        w.writeVarInt(nextItemId);

        // Tile saves
        if (tileSaves == null) tileSaves = new WorldGrid.TileSave[0];
        w.writeVarInt(tileSaves.length);
        for (WorldGrid.TileSave ts : tileSaves) {
            w.writeVarInt(ts.cx);
            w.writeVarInt(ts.cy);

            w.writeFloat(ts.f0);
            w.writeFloat(ts.f1);

            w.writeSVarInt(ts.i0);
            w.writeSVarInt(ts.i1);
            w.writeSVarInt(ts.i2);

            w.writeByte(ts.b0 ? 1 : 0);
        }

        // Items
        if (items == null) items = new WorldGrid.ItemSave[0];
        w.writeVarInt(items.length);
        for (WorldGrid.ItemSave it : items) {
            w.writeVarInt(it.id);
            w.writeByte(it.typeId & 0xFF);
            w.writeFloat(it.value);

            w.writeVarInt(it.cx);
            w.writeVarInt(it.cy);
            w.writeByte(it.u & 0xFF);
            w.writeByte(it.v & 0xFF);
        }

        return w.toByteArray();
    }

    // ---------------- Decoding ----------------

    private static LoadedData decode(byte[] bytes) {
        Reader r = new Reader(bytes);

        int magic = r.readInt();
        int ver = r.readUByte();

        if (magic != MAGIC) throw new RuntimeException("Bad save magic");
        if (ver != VERSION) throw new RuntimeException("Unsupported save version: " + ver);

        LoadedData out = new LoadedData();
        out.w = r.readVarInt();
        out.h = r.readVarInt();

        int total = out.w * out.h;
        out.tiles = new int[total];
        out.rots  = new int[total];

        // Decode RLE packed cells
        int i = 0;
        while (i < total) {
            int runLen = r.readVarInt();
            int packed = r.readVarInt();

            int tile = packed >>> 2;
            int rot  = packed & 3;

            for (int k = 0; k < runLen; k++) {
                if (i >= total) throw new RuntimeException("RLE overflow");
                out.tiles[i] = tile;
                out.rots[i]  = rot;
                i++;
            }
        }

        out.nextItemId = r.readVarInt();

        // Tile saves
        int tileSaveCount = r.readVarInt();
        out.tileSaves = new WorldGrid.TileSave[tileSaveCount];
        for (int t = 0; t < tileSaveCount; t++) {
            WorldGrid.TileSave ts = new WorldGrid.TileSave();
            ts.cx = r.readVarInt();
            ts.cy = r.readVarInt();

            ts.f0 = r.readFloat();
            ts.f1 = r.readFloat();

            ts.i0 = r.readSVarInt();
            ts.i1 = r.readSVarInt();
            ts.i2 = r.readSVarInt();

            ts.b0 = (r.readUByte() != 0);
            out.tileSaves[t] = ts;
        }

        // Items
        int itemCount = r.readVarInt();
        out.items = new WorldGrid.ItemSave[itemCount];
        for (int t = 0; t < itemCount; t++) {
            WorldGrid.ItemSave it = new WorldGrid.ItemSave();
            it.id = r.readVarInt();
            it.typeId = (byte) r.readUByte();
            it.value = r.readFloat();

            it.cx = r.readVarInt();
            it.cy = r.readVarInt();
            it.u  = r.readUByte();
            it.v  = r.readUByte();
            out.items[t] = it;
        }

        return out;
    }

    // ---------------- Helpers ----------------

    private static boolean isWeb() {
        return Gdx.app != null && Gdx.app.getType() == ApplicationType.WebGL;
    }

    private static final class Writer {
        private byte[] buf;
        private int pos;

        Writer(int initialCap) {
            buf = new byte[Math.max(256, initialCap)];
            pos = 0;
        }

        byte[] toByteArray() {
            byte[] out = new byte[pos];
            System.arraycopy(buf, 0, out, 0, pos);
            return out;
        }

        private void ensure(int add) {
            int need = pos + add;
            if (need <= buf.length) return;
            int n = buf.length;
            while (n < need) n *= 2;
            byte[] nb = new byte[n];
            System.arraycopy(buf, 0, nb, 0, pos);
            buf = nb;
        }

        void writeByte(int v) {
            ensure(1);
            buf[pos++] = (byte) v;
        }

        void writeInt(int v) { // little-endian
            ensure(4);
            buf[pos++] = (byte) (v);
            buf[pos++] = (byte) (v >>> 8);
            buf[pos++] = (byte) (v >>> 16);
            buf[pos++] = (byte) (v >>> 24);
        }

        void writeFloat(float f) {
            writeInt(Float.floatToIntBits(f));
        }

        void writeVarInt(int v) {
            // unsigned LEB128 (expects v >= 0)
            while ((v & ~0x7F) != 0) {
                writeByte((v & 0x7F) | 0x80);
                v >>>= 7;
            }
            writeByte(v);
        }

        void writeSVarInt(int v) {
            // ZigZag + varint (handles negatives if you ever use them)
            int zz = (v << 1) ^ (v >> 31);
            writeVarInt(zz);
        }
    }

    private static final class Reader {
        private final byte[] buf;
        private int pos;

        Reader(byte[] buf) {
            this.buf = buf;
            this.pos = 0;
        }

        int readUByte() {
            if (pos >= buf.length) throw new RuntimeException("EOF");
            return buf[pos++] & 0xFF;
        }

        int readInt() { // little-endian
            int b0 = readUByte();
            int b1 = readUByte();
            int b2 = readUByte();
            int b3 = readUByte();
            return (b0) | (b1 << 8) | (b2 << 16) | (b3 << 24);
        }

        float readFloat() {
            return Float.intBitsToFloat(readInt());
        }

        int readVarInt() {
            int result = 0;
            int shift = 0;
            while (true) {
                int b = readUByte();
                result |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) break;
                shift += 7;
                if (shift > 35) throw new RuntimeException("VarInt too long");
            }
            return result;
        }

        int readSVarInt() {
            int zz = readVarInt();
            return (zz >>> 1) ^ -(zz & 1);
        }
    }
}
