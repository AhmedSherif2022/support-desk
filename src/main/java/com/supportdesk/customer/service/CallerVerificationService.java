package com.supportdesk.customer.service;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.service.CurrentUserService;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.customer.dto.CustomerDtos.VerificationStatusDto;
import com.supportdesk.customer.entity.CallerVerification;
import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.entity.VerificationCheck;
import com.supportdesk.customer.exception.CallerNotVerifiedException;
import com.supportdesk.customer.repository.CallerVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class CallerVerificationService
{
    static final int MIN_CHECKS = 2;                                   // E2 rule
    static final Duration VALID_FOR = Duration.ofMinutes(30);          // safety limit if the call is not ended

    private final CallerVerificationRepository verificationRepository;
    private final CustomerLookupService customerLookup;
    private final CurrentUserService currentUserService;
    private final Clock clock;


    @Transactional
    public VerificationStatusDto verify(String cif, Set<VerificationCheck> passedChecks)
    {
        if (passedChecks == null || passedChecks.size() < MIN_CHECKS) {
            throw new BusinessRuleException(ErrorCode.TOO_FEW_CHECKS);
        }
        Customer customer = customerLookup.getByCif(cif);
        User agent = currentUserService.getCurrentUserEntity();
        Instant now = clock.instant();

        endActive(agent.getId(), customer, now);                       // only one active per agent + customer
        CallerVerification verification = verificationRepository.save(
                new CallerVerification(customer, agent, passedChecks, now, now.plus(VALID_FOR)));

        return toDto(customer, verification);
    }


    @Transactional
    public void endCall(String cif)
    {
        Customer customer = customerLookup.getByCif(cif);
        endActive(currentUserService.getCurrentUserId(), customer, clock.instant());
    }


    @Transactional(readOnly = true)
    public VerificationStatusDto getStatus(String cif)
    {
        Customer customer = customerLookup.getByCif(cif);
        return findActive(currentUserService.getCurrentUserId(), customer)
                .map(v -> toDto(customer, v))
                .orElse(new VerificationStatusDto(customer.getCif(), false, Set.of(), null, null));
    }


    @Transactional(readOnly = true)
    public CallerVerification requireVerified(Customer customer)
    {
        return findActive(currentUserService.getCurrentUserId(), customer)
                .orElseThrow(CallerNotVerifiedException::new);
    }



    private Optional<CallerVerification> findActive(Long agentId, Customer customer)
    {
        return verificationRepository
                .findFirstByAgentIdAndCustomerIdAndEndedAtIsNullAndExpiresAtAfterOrderByVerifiedAtDesc(
                        agentId, customer.getId(), clock.instant());
    }

    private void endActive(Long agentId, Customer customer, Instant now)
    {
        findActive(agentId, customer).ifPresent(v -> v.end(now));     // saved at the end of the transaction
    }

    private static VerificationStatusDto toDto(Customer customer, CallerVerification v)
    {
        return new VerificationStatusDto(customer.getCif(), true, v.getPassedChecks(),
                v.getVerifiedAt(), v.getExpiresAt());
    }
}
