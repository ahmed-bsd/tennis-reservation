package com.tennis.user;

import com.tennis.user.User;
import com.tennis.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(u -> new User(
                        u.getId(),
                        u.getFirstName(),
                        u.getLastName()
                ))
                .toList();
    }
}