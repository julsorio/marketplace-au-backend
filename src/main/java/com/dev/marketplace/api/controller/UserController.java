package com.dev.marketplace.api.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.marketplace.api.model.User;
import com.dev.marketplace.api.request.dto.UserSummary;
import com.dev.marketplace.api.security.UserPrincipal;

@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserSummary> me(@AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();
        
        return ResponseEntity.ok(new UserSummary(user.getId(), user.getEmail(), user.getDisplayName(), user.getAvatarUrl()));
    }
}
