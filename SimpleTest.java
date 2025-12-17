import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleTest {
    public static void main(String[] args) {
        try {
            // Test 1: Check if the application is running
            System.out.println("Testing if application is running...");
            String homePage = sendGetRequest("http://localhost:8081/");
            if (homePage.contains("Blog") || homePage.contains("blog") || homePage.contains("首页")) {
                System.out.println("Application is running and accessible");
            } else {
                System.out.println("Application may not be running correctly");
                System.out.println("Home page content: " + homePage.substring(0, Math.min(200, homePage.length())));
            }
            
            // Test 2: Check if registration page is accessible
            System.out.println("\nTesting registration page access...");
            String registerPage = sendGetRequest("http://localhost:8081/register");
            if (registerPage.contains("register") || registerPage.contains("注册")) {
                System.out.println("Registration page is accessible");
                
                // Extract CSRF token if present
                Pattern csrfPattern = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
                Matcher matcher = csrfPattern.matcher(registerPage);
                if (matcher.find()) {
                    String csrfToken = matcher.group(1);
                    System.out.println("CSRF token found: " + csrfToken.substring(0, Math.min(10, csrfToken.length())) + "...");
                } else {
                    System.out.println("No CSRF token found");
                }
            } else {
                System.out.println("Registration page may not be accessible");
            }
            
            // Test 3: Check if login page is accessible
            System.out.println("\nTesting login page access...");
            String loginPage = sendGetRequest("http://localhost:8081/login");
            if (loginPage.contains("login") || loginPage.contains("登录")) {
                System.out.println("Login page is accessible");
            } else {
                System.out.println("Login page may not be accessible");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static String sendGetRequest(String url) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(true);
        
        int responseCode = connection.getResponseCode();
        System.out.println("Response code for " + url + ": " + responseCode);
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }
}