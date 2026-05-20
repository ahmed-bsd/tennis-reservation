package com.tennis.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
    @RequestMapping("/users")
    public class UserController {

        @Autowired
        private UserService userService;

        @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
        @GetMapping
        public List<User> getAllUsers() {
            return userService.getAllUsers();
        }
    }

