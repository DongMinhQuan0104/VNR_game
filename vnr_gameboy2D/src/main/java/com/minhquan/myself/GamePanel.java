package com.minhquan.myself;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;

public class GamePanel extends JPanel implements Runnable {

    // 1. CẤU HÌNH KÍCH THƯỚC MÀN HÌNH
    final int originalTileSize = 16;
    final int scale = 2;
    public final int tileSize = originalTileSize * scale;
    final int maxScreenCol = 20;
    final int maxScreenRow = 20;
    final int screenWidth = tileSize * maxScreenCol;
    final int screenHeight = tileSize * maxScreenRow;

    public int[][] collisionMap = new int[maxScreenCol][maxScreenRow];

    // 2. CẤU HÌNH FPS VÀ HÌNH ẢNH
    KeyHandler keyHandler = new KeyHandler();
    int FPS = 60;
    Thread gameThread;
    BufferedImage mapImage;
    Player player;

    Sound theme = new Sound();

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyHandler);
        this.setFocusable(true);
        this.requestFocus();
        loadMap();
        loadCollisionMap();
        player = new Player(this, keyHandler);
        setRandomSpawnPoint();
    }

    public void playMusic(int i) {
        theme.setFile(i);
        theme.play();
        theme.loop(); // Nhạc nền thì phải lặp vô tận
        theme.setVolume(-20.0f);
    }

    public void loadMap() {
        try {
            // Chắc chắn bạn đã có file map.png trong resources/maps/
            var is = getClass().getResourceAsStream("/maps/route-101-map.png");
            if (is != null) {
                mapImage = ImageIO.read(is);
                System.out.println("✅ Map load thành công!");
                System.out.println("Kích thước gốc của ảnh map: W=" + mapImage.getWidth() + ", H=" + mapImage.getHeight());
            } else {
                System.out.println("❌ Lỗi: Không tìm thấy file map tại /maps/map.png");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
        playMusic(0);
    }

    @Override
    public void run() {
        double drawInterval = 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    public void update() {
        player.update();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // 1. VẼ MAP NỀN TRƯỚC (SỬA LẠI CHIẾN LƯỢC VẼ)
        if (mapImage != null) {
            // SỬA Ở ĐÂY: Không ép stretching phủ kín window.
            // Chúng ta chỉ vẽ nó ở đúng kích thước gốc đã nhân scale (width*scale, height*scale).
            // Điều này đảm bảo ảnh luôn hiển thị ở đúng tỷ lệ và không bị biến mất.

            int nativeScaledWidth = mapImage.getWidth() * scale;
            int nativeScaledHeight = mapImage.getHeight() * scale;

            // Vẽ tấm map ở góc (0,0) với kích thước gốc phóng to
            g2.drawImage(mapImage, 0, 0, nativeScaledWidth, nativeScaledHeight, null);

            // Debug: In ra console kích thước map thực tế đang được vẽ
            // System.out.println("Vẽ map tại 0,0 với kích thước: W=" + nativeScaledWidth + ", H=" + nativeScaledHeight);
        }

        // 2. VẼ PLAYER SAU (GỌI TRỰC TIẾP TỪ CLASS PLAYER)
        if (player != null) {
            player.draw(g2);
        }

        g2.dispose();
    }

    public void loadCollisionMap() {
        try {
            // Trỏ đường dẫn tới file CSV bạn vừa xuất ra (đảm bảo file nằm trong thư mục resources/maps/)
            InputStream is = getClass().getResourceAsStream("/maps/collision.csv");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            int row = 0;
            String line;

            // Đọc từng dòng cho đến khi hết file hoặc đủ số hàng
            while ((line = br.readLine()) != null && row < maxScreenRow) {

                // Tách dòng văn bản thành các con số, dựa vào dấu phẩy
                String[] numbers = line.split(",");

                for (int col = 0; col < maxScreenCol && col < numbers.length; col++) {
                    // Ép kiểu chữ thành số nguyên và nhét vào ma trận
                    int num = Integer.parseInt(numbers[col].trim());
                    collisionMap[col][row] = num;
                }
                row++;
            }
            br.close();
            System.out.println("✅ Load ma trận va chạm (CSV) thành công!");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Lỗi: Không đọc được file /maps/collision.csv");
        }
    }

    public void setRandomSpawnPoint() {
        Random random = new Random();
        boolean isSpawned = false; // Công tắc kiểm tra xem đã tìm được chỗ chưa

        // Cứ lặp đi lặp lại việc bốc thăm nếu chưa tìm được chỗ
        while (isSpawned == false) {

            // Random số Cột (từ 0 đến 19) và số Hàng (từ 0 đến 19)
            int randomCol = random.nextInt(maxScreenCol);
            int randomRow = random.nextInt(maxScreenRow);

            // Rà soát ma trận: Ô vừa bốc được có phải là đất trống không?
            if (collisionMap[randomCol][randomRow] == -1) {

                // Nếu đúng đất trống, gán tọa độ cho Player
                player.x = randomCol * tileSize;
                player.y = randomRow * tileSize;

                // Bật công tắc để thoát khỏi vòng lặp while
                isSpawned = true;

                System.out.println("✅ Random Respawn thành công tại Cột " + randomCol + ", Hàng " + randomRow);
            }
        }
    }
}
