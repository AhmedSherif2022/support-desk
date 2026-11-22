package com.supportdesk.monitor.service;
import com.supportdesk.monitor.entity.ErrorRule;
import com.supportdesk.monitor.repository.ErrorRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class ErrorRuleService
{
    private final ErrorRuleRepository ruleRepository;


    @Transactional(readOnly = true)
    public Optional<ErrorRule> ruleFor(String errorCode)
    {
        if (errorCode == null || errorCode.isBlank()) {
            return Optional.empty();
        }
        return ruleRepository.findByErrorCodeAndActiveTrue(errorCode.trim().toUpperCase(Locale.ROOT));
    }


    @Transactional(readOnly = true)
    public List<ErrorRule> all()
    {
        return ruleRepository.findAllByOrderByErrorCodeAsc();
    }
}
