package com.nushungry.config;

import com.nushungry.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    // 使用通配符模式，允许任何端口的 localhost 和任何 IP 地址访问
                    corsConfig.setAllowedOriginPatterns(java.util.Arrays.asList(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://*:5173",
                        "http://*:5174",
                        "http://*:5175"
                    ));
                    corsConfig.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfig.setAllowedHeaders(java.util.Arrays.asList("*"));
                    corsConfig.setExposedHeaders(java.util.Arrays.asList("Authorization"));
                    corsConfig.setAllowCredentials(true);
                    corsConfig.setMaxAge(3600L);
                    return corsConfig;
                }))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. OPTIONS请求全部允许
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                        // 2. 认证相关接口
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/password/**").permitAll()

                        // 3. 管理员专用接口（必须在公共接口之前）
                        .requestMatchers("/api/cafeterias/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/stalls/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/reviews/admin/**").hasRole("ADMIN")

                        // 4. 写操作需要认证（在公共接口之前）
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/cafeterias").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/cafeterias/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/cafeterias/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/stalls").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/stalls/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/stalls/**").hasRole("ADMIN")

                        // 5. 公开读取接口（最后配置 - 只有GET请求）
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/cafeterias/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/stalls/**").permitAll()
                        .requestMatchers("/api/images/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()

                        // 6. 评价接口：GET 请求允许匿名访问，其他需要认证
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/reviews/**").permitAll()
                        .requestMatchers("/api/reviews/**").authenticated()

                        // 7. 搜索历史接口：GET请求允许匿名访问（未登录返回空），其他需要认证
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/search-history/**").permitAll()
                        .requestMatchers("/api/search-history/**").authenticated()

                        // 8. 收藏接口：GET请求允许匿名访问（检查收藏状态），其他需要认证
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/favorites/**").permitAll()
                        .requestMatchers("/api/favorites/**").authenticated()

                        // 9. 其他所有请求需要认证
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
