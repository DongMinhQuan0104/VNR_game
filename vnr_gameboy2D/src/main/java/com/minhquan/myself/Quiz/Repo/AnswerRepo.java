package com.minhquan.myself.Quiz.Repo;

import com.minhquan.myself.Quiz.Entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerRepo extends JpaRepository<Answer,Long> {
}
