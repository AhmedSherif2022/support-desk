package com.supportdesk.ticket.controller;

import com.supportdesk.ticket.dto.PrioritySuggestionDto;
import com.supportdesk.ticket.service.PrioritySuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('AGENT')")
@RequiredArgsConstructor
public class PrioritySuggestionController implements PrioritySuggestionApi
{
    private final PrioritySuggestionService suggestionService;

    @Override
    public PrioritySuggestionDto suggestedPriority(String code)
    {
        return suggestionService.suggest(code);
    }
}
