package com.nushungry.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * User Service 主应用类
 *
 * 注意:
 * - @EnableJpaAuditing 已移至 JpaConfig
 * - @EnableFeignClients 已移至 FeignConfig
 * - 分离配置便于测试时选择性加载
 */
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}