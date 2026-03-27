package com.minhquan.myself.Quiz.Repo;

import com.minhquan.myself.Quiz.Entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionRepo extends JpaRepository<Question,Long> {

    @Query(value = "SELECT * FROM question ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Question> findRandomQuestion();
}
