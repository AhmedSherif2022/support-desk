package com.supportdesk.ticket.controller;

import com.supportdesk.common.exception.ApiError;
import com.supportdesk.ticket.dto.CategoryAdminDtos.CategoryDto;
import com.supportdesk.ticket.dto.CategoryAdminDtos.CreateCategoryRequest;
import com.supportdesk.ticket.dto.CategoryAdminDtos.CreateSubCategoryRequest;
import com.supportdesk.ticket.dto.CategoryAdminDtos.SubCategoryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

/**
 * HTTP contract for managing ticket categories and sub-categories (Guide Admin).
 * {@link CategoryAdminController} implements this and delegates. Agents keep using GET /api/tickets/categories.
 */
@RequestMapping("/api/admin/categories")
@Tag(name = "Ticket categories (admin)", description = "Guide Admin: list and add ticket categories and their "
        + "sub-categories. Two levels only; a ticket always uses a sub-category. New ones appear at once in "
        + "GET /api/tickets/categories for agents.")
public interface CategoryAdminApi
{
    @GetMapping
    @Operation(summary = "All categories with their sub-categories",
            description = "Active and inactive, sorted by name; each category lists its sub-categories.")
    @ApiResponse(responseCode = "200", description = "The category tree")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not a GUIDE_ADMIN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    List<CategoryDto> all();

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a top-level category",
            description = "The code is stored upper-case and must be unique across categories and sub-categories. "
                    + "Add at least one sub-category before agents can use it. Audited as CATEGORY_CREATED.")
    @ApiResponse(responseCode = "201", description = "The new category (no sub-categories yet)")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: code, name or defaultPriority missing or "
            + "badly formed (fieldErrors says which)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not a GUIDE_ADMIN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "CATEGORY_CODE_TAKEN: the code is already used",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    CategoryDto createCategory(@Valid @RequestBody CreateCategoryRequest request);

    @GetMapping("/{code}/subcategories")
    @Operation(summary = "Sub-categories of one category", description = "Active and inactive, sorted by name.")
    @ApiResponse(responseCode = "200", description = "The sub-categories (may be empty)")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not a GUIDE_ADMIN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "CATEGORY_NOT_FOUND: unknown code; NOT_A_TOP_CATEGORY: the code "
            + "is itself a sub-category",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    List<SubCategoryDto> subCategories(@PathVariable String code);

    @PostMapping("/{code}/subcategories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a sub-category under a category",
            description = "Agents can pick it for new tickets right away. Leave defaultPriority empty to use the "
                    + "parent's. With referenceRequired, tickets need a TRX-... reference; referencePattern (a regular "
                    + "expression) and referenceHint are optional. Audited as CATEGORY_CREATED.")
    @ApiResponse(responseCode = "201", description = "The new sub-category")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: code or name missing or badly formed",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not a GUIDE_ADMIN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "CATEGORY_CODE_TAKEN: the code is already used",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "CATEGORY_NOT_FOUND, NOT_A_TOP_CATEGORY, CATEGORY_INACTIVE "
            + "(the parent is not used any more) or INVALID_REFERENCE_PATTERN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    SubCategoryDto createSubCategory(@PathVariable String code, @Valid @RequestBody CreateSubCategoryRequest request);
}
