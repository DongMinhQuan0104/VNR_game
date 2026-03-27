package com.minhquan.myself.Main;

import com.minhquan.myself.Quiz.Entity.User;
import com.minhquan.myself.Quiz.Service.QuizService;
import com.minhquan.myself.Quiz.Service.UserService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.swing.*;

@SpringBootApplication(scanBasePackages = "com.minhquan.myself.Quiz")
@EntityScan("com.minhquan.myself.Quiz.Entity")
@EnableJpaRepositories("com.minhquan.myself.Quiz.Repo")
public class Main {
    public static User currentUser;
    public static UserService userService;
    public static QuizService quizService;
    public static void main(String[] args) {

        ApplicationContext context = new SpringApplicationBuilder(Main.class)
                .headless(false)
                .run(args);
        userService = context.getBean(UserService.class);
        quizService = context.getBean(QuizService.class);
        String playerName = JOptionPane.showInputDialog(
                null,
                "Chào mừng đến với VNR-202 quiz\nVui lòng nhập tên của bạn:",
                "Đăng nhập",
                JOptionPane.QUESTION_MESSAGE
        );
        currentUser = userService.loginOrRegister(playerName);
        System.out.println("🎮 Người chơi hiện tại: " + currentUser.getUsername() + " | Điểm: " + currentUser.getTotalScore());
        JFrame window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.setTitle("Vnr_gameboy2D");
        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        gamePanel.startGameThread();
    }
    public static void addScoreAndSave(int pointsToAdd) {
        if (currentUser != null && userService != null) {
            Double currentScore = currentUser.getTotalScore();
            currentUser.setTotalScore(currentScore + pointsToAdd);
            userService.saveUser(currentUser);

            System.out.println("🌟 " + currentUser.getUsername() + " vừa nhận được " + pointsToAdd + " điểm! Tổng: " + currentUser.getTotalScore());
        }
    }
}