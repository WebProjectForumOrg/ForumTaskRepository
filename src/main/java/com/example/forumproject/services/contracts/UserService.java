package com.example.forumproject.services.contracts;

import com.example.forumproject.models.User;
import com.example.forumproject.models.UserPhoneNumber;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> getAllUsers();

    Optional<User> getUserById(int id);

    User createUser(User user);

    void deleteUserById(int id);

    UserPhoneNumber addPhoneNumber(int id, String phoneNumber);

    User save(User user);
}
