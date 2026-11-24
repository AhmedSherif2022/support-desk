package com.supportdesk.health.controller;
import com.supportdesk.health.dto.ServiceHealthDtos.ServiceHealthResponse;
import com.supportdesk.health.service.ServiceHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('AGENT')")
@RequiredArgsConstructor
public class ServiceHealthController implements ServiceHealthApi
{
    private final ServiceHealthService serviceHealthService;

    @Override
    public ServiceHealthResponse panel()
    {
        return serviceHealthService.panel();
    }
}
