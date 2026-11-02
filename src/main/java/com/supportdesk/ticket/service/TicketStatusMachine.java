package com.supportdesk.ticket.service;
import com.supportdesk.ticket.entity.TicketStatus;
import com.supportdesk.ticket.exception.InvalidStatusTransitionException;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.supportdesk.ticket.entity.TicketStatus.*;


@Component
public class TicketStatusMachine
{

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED = new EnumMap<>(TicketStatus.class);


    private static final Set<TicketStatus> SYSTEM_ONLY = EnumSet.of(ESCALATED, WITH_SRE, SRE_FIXED);

    static {
        ALLOWED.put(NEW,                 EnumSet.of(TROUBLESHOOTING, WAITING_ON_CUSTOMER, ESCALATED, RESOLVED));
        ALLOWED.put(TROUBLESHOOTING,     EnumSet.of(WAITING_ON_CUSTOMER, ESCALATED, RESOLVED));
        ALLOWED.put(WAITING_ON_CUSTOMER, EnumSet.of(TROUBLESHOOTING, ESCALATED, RESOLVED));
        ALLOWED.put(ESCALATED,           EnumSet.of(WITH_SRE, SRE_FIXED));
        ALLOWED.put(WITH_SRE,            EnumSet.of(SRE_FIXED));
        ALLOWED.put(SRE_FIXED,           EnumSet.of(RESOLVED, WITH_SRE));   // confirmed / still not working
        ALLOWED.put(RESOLVED,            EnumSet.of(REOPENED, CLOSED));
        ALLOWED.put(REOPENED,            EnumSet.of(TROUBLESHOOTING, WAITING_ON_CUSTOMER, ESCALATED, RESOLVED));
        ALLOWED.put(CLOSED,              EnumSet.noneOf(TicketStatus.class)); // final
    }


    public boolean canMove(TicketStatus from, TicketStatus to)
    {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }


    public void check(TicketStatus from, TicketStatus to)
    {
        if (!canMove(from, to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }


    public boolean isAgentChoice(TicketStatus to)
    {
        return !SYSTEM_ONLY.contains(to);
    }


    public Set<TicketStatus> agentChoicesFrom(TicketStatus from)
    {
        Set<TicketStatus> result = EnumSet.noneOf(TicketStatus.class);
        for (TicketStatus to : ALLOWED.getOrDefault(from, Set.of())) {
            if (isAgentChoice(to)) {
                result.add(to);
            }
        }
        return Collections.unmodifiableSet(result);
    }
}
