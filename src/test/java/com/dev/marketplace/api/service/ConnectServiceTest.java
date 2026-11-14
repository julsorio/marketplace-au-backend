package com.dev.marketplace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dev.marketplace.api.model.User;
import com.dev.marketplace.api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ConnectServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StripeGateway stripeGateway;
    @InjectMocks
    private ConnectService connectService;

    @Test
    void enLocalNoSeEnviaUrlDePerfilAStripe() {
        ReflectionTestUtils.setField(connectService, "frontendUrl", "http://localhost:4200");
        assertThat(connectService.publicProfileUrl("u1")).isNull();
    }

    @Test
    void enProduccionLaUrlEsElPerfilPublicoDelVendedor() {
        ReflectionTestUtils.setField(connectService, "frontendUrl", "https://marketplace.example.au/");
        assertThat(connectService.publicProfileUrl("u1")).isEqualTo("https://marketplace.example.au/users/u1");
    }

    @Test
    void creaLaCuentaSoloSiNoExisteYDevuelveElEnlace() {
        ReflectionTestUtils.setField(connectService, "frontendUrl", "http://localhost:4200");
        User user = new User();
        user.setId("u1");
        user.setEmail("vendedor@example.com");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(stripeGateway.createConnectedAccount(eq("vendedor@example.com"), eq("u1"), isNull()))
                .thenReturn("acct_1");
        when(stripeGateway.createOnboardingLink(eq("acct_1"), anyString(), anyString())).thenReturn("https://stripe/link");

        assertThat(connectService.createOnboardingLink("u1").url()).isEqualTo("https://stripe/link");
        assertThat(user.getStripeAccountId()).isEqualTo("acct_1");
        verify(stripeGateway).createOnboardingLink("acct_1",
                "http://localhost:4200/payments/setup?onboarding=refresh",
                "http://localhost:4200/payments/setup?onboarding=return");
    }
}
