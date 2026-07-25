package org.omnaphade.user_service.controller;

import jakarta.validation.Valid;
import org.omnaphade.user_service.dtos.ProfileCreateDTO;
import org.omnaphade.user_service.dtos.ProfileResponseDTO;
import org.omnaphade.user_service.dtos.SkillDTO;
import org.omnaphade.user_service.dtos.UserSkillDTO;
import org.omnaphade.user_service.service.IUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<ProfileResponseDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{userId}/profile")
    public ProfileResponseDTO getUserProfile(@PathVariable Long userId) {
        return userService.getUserProfile(userId);
    }

    @PostMapping("/{userId}/profile")
    public ResponseEntity<ProfileResponseDTO> createUserProfile(
            @PathVariable Long userId,
            @Valid @RequestBody ProfileCreateDTO profileCreateDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUserProfile(userId, profileCreateDTO));
    }

    @PutMapping("/{userId}/profile")
    public ProfileResponseDTO updateUserProfile(
            @PathVariable Long userId,
            @Valid @RequestBody ProfileCreateDTO profileCreateDTO) {
        return userService.updateUserProfile(userId, profileCreateDTO);
    }

    @GetMapping("/{userId}/skills")
    public List<SkillDTO> getUserSkills(@PathVariable Long userId) {
        return userService.getUserSkills(userId);
    }

    @PostMapping("/{userId}/skills")
    public ResponseEntity<SkillDTO> addSkillToUser(@PathVariable Long userId,
                                                    @RequestBody UserSkillDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addSkill(userId, dto));
    }

    @DeleteMapping("/{userId}/skills/{skillId}")
    public void deleteSkillFromUser(@PathVariable Long userId,
                                    @PathVariable Long skillId) {
        userService.removeSkillFromUser(userId, skillId);
    }

    @GetMapping("/skills")
    public List<SkillDTO> getAllSkills() {
        return userService.getAllSkills();
    }

    @PostMapping("/skills")
    public ResponseEntity<SkillDTO> createSkill(@RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createSkill(body.get("name")));
    }
}