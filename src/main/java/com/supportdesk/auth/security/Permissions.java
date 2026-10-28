package com.supportdesk.auth.security;
import com.supportdesk.auth.entity.Role;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class Permissions
{

    private Permissions()
    {
    }

    public record Rule(String pattern, Set<Role> roles)
    {
        public String[] roleNames()
        {
            return roles.stream().map(Enum::name).toArray(String[]::new);
        }
    }



    public static final List<Rule> RULES = List.of(
            new Rule("/api/admin/**",     Set.of(Role.GUIDE_ADMIN)),
            new Rule("/api/review/**",    Set.of(Role.REVIEWER)),
            new Rule("/api/audit/**",     Set.of(Role.GUIDE_ADMIN, Role.REVIEWER)),
            new Rule("/api/customers/**", Set.of(Role.AGENT)),
            new Rule("/api/tickets/**",   Set.of(Role.AGENT)),
            new Rule("/api/queue/**",     Set.of(Role.AGENT))

            // Add when these packages are built (agree the URLs with the team):
            // , new Rule("/api/guide-runs/**",  Set.of(Role.AGENT))    // E4: run a guide on a call
            // , new Rule("/api/escalations/**", Set.of(Role.AGENT))    // E5: escalate, ask SRE for update
            // , new Rule("/api/incidents/**",   Set.of(Role.AGENT))    // E6: incident call list
    );

    public static final List<String> PUBLIC_POST = List.of(
            "/api/auth/login",
            "/api/auth/refresh");


    public static final List<String> PUBLIC_ANY = List.of(
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/error");


    private static final Map<Role, Set<String>> SCREENS = new EnumMap<>(Map.of(
            Role.AGENT,       Set.of("HOME", "CUSTOMER_SEARCH", "MY_TICKETS", "NOTIFICATIONS"),
            Role.GUIDE_ADMIN, Set.of("GUIDE_LIBRARY", "ERROR_RULES", "TRIAGE", "AGENT_FEEDBACK",
                    "GUIDE_ANALYTICS", "AUDIT_LOG", "NOTIFICATIONS"),
            Role.REVIEWER,    Set.of("REVIEW_QUEUE", "AUDIT_LOG", "NOTIFICATIONS")));

    public static Set<String> screensFor(Role role)
    {
        return SCREENS.getOrDefault(role, Set.of());
    }

    public static boolean isAllowed(Rule rule, Role role)
    {
        return rule.roles().contains(role);
    }
}
