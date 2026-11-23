package com.supportdesk.customer.controller;

import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.enums.Role;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Typing only the CIF number works everywhere a CIF comes in, against the real database and the local seed
 * (Ahmed Ali, CIF-100101): the search box, the customer tabs, ticket create and the ticket list, and the audit filter.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CifWithoutPrefixIntegrationTest
{
    private static final String NUMBER = "100101";
    private static final String CIF = "CIF-100101";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;

    private String agent;

    @BeforeEach
    void newAgent()
    {
        agent = tokenFor(Role.AGENT);
    }

    @Test
    void searchBoxFindsTheCustomerByTheNumberAlone() throws Exception
    {
        for (String typed : new String[] {NUMBER, "cif" + NUMBER, "CIF " + NUMBER, CIF}) {
            getAs("/api/customers/search?q=" + typed, agent)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.results[*].cif", hasItem(CIF)))
                    .andExpect(jsonPath("$.results[?(@.cif == '" + CIF + "')].matchedBy").value("CIF"));
        }
    }

    @Test
    void everyCustomerTabWorksWithTheNumberAlone() throws Exception
    {
        postJson("/api/customers/" + NUMBER + "/verify", agent, "{\"passedChecks\":[\"DATE_OF_BIRTH\",\"MOBILE_ON_FILE\"]}")
                .andExpect(status().isOk());
        getAs("/api/customers/" + NUMBER + "/verification", agent).andExpect(status().isOk());
        getAs("/api/customers/" + NUMBER, agent).andExpect(status().isOk())
                .andExpect(jsonPath("$.cif").value(CIF));
        getAs("/api/customers/" + NUMBER + "/diagnosis", agent).andExpect(status().isOk());
        getAs("/api/customers/" + NUMBER + "/activity", agent).andExpect(status().isOk());
        getAs("/api/customers/" + NUMBER + "/tickets", agent).andExpect(status().isOk());
        postJson("/api/customers/" + NUMBER + "/reveal", agent, "{\"field\":\"MOBILE\"}").andExpect(status().isOk());
        mockMvc.perform(post("/api/customers/" + NUMBER + "/end-call").header(HttpHeaders.AUTHORIZATION, agent))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void unknownNumberIsStillCustomerNotFound() throws Exception
    {
        getAs("/api/customers/999999999/verification", agent)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    void ticketCreateAndTicketListAcceptTheNumberAlone() throws Exception
    {
        postJson("/api/customers/" + NUMBER + "/verify", agent, "{\"passedChecks\":[\"DATE_OF_BIRTH\",\"MOBILE_ON_FILE\"]}")
                .andExpect(status().isOk());
        postJson("/api/tickets", agent, "{\"cif\":\"" + NUMBER + "\",\"categoryCode\":\"OTP_NOT_RECEIVED\",\"priority\":\"MEDIUM\","
                + "\"description\":\"OTP SMS never arrives.\",\"platform\":\"MOBILE\",\"allowDuplicate\":true}")
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.customerCif").value(CIF));

        getAs("/api/tickets?q=" + NUMBER, agent)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", not(empty())))
                .andExpect(jsonPath("$.items[*].customerCif", everyItem(equalTo(CIF))));
    }

    @Test
    void auditFilterFindsCustomerRowsByTheNumberAlone() throws Exception
    {
        getAs("/api/customers/search?q=" + NUMBER, agent).andExpect(status().isOk());
        postJson("/api/customers/" + NUMBER + "/verify", agent, "{\"passedChecks\":[\"DATE_OF_BIRTH\",\"MOBILE_ON_FILE\"]}")
                .andExpect(status().isOk());   // audits CALLER_VERIFIED on entity CUSTOMER, id CIF-100101

        getAs("/api/audit/logs?entity=CUSTOMER&entityId=" + NUMBER, tokenFor(Role.GUIDE_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", not(empty())))
                .andExpect(jsonPath("$.items[*].entityId", everyItem(equalTo(CIF))));
    }


    private String tokenFor(Role role)
    {
        String username = "cif_" + role.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);
        String mobile = String.format("+2096%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        User user = userRepository.save(new User(username, "$2a$10$unused", "CIF Test",
                username + "@test.local", mobile, role));
        return "Bearer " + jwtService.generateToken(user).token();
    }

    private ResultActions postJson(String url, String token, String body) throws Exception
    {
        return mockMvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions getAs(String url, String token) throws Exception
    {
        return mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, token));
    }
}
