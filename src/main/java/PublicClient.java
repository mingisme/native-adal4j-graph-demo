import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.graph.models.extensions.User;

import javax.naming.ServiceUnavailableException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PublicClient {


    //    private final static String AUTHORITY = "https://login.microsoftonline.com/common/";
    private final static String AUTHORITY = "https://login.chinacloudapi.cn/common/";

    private final static String CLIENT_ID = "xxxx";// "7f4daa23-b0b9-4e44-bba2-12b2a36e8502";//"59a4bfa9-7ce6-45c2-928c-e60505ec5697";

    public static void main(String args[]) throws Exception {

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                System.in))) {
//            System.out.print("Enter username: ");
            String username = "xxxx@devwang.partner.onmschina.cn"; //br.readLine();
//            System.out.print("Enter password: ");
            String password = "xxxxx"; //br.readLine();

            // Request access token from AAD
            AuthenticationResult result = getAccessTokenFromUserCredentials(
                    username, password);
            // Get user info from Microsoft Graph
            String accessToken = result.getAccessToken();
            System.out.println("Access token is: " + accessToken);
            String userInfo = getUserInfoFromGraph(accessToken);
            System.out.print(userInfo);
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation().create();
            User user = Graph.getUser(accessToken);

            System.out.println(gson.toJson(user));

            List<User> users = Graph.getUsersOfGroup(accessToken,"xxxx");

            System.out.println(gson.toJson(users));
        }
    }

    private static AuthenticationResult getAccessTokenFromUserCredentials(
            String username, String password) throws Exception {
        AuthenticationContext context;
        AuthenticationResult result;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            context = new AuthenticationContext(AUTHORITY, false, service);
//            Future<AuthenticationResult> future = context.acquireToken(
//                    "https://graph.microsoft.com", CLIENT_ID, username, password,
//                    null);
            Future<AuthenticationResult> future = context.acquireToken(
                    "https://microsoftgraph.chinacloudapi.cn", CLIENT_ID, username, password,
                    null);

            result = future.get();
        } finally {
            service.shutdown();
        }

        if (result == null) {
            throw new ServiceUnavailableException(
                    "authentication result was null");
        }
        return result;
    }

    private static String getUserInfoFromGraph(String accessToken) throws IOException {

        //URL url = new URL("https://graph.microsoft.com/v1.0/me");
        //URL url = new URL("https://microsoftgraph.chinacloudapi.cn/v1.0/me/events?$filter=start/dateTime ge '2017-07-01T08:00'");
//        URL url = new URL("https://microsoftgraph.chinacloudapi.cn/v1.0/me?$filter=surname eq 'aa'");

        URL url = new URL("https://microsoftgraph.chinacloudapi.cn/v1.0/groups/9acce987-bc7d-459a-8636-b13894b2fc55/members");
        //URL url = new URL("https://microsoftgraph.chinacloudapi.cn/v1.0/groups/9acce987-bc7d-459a-8636-b13894b2fc55/members?$filter=value/givenName eq 'bob'");
        //URL url = new URL("https://microsoftgraph.chinacloudapi.cn/v1.0/groups/9acce987-bc7d-459a-8636-b13894b2fc55/members?$filter=@odata.type eq '#microsoft.graph.user'");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/json");

        int httpResponseCode = conn.getResponseCode();
        if (httpResponseCode == 200) {
            BufferedReader in = null;
            StringBuilder response;
            try {
                in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                String inputLine;
                response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            } finally {
                in.close();
            }
            return response.toString();
        } else {
            return String.format("Connection returned HTTP code: %s with message: %s",
                    httpResponseCode, conn.getResponseMessage());
        }
    }
}

