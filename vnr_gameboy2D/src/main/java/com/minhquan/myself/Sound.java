package com.minhquan.myself;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.net.URL;

public class Sound {
    Clip clip;
    URL[] soundURL = new URL[30];
    FloatControl volumeControl;

    public Sound() {
        // Khai báo sẵn các đường dẫn file nhạc vào mảng
        // Index 0: Nhạc nền chính
        soundURL[0] = getClass().getResource("/sounds/theme.wav");

        // Sau này bạn có thể thêm hiệu ứng ở đây
        // soundURL[1] = getClass().getResource("/sounds/hit.wav");
        // soundURL[2] = getClass().getResource("/sounds/levelup.wav");
    }

    // Hàm nạp file nhạc vào bộ nhớ (chuẩn bị phát)
    public void setFile(int i) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundURL[i]);
            clip = AudioSystem.getClip();
            clip.open(ais);
            volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        } catch (Exception e) {
            System.out.println("❌ Lỗi: Không tìm thấy file âm thanh số " + i);
            e.printStackTrace();
        }
    }

    // Phát nhạc 1 lần (Dùng cho tiếng bước chân, tiếng chém, hiệu ứng)
    public void play() {
        if (clip != null) {
            clip.start();
        }
    }

    // Phát nhạc lặp đi lặp lại vô tận (Dùng cho Nhạc nền - Background Music)
    public void loop() {
        if (clip != null) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    // Dừng nhạc (Dùng khi chuyển map hoặc tắt game)
    public void stop() {
        if (clip != null) {
            clip.stop();
        }
    }

    public void setVolume(float volume) {
        if (volumeControl != null) {
            // Đặt giới hạn an toàn để không bị văng lỗi nếu nhập số quá nhỏ/quá lớn
            if (volume < -80.0f) volume = -80.0f;
            if (volume > 6.0f) volume = 6.0f;

            volumeControl.setValue(volume);
        }
    }

}
