import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class RegistrationTest {
    public static void main(String[] args) {
        try {
            // Test user registration
            String username = "testuser" + System.currentTimeMillis();
            String email = username + "@test.com";
            String password = "testpass123";
            
            System.out.println("Testing user registration...");
            System.out.println("Username: " + username);
            System.out.println("Email: " + email);
            System.out.println("Password: " + password);
            
            // Registration request
            String registerResponse = sendPostRequest("http://localhost:8081/register", 
                "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) + 
                "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8) + 
                "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8) + 
                "&confirmPassword=" + URLEncoder.encode(password, StandardCharsets.UTF_8));
            
            System.out.println("Registration response: " + registerResponse);
            
            // Wait for registration to complete
            Thread.sleep(2000);
            
            // Test user login
            System.out.println("\nTesting user login...");
            String loginResponse = sendPostRequest("http://localhost:8081/login", 
                "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) + 
                "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8));
            
            System.out.println("Login response: " + loginResponse);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static String sendPostRequest(String url, String postData) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);
        
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = postData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = connection.getResponseCode();
        System.out.println("Response code: " + responseCode);
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }
}