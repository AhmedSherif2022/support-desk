package com.supportdesk.monitor.service;
import com.supportdesk.monitor.entity.ErrorRule;
import com.supportdesk.monitor.repository.ErrorRuleRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ErrorRuleServiceTest
{
    private final ErrorRuleRepository ruleRepository = mock(ErrorRuleRepository.class);
    private final ErrorRuleService service = new ErrorRuleService(ruleRepository);

    @Test
    void findsTheRuleWhateverTheCaseAndSpaces()
    {
        ErrorRule rule = mock(ErrorRule.class);
        when(ruleRepository.findByErrorCodeAndActiveTrue("ERR_AUTH_503")).thenReturn(Optional.of(rule));

        assertThat(service.ruleFor("  err_auth_503 ")).contains(rule);
    }

    @Test
    void unknownCodeHasNoRule()
    {
        when(ruleRepository.findByErrorCodeAndActiveTrue("ERR_NEW_999")).thenReturn(Optional.empty());

        assertThat(service.ruleFor("ERR_NEW_999")).isEmpty();
    }

    @Test
    void blankOrMissingCodeNeverReachesTheDatabase()
    {
        assertThat(service.ruleFor(null)).isEmpty();
        assertThat(service.ruleFor("   ")).isEmpty();

        verify(ruleRepository, never()).findByErrorCodeAndActiveTrue(anyString());
    }
}
