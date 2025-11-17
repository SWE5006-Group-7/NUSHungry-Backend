import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class CheckAdminPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 从数据库获取的密码哈希
        String hash = "$2a$10$8zLN.IqSdVYGfKZ8PZ0X6ufW8YxMJ6YbFYqGU9.qZHDJZnWJkZ7Pm";
        
        // 尝试常见密码
        String[] passwords = {
            "admin",
            "admin123", 
            "Admin@123",
            "Admin123",
            "Admin@123!",
            "password",
            "Password123",
            "nushungry",
            "NUSHungry123"
        };
        
        System.out.println("尝试验证管理员密码...\n");
        for (String pwd : passwords) {
            boolean matches = encoder.matches(pwd, hash);
            System.out.println("密码: " + pwd + " => " + (matches ? "✓ 匹配成功!" : "✗ 不匹配"));
            if (matches) {
                System.out.println("\n找到正确密码: " + pwd);
                break;
            }
        }
    }
}
