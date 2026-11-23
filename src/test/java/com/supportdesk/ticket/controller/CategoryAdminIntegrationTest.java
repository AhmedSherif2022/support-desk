package com.supportdesk.ticket.controller;

import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.entity.AuditLog;
import com.supportdesk.audit.repository.AuditLogRepository;
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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Category admin API against the real database: the Guide Admin adds a category and a sub-category, both GETs show
 * them, CATEGORY_CREATED is written (so migration V20 widened the audit check), and an agent can open a ticket in
 * the new sub-category. Codes are random so the test can run again on the same database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CategoryAdminIntegrationTest
{
    private static final String URL = "/api/admin/categories";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private JwtService jwtService;

    private String admin;
    private String agent;
    private String category;
    private String sub;
    private long baseline;

    @BeforeEach
    void setUp()
    {
        admin = tokenFor(Role.GUIDE_ADMIN);
        agent = tokenFor(Role.AGENT);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        category = "IT_CAT_" + suffix;
        sub = "IT_SUB_" + suffix;
        baseline = auditLogRepository.findAll().stream().mapToLong(AuditLog::getId).max().orElse(0);
    }

    @Test
    void postAndGetCategoriesAndSubCategoriesEndToEnd() throws Exception
    {
        // POST a category (lower-case code is stored upper-case)
        postJson(URL, admin, "{\"code\":\"" + category.toLowerCase() + "\",\"name\":\"IT Cards\",\"defaultPriority\":\"MEDIUM\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(category))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.subCategories").isEmpty());

        // GET its sub-categories: none yet
        getAs(URL + "/" + category + "/subcategories", admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // POST a sub-category without a priority: it takes the parent's
        postJson(URL + "/" + category + "/subcategories", admin,
                "{\"code\":\"" + sub + "\",\"name\":\"IT Card Blocked\",\"defaultErrorCode\":\"it_card_err\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(sub))
                .andExpect(jsonPath("$.parentCode").value(category))
                .andExpect(jsonPath("$.defaultPriority").value("MEDIUM"))
                .andExpect(jsonPath("$.referenceRequired").value(false))
                .andExpect(jsonPath("$.defaultErrorCode").value("IT_CARD_ERR"));

        // GET the sub-categories and the whole tree
        getAs(URL + "/" + category + "/subcategories", admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value(sub));
        getAs(URL, admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem(category)))
                .andExpect(jsonPath("$[?(@.code == '" + category + "')].subCategories[0].code").value(sub))
                .andExpect(jsonPath("$[?(@.code == 'PAYMENTS')].subCategories[*].code", hasItem("PAYMENT_FAILED")));

        // both creations are audited (this insert fails if V20 did not widen the audit check)
        List<AuditLog> audits = auditLogRepository.findAll().stream()
                .filter(a -> a.getId() > baseline && a.getAction() == AuditAction.CATEGORY_CREATED)
                .toList();
        assertThat(audits).extracting(AuditLog::getEntityId).containsExactlyInAnyOrder(category, sub);
        assertThat(audits).extracting(AuditLog::getEntity).containsOnly("TICKET_CATEGORY");
    }

    @Test
    void agentsSeeTheNewSubCategoryAndCanOpenATicketInIt() throws Exception
    {
        postJson(URL, admin, "{\"code\":\"" + category + "\",\"name\":\"IT Cards\",\"defaultPriority\":\"LOW\"}")
                .andExpect(status().isCreated());
        postJson(URL + "/" + category + "/subcategories", admin,
                "{\"code\":\"" + sub + "\",\"name\":\"IT Card Blocked\",\"defaultPriority\":\"HIGH\"}")
                .andExpect(status().isCreated());

        getAs("/api/tickets/categories", agent)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == '" + sub + "')].parentCode").value(category))
                .andExpect(jsonPath("$[?(@.code == '" + sub + "')].defaultPriority").value("HIGH"));

        // verify the caller by the CIF number alone, then open a ticket in the new sub-category
        postJson("/api/customers/100101/verify", agent, "{\"passedChecks\":[\"DATE_OF_BIRTH\",\"MOBILE_ON_FILE\"]}")
                .andExpect(status().isOk());
        postJson("/api/tickets", agent, "{\"cif\":\"100101\",\"categoryCode\":\"" + sub + "\",\"priority\":\"HIGH\","
                + "\"description\":\"Card blocked after 3 wrong PINs.\",\"platform\":\"MOBILE\",\"allowDuplicate\":true}")
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.customerCif").value("CIF-100101"));
    }

    @Test
    void wrongRequestsAreRefusedWithTheRightCodes() throws Exception
    {
        postJson(URL, admin, "{\"code\":\"payments\",\"name\":\"Payments again\",\"defaultPriority\":\"HIGH\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_CODE_TAKEN"));
        postJson(URL + "/PAYMENT_FAILED/subcategories", admin, "{\"code\":\"" + sub + "\",\"name\":\"Third level\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NOT_A_TOP_CATEGORY"));
        getAs(URL + "/NO_SUCH_CATEGORY/subcategories", admin)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
        postJson(URL + "/PAYMENTS/subcategories", admin,
                "{\"code\":\"" + sub + "\",\"name\":\"Bad pattern\",\"referenceRequired\":true,\"referencePattern\":\"^TRX-(\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_REFERENCE_PATTERN"));
        postJson(URL, admin, "{\"code\":\"1 bad\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        postJson(URL, agent, "{\"code\":\"" + category + "\",\"name\":\"x\",\"defaultPriority\":\"LOW\"}")
                .andExpect(status().isForbidden());
        getAs(URL, agent).andExpect(status().isForbidden());
    }


    private String tokenFor(Role role)
    {
        String username = "cat_" + role.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);
        String mobile = String.format("+2097%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        User user = userRepository.save(new User(username, "$2a$10$unused", "Category Test",
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
