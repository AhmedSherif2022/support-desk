package com.supportdesk.ticket.dto;

import com.supportdesk.ticket.entity.TicketCategory;
import com.supportdesk.ticket.enums.TicketPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Guide Admin: create and list ticket categories (top level) and their sub-categories. */
public final class CategoryAdminDtos
{
    private static final String CODE_RULE = "^[A-Za-z][A-Za-z0-9_]{1,49}$";
    private static final String CODE_MESSAGE = "Use 2 to 50 letters, digits or _, starting with a letter.";

    private CategoryAdminDtos()
    {
    }

    public record CreateCategoryRequest(
            @Schema(description = "Unique code; stored upper-case.", example = "CARDS")
            @NotBlank @Pattern(regexp = CODE_RULE, message = CODE_MESSAGE) String code,
            @Schema(example = "Cards") @NotBlank @Size(max = 100) String name,
            @Schema(description = "Default priority of its sub-categories when they do not set one.", example = "MEDIUM")
            @NotNull TicketPriority defaultPriority)
    {
    }

    public record CreateSubCategoryRequest(
            @Schema(description = "Unique code across all categories; stored upper-case.", example = "CARD_BLOCKED")
            @NotBlank @Pattern(regexp = CODE_RULE, message = CODE_MESSAGE) String code,
            @Schema(example = "Card Blocked") @NotBlank @Size(max = 100) String name,
            @Schema(description = "Leave empty to use the parent category's priority.", example = "HIGH",
                    types = {"string", "null"}) TicketPriority defaultPriority,
            @Schema(description = "True when a ticket in this sub-category needs a transaction reference (TRX-...). "
                    + "Default false.", example = "false", types = {"boolean", "null"}) Boolean referenceRequired,
            @Schema(description = "Regular expression the reference must match.", example = "^TRX-\\d{4}-\\d{5}$",
                    types = {"string", "null"}) @Size(max = 100) String referencePattern,
            @Schema(description = "Shown to the agent when the reference does not match.",
                    example = "Use the format TRX-2026-00981", types = {"string", "null"}) @Size(max = 150) String referenceHint,
            @Schema(description = "Error code pre-filled on new tickets.", example = "CARD_BLOCKED_ERR",
                    types = {"string", "null"})
            @Size(max = 40) @Pattern(regexp = "^[A-Za-z0-9_]*$", message = "Use letters, digits or _.") String defaultErrorCode)
    {
    }

    public record SubCategoryDto(
            @Schema(example = "CARD_BLOCKED") String code,
            @Schema(example = "Card Blocked") String name,
            @Schema(example = "CARDS") String parentCode,
            @Schema(example = "HIGH") TicketPriority defaultPriority,
            @Schema(example = "false") boolean referenceRequired,
            @Schema(types = {"string", "null"}) String referencePattern,
            @Schema(types = {"string", "null"}) String referenceHint,
            @Schema(types = {"string", "null"}) String defaultErrorCode,
            @Schema(description = "Inactive ones are hidden from agents.", example = "true") boolean active)
    {
        public static SubCategoryDto from(TicketCategory c)
        {
            return new SubCategoryDto(c.getCode(), c.getName(), c.getParent().getCode(), c.getDefaultPriority(),
                    c.isReferenceRequired(), c.getReferencePattern(), c.getReferenceHint(), c.getDefaultErrorCode(),
                    c.isActive());
        }
    }

    public record CategoryDto(
            @Schema(example = "CARDS") String code,
            @Schema(example = "Cards") String name,
            @Schema(example = "MEDIUM") TicketPriority defaultPriority,
            @Schema(description = "Inactive ones (and their sub-categories) are hidden from agents.", example = "true")
            boolean active,
            @Schema(description = "Sorted by name. A ticket always uses one of these.") List<SubCategoryDto> subCategories)
    {
        public static CategoryDto from(TicketCategory c, List<SubCategoryDto> subCategories)
        {
            return new CategoryDto(c.getCode(), c.getName(), c.getDefaultPriority(), c.isActive(), subCategories);
        }
    }
}
