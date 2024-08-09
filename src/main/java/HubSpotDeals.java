import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Deal;
import model.User;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import response.ResponseBean;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HubSpotDeals {
    private static final String DATASET_URL = "https://candidate.hubteam.com/candidateTest/v3/problem/dataset?userKey=769f2f3d0be5b67540c665429059";
    private static final String RESULT_URL = "https://candidate.hubteam.com/candidateTest/v3/problem/result?userKey=769f2f3d0be5b67540c665429059";

    public static void main(String[] args) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            ObjectMapper mapper = new ObjectMapper();

            // Step 1: Fetch data
            Map<String, List<?>> data = fetchData(httpClient, mapper);
            List<User> users = mapper.convertValue(data.get("users"), new TypeReference<>() {});
            List<Deal> deals = mapper.convertValue(data.get("deals"), new TypeReference<>() {});

            // Step 2: Process the data
            List<ResponseBean> results = processUserDeals(users, deals);

            // Step 3: Send the result
            sendResults(httpClient, mapper, results);
        }
    }

    private static Map<String, List<?>> fetchData(CloseableHttpClient httpClient, ObjectMapper mapper) throws IOException {
        HttpGet getRequest = new HttpGet(DATASET_URL);
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String result = EntityUtils.toString(response.getEntity());
            return mapper.readValue(result, new TypeReference<>() {});
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<ResponseBean> processUserDeals(List<User> users, List<Deal> deals) {
        return users.stream()
                .map(user -> {
                    List<Integer> viewableDeals = deals.stream()
                            .filter(deal -> canView(user, deal, users))
                            .map(Deal::getDealId)
                            .collect(Collectors.toList());

                    List<Integer> editableDeals = deals.stream()
                            .filter(deal -> canEdit(user, deal, users))
                            .map(Deal::getDealId)
                            .collect(Collectors.toList());

                    return new ResponseBean(user.getUserId(), viewableDeals, editableDeals);
                })
                .collect(Collectors.toList());
    }

    private static void sendResults(CloseableHttpClient httpClient, ObjectMapper mapper, List<ResponseBean> results) throws IOException {
        HttpPost postRequest = new HttpPost(RESULT_URL);
        StringEntity jsonEntity = new StringEntity(mapper.writeValueAsString(Collections.singletonMap("results", results)));
        postRequest.setEntity(jsonEntity);
        postRequest.setHeader("Content-type", "application/json");

        try (CloseableHttpResponse postResponse = httpClient.execute(postRequest)) {
            System.out.println(EntityUtils.toString(postResponse.getEntity()));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean canView(User user, Deal deal, List<User> users) {
        return switch (user.getViewPermissionLevel()) {
            case "ALL" -> true;
            case "OWNED_ONLY" -> user.getUserId() == deal.getOwnerUserId();
            case "OWNED_OR_TEAM" -> user.getUserId() == deal.getOwnerUserId() || isOnSameTeam(user, deal, users);
            default -> false;
        };
    }

    private static boolean canEdit(User user, Deal deal, List<User> users) {
        return switch (user.getEditPermissionLevel()) {
            case "ALL" -> true;
            case "OWNED_ONLY" -> user.getUserId() == deal.getOwnerUserId();
            case "OWNED_OR_TEAM" -> user.getUserId() == deal.getOwnerUserId() || isOnSameTeam(user, deal, users);
            default -> false;
        };
    }

    private static boolean isOnSameTeam(User user, Deal deal, List<User> users) {
        return users.stream()
                .filter(u -> u.getUserId() == deal.getOwnerUserId())
                .findFirst()
                .map(dealOwner -> user.getTeamIds().stream().anyMatch(dealOwner.getTeamIds()::contains))
                .orElse(false);
    }
}
