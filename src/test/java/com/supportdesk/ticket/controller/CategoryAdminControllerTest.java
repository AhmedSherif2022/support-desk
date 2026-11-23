package com.supportdesk.ticket.controller;
import com.supportdesk.auth.config.SecurityConfig;
import com.supportdesk.auth.enums.Role;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.security.AuthenticatedUser;
import com.supportdesk.auth.security.SecurityErrorWriter;
import com.supportdesk.auth.service.JwtService;
import com.supportdesk.common.config.ClockConfig;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.ticket.dto.CategoryAdminDtos.CategoryDto;
import com.supportdesk.ticket.dto.CategoryAdminDtos.SubCategoryDto;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.service.CategoryAdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Category admin API through the real SecurityConfig: the Guide Admin lists and adds categories; others cannot. */
@WebMvcTest(CategoryAdminController.class)
@Import({SecurityConfig.class, ClockConfig.class, SecurityErrorWriter.class})
class CategoryAdminControllerTest
{
    private static final String URL = "/api/admin/categories";
    private static final SubCategoryDto CARD_BLOCKED = new SubCategoryDto("CARD_BLOCKED", "Card Blocked", "CARDS",
            TicketPriority.HIGH, false, null, null, null, true);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryAdminService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;


    @Test
    void guideAdminSeesTheTree() throws Exception
    {
        when(service.all()).thenReturn(List.of(
                new CategoryDto("CARDS", "Cards", TicketPriority.MEDIUM, true, List.of(CARD_BLOCKED))));

        mockMvc.perform(get(URL).with(loggedInAs(Role.GUIDE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CARDS"))
                .andExpect(jsonPath("$[0].subCategories[0].code").value("CARD_BLOCKED"))
                .andExpect(jsonPath("$[0].subCategories[0].parentCode").value("CARDS"));
    }

    @Test
    void guideAdminAddsACategoryAndGets201() throws Exception
    {
        when(service.createCategory(any()))
                .thenReturn(new CategoryDto("CARDS", "Cards", TicketPriority.MEDIUM, true, List.of()));

        mockMvc.perform(post(URL).with(loggedInAs(Role.GUIDE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"cards\",\"name\":\"Cards\",\"defaultPriority\":\"MEDIUM\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CARDS"))
                .andExpect(jsonPath("$.subCategories").isEmpty());
    }

    @Test
    void aCategoryNeedsCodeNameAndPriority() throws Exception
    {
        mockMvc.perform(post(URL).with(loggedInAs(Role.GUIDE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"1bad code\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.code").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.defaultPriority").exists());

        verify(service, never()).createCategory(any());
    }

    @Test
    void guideAdminListsAndAddsSubCategories() throws Exception
    {
        when(service.subCategories("CARDS")).thenReturn(List.of(CARD_BLOCKED));
        when(service.createSubCategory(eq("CARDS"), any())).thenReturn(CARD_BLOCKED);

        mockMvc.perform(get(URL + "/CARDS/subcategories").with(loggedInAs(Role.GUIDE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CARD_BLOCKED"));

        mockMvc.perform(post(URL + "/CARDS/subcategories").with(loggedInAs(Role.GUIDE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"CARD_BLOCKED\",\"name\":\"Card Blocked\",\"defaultPriority\":\"HIGH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentCode").value("CARDS"))
                .andExpect(jsonPath("$.defaultPriority").value("HIGH"));
    }

    @Test
    void aTakenCodeIsAConflict() throws Exception
    {
        when(service.createCategory(any())).thenThrow(new BusinessRuleException(ErrorCode.CATEGORY_CODE_TAKEN));

        mockMvc.perform(post(URL).with(loggedInAs(Role.GUIDE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"PAYMENTS\",\"name\":\"Payments\",\"defaultPriority\":\"HIGH\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_CODE_TAKEN"));
    }

    @ParameterizedTest(name = "{0} cannot manage categories")
    @EnumSource(value = Role.class, names = {"AGENT", "REVIEWER"})
    void otherRolesAreForbidden(Role role) throws Exception
    {
        mockMvc.perform(get(URL).with(loggedInAs(role)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(URL).with(loggedInAs(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"CARDS\",\"name\":\"Cards\",\"defaultPriority\":\"MEDIUM\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(service, never()).createCategory(any());
    }

    @Test
    void categoriesNeedALogin() throws Exception
    {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }


    private static RequestPostProcessor loggedInAs(Role role)
    {
        var badge = new AuthenticatedUser(1L, role.name().toLowerCase(), role);
        return authentication(new UsernamePasswordAuthenticationToken(
                badge, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }
}
