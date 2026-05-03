package com.example.blog.controller;

import com.example.blog.dto.SpaceStyleDto;
import com.example.blog.dto.SpaceStyleVersionDto;
import com.example.blog.model.User;
import com.example.blog.service.FileStorageService;
import com.example.blog.service.SpaceStyleService;
import com.example.blog.service.UserService;
import com.example.blog.service.ViewModeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
public class SpaceStyleController {
    private final UserService userService;
    private final SpaceStyleService spaceStyleService;
    private final FileStorageService fileStorageService;
    private final ViewModeService viewModeService;
    private final ObjectMapper objectMapper;

    public SpaceStyleController(UserService userService,
                                SpaceStyleService spaceStyleService,
                                FileStorageService fileStorageService,
                                ViewModeService viewModeService,
                                ObjectMapper objectMapper) {
        this.userService = userService;
        this.spaceStyleService = spaceStyleService;
        this.fileStorageService = fileStorageService;
        this.viewModeService = viewModeService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/space/style")
    public String styleEditor(Principal principal, Model model) {
        User currentUser = userService.getByUsername(principal.getName());
        SpaceStyleDto style = spaceStyleService.getPublicStyle(currentUser);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("spaceStyle", style);
        model.addAttribute("spaceStyleJson", writeJson(style));
        model.addAttribute("spaceStyleVersionsJson", writeJson(spaceStyleService.listVersions(currentUser)));
        model.addAttribute("selectedCategory", "latest");
        return viewModeService.view("space/style");
    }

    @GetMapping("/api/me/space-style")
    @ResponseBody
    public SpaceStyleDto getMyStyle(Principal principal) {
        return spaceStyleService.getPublicStyle(currentUser(principal));
    }

    @PutMapping("/api/me/space-style")
    @ResponseBody
    public SpaceStyleDto saveMyStyle(@RequestBody SpaceStyleDto request, Principal principal) {
        return spaceStyleService.save(currentUser(principal), request);
    }

    @PostMapping("/api/me/space-style/reset")
    @ResponseBody
    public SpaceStyleDto resetMyStyle(Principal principal) {
        return spaceStyleService.reset(currentUser(principal));
    }

    @GetMapping("/api/me/space-style/versions")
    @ResponseBody
    public List<SpaceStyleVersionDto> versions(Principal principal) {
        return spaceStyleService.listVersions(currentUser(principal));
    }

    @PostMapping("/api/me/space-style/versions/{versionId}/restore")
    @ResponseBody
    public SpaceStyleDto restore(@PathVariable Long versionId, Principal principal) {
        return spaceStyleService.restore(currentUser(principal), versionId);
    }

    @PostMapping(value = "/api/me/space-style/background", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadBackground(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeSpaceBackground(file);
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("Background image is required");
        }
        return ResponseEntity.ok(Map.of("url", url));
    }

    private User currentUser(Principal principal) {
        return userService.getByUsername(principal.getName());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
