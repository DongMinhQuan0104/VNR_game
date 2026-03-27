package com.minhquan.myself.Quiz.Repo;

import com.minhquan.myself.Quiz.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User,Long> {
    Optional<User> findByUsername(String username);

    List<User> findTop3ByOrderByTotalScoreDesc();
}
