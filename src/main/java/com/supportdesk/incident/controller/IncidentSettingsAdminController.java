package com.supportdesk.incident.controller;
import com.supportdesk.incident.dto.IncidentDtos.IncidentSettingsDto;
import com.supportdesk.incident.dto.IncidentDtos.UpdateIncidentSettingsRequest;
import com.supportdesk.incident.service.IncidentSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('GUIDE_ADMIN')")
@RequiredArgsConstructor
public class IncidentSettingsAdminController implements IncidentSettingsAdminApi
{
    private final IncidentSettingsService settingsService;

    @Override
    public IncidentSettingsDto current()
    {
        return settingsService.current();
    }

    @Override
    public IncidentSettingsDto update(UpdateIncidentSettingsRequest request)
    {
        return settingsService.update(request);
    }
}
