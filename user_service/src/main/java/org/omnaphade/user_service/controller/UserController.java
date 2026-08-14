package org.omnaphade.user_service.controller;

import jakarta.validation.Valid;
import org.omnaphade.user_service.dtos.ProfileCreateDTO;
import org.omnaphade.user_service.dtos.ProfileResponseDTO;
import org.omnaphade.user_service.dtos.SkillDTO;
import org.omnaphade.user_service.dtos.UserSkillDTO;
import org.omnaphade.user_service.service.IUserService;
import org.omnaphade.user_service.storage.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final IUserService userService;
    private final FileStorageService fileStorageService;

    public UserController(IUserService userService, FileStorageService fileStorageService) {
        this.userService = userService;
        this.fileStorageService = fileStorageService;
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

    @PostMapping(value = "/{userId}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponseDTO uploadAvatar(@PathVariable Long userId, @RequestParam("file") MultipartFile file) {
        return userService.uploadAvatar(userId, file);
    }

    @GetMapping("/{userId}/avatar")
    public ResponseEntity<Resource> downloadAvatar(@PathVariable Long userId) {
        String path = userService.getAvatarPath(userId);
        Resource resource = fileStorageService.loadAsResource(path);
        String filename = Paths.get(path).getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
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