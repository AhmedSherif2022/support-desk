package com.supportdesk.health.controller;
import com.supportdesk.health.dto.ServiceHealthDtos.ServiceHealthSettingsDto;
import com.supportdesk.health.dto.ServiceHealthDtos.UpdateServiceHealthSettingsRequest;
import com.supportdesk.health.service.ServiceHealthSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('GUIDE_ADMIN')")
@RequiredArgsConstructor
public class ServiceHealthSettingsAdminController implements ServiceHealthSettingsAdminApi
{
    private final ServiceHealthSettingsService settingsService;

    @Override
    public ServiceHealthSettingsDto current()
    {
        return settingsService.current();
    }

    @Override
    public ServiceHealthSettingsDto update(UpdateServiceHealthSettingsRequest request)
    {
        return settingsService.update(request);
    }
}
