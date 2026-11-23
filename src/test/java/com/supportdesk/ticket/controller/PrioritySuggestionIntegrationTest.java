package com.supportdesk.ticket.controller;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Priority suggestion against the real database: a new sub-category starts from its default, and once agents
 * have opened 3 tickets in it with another priority, that priority is suggested. Also checks the 30 sample tickets.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PrioritySuggestionIntegrationTest
{
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbc;

    private String admin;
    private String agent;
    private String category;
    private String sub;

    @BeforeEach
    void setUp()
    {
        admin = tokenFor(Role.GUIDE_ADMIN);
        agent = tokenFor(Role.AGENT);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        category = "IT_PRIO_" + suffix;
        sub = "IT_PRIO_SUB_" + suffix;
    }

    @Test
    void suggestionStartsFromTheDefaultThenLearnsFromAgents() throws Exception
    {
        postJson("/api/admin/categories", admin, "{\"code\":\"" + category + "\",\"name\":\"IT Prio\",\"defaultPriority\":\"LOW\"}")
                .andExpect(status().isCreated());
        postJson("/api/admin/categories/" + category + "/subcategories", admin,
                "{\"code\":\"" + sub + "\",\"name\":\"IT Prio Sub\",\"defaultPriority\":\"MEDIUM\"}")
                .andExpect(status().isCreated());

        // no tickets yet: the sub-category's default
        suggestion(sub).andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedPriority").value("MEDIUM"))
                .andExpect(jsonPath("$.basedOn").value("CATEGORY_DEFAULT"))
                .andExpect(jsonPath("$.slaTargetMinutes").value(24 * 60))
                .andExpect(jsonPath("$.category").value("IT Prio / IT Prio Sub"));

        // agents open 3 tickets as HIGH
        postJson("/api/customers/100101/verify", agent, "{\"passedChecks\":[\"DATE_OF_BIRTH\",\"MOBILE_ON_FILE\"]}")
                .andExpect(status().isOk());
        for (int i = 0; i < 3; i++) {
            postJson("/api/tickets", agent, "{\"cif\":\"100101\",\"categoryCode\":\"" + sub + "\",\"priority\":\"HIGH\","
                    + "\"description\":\"Priority test " + i + "\",\"platform\":\"WEB\",\"allowDuplicate\":true}")
                    .andExpect(status().is2xxSuccessful());
        }

        suggestion(sub).andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedPriority").value("HIGH"))
                .andExpect(jsonPath("$.categoryDefault").value("MEDIUM"))
                .andExpect(jsonPath("$.basedOn").value("HISTORY"))
                .andExpect(jsonPath("$.recentTickets.HIGH").value(3))
                .andExpect(jsonPath("$.slaTargetMinutes").value(8 * 60))
                .andExpect(jsonPath("$.reason").value("Agents chose HIGH for 3 of 3 tickets in IT Prio Sub in the last "
                        + "90 days (default MEDIUM)."));
    }

    @Test
    void topLevelUnknownAndWrongRoleAreRefused() throws Exception
    {
        suggestion("PAYMENTS").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SUBCATEGORY_REQUIRED"));
        suggestion("NO_SUCH_CODE").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
        mockMvc.perform(get("/api/tickets/categories/OTP_NOT_RECEIVED/suggested-priority")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isForbidden());
    }

    @Test
    void thirtySampleTicketsAreSeededAcrossCategoriesAndPriorities()
    {
        Map<String, Object> seed = jdbc.queryForMap("""
                SELECT COUNT(*)                     AS tickets,
                       COUNT(DISTINCT t.priority)    AS priorities,
                       COUNT(DISTINCT t.category_id) AS sub_categories,
                       COUNT(DISTINCT c.parent_id)   AS categories,
                       COUNT(DISTINCT t.status)      AS statuses,
                       COUNT(DISTINCT t.customer_id) AS customers
                FROM tickets t JOIN ticket_categories c ON c.id = t.category_id
                WHERE t.ticket_number BETWEEN 2010 AND 2039
                """);

        assertThat(((Number) seed.get("tickets")).intValue()).isEqualTo(30);
        assertThat(((Number) seed.get("priorities")).intValue()).isEqualTo(4);
        assertThat(((Number) seed.get("sub_categories")).intValue()).isGreaterThanOrEqualTo(15);
        assertThat(((Number) seed.get("categories")).intValue()).isEqualTo(7);
        assertThat(((Number) seed.get("statuses")).intValue()).isGreaterThanOrEqualTo(6);
        assertThat(((Number) seed.get("customers")).intValue()).isEqualTo(10);
    }


    private ResultActions suggestion(String code) throws Exception
    {
        return mockMvc.perform(get("/api/tickets/categories/" + code + "/suggested-priority")
                .header(HttpHeaders.AUTHORIZATION, agent));
    }

    private String tokenFor(Role role)
    {
        String username = "prio_" + role.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);
        String mobile = String.format("+2095%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        User user = userRepository.save(new User(username, "$2a$10$unused", "Priority Test",
                username + "@test.local", mobile, role));
        return "Bearer " + jwtService.generateToken(user).token();
    }

    private ResultActions postJson(String url, String token, String body) throws Exception
    {
        return mockMvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
