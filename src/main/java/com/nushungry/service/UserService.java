package com.nushungry.service;

import com.nushungry.dto.AuthResponse;
import com.nushungry.dto.LoginRequest;
import com.nushungry.dto.RegisterRequest;
import com.nushungry.dto.UserProfileResponse;
import com.nushungry.model.User;
import com.nushungry.model.UserRole;
import com.nushungry.repository.UserRepository;
import com.nushungry.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse register(RegisterRequest request) {
        return register(request, null, null);
    }

    public AuthResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setEnabled(true);
        user.setRole(UserRole.ROLE_USER);

        User savedUser = userRepository.save(user);

        // Generate Access Token with userId and role claims
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("userId", savedUser.getId());
        claims.put("role", savedUser.getRole().getValue());
        String accessToken = jwtUtil.generateAccessToken(savedUser.getUsername(), claims);

        // Generate Refresh Token
        String refreshToken = refreshTokenService.createRefreshToken(savedUser.getId(), ipAddress, userAgent);

        AuthResponse response = new AuthResponse();
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtUtil.getAccessTokenExpiration());
        response.setId(savedUser.getId());
        response.setUsername(savedUser.getUsername());
        response.setEmail(savedUser.getEmail());
        response.setAvatarUrl(savedUser.getAvatarUrl());
        response.setRole(savedUser.getRole().getValue());

        return response;
    }

    public AuthResponse login(LoginRequest request) {
        return login(request, null, null);
    }

    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Load user details
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate Access Token with userId and role claims
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().getValue());
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), claims);

        // Generate Refresh Token
        String refreshToken = refreshTokenService.createRefreshToken(user.getId(), ipAddress, userAgent);

        AuthResponse response = new AuthResponse();
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtUtil.getAccessTokenExpiration());
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setRole(user.getRole().getValue());

        return response;
    }

    public UserProfileResponse getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return Optional<User>
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 根据邮箱查找用户
     * @param email 邮箱
     * @return Optional<User>
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public String uploadAvatar(MultipartFile file) throws IOException {
        User user = getCurrentUser();

        // Create upload directory if it doesn't exist
        String uploadDir = "uploads/avatars/";
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.contains(".")
            ? originalFilename.substring(originalFilename.lastIndexOf("."))
            : ".jpg";
        String filename = UUID.randomUUID().toString() + fileExtension;

        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        // Update user avatar URL
        String avatarUrl = "/uploads/avatars/" + filename;
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        return avatarUrl;
    }

    /**
     * 上传并裁剪头像
     * @param file 上传的文件
     * @param x 裁剪起始X坐标
     * @param y 裁剪起始Y坐标
     * @param width 裁剪宽度
     * @param height 裁剪高度
     * @return 头像URL
     * @throws IOException IO异常
     */
    public String uploadAvatarWithCrop(MultipartFile file, int x, int y, int width, int height) throws IOException {
        User user = getCurrentUser();

        // Create upload directory if it doesn't exist
        String uploadDir = "uploads/avatars/";
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String filename = UUID.randomUUID().toString() + ".jpg";
        Path filePath = uploadPath.resolve(filename);

        // Read the original image
        BufferedImage originalImage = ImageIO.read(file.getInputStream());

        // Crop and resize to 200x200 (standard avatar size)
        Thumbnails.of(originalImage)
                .sourceRegion(x, y, width, height)
                .size(200, 200)
                .outputFormat("jpg")
                .outputQuality(0.9)
                .toFile(filePath.toFile());

        // Update user avatar URL
        String avatarUrl = "/uploads/avatars/" + filename;
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        return avatarUrl;
    }

    /**
     * 重置用户密码
     * @param email 邮箱
     * @param newPassword 新密码(明文)
     * @throws RuntimeException 如果用户不存在
     */
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
