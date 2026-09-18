package com.dhatchina.dhatchinamart.service;

import com.dhatchina.dhatchinamart.service.sms.SmsProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @Mock
    private SmsProvider provider;

    @Test
    void buildsMessageWithOtpAndDefaultExpiryAndDelegates() {
        when(provider.send(org.mockito.ArgumentMatchers.eq("9876543210"),
                org.mockito.ArgumentMatchers.eq("483921"),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
        SmsService service = new SmsService(provider);

        boolean delivered = service.sendOtp("9876543210", "483921");

        assertTrue(delivered);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(provider).send(org.mockito.ArgumentMatchers.eq("9876543210"),
                org.mockito.ArgumentMatchers.eq("483921"), message.capture());
        assertTrue(message.getValue().contains("483921"));
        assertTrue(message.getValue().contains("5 minutes"));
        assertTrue(message.getValue().contains("DhatchinaMart"));
    }

    @Test
    void usesConfiguredExpiryMinutesInMessage() {
        SmsService service = new SmsService(provider, 10);

        service.sendOtp("9876543210", "111111");

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(provider).send(org.mockito.ArgumentMatchers.eq("9876543210"),
                org.mockito.ArgumentMatchers.eq("111111"), message.capture());
        assertTrue(message.getValue().contains("10 minutes"));
        assertFalse(message.getValue().contains("5 minutes"));
    }

    @Test
    void propagatesProviderFailure() {
        when(provider.send(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        SmsService service = new SmsService(provider);

        assertFalse(service.sendOtp("9876543210", "222222"));
    }
}