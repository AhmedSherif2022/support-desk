package com.supportdesk.azure.config;

import com.supportdesk.azure.client.AzureDevOpsClient;
import com.supportdesk.azure.client.FakeAzureDevOpsClient;
import com.supportdesk.azure.client.OffAzureDevOpsClient;
import com.supportdesk.azure.client.RestAzureDevOpsClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Picks the Azure DevOps client from supportdesk.azure.mode (OFF, FAKE or REST). The HTTP client is only built in
 * REST mode.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AzureDevOpsProperties.class)
public class AzureDevOpsConfig
{
    @Bean
    public AzureDevOpsClient azureDevOpsClient(AzureDevOpsProperties props,
                                               ObjectProvider<RestClient.Builder> restClientBuilder)
    {
        log.info("Azure DevOps integration mode: {}", props.mode());
        return switch (props.mode()) {
            case REST -> new RestAzureDevOpsClient(props, restClientBuilder.getObject());
            case FAKE -> new FakeAzureDevOpsClient();
            case OFF -> new OffAzureDevOpsClient();
        };
    }
}
