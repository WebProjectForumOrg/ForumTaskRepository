package com.example.forumproject.controllers;

import com.example.forumproject.helpers.UserMapper;
import com.example.forumproject.models.User;
import com.example.forumproject.models.dtos.UserCreationDto;
import com.example.forumproject.services.contracts.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private UserMapper userMapper;
    private UserService userService;

    @Autowired
    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable int id) {
        Optional<User> userOptional = userService.getUserById(id);

        if (userOptional.isPresent()) {
            return userOptional.get();
        } else throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/username/{username}")
    public User getByUsername(@PathVariable String username) {
        return userService.getByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public User createUser(@RequestBody UserCreationDto userDto) {
        User user = userMapper.fromDto(userDto);
        return userService.createUser(user);
    }

//    @PutMapping("/{id}")
//    public User updateUser(@PathVariable int id, @RequestBody User user) {
//        user.setId(id);
//        return userService.save(user);
//    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable int id) {
        userService.deleteUserById(id);
    }

}
