package com.supportdesk.ticket.service;

import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.ticket.dto.CategoryAdminDtos.CategoryDto;
import com.supportdesk.ticket.dto.CategoryAdminDtos.CreateCategoryRequest;
import com.supportdesk.ticket.dto.CategoryAdminDtos.CreateSubCategoryRequest;
import com.supportdesk.ticket.dto.CategoryAdminDtos.SubCategoryDto;
import com.supportdesk.ticket.entity.TicketCategory;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.exception.CategoryNotFoundException;
import com.supportdesk.ticket.repository.TicketCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Guide Admin: list and create ticket categories and sub-categories. Two levels only: a category holds
 * sub-categories, and a ticket always uses a sub-category. Agents keep reading GET /api/tickets/categories.
 */
@Service
@RequiredArgsConstructor
public class CategoryAdminService
{
    static final String ENTITY = "TICKET_CATEGORY";

    private final TicketCategoryRepository categoryRepository;
    private final AuditService auditService;

    /** Every category with its sub-categories, active or not, sorted by name. */
    @Transactional(readOnly = true)
    public List<CategoryDto> all()
    {
        Map<Long, List<SubCategoryDto>> subsByParent = categoryRepository.findByParentIsNotNullOrderByNameAsc().stream()
                .collect(Collectors.groupingBy(c -> c.getParent().getId(), LinkedHashMap::new,
                        Collectors.mapping(SubCategoryDto::from, Collectors.toList())));
        return categoryRepository.findByParentIsNullOrderByNameAsc().stream()
                .map(c -> CategoryDto.from(c, subsByParent.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    @Transactional
    public CategoryDto createCategory(CreateCategoryRequest request)
    {
        String code = requireFreeCode(request.code());
        TicketCategory category = categoryRepository.save(new TicketCategory(code, request.name().trim(), null,
                request.defaultPriority(), false, null, null, null));

        audit(category);
        return CategoryDto.from(category, List.of());
    }

    @Transactional(readOnly = true)
    public List<SubCategoryDto> subCategories(String parentCode)
    {
        TicketCategory parent = topCategory(parentCode);
        return categoryRepository.findByParentIdOrderByNameAsc(parent.getId()).stream()
                .map(SubCategoryDto::from)
                .toList();
    }

    @Transactional
    public SubCategoryDto createSubCategory(String parentCode, CreateSubCategoryRequest request)
    {
        TicketCategory parent = topCategory(parentCode);
        if (!parent.isActive()) {
            throw new BusinessRuleException(ErrorCode.CATEGORY_INACTIVE,
                    "Category " + parent.getCode() + " is no longer used; activate it before adding sub-categories.");
        }
        String code = requireFreeCode(request.code());
        String pattern = blankToNull(request.referencePattern());
        checkPattern(pattern);
        TicketPriority priority = request.defaultPriority() != null ? request.defaultPriority() : parent.getDefaultPriority();
        String errorCode = blankToNull(request.defaultErrorCode());

        TicketCategory sub = categoryRepository.save(new TicketCategory(code, request.name().trim(), parent, priority,
                Boolean.TRUE.equals(request.referenceRequired()), pattern, blankToNull(request.referenceHint()),
                errorCode == null ? null : errorCode.toUpperCase(Locale.ROOT)));

        audit(sub);
        return SubCategoryDto.from(sub);
    }

    /** The category must exist and be top level: sub-categories cannot have their own sub-categories. */
    private TicketCategory topCategory(String code)
    {
        TicketCategory category = categoryRepository.findByCodeIgnoreCase(code == null ? "" : code.trim())
                .orElseThrow(() -> new CategoryNotFoundException(code));
        if (category.isSubCategory()) {
            throw new BusinessRuleException(ErrorCode.NOT_A_TOP_CATEGORY,
                    category.getCode() + " is a sub-category of " + category.getParent().getCode()
                            + "; add sub-categories under a top-level category.");
        }
        return category;
    }

    private String requireFreeCode(String requested)
    {
        String code = requested.trim().toUpperCase(Locale.ROOT);
        if (categoryRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException(ErrorCode.CATEGORY_CODE_TAKEN, "Category code " + code + " already exists.");
        }
        return code;
    }

    private static void checkPattern(String pattern)
    {
        if (pattern == null) {
            return;
        }
        try {
            Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            throw new BusinessRuleException(ErrorCode.INVALID_REFERENCE_PATTERN);
        }
    }

    private void audit(TicketCategory c)
    {
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("code", c.getCode());
        after.put("name", c.getName());
        after.put("parentCode", c.getParent() == null ? null : c.getParent().getCode());
        after.put("defaultPriority", c.getDefaultPriority().name());
        after.put("referenceRequired", c.isReferenceRequired());
        auditService.recordForCurrentUser(AuditAction.CATEGORY_CREATED, ENTITY, c.getCode(), null, after);
    }

    private static String blankToNull(String value)
    {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
