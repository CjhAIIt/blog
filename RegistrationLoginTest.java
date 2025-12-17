import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegistrationLoginTest {
    private static final String BASE_URL = "http://localhost:8081";
    private static String sessionId = "";
    
    public static void main(String[] args) {
        System.out.println("=== 开始运行注册登录测试 ===");
        try {
            System.out.println("=== 测试注册和登录功能 ===");
            
            // 生成随机用户名
            String timestamp = String.valueOf(System.currentTimeMillis());
            String username = "test" + (System.currentTimeMillis() % 100000); // 生成较短的随机数，确保用户名不超过20个字符
            String email = username + "@test.com";
            String password = "testpass123";
            
            System.out.println("1. 测试注册功能...");
            System.out.println("   用户名: " + username);
            System.out.println("   邮箱: " + email);
            System.out.println("   密码: " + password);
            
            // 获取注册页面和CSRF token
            System.out.println("   获取注册页面...");
            String registerPage = getPageContent("/register");
            String csrfToken = extractCsrfToken(registerPage);
            System.out.println("   CSRF Token: " + csrfToken);
            
            // 提交注册表单
        System.out.println("   提交注册表单...");
        boolean registrationSuccess = registerUser(username, email, password, csrfToken);
        System.out.println("   注册结果: " + (registrationSuccess ? "成功" : "失败"));
        
        if (registrationSuccess) {
            System.out.println("   注册成功，测试登录功能...");
            // 获取登录页面和CSRF token
            String loginPage = getPageContent("/login");
            String loginCsrfToken = extractCsrfToken(loginPage);
            boolean loginSuccess = loginUser(username, password, loginCsrfToken);
            System.out.println("   登录测试结果: " + (loginSuccess ? "成功" : "失败"));
            
            if (loginSuccess) {
                System.out.println("   问题已解决：新注册的账号可以正常登录");
            } else {
                System.out.println("   问题仍然存在：新注册的账号无法登录");
            }
        } else {
            System.out.println("   注册失败，无法测试登录功能");
        }
            

            
            System.out.println("\n=== 测试完成 ===");
            
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String getPageContent(String path) throws IOException {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // 设置会话cookie
        if (!sessionId.isEmpty()) {
            connection.setRequestProperty("Cookie", sessionId);
        }
        
        connection.setRequestMethod("GET");
        
        // 获取响应
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            // 保存会话cookie
            String cookieHeader = connection.getHeaderField("Set-Cookie");
            if (cookieHeader != null) {
                sessionId = cookieHeader.split(";")[0];
            }
            
            // 处理重定向
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                String newUrl = connection.getHeaderField("Location");
                if (newUrl != null && !newUrl.startsWith("http")) {
                    newUrl = BASE_URL + newUrl;
                }
                if (newUrl != null) {
                    url = new URL(newUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    if (!sessionId.isEmpty()) {
                        connection.setRequestProperty("Cookie", sessionId);
                    }
                    connection.setRequestMethod("GET");
                }
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            reader.close();
            return response.toString();
        } else {
            throw new IOException("HTTP错误: " + responseCode);
        }
    }
    
    private static boolean registerUser(String username, String email, String password, String csrfToken) throws IOException {
        System.out.println("   开始注册用户...");
        URL url = new URL(BASE_URL + "/register");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // 设置会话cookie
        if (!sessionId.isEmpty()) {
            connection.setRequestProperty("Cookie", sessionId);
        }
        
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        
        // 构建表单数据
        String formData = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) + 
                         "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8) + 
                         "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8) + 
                         "&confirmPassword=" + URLEncoder.encode(password, StandardCharsets.UTF_8) +
                         "&_csrf=" + URLEncoder.encode(csrfToken, StandardCharsets.UTF_8);
        
        System.out.println("   提交的表单数据: " + formData);
        
        // 发送请求
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(formData.getBytes());
        outputStream.flush();
        
        // 获取响应
        int responseCode = connection.getResponseCode();
        
        // 保存会话cookie
        String cookieHeader = connection.getHeaderField("Set-Cookie");
        if (cookieHeader != null) {
            sessionId = cookieHeader.split(";")[0];
        }
        
        String responseBody = getResponseText(connection);
        System.out.println("   注册响应码: " + responseCode);
        
        // 如果重定向到登录页面，说明注册成功了
        if (responseCode == 302) {
            String location = connection.getHeaderField("Location");
            if (location != null && location.contains("/login")) {
                System.out.println("   注册成功，重定向到登录页面");
                return true;
            }
        }
        
        // 检查响应内容是否是登录页面（表示注册成功）
        if (responseCode == 200 && responseBody.contains("登录") && responseBody.contains("还没有账户？")) {
            System.out.println("   注册成功，显示登录页面");
            return true;
        }
        
        // 检查是否有错误信息
        if (responseBody.contains("错误") || responseBody.contains("失败") || responseBody.contains("已存在")) {
            System.out.println("   注册失败: 响应中包含错误信息");
            return false;
        }
        
        System.out.println("   注册状态未知，假设成功");
        return true;
    }
    
    private static boolean loginUser(String username, String password, String csrfToken) throws IOException {
        System.out.println("   开始登录用户: " + username);
        URL url = new URL(BASE_URL + "/login");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // 设置会话cookie
        if (!sessionId.isEmpty()) {
            connection.setRequestProperty("Cookie", sessionId);
        }
        
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        
        // 构建表单数据
        String formData = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) + 
                         "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8) + 
                         "&_csrf=" + URLEncoder.encode(csrfToken, StandardCharsets.UTF_8);
        
        System.out.println("   提交的登录表单数据: " + formData);
        
        // 发送请求
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(formData.getBytes());
        outputStream.flush();
        
        // 获取响应
        int responseCode = connection.getResponseCode();
        System.out.println("   登录响应码: " + responseCode);
        
        // 读取响应内容
        String responseBody = getResponseText(connection);
        System.out.println("   登录响应内容: " + responseBody.substring(0, Math.min(200, responseBody.length())) + "...");
        
        // 保存会话cookie
        String cookieHeader = connection.getHeaderField("Set-Cookie");
        if (cookieHeader != null) {
            sessionId = cookieHeader.split(";")[0];
        }
        
        // 检查是否重定向到首页（表示登录成功）
        if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            String location = connection.getHeaderField("Location");
            System.out.println("   登录重定向位置: " + location);
            return location != null && (location.equals("/") || location.endsWith("/"));
        }
        
        // 检查响应内容是否是首页（表示登录成功）
        if (responseCode == 200 && responseBody.contains("首页") && responseBody.contains("简单博客系统")) {
            System.out.println("   登录成功，返回首页内容");
            return true;
        }
        
        // 检查响应中是否有错误信息
        if (responseBody.contains("错误") || responseBody.contains("失败") || responseBody.contains("用户名或密码")) {
            System.out.println("   登录失败: 响应中包含错误信息");
        }
        
        return false;
    }
    
    private static String getResponseText(HttpURLConnection connection) {
        try {
            // 尝试从正常流读取
            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } catch (Exception e) {
            try {
                // 如果正常流失败，尝试从错误流读取
                InputStream errorStream = connection.getErrorStream();
                if (errorStream != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    return response.toString();
                }
            } catch (Exception ex) {
                // 忽略错误
            }
            return "无法读取响应内容";
        }
    }
    
    private static String extractCsrfToken(String html) {
        Pattern pattern = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}