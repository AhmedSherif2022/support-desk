package com.supportdesk.ticket.service;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.ticket.dto.CategoryAdminDtos.CategoryDto;
import com.supportdesk.ticket.dto.CategoryAdminDtos.CreateCategoryRequest;
import com.supportdesk.ticket.dto.CategoryAdminDtos.CreateSubCategoryRequest;
import com.supportdesk.ticket.dto.CategoryAdminDtos.SubCategoryDto;
import com.supportdesk.ticket.entity.TicketCategory;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.exception.CategoryNotFoundException;
import com.supportdesk.ticket.repository.TicketCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class CategoryAdminServiceTest
{
    private final TicketCategoryRepository repository = mock(TicketCategoryRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private final CategoryAdminService service = new CategoryAdminService(repository, auditService);

    private TicketCategory payments;

    @BeforeEach
    void setUp()
    {
        payments = category(1L, "PAYMENTS", "Payments", null, TicketPriority.HIGH);
        when(repository.save(any(TicketCategory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.findByCodeIgnoreCase("PAYMENTS")).thenReturn(Optional.of(payments));
    }

    @Test
    void newCategoryIsSavedUpperCaseAndAudited()
    {
        CategoryDto created = service.createCategory(new CreateCategoryRequest(" cards ", " Cards ", TicketPriority.MEDIUM));

        assertThat(created.code()).isEqualTo("CARDS");
        assertThat(created.name()).isEqualTo("Cards");
        assertThat(created.subCategories()).isEmpty();
        verify(auditService).recordForCurrentUser(eq(AuditAction.CATEGORY_CREATED), eq("TICKET_CATEGORY"), eq("CARDS"),
                isNull(), any());
    }

    @Test
    void aTakenCodeIsRefusedAndNothingIsSaved()
    {
        when(repository.existsByCodeIgnoreCase("PAYMENTS")).thenReturn(true);

        assertThatThrownBy(() -> service.createCategory(new CreateCategoryRequest("payments", "Pay", TicketPriority.HIGH)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("CATEGORY_CODE_TAKEN");
        verify(repository, never()).save(any());
    }

    @Test
    void subCategoryUsesTheParentsPriorityWhenNoneIsGiven()
    {
        SubCategoryDto sub = service.createSubCategory("PAYMENTS", new CreateSubCategoryRequest(
                "wallet_failed", "Wallet Failed", null, true, "^TRX-\\d{4}-\\d{5}$", "Use TRX-2026-00981", "wallet_err"));

        assertThat(sub.code()).isEqualTo("WALLET_FAILED");
        assertThat(sub.parentCode()).isEqualTo("PAYMENTS");
        assertThat(sub.defaultPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(sub.referenceRequired()).isTrue();
        assertThat(sub.defaultErrorCode()).isEqualTo("WALLET_ERR");
        verify(auditService).recordForCurrentUser(eq(AuditAction.CATEGORY_CREATED), anyString(), eq("WALLET_FAILED"),
                isNull(), any());
    }

    @Test
    void subCategoriesCannotHaveSubCategories()
    {
        TicketCategory paymentFailed = category(2L, "PAYMENT_FAILED", "Payment Failed", payments, TicketPriority.HIGH);
        when(repository.findByCodeIgnoreCase("PAYMENT_FAILED")).thenReturn(Optional.of(paymentFailed));

        assertThatThrownBy(() -> service.createSubCategory("PAYMENT_FAILED",
                new CreateSubCategoryRequest("X1", "X", null, null, null, null, null)))
                .extracting("code").isEqualTo("NOT_A_TOP_CATEGORY");
    }

    @Test
    void unknownParentIsCategoryNotFound()
    {
        assertThatThrownBy(() -> service.subCategories("NOPE")).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void inactiveParentAndBrokenPatternAreRefused()
    {
        ReflectionTestUtils.setField(payments, "active", false);
        assertThatThrownBy(() -> service.createSubCategory("PAYMENTS",
                new CreateSubCategoryRequest("X1", "X", null, null, null, null, null)))
                .extracting("code").isEqualTo("CATEGORY_INACTIVE");

        ReflectionTestUtils.setField(payments, "active", true);
        assertThatThrownBy(() -> service.createSubCategory("PAYMENTS",
                new CreateSubCategoryRequest("X1", "X", null, true, "^TRX-(", null, null)))
                .extracting("code").isEqualTo("INVALID_REFERENCE_PATTERN");
        verify(repository, never()).save(any());
    }

    @Test
    void treeGroupsSubCategoriesUnderTheirParent()
    {
        TicketCategory otp = category(3L, "OTP", "OTP", null, TicketPriority.MEDIUM);
        when(repository.findByParentIsNullOrderByNameAsc()).thenReturn(List.of(otp, payments));
        when(repository.findByParentIsNotNullOrderByNameAsc()).thenReturn(List.of(
                category(4L, "PAYMENT_FAILED", "Payment Failed", payments, TicketPriority.HIGH)));

        List<CategoryDto> tree = service.all();

        assertThat(tree).extracting(CategoryDto::code).containsExactly("OTP", "PAYMENTS");
        assertThat(tree.get(0).subCategories()).isEmpty();
        assertThat(tree.get(1).subCategories()).extracting(SubCategoryDto::code).containsExactly("PAYMENT_FAILED");
    }


    private static TicketCategory category(Long id, String code, String name, TicketCategory parent, TicketPriority p)
    {
        TicketCategory c = new TicketCategory(code, name, parent, p, false, null, null, null);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
