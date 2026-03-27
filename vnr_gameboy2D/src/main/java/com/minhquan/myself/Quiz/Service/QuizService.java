package com.minhquan.myself.Quiz.Service;

import com.minhquan.myself.Quiz.Entity.Answer;
import com.minhquan.myself.Quiz.Entity.Question;
import com.minhquan.myself.Quiz.Repo.QuestionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {
    private final QuestionRepo questionRepository;

    @Transactional(readOnly = true)
    public void printQuestionToConsole() {
        questionRepository.findRandomQuestion().ifPresentOrElse(
                q -> {
                    System.out.println("\n***************************************************");
                    System.out.println("CÂU HỎI: " + q.getContent());
                    System.out.println("ĐIỂM: " + q.getScore());
                    System.out.println("---------------------------------------------------");

                    List<Answer> answers = q.getAnswers();

                    // Sắp xếp đáp án theo order_index nếu có, nếu không thì giữ nguyên
                    if (answers != null) {
                        answers.sort(Comparator.comparing(
                                a -> a.getOrder_index() != null ? a.getOrder_index() : 0
                        ));

                        char label = 'A';
                        for (Answer ans : answers) {
                            System.out.println(label + ". " + ans.getContent());
                            // Dòng dưới này để bạn debug xem cái nào đúng (có thể xóa khi chạy thật)
                            // if(ans.getIs_correct()) System.out.print(" (Đúng)");
                            label++;
                        }
                    }

                    System.out.println("***************************************************\n");
                },
                () -> System.out.println("⚠️ Giỏ hàng câu hỏi đang trống! Hãy nạp SQL vào Supabase.")
        );
    }

    @Transactional(readOnly = true)
    public Question getRandomQuestionForGame() {
        return questionRepository.findRandomQuestion().orElse(null);
    }
}
