package com.supportdesk.azure.client;

import com.supportdesk.azure.client.AzureDevOpsClient.WorkItem;
import com.supportdesk.azure.client.AzureDevOpsClient.WorkItemRequest;
import com.supportdesk.azure.config.AzureDevOpsProperties;
import com.supportdesk.azure.exception.AzureDevOpsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** The real client against a mock Azure DevOps: PAT auth, JSON Patch create, and reuse by the key tag. */
class RestAzureDevOpsClientTest
{
    private static final String BASE = "https://dev.azure.com/bank/support/_apis";
    private static final WorkItemRequest REQUEST = new WorkItemRequest("TICKET-2001", "[Ticket #2001] Payments",
            "<p>details</p>", 2, List.of("support-desk", "service:PAYMENTS"));

    private MockRestServiceServer azure;
    private RestAzureDevOpsClient client;

    @BeforeEach
    void setUp()
    {
        RestClient.Builder builder = RestClient.builder();
        azure = MockRestServiceServer.bindTo(builder).build();
        client = new RestAzureDevOpsClient(new AzureDevOpsProperties(AzureDevOpsProperties.Mode.REST, "bank", "support",
                "Bug", "my-pat", "https://portal", "secret", null), builder);
    }

    @Test
    void createsTheWorkItemWhenNoneCarriesTheKeyTag()
    {
        String auth = "Basic " + Base64.getEncoder().encodeToString(":my-pat".getBytes(StandardCharsets.UTF_8));
        azure.expect(requestTo(BASE + "/wit/wiql?api-version=7.1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", auth))
                .andExpect(jsonPath("$.query").value(org.hamcrest.Matchers.containsString("PortalKey-TICKET-2001")))
                .andRespond(withSuccess("{\"workItems\":[]}", MediaType.APPLICATION_JSON));
        azure.expect(requestTo(BASE + "/wit/workitems/$Bug?api-version=7.1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType("application/json-patch+json"))
                .andExpect(jsonPath("$[0].path").value("/fields/System.Title"))
                .andExpect(jsonPath("$[2].value").value(2))
                .andExpect(jsonPath("$[3].value").value("support-desk; service:PAYMENTS; PortalKey-TICKET-2001"))
                .andRespond(withSuccess("{\"id\":4711,\"rev\":1,\"fields\":{\"System.State\":\"New\"}}",
                        MediaType.APPLICATION_JSON));

        WorkItem created = client.createWorkItem(REQUEST);

        assertThat(created).isEqualTo(new WorkItem(4711, "New", 1));
        azure.verify();
    }

    @Test
    void reusesTheWorkItemAlreadyTaggedWithTheKeySoARetryNeverDuplicates()
    {
        azure.expect(requestTo(BASE + "/wit/wiql?api-version=7.1"))
                .andRespond(withSuccess("{\"workItems\":[{\"id\":4711}]}", MediaType.APPLICATION_JSON));
        azure.expect(requestTo(BASE + "/wit/workitems/4711?api-version=7.1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":4711,\"rev\":3,\"fields\":{\"System.State\":\"Active\"}}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.createWorkItem(REQUEST)).isEqualTo(new WorkItem(4711, "Active", 3));
        azure.verify();   // no POST to /workitems/$Bug
    }

    @Test
    void addsACommentAndTurnsHttpErrorsIntoAzureDevOpsException()
    {
        azure.expect(requestTo(BASE + "/wit/workItems/4711/comments?api-version=7.1-preview.4"))
                .andExpect(jsonPath("$.text").value("Any update?"))
                .andRespond(withSuccess());
        azure.expect(requestTo(BASE + "/wit/workitems/4711?api-version=7.1"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        client.addComment(4711, "Any update?");
        assertThatThrownBy(() -> client.getWorkItem(4711)).isInstanceOf(AzureDevOpsException.class);
        azure.verify();
    }

    @Test
    void restModeWithoutCredentialsFailsAtStartup()
    {
        assertThatThrownBy(() -> new RestAzureDevOpsClient(new AzureDevOpsProperties(AzureDevOpsProperties.Mode.REST,
                "bank", "support", "Bug", "", "https://portal", "secret", null), RestClient.builder()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AZURE_PAT");
    }
}
