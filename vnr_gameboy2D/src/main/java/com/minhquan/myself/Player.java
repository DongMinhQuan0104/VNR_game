package com.minhquan.myself;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Player {
    GamePanel gp;
    KeyHandler keyHandler;

    public int x,y;
    public int speed;

    BufferedImage playerSheet;
    BufferedImage[][] sprites;

    String direction = "down"; // Hướng mặc định ban đầu
    int spriteCounter = 0; // Đếm số frame để chuyển dáng
    int spriteNum = 0; // Chỉ số dáng (0, 1, 2, 3)

    public Player(GamePanel gp, KeyHandler keyH) {
        this.gp = gp;
        this.keyHandler = keyH;
        this.speed = 2;
        this.direction = "down";

        loadPlayerImages();
    }

    public void loadPlayerImages() {
        try {
            // Đọc file ảnh lớn từ thư mục resources/player/
            playerSheet = ImageIO.read(getClass().getResourceAsStream("/players/player-male.png"));

            // Số hàng và số cột của Sprite Sheet
            int rows = 4;
            int cols = 4;

            // Tính toán kích thước của 1 ô nhỏ (trong file ảnh gốc)
            // Cảnh báo: Bạn phải biết kích thước file gốc. Giả sử file gốc là 128x128.
            // Thì 1 ô là 128 / 4 = 32 pixel.
            // Nếu bạn không biết, bạn phải tự mở ảnh xem width/height của file gốc.
            // Ở đây tôi tự tính dựa trên file bạn gửi: assume width=128, height=128
            int originalSpriteWidth = playerSheet.getWidth() / cols; // ví dụ: 32
            int originalSpriteHeight = playerSheet.getHeight() / rows; // ví dụ: 32

            sprites = new BufferedImage[rows][cols];

            // Vòng lặp cắt ảnh
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    // Cắt ô ảnh tại tọa độ (x_gốc, y_gốc, width_gốc, height_gốc)
                    sprites[i][j] = playerSheet.getSubimage(
                            j * originalSpriteWidth,
                            i * originalSpriteHeight,
                            originalSpriteWidth,
                            originalSpriteHeight
                    );
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi: Không tìm thấy file resources/player/brendan.png");
        }
    }

    public void update() {
        boolean isMoving = false;

        // BƯỚC 1: KIỂM TRA XEM CÓ BẤM PHÍM KHÔNG VÀ LẤY HƯỚNG
        if (keyHandler.upPressed || keyHandler.downPressed || keyHandler.leftPressed || keyHandler.rightPressed) {

            if (keyHandler.upPressed) {
                direction = "up";
            } else if (keyHandler.downPressed) {
                direction = "down";
            } else if (keyHandler.leftPressed) {
                direction = "left";
            } else if (keyHandler.rightPressed) {
                direction = "right";
            }

            // ==========================================
            // BƯỚC 2: TÍNH TOÁN VÀ KIỂM TRA VA CHẠM (HITBOX CẢI TIẾN)
            // ==========================================
            boolean collisionOn = false;

            // Tính trước tọa độ X, Y nếu như nhân vật thực sự bước đi
            int nextX = x;
            int nextY = y;
            switch (direction) {
                case "up":    nextY -= speed; break;
                case "down":  nextY += speed; break;
                case "left":  nextX -= speed; break;
                case "right": nextX += speed; break;
            }

            // 1. TẠO HỘP VA CHẠM (HITBOX) CHO NHÂN VẬT
            // Nhân vật vẽ ra là 48x48. Ta gọt bớt các phần không khí xung quanh,
            // chỉ lấy phần thân dưới (từ bụng xuống gót chân) để làm khối va chạm rắn.
            int solidLeft = nextX + 12;   // Thụt lề trái vào 12px
            int solidRight = nextX + 36;  // Thụt lề phải vào 12px
            int solidTop = nextY + 24;    // Chỉ lấy từ nửa thân dưới (bụng)
            int solidBottom = nextY + 46; // Trừ đi 2px ở gót chân để di chuyển mượt không bị kẹt

            // Kiểm tra lọt viền màn hình (Vòng an ninh 1)
            if (solidLeft < 0 || solidRight > gp.screenWidth ||
                    solidTop < 0 || solidBottom > gp.screenHeight) {
                collisionOn = true;
            }
            else {
                // 2. KIỂM TRA VẬT CẢN BẰNG 4 GÓC CỦA HITBOX (Vòng an ninh 2)
                // Đổi tọa độ Pixel của 4 cạnh hộp sang tọa độ Ô lưới (Cột, Hàng)
                int leftCol = solidLeft / gp.tileSize;
                int rightCol = solidRight / gp.tileSize;
                int topRow = solidTop / gp.tileSize;
                int bottomRow = solidBottom / gp.tileSize;

                // Tùy theo hướng đi, ta chỉ cần soi 2 góc ở phía trước mặt nhân vật
                switch (direction) {
                    case "up":
                        if (gp.collisionMap[leftCol][topRow] != -1 || gp.collisionMap[rightCol][topRow] != -1) {
                            collisionOn = true;
                        }
                        break;
                    case "down":
                        if (gp.collisionMap[leftCol][bottomRow] != -1 || gp.collisionMap[rightCol][bottomRow] != -1) {
                            collisionOn = true;
                        }
                        break;
                    case "left":
                        if (gp.collisionMap[leftCol][topRow] != -1 || gp.collisionMap[leftCol][bottomRow] != -1) {
                            collisionOn = true;
                        }
                        break;
                    case "right":
                        if (gp.collisionMap[rightCol][topRow] != -1 || gp.collisionMap[rightCol][bottomRow] != -1) {
                            collisionOn = true;
                        }
                        break;
                }
            }

            // BƯỚC 3: DI CHUYỂN NẾU MỌI THỨ AN TOÀN
            if (collisionOn == false) {
                x = nextX; // Cập nhật thẳng tọa độ mới đã tính toán
                y = nextY;
            }

            isMoving = true;
        }

        // ==========================================
        // LOGIC CHUYỂN DÁNG (ANIMATION) - GIỮ NGUYÊN
        // ==========================================
        if (isMoving) {
            spriteCounter++;
            if (spriteCounter > 10) {
                spriteNum++;
                if (spriteNum >= 4) {
                    spriteNum = 0;
                }
                spriteCounter = 0;
            }
        } else {
            spriteNum = 0;
        }
    }

    public void draw(Graphics2D g2) {
        BufferedImage image = null;

        // 1. Xác định hàng dựa trên hướng
        int row = 0;
        switch (direction) {
            case "down":  row = 0; break;
            case "left":    row = 1; break;
            case "right": row = 2; break;
            case "up":  row = 3; break;
        }

        // 2. Lấy khung hình trực tiếp từ mảng đã cắt sẵn (nhanh và tối ưu hơn rất nhiều)
        if (sprites != null) {
            image = sprites[row][spriteNum];
            int drawWidth = (int) (gp.tileSize * 1.5);  // To gấp rưỡi
            int drawHeight = (int) (gp.tileSize * 1.5); // To gấp rưỡi
            g2.drawImage(image, x, y, drawWidth, drawHeight, null);
        }
    }
}
