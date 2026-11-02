package com.supportdesk.ticket.entity;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "ticket_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketCategory extends BaseEntity
{


    @Column(nullable = false, unique = true, length = 50)
    private String code;


    @Column(nullable = false, length = 100)
    private String name;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private TicketCategory parent;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TicketPriority defaultPriority;


    @Column(nullable = false)
    private boolean referenceRequired;


    @Column(length = 100)
    private String referencePattern;


    @Column(length = 150)
    private String referenceHint;


    @Column(length = 40)
    private String defaultErrorCode;


    @Column(nullable = false)
    private boolean active = true;

    public TicketCategory(String code, String name, TicketCategory parent, TicketPriority defaultPriority,
                          boolean referenceRequired, String referencePattern, String referenceHint,
                          String defaultErrorCode)
    {
        this.code = code;
        this.name = name;
        this.parent = parent;
        this.defaultPriority = defaultPriority;
        this.referenceRequired = referenceRequired;
        this.referencePattern = referencePattern;
        this.referenceHint = referenceHint;
        this.defaultErrorCode = defaultErrorCode;
    }


    public boolean isSubCategory()
    {
        return parent != null;
    }

    public String fullName()
    {
        return parent == null ? name : parent.getName() + " / " + name;
    }
}
