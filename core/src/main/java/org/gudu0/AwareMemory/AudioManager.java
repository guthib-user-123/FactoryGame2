package org.gudu0.AwareMemory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.ObjectMap;

@SuppressWarnings("unused")
public final class AudioManager {
    private float sfxVolume = 1.0f;   // 0..1
    private boolean muted = false;

    // simple cooldowns to prevent spam
    private final ObjectMap<String, Long> lastPlayMs = new ObjectMap<>();

    private Sound place;
    private Sound delete;
    private Sound sell;
    private Sound process;
    private Sound click;
    private Sound claim;

    public void load() {
        // Use MP3/WAV for WebGL friendliness; OGG may fail on Safari/GWT.
        // Keep these short because Sound is fully loaded into RAM.

        place   = loadSoundOptional("sfx/place.mp3")    ;
        delete  = loadSoundOptional("sfx/delete.mp3");
        sell    = loadSoundOptional("sfx/sell.mp3");
        process = loadSoundOptional("sfx/process.mp3");
        click   = loadSoundOptional("sfx/click.mp3");
        claim   = loadSoundOptional("sfx/claim.mp3");
    }

    public void dispose() {
        if (place != null) place.dispose();
        if (delete != null) delete.dispose();
        if (sell != null) sell.dispose();
        if (process != null) process.dispose();
        if (click != null) click.dispose();
        if (claim != null) claim.dispose();
    }

    public boolean getMute() {return muted;};
    public void setMuted(boolean muted) { this.muted = muted; }
    public void setSfxVolume(float v) { this.sfxVolume = clamp01(v); }

    public void playPlace()   { play(place,   "place",   80); }   // ms cooldown
    public void playDelete()  { play(delete,  "delete",  60); }
    public void playSell()    { play(sell,    "sell",    30); }
    public void playProcess() { play(process, "process", 20); }
    public void playClick()   { play(click,   "click",   20); }
    public void playClaim()   { play(claim,   "claim",   20); }


    private void play(Sound s, String key, int cooldownMs) {
        if (muted || s == null || sfxVolume <= 0f) return;

        long now = System.currentTimeMillis();
        Long last = lastPlayMs.get(key);
        if (last != null && now - last < cooldownMs) return;

        lastPlayMs.put(key, now);
        s.play(sfxVolume);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (Math.min(v, 1f));
    }

    private Sound loadSoundOptional(String path) {
        try {
            FileHandle fh = Gdx.files.internal(path);

            // This check is nice, but still keep try/catch because some backends can throw anyway.
            if (!fh.exists()) {
                Gdx.app.log("Audio", "Missing sound: " + path);
                return null;
            }

            return Gdx.audio.newSound(fh);
        } catch (Exception e) {
            Gdx.app.log("Audio", "Failed to load sound: " + path + " (" + e.getClass().getSimpleName() + ")");
            return null;
        }
    }

}
