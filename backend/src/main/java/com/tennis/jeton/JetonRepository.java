package com.tennis.jeton;

import com.tennis.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JetonRepository extends JpaRepository<JetonAccount, Long> {
    Optional<JetonAccount> findByUser(User user);
}
