package com.minhquan.myself.Quiz.Service;

import com.minhquan.myself.Quiz.Entity.User;
import com.minhquan.myself.Quiz.Repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepository;

    public User loginOrRegister(String username) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setTotalScore(0.0);
        return userRepository.save(newUser);
    }

    public List<User> getTop3Players() {
        return userRepository.findTop3ByOrderByTotalScoreDesc();
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }
}
