package com.supportdesk.azure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.supportdesk.azure.config.AzureDevOpsProperties;
import com.supportdesk.azure.exception.AzureDevOpsException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Azure DevOps REST API 7.1 (dev.azure.com/{organization}/{project}). Authenticates with a personal access token.
 * Creating a work item first looks for one tagged with the idempotency key, so a retry after a lost response
 * reuses it instead of creating a duplicate.
 */
public class RestAzureDevOpsClient implements AzureDevOpsClient
{
    static final String API = "api-version=7.1";
    static final String COMMENTS_API = "api-version=7.1-preview.4";
    static final MediaType JSON_PATCH = MediaType.valueOf("application/json-patch+json");

    private final RestClient http;
    private final String workItemType;

    public RestAzureDevOpsClient(AzureDevOpsProperties props, RestClient.Builder builder)
    {
        if (isBlank(props.organization()) || isBlank(props.project()) || isBlank(props.personalAccessToken())) {
            throw new IllegalStateException("supportdesk.azure.mode=REST needs organization, project and "
                    + "personal-access-token (AZURE_ORG, AZURE_PROJECT, AZURE_PAT).");
        }
        String basic = Base64.getEncoder().encodeToString((":" + props.personalAccessToken())
                .getBytes(StandardCharsets.UTF_8));
        this.http = builder
                .baseUrl("https://dev.azure.com/" + props.organization() + "/" + props.project() + "/_apis")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .build();
        this.workItemType = props.workItemType();
    }

    @Override
    public WorkItem createWorkItem(WorkItemRequest request)
    {
        try {
            Long existing = findIdByTag(request.keyTag());
            if (existing != null) {
                return getWorkItem(existing);                     // created before; the response was lost
            }
            List<Map<String, Object>> patch = List.of(
                    add("/fields/System.Title", request.title()),
                    add("/fields/System.Description", request.descriptionHtml()),
                    add("/fields/Microsoft.VSTS.Common.Priority", request.priority()),
                    add("/fields/System.Tags", String.join("; ", withKeyTag(request))));
            JsonNode created = http.post()
                    .uri("/wit/workitems/${type}?" + API, workItemType)
                    .contentType(JSON_PATCH)
                    .body(patch)
                    .retrieve()
                    .body(JsonNode.class);
            return toWorkItem(created);
        } catch (RestClientException | IllegalStateException e) {
            throw new AzureDevOpsException("Azure DevOps create work item failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void addComment(long workItemId, String text)
    {
        try {
            http.post()
                    .uri("/wit/workItems/{id}/comments?" + COMMENTS_API, workItemId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new AzureDevOpsException("Azure DevOps add comment failed: " + e.getMessage(), e);
        }
    }

    @Override
    public WorkItem getWorkItem(long workItemId)
    {
        try {
            return toWorkItem(http.get()
                    .uri("/wit/workitems/{id}?" + API, workItemId)
                    .retrieve()
                    .body(JsonNode.class));
        } catch (RestClientException | IllegalStateException e) {
            throw new AzureDevOpsException("Azure DevOps get work item failed: " + e.getMessage(), e);
        }
    }


    /** WIQL search for the work item carrying the key tag; null when there is none. */
    private Long findIdByTag(String tag)
    {
        String wiql = "SELECT [System.Id] FROM WorkItems WHERE [System.TeamProject] = @project "
                + "AND [System.Tags] CONTAINS '" + tag.replace("'", "''") + "'";
        JsonNode result = http.post()
                .uri("/wit/wiql?" + API)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("query", wiql))
                .retrieve()
                .body(JsonNode.class);
        JsonNode items = result == null ? null : result.path("workItems");
        return items != null && items.isArray() && !items.isEmpty() ? items.get(0).path("id").asLong() : null;
    }

    private static WorkItem toWorkItem(JsonNode json)
    {
        if (json == null || !json.hasNonNull("id")) {
            throw new IllegalStateException("Azure DevOps returned no work item");
        }
        return new WorkItem(json.get("id").asLong(), json.path("fields").path("System.State").asText(null),
                json.path("rev").asInt());
    }

    private static List<String> withKeyTag(WorkItemRequest request)
    {
        List<String> tags = new java.util.ArrayList<>(request.tags());
        tags.add(request.keyTag());
        return tags;
    }

    private static Map<String, Object> add(String path, Object value)
    {
        Map<String, Object> op = new LinkedHashMap<>();
        op.put("op", "add");
        op.put("path", path);
        op.put("value", value);
        return op;
    }

    private static boolean isBlank(String s)
    {
        return s == null || s.isBlank();
    }
}
