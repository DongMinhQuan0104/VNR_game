package com.minhquan.myself.Entity;

import com.minhquan.myself.Main.GamePanel;
import com.minhquan.myself.Main.KeyHandler;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
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

    public boolean inGrass = false;
    public BufferedImage grassOverlayImage;
    public int grassCol = 0;
    public int grassRow = 0;

    public int lastCol = -1;
    public int lastRow = -1;

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

        // ==========================================
        // BƯỚC 1: KIỂM TRA XEM CÓ BẤM PHÍM KHÔNG VÀ LẤY HƯỚNG
        // ==========================================
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
            // BƯỚC 2: TÍNH TOÁN VÀ KIỂM TRA VA CHẠM (HITBOX)
            // ==========================================
            boolean collisionOn = false;

            int nextX = x;
            int nextY = y;
            switch (direction) {
                case "up":    nextY -= speed; break;
                case "down":  nextY += speed; break;
                case "left":  nextX -= speed; break;
                case "right": nextX += speed; break;
            }

            int solidLeft = nextX + 12;
            int solidRight = nextX + 36;
            int solidTop = nextY + 24;
            int solidBottom = nextY + 46;

            if (solidLeft < 0 || solidRight > gp.screenWidth ||
                    solidTop < 0 || solidBottom > gp.screenHeight) {
                collisionOn = true;
            } else {
                int leftCol = solidLeft / gp.tileSize;
                int rightCol = solidRight / gp.tileSize;
                int topRow = solidTop / gp.tileSize;
                int bottomRow = solidBottom / gp.tileSize;

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

            // ==========================================
            // BƯỚC 3: DI CHUYỂN NẾU MỌI THỨ AN TOÀN
            // ==========================================
            if (collisionOn == false) {
                x = nextX;
                y = nextY;
                isMoving = true; // Đánh dấu là nhân vật có thực sự nhúc nhích
            }
        } // <-- CHÚ Ý: Đóng ngoặc của if(bấm phím) ở đây

        // ====================================================
        // BƯỚC 4: LOGIC TÍNH TRỌNG TÂM, BỤI CỎ VÀ RANDOM ENCOUNTER
        // ====================================================

        // 1. Tính TÂM của nhân vật (Trục X) và GÓT CHÂN (Trục Y)
        int centerX = x + (gp.tileSize / 2);
        int bottomY = y + (int) (gp.tileSize * 0.9);

        // 2. Quy đổi tọa độ Pixel ra tọa độ Cột/Hàng của map lưới
        int currentCol = centerX / gp.tileSize;
        int currentRow = bottomY / gp.tileSize;

        // 3. Kiểm tra an toàn: Không soi ra ngoài lề bản đồ
        if (currentCol >= 0 && currentCol < gp.maxScreenCol && currentRow >= 0 && currentRow < gp.maxScreenRow) {

            // 4. Soi vào ma trận cỏ (0 là cỏ, -1 là đất)
            if (gp.grassMap[currentCol][currentRow] == 0) {
                inGrass = true;
                grassCol = currentCol;
                grassRow = currentRow;

                // RANDOM ENCOUNTER: CHỈ TÍNH KHI BƯỚC SANG Ô MỚI
                if (currentCol != lastCol || currentRow != lastRow) {
                    checkWildEncounter();
                }

            } else {
                inGrass = false;
            }

            // CHỐT LẠI: LƯU TỌA ĐỘ HIỆN TẠI THÀNH "VẾT CHÂN CŨ" CHO FRAME TIẾP THEO
            lastCol = currentCol;
            lastRow = currentRow;
        }

        // ==========================================
        // BƯỚC 5: LOGIC CHUYỂN DÁNG (ANIMATION)
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
            spriteNum = 0; // Đứng im thì trả về dáng số 0
        }
    }

        public void draw (Graphics2D g2){
            if (sprites != null) {
                int row = 0;
                switch (direction) {
                    case "down":
                        row = 0;
                        break;
                    case "left":
                        row = 1;
                        break;
                    case "right":
                        row = 2;
                        break;
                    case "up":
                        row = 3;
                        break;
                }

                BufferedImage image = sprites[row][spriteNum];

                // Nhân vật của bạn cao 48px (1.5 * 32)
                int drawWidth = (int) (gp.tileSize * 1.5);
                int drawHeight = (int) (gp.tileSize * 1.5);

                // 1. VẼ NHÂN VẬT TRƯỚC
                g2.drawImage(image, x, y, drawWidth, drawHeight, null);

                // ====================================================
                // 2. VẼ ĐÈ BỤI CỎ (CHỈ CHE PHẦN CHÂN)
                // ====================================================
                if (inGrass == true && grassOverlayImage != null) {

                    // Tọa độ X của ô cỏ chốt theo lưới
                    int overlayX = grassCol * gp.tileSize;

                    // Tọa độ Y: Ta phải đẩy bụi cỏ xuống dưới gót chân
                    // Thông thường, ta sẽ vẽ nó khớp với cạnh dưới của ô lưới
                    int overlayY = grassRow * gp.tileSize;

                    // MẸO: Vì nhân vật cao 48px, ta có thể hạ bụi cỏ xuống
                    // để nó chỉ che khoảng 1/3 thân dưới của Brendan
                    // Bạn có thể cộng thêm 4-8 pixel nếu muốn che sâu hơn
                    int adjustmentY = 4;

                    g2.drawImage(grassOverlayImage, overlayX, overlayY + adjustmentY, gp.tileSize, gp.tileSize, null);
                }
            }
        }

        // ====================================================
        // HÀM XỬ LÝ TỈ LỆ GẶP POKEMON
        // ====================================================
        public void checkWildEncounter() {
            java.util.Random random = new java.util.Random();
            int chance = random.nextInt(100);

            if (chance < 10) {
                System.out.println("⚔️ [BỤP!!!] Chạm trán Pokémon hoang dã!");

                // SỬA Ở ĐÂY: Đổi trạng thái game sang Chuyển cảnh
                // (Khi trạng thái này bật, hàm update() sẽ không cho nhân vật đi nữa)
                gp.gameState = gp.transitionState;
                gp.startTransition();
            } else {
                System.out.println("🌿 Xào xạc... (Không có gì)");
            }
        }
}
