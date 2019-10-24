import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.graph.logger.DefaultLogger;
import com.microsoft.graph.logger.LoggerLevel;
import com.microsoft.graph.models.extensions.DirectoryObject;
import com.microsoft.graph.models.extensions.Event;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionWithReferencesPage;
import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionWithReferencesRequestBuilder;
import com.microsoft.graph.requests.extensions.IEventCollectionPage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Graph
 */
public class Graph {

    private static IGraphServiceClient graphClient = null;
    private static SimpleAuthProvider authProvider = null;

    private static void ensureGraphClient(String accessToken) {
        if (graphClient == null) {
            // Create the auth provider
            authProvider = new SimpleAuthProvider(accessToken);

            // Create default logger to only log errors
            DefaultLogger logger = new DefaultLogger();
            logger.setLoggingLevel(LoggerLevel.ERROR);

            // Build a Graph client
            graphClient = GraphServiceClient.builder()
                    .authenticationProvider(authProvider)
                    .logger(logger)
                    .buildClient();
            graphClient.setServiceRoot("https://microsoftgraph.chinacloudapi.cn/v1.0");
        }
    }

    public static User getUser(String accessToken) {
        ensureGraphClient(accessToken);

        // GET /me to get authenticated user
        User me = graphClient
                .me()
                .buildRequest()
                .get();

        return me;
    }

    public static List<Event> getEvents(String accessToken) {
        ensureGraphClient(accessToken);

        // Use QueryOption to specify the $orderby query parameter
        final List<Option> options = new LinkedList<Option>();
        // Sort results by createdDateTime, get newest first
        options.add(new QueryOption("orderby", "createdDateTime DESC"));

        // GET /me/events
        IEventCollectionPage eventPage = graphClient
                .me()
                .events()
                .buildRequest(options)
                .select("subject,organizer,start,end")
                .get();

        return eventPage.getCurrentPage();
    }

    public static List<User> getUsersOfGroup(String accessToken, String groupId) {
        ensureGraphClient(accessToken);

        List<User> users = new ArrayList<>();
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation().create();
        AtomicInteger x = new AtomicInteger(0);
        IDirectoryObjectCollectionWithReferencesPage iDirectoryObjectCollectionWithReferencesPage = graphClient.groups(groupId).members().buildRequest().top(3).get();
        List<DirectoryObject> currentPage = iDirectoryObjectCollectionWithReferencesPage.getCurrentPage();
        collectUsers(currentPage, users, gson, x);
        IDirectoryObjectCollectionWithReferencesRequestBuilder nextPage = iDirectoryObjectCollectionWithReferencesPage.getNextPage();
        while (nextPage != null) {
            iDirectoryObjectCollectionWithReferencesPage = nextPage.buildRequest().get();
            currentPage = iDirectoryObjectCollectionWithReferencesPage.getCurrentPage();
            collectUsers(currentPage, users, gson, x);
            nextPage = iDirectoryObjectCollectionWithReferencesPage.getNextPage();
        }

        return users;
    }

    private static void collectUsers(List<DirectoryObject> currentPage, List<User> users, Gson gson, AtomicInteger x) {
        List<User> addOnUsers = currentPage.stream().filter(o -> "#microsoft.graph.user".equals(o.oDataType)).map(o -> {
            String json = o.getRawObject().toString();
            return gson.fromJson(json, User.class);
        }).collect(Collectors.toList());
        if (!addOnUsers.isEmpty()) {
            System.out.println(x.getAndIncrement() + ": " + gson.toJson(addOnUsers));
            users.addAll(addOnUsers);
        }
    }
}
