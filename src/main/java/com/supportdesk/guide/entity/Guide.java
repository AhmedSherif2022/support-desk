package com.supportdesk.guide.entity;

import com.supportdesk.common.entity.BaseEntity;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.guide.enums.GuideMatchType;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.entity.TicketCategory;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "guides")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Guide extends BaseEntity
{
    @Column(nullable = false, unique = true, length = 150)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private TicketCategory category;

    @ElementCollection
    @CollectionTable(name = "guide_platforms", joinColumns = @JoinColumn(name = "guide_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private Set<Platform> platforms = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "guide_error_codes", joinColumns = @JoinColumn(name = "guide_id"))
    @Column(name = "error_code", nullable = false, length = 40)
    private Set<String> errorCodes = new HashSet<>();

    public Optional<GuideMatchType> matchFor(Ticket ticket)
    {
        if (!platforms.contains(ticket.getPlatform())) {
            return Optional.empty();
        }
        if (errorCodes.contains(ticket.customerErrorCode())) {
            return Optional.of(GuideMatchType.ERROR_CODE);
        }
        return category.getId().equals(ticket.getCategory().getId())
                ? Optional.of(GuideMatchType.CATEGORY) : Optional.empty();
    }
}
