package com.supportdesk.azure.service;

import com.supportdesk.azure.config.AzureDevOpsProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** E5-S1: a masked, stable customer token instead of personal data. */
class CustomerTokenServiceTest
{
    private static CustomerTokenService service(String secret)
    {
        return new CustomerTokenService(new AzureDevOpsProperties(AzureDevOpsProperties.Mode.FAKE, null, null, "Bug",
                null, "http://localhost:4200", secret, null));
    }

    @Test
    void sameCustomerSameTokenOtherCustomerOtherToken()
    {
        CustomerTokenService tokens = service("secret-1");

        assertThat(tokens.tokenFor(7L)).isEqualTo(tokens.tokenFor(7L)).matches("CUST-[0-9A-F]{12}");
        assertThat(tokens.tokenFor(7L)).isNotEqualTo(tokens.tokenFor(8L));
    }

    @Test
    void tokenDependsOnTheSecretSoItCannotBeRecomputedOutside()
    {
        assertThat(service("secret-1").tokenFor(7L)).isNotEqualTo(service("secret-2").tokenFor(7L));
    }
}
