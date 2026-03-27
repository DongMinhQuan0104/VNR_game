package com.minhquan.myself.Main;

import com.minhquan.myself.Audio.Sound;

import com.minhquan.myself.Entity.Player;
import com.minhquan.myself.Quiz.Entity.Answer;
import com.minhquan.myself.Quiz.Entity.Question;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.minhquan.myself.Main.Main.quizService;

public class GamePanel extends JPanel implements Runnable {

    // 1. CẤU HÌNH KÍCH THƯỚC
    final int originalTileSize = 16;
    final int scale = 2;
    public final int tileSize = originalTileSize * scale;

    public int maxScreenCol;
    public int maxScreenRow;
    public int screenWidth;
    public int screenHeight;

    public int[][] collisionMap;
    public int[][] grassMap;

    // 2. CẤU HÌNH FPS VÀ HÌNH ẢNH
    KeyHandler keyHandler = new KeyHandler();
    int FPS = 60;
    Thread gameThread;
    BufferedImage mapImage;
    Player player;

    Sound theme = new Sound();

    public final int playState = 1;
    public final int transitionState = 2;
    public int gameState = playState;

    public BufferedImage blackImage;
    public int transitionCounter = 0;
    public int blinkCount = 0;
    public boolean drawBlack = false;

    public final int transitionTileSize = 32;
    public int[][] transitionGrid;
    public int tilesFilled = 0;
    public int maxTitles = 0;

    public int transitionPhase = 0;
    public int pauseCounter = 0;
    public int currentTransCol = 0;
    public int currentTransRow = 0;
    public boolean transitionFinished = false;

    final int originalCombatSpriteSize = 64;
    final int drawCombatSpriteSize = 200;

    public BufferedImage playerCombatSprite;
    public List<BufferedImage> trainerCombatSprites = new ArrayList<>();
    public BufferedImage currentEnemySprite;
    public BufferedImage combatBg1, combatBg2;

    public boolean inCombat = false;
    public Font customFont;
    public int commandNum = 0;
    public int menuCooldown = 0;

    // ==========================================
    // CÁC BIẾN MỚI CHO LOGIC TRƯỢT VÀ HIỆN MENU
    // ==========================================
    public boolean isSlidingIn = false; // Đang chạy hiệu ứng trượt nhân vật
    public boolean showMenu = false;    // Bật menu FIGHT/RUN
    public int playerSlideX, enemySlideX;
    public int playerTargetX, enemyTargetX;
    public final int slideSpeed = 15;   // Tốc độ lướt vào sân

    public boolean showQuiz = false; // Bật lên khi đổi sang combat-2.png
    public Question currentQuestion = null;
    public List<Answer> currentAnswers = null;

    public int quizCommandNum = 0; // 0:A, 1:B, 2:C, 3:D
    public boolean isAnswered = false; // Đã bấm Enter chọn chưa?
    public boolean onlyRunMode = false; // Chỉ hiện nút RUN sau khi trả lời xong

    public GamePanel() {
        loadMap("/maps/route-101-map.png");

        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyHandler);
        this.setFocusable(true);
        this.requestFocus();

        loadCollisionMap();
        loadGrassMap();
        loadCombatSprites();

        player = new Player(this, keyHandler);
        setRandomSpawnPoint();

        try {
            blackImage = ImageIO.read(getClass().getResourceAsStream("/maps/black.png"));
            InputStream is = getClass().getResourceAsStream("/fonts/SVN-Determination.otf");
            customFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(36f);
        } catch (IOException e) {
            System.out.println("❌ Không tìm thấy ảnh đen!");
        }catch (Exception e) {
            System.out.println("❌ Không tìm thấy ảnh đen hoặc lỗi load Font chữ!");
            customFont = new Font("Arial", Font.BOLD, 36);
        }
    }

    public void playMusic(int i) {
        theme.setFile(i);
        theme.play();
        theme.loop();
        theme.setVolume(-20.0f);
    }

    public void loadMap(String mapPath) {
        try {
            var is = getClass().getResourceAsStream(mapPath);
            if (is != null) {
                mapImage = ImageIO.read(is);
                screenWidth = mapImage.getWidth() * scale;
                screenHeight = mapImage.getHeight() * scale;

                maxScreenCol = (int) Math.ceil((double) screenWidth / tileSize);
                maxScreenRow = (int) Math.ceil((double) screenHeight / tileSize);

                collisionMap = new int[maxScreenCol][maxScreenRow];
                grassMap = new int[maxScreenCol][maxScreenRow];
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window != null) {
                    this.setPreferredSize(new Dimension(screenWidth, screenHeight));
                    window.pack();
                    window.setLocationRelativeTo(null);
                }
                System.out.println("✅ Map load thành công: " + mapPath);
            } else {
                System.out.println("❌ Lỗi: Không tìm thấy file map!");
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

    public void startTransition() {
        gameState = transitionState;
        transitionPhase = 0;
        transitionCounter = 0;
        blinkCount = 0;
        drawBlack = false;
        pauseCounter = 0;

        int cols = (int) Math.ceil((double) screenWidth / transitionTileSize);
        int rows = (int) Math.ceil((double) screenHeight / transitionTileSize);
        transitionGrid = new int[cols][rows];
        tilesFilled = 0;

        currentTransCol = 0;
        currentTransRow = 0;
        transitionFinished = false;
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
        if (gameState == playState) {
            if (!inCombat) {
                player.update();
            }
            else {
                // ====================================================
                // LOGIC TRƯỢT TRƯỚC, HIỆN MENU/QUIZ SAU
                // ====================================================
                if (isSlidingIn) {
                    // 1. Player trượt từ trái sang phải (+)
                    if (playerSlideX < playerTargetX) {
                        playerSlideX += slideSpeed;
                        if (playerSlideX > playerTargetX) playerSlideX = playerTargetX;
                    }

                    // 2. Enemy trượt từ phải sang trái (-)
                    if (enemySlideX > enemyTargetX) {
                        enemySlideX -= slideSpeed;
                        if (enemySlideX < enemyTargetX) enemySlideX = enemyTargetX;
                    }

                    // 3. Khi cả 2 đã đến đúng vị trí -> Dừng trượt, Mở Menu
                    if (playerSlideX == playerTargetX && enemySlideX == enemyTargetX) {
                        isSlidingIn = false;
                        showMenu = true;
                    }
                }
                else {
                    if (menuCooldown > 0) {
                        menuCooldown--;
                    }

                    // ----------------------------------------------------
                    // NHÁNH 1: LOGIC KHI ĐANG HIỆN BẢNG CÂU HỎI (QUIZ)
                    // ----------------------------------------------------
                    if (showQuiz) {
                        if (!isAnswered) { // Chưa chọn đáp án -> Lắng nghe phím điều hướng
                            if (keyHandler.upPressed && menuCooldown == 0) {
                                if (quizCommandNum >= 2) quizCommandNum -= 2;
                                menuCooldown = 15; keyHandler.upPressed = false;
                            }
                            if (keyHandler.downPressed && menuCooldown == 0) {
                                if (quizCommandNum <= 1) quizCommandNum += 2;
                                menuCooldown = 15; keyHandler.downPressed = false;
                            }
                            if (keyHandler.leftPressed && menuCooldown == 0) {
                                if (quizCommandNum % 2 != 0) quizCommandNum--;
                                menuCooldown = 15; keyHandler.leftPressed = false;
                            }
                            if (keyHandler.rightPressed && menuCooldown == 0) {
                                if (quizCommandNum % 2 == 0) quizCommandNum++;
                                menuCooldown = 15; keyHandler.rightPressed = false;
                            }

                            // Nhấn Enter để chốt đáp án
                            if (keyHandler.enterPressed && menuCooldown == 0) {
                                menuCooldown = 30;
                                isAnswered = true; // Khóa phím, hiện Xanh/Đỏ

                                if (currentAnswers.get(quizCommandNum).getIs_correct()) {
                                    System.out.println("✅ ĐÚNG!");
                                    com.minhquan.myself.Main.Main.addScoreAndSave(currentQuestion.getScore().intValue());
                                } else {
                                    System.out.println("❌ SAI!");
                                }
                                keyHandler.enterPressed = false;
                            }
                        }
                        else {
                            // Đã trả lời xong -> Nhấn Enter để quay về Menu combat-1
                            if (keyHandler.enterPressed && menuCooldown == 0) {
                                showQuiz = false;
                                showMenu = true;
                                onlyRunMode = true;
                                commandNum = 1;
                                isAnswered = false;
                                menuCooldown = 20;
                                keyHandler.enterPressed = false;

                                // ---- ĐÂY LÀ ĐOẠN ĐỔI MAP VỀ COMBAT 1 ----
                                if (combatBg1 != null) {
                                    this.mapImage = combatBg1;
                                }
                            }
                        }
                    }
                    // ----------------------------------------------------
                    // NHÁNH 2: LOGIC KHI ĐANG HIỆN MENU (FIGHT / RUN)
                    // ----------------------------------------------------
                    else if (showMenu) {
                        if (!onlyRunMode) { // Nếu chưa trả lời -> Cho chọn qua lại
                            if (keyHandler.leftPressed && menuCooldown == 0) {
                                commandNum = 0; menuCooldown = 15;
                            }
                            if (keyHandler.rightPressed && menuCooldown == 0) {
                                commandNum = 1; menuCooldown = 15;
                            }
                        } else { // Trả lời rồi -> Ép ở vị trí RUN
                            commandNum = 1;
                        }

                        if (keyHandler.enterPressed && menuCooldown == 0) {
                            menuCooldown = 20;

                            if (commandNum == 0 && !onlyRunMode){ // CHỌN FIGHT
                                System.out.println("TẤN CÔNG!");
                                showMenu = false;

                                // Đổi nền sang bảng Quiz (combat-2.png)
                                if (combatBg2 != null) {
                                    this.mapImage = combatBg2;
                                }

                                currentQuestion = quizService.getRandomQuestionForGame();
                                if (currentQuestion != null) {
                                    currentAnswers = currentQuestion.getAnswers();
                                    currentAnswers.sort(java.util.Comparator.comparing(
                                            a -> a.getOrder_index() != null ? a.getOrder_index() : 0
                                    ));
                                }

                                showQuiz = true;
                                quizCommandNum = 0;
                            }
                            else if (commandNum == 1){ // CHỌN RUN
                                System.out.println("BỎ CHẠY!");
                                inCombat = false;
                                showMenu = false;
                                onlyRunMode = false;

                                loadMap("/maps/route-101-map.png");
                                loadCollisionMap();
                                loadGrassMap();

                                keyHandler.enterPressed = false;
                                keyHandler.leftPressed = false;
                                keyHandler.rightPressed = false;
                                keyHandler.upPressed = false;
                                keyHandler.downPressed = false;
                            }
                        }
                    }
                }
            }
        }
        else if (gameState == transitionState) {
            // (PHASE 0, 1, 2 GIỮ NGUYÊN)
            if (transitionPhase == 0) {
                transitionCounter++;
                if (transitionCounter >= 10) { drawBlack = !drawBlack; transitionCounter = 0; blinkCount++; if (blinkCount >= 6) { drawBlack = false; transitionPhase = 1; } }
            }
            else if (transitionPhase == 1) {
                pauseCounter++;
                if (pauseCounter >= 30) { transitionPhase = 2; }
            }
            else if (transitionPhase == 2) {
                int cols = (int) Math.ceil((double) screenWidth / transitionTileSize);
                int rows = (int) Math.ceil((double) screenHeight / transitionTileSize);
                for (int i = 0; i < 3; i++) {
                    if (!transitionFinished) {
                        transitionGrid[currentTransCol][currentTransRow] = 1; currentTransCol++;
                        if (currentTransCol >= cols) { currentTransCol = 0; currentTransRow++; }
                        if (currentTransRow >= rows) {
                            loadMap(combatBg1);
                            inCombat = true; showMenu = false;
                            if (!trainerCombatSprites.isEmpty()) { currentEnemySprite = trainerCombatSprites.get(new Random().nextInt(trainerCombatSprites.size())); }
                            cols = (int) Math.ceil((double) screenWidth / transitionTileSize); rows = (int) Math.ceil((double) screenHeight / transitionTileSize);
                            transitionGrid = new int[cols][rows]; for(int c = 0; c < cols; c++){ for(int r = 0; r < rows; r++){ transitionGrid[c][r] = 1; } }
                            currentTransCol = cols - 1; currentTransRow = rows - 1; transitionPhase = 3; transitionFinished = false; break;
                        }
                    }
                }
            }
            // PHASE 3: QUÉT SÁNG - ĐÃ TRẢ LẠI TỌA ĐỘ GỐC CỘT 5 VÀ 17
            else if (transitionPhase == 3) {
                int cols = (int) Math.ceil((double) screenWidth / transitionTileSize);
                for (int i = 0; i < 3; i++) {
                    if (!transitionFinished) {
                        transitionGrid[currentTransCol][currentTransRow] = 0; currentTransCol--;
                        if (currentTransCol < 0) { currentTransCol = cols - 1; currentTransRow--; }
                        if (currentTransRow < 0) { transitionFinished = true; break; }
                    }
                }

                if (transitionFinished) {
                    System.out.println("🔥 HIỆU ỨNG QUÉT XONG -> BẮT ĐẦU TRƯỢT NHÂN VẬT!");
                    gameState = playState;
                    isSlidingIn = true;
                    showMenu = false;

                    // ---- ĐÃ TRẢ LẠI TỌA ĐỘ TRƯỢT GỐC ----
                    int playerCol = 5;
                    int enemyCol = 17;

                    playerTargetX = (playerCol * tileSize) + (tileSize / 2) - (drawCombatSpriteSize / 2);
                    enemyTargetX = (enemyCol * tileSize) + (tileSize / 2) - (drawCombatSpriteSize / 2);

                    playerSlideX = -drawCombatSpriteSize;
                    enemySlideX = screenWidth;
                }
            }
        }
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (mapImage != null) { g2.drawImage(mapImage, 0, 0, screenWidth, screenHeight, null); }
        if (player != null && !inCombat) { player.draw(g2); }

        if (gameState == transitionState) {
            if (transitionPhase == 0) {
                if (drawBlack == true && blackImage != null) { g2.drawImage(blackImage, 0, 0, screenWidth, screenHeight, null); }
            }
            else if ((transitionPhase == 2 || transitionPhase == 3) && transitionGrid != null) {
                g2.setColor(Color.BLACK);
                int cols = (int) Math.ceil((double) screenWidth / transitionTileSize);
                int rows = (int) Math.ceil((double) screenHeight / transitionTileSize);
                for (int c = 0; c < cols; c++) {
                    for (int r = 0; r < rows; r++) {
                        // Vẫn giữ vòng bảo vệ chống tràn mảng
                        if (c < transitionGrid.length && r < transitionGrid[0].length) {
                            if (transitionGrid[c][r] == 1) {
                                g2.fillRect(c * transitionTileSize, r * transitionTileSize, transitionTileSize, transitionTileSize);
                            }
                        }
                    }
                }
            }
        }

        if (inCombat && gameState == playState) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

            // ----------------------------------------------------
            // PHẦN A: VẼ 2 NHÂN VẬT - ĐÃ TRẢ LẠI TỌA ĐỘ GỐC HÀNG 8 VÀ 3
            // ----------------------------------------------------
            int playerRow = 8;
            int enemyRow = 3;

            int playerCenterY = (playerRow * tileSize) + (tileSize / 2);
            int enemyCenterY = (enemyRow * tileSize) + (tileSize / 2);

            int drawPlayerY = playerCenterY - (drawCombatSpriteSize / 2);
            int drawEnemyY = enemyCenterY - (drawCombatSpriteSize / 2);

            if (playerCombatSprite != null) {
                g2.drawImage(playerCombatSprite, playerSlideX, drawPlayerY, drawCombatSpriteSize, drawCombatSpriteSize, null);
            }
            if (currentEnemySprite != null) {
                g2.drawImage(currentEnemySprite, enemySlideX, drawEnemyY, drawCombatSpriteSize, drawCombatSpriteSize, null);
            }

            // ----------------------------------------------------
            // PHẦN B: MENU FIGHT/RUN
            // ----------------------------------------------------
            if (showMenu) {
                g2.setFont(customFont);
                int fightCol = 12; int fightRow = 12;
                int runCol = 20; int runRow = 14;
                int fightX = fightCol * tileSize; int fightY = (fightRow * tileSize) + tileSize - 6;
                int runX = runCol * tileSize; int runY = (runRow * tileSize) + tileSize - 6;

                if (!onlyRunMode) {
                    if (commandNum == 0) {
                        g2.setColor(Color.BLACK); g2.drawString("> FIGHT", fightX, fightY);
                    } else {
                        g2.setColor(Color.DARK_GRAY); g2.drawString("  FIGHT", fightX, fightY);
                    }
                }

                if (commandNum == 1) {
                    g2.setColor(Color.BLACK); g2.drawString("> RUN", runX, runY);
                } else {
                    g2.setColor(Color.DARK_GRAY); g2.drawString("  RUN", runX, runY);
                }
            }
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        // ----------------------------------------------------
        // PHẦN C: GIAO DIỆN QUIZ (Giữ nguyên kích thước nhỏ vừa vặn)
        // ----------------------------------------------------
        if (showQuiz && currentQuestion != null) {
            g2.setFont(customFont.deriveFont(Font.BOLD, 16f));
            g2.setColor(Color.BLACK);
            int questionX = tileSize;
            int questionY = tileSize * 1 + 20;
            drawWrappedText(g2, currentQuestion.getContent(), questionX, questionY, tileSize * 11, 25);

            g2.setFont(customFont.deriveFont(Font.PLAIN, 15f));
            if (currentAnswers != null && currentAnswers.size() >= 4) {
                int ansColLeft = 2; int ansColRight = 13;
                int ansRowTop = 13; int ansRowBot = 15;
                int leftX = ansColLeft * tileSize; int rightX = ansColRight * tileSize;
                int topY = (ansRowTop * tileSize) - 10; int botY = (ansRowBot * tileSize) - 10;

                int[] posX = {leftX, rightX, leftX, rightX};
                int[] posY = {topY, topY, botY, botY};

                for (int i = 0; i < 4; i++) {
                    String label = (char)('A' + i) + ". ";
                    if (isAnswered) {
                        if (currentAnswers.get(i).getIs_correct()) g2.setColor(new Color(0, 180, 0));
                        else if (i == quizCommandNum) g2.setColor(Color.RED);
                        else g2.setColor(Color.GRAY);
                    } else {
                        g2.setColor(Color.WHITE);
                    }
                    g2.drawString(label + currentAnswers.get(i).getContent(), posX[i], posY[i]);

                    if (i == quizCommandNum && !isAnswered) {
                        g2.setColor(Color.YELLOW);
                        g2.drawString(">", posX[i] - 20, posY[i]);
                    }
                }
            }
        }
        g2.dispose();
    }

    public void loadGrassMap() {
        // ... (Giữ nguyên cũ)
        try {
            InputStream is = getClass().getResourceAsStream("/maps/grass.csv");
            if (is == null) return;
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            int row = 0;
            String line;
            while ((line = br.readLine()) != null && row < maxScreenRow) {
                String[] numbers = line.split(",");
                for (int col = 0; col < maxScreenCol && col < numbers.length; col++) {
                    grassMap[col][row] = Integer.parseInt(numbers[col].trim());
                }
                row++;
            }
            br.close();
            System.out.println("✅ Load ma trận Bụi Cỏ thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Lỗi: Không đọc được file grass.csv");
        }
    }

    public void loadCollisionMap() {
        // ... (Giữ nguyên cũ)
        try {
            InputStream is = getClass().getResourceAsStream("/maps/collision.csv");
            if (is == null) return;
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            int row = 0;
            String line;
            while ((line = br.readLine()) != null && row < maxScreenRow) {
                String[] numbers = line.split(",");
                for (int col = 0; col < maxScreenCol && col < numbers.length; col++) {
                    collisionMap[col][row] = Integer.parseInt(numbers[col].trim());
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

    public void loadCombatSprites() {
        try {
            playerCombatSprite = ImageIO.read(getClass().getResourceAsStream("/players/player-male-combat.png"));

            int i = 1;
            while (true) {
                String imagePath = "/players/combat/trainer" + i + ".png";
                InputStream is = getClass().getResourceAsStream(imagePath);
                if (is == null) {
                    break;
                }
                trainerCombatSprites.add(ImageIO.read(is));
                i++;
            }
            combatBg1 = ImageIO.read(getClass().getResourceAsStream("/maps/combat-1.png"));
            combatBg2 = ImageIO.read(getClass().getResourceAsStream("/maps/combat-2.png"));
            System.out.println("✅ Load ảnh Combat Sprites thành công!");
        } catch (Exception e) {
            System.out.println("❌ Lỗi: Không tìm thấy ảnh combat hoặc trainer!");
            e.printStackTrace();
        }
    }

    public void setRandomSpawnPoint() {
        // ... (Giữ nguyên cũ)
        Random random = new Random();
        boolean isSpawned = false;
        int attempts = 0;

        int minCol = 1;
        int maxCol = maxScreenCol - 2;
        int minRow = 1;
        int maxRow = maxScreenRow - 2;

        while (isSpawned == false && attempts < 1000) {
            int randomCol = random.nextInt(maxCol - minCol + 1) + minCol;
            int randomRow = random.nextInt(maxRow - minRow + 1) + minRow;

            if (collisionMap[randomCol][randomRow] == -1 &&
                    collisionMap[randomCol + 1][randomRow] == -1 &&
                    collisionMap[randomCol - 1][randomRow] == -1 &&
                    collisionMap[randomCol][randomRow + 1] == -1 &&
                    collisionMap[randomCol][randomRow - 1] == -1) {

                player.x = randomCol * tileSize;
                player.y = randomRow * tileSize;
                isSpawned = true;
                System.out.println("✅ Random Respawn AN TOÀN tại Cột " + randomCol + ", Hàng " + randomRow);
            }
            attempts++;
        }

        if (!isSpawned) {
            System.out.println("⚠️ Không tìm thấy điểm Spawn rộng rãi, set mặc định tại (1,1)");
            player.x = tileSize;
            player.y = tileSize;
        }
    }

    // HÀM MỚI: Tái sử dụng ảnh đã có sẵn trên RAM (Không đọc lại ổ cứng)
    public void loadMap(BufferedImage preloadedImage) {
        if (preloadedImage != null) {
            mapImage = preloadedImage; // Lấy luôn ảnh truyền vào

            // Tính toán lại kích thước cửa sổ và lưới
            screenWidth = mapImage.getWidth() * scale;
            screenHeight = mapImage.getHeight() * scale;

            maxScreenCol = (int) Math.ceil((double) screenWidth / tileSize);
            maxScreenRow = (int) Math.ceil((double) screenHeight / tileSize);

            collisionMap = new int[maxScreenCol][maxScreenRow];
            grassMap = new int[maxScreenCol][maxScreenRow];

            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                this.setPreferredSize(new Dimension(screenWidth, screenHeight));
                window.pack();
                window.setLocationRelativeTo(null);
            }
            System.out.println("⚡ Đã Load Map siêu tốc từ RAM!");
        }
    }

    public void drawWrappedText(Graphics2D g2, String text, int x, int y, int maxWidth, int lineHeight) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (g2.getFontMetrics().stringWidth(line + word) < maxWidth) {
                line.append(word).append(" ");
            } else {
                g2.drawString(line.toString(), x, y);
                line = new StringBuilder(word + " ");
                y += lineHeight;
            }
        }
        g2.drawString(line.toString(), x, y);
    }
}