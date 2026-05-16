package com.graphics;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class SoundPlayer {
    private String resourcePath;

    public SoundPlayer(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public void play() {
        new Thread(() -> {
            try (InputStream in = SoundPlayer.class.getResourceAsStream(resourcePath)) {
                if (in == null)
                    return; // Silencioso si no encuentra el archivo
                BufferedInputStream bin = new BufferedInputStream(in);
                AudioInputStream ais = AudioSystem.getAudioInputStream(bin);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.start();
                Thread.sleep(clip.getMicrosecondLength() / 1000);
                clip.close();
            } catch (Exception e) {
                // Ignore audio errors so game won't crash if files are missing
            }
        }).start();
    }
}
