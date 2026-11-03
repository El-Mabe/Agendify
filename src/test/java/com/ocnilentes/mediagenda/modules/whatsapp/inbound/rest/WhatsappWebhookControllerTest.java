package com.ocnilentes.mediagenda.modules.whatsapp.inbound.rest;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ocnilentes.mediagenda.bootstrap.MediAgendaApplication;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.port.WhatsappMessageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = MediAgendaApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "whatsapp.api-url=https://graph.facebook.com",
        "whatsapp.version=v23.0",
        "whatsapp.token=test-token",
        "whatsapp.phone-number-id=123",
        "whatsapp.verify-token=mediagenda-local-verify"
})
class WhatsappWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WhatsappMessageClient whatsappMessageClient;

    @BeforeEach
    void setUp() {
        when(whatsappMessageClient.sendTextMessage(anyString(), anyString())).thenReturn("wamid.test");
        when(whatsappMessageClient.sendInteractiveButtons(anyString(), anyString(), anyList()))
                .thenReturn("wamid.buttons");
        when(whatsappMessageClient.sendInteractiveList(
                anyString(), anyString(), anyString(), anyString(), anyList()
        )).thenReturn("wamid.list");
    }

    @Test
    void shouldVerifyWebhookWhenTokenMatches() throws Exception {
        mockMvc.perform(get("/api/v1/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "mediagenda-local-verify")
                        .param("hub.challenge", "123456"))
                .andExpect(status().isOk())
                .andExpect(content().string("123456"));
    }

    @Test
    void shouldRejectWebhookWhenTokenDoesNotMatch() throws Exception {
        mockMvc.perform(get("/api/v1/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "wrong-token")
                        .param("hub.challenge", "123456"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAcceptIncomingMessageWebhook() throws Exception {
        String payload = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [
                    {
                      "id": "WABA_ID",
                      "changes": [
                        {
                          "field": "messages",
                          "value": {
                            "messaging_product": "whatsapp",
                            "metadata": {
                              "display_phone_number": "15551902499",
                              "phone_number_id": "1229730536897494"
                            },
                            "contacts": [
                              {
                                "profile": { "name": "Test User" },
                                "wa_id": "573043370408"
                              }
                            ],
                            "messages": [
                              {
                                "from": "573043370408",
                                "id": "wamid.test-hola",
                                "timestamp": "1710000000",
                                "type": "text",
                                "text": { "body": "Hola" }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(whatsappMessageClient, timeout(2000)).sendTextMessage(eq("573043370408"), anyString());
    }

    @Test
    void shouldAcceptIncomingListReplyWebhook() throws Exception {
        String payload = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [
                    {
                      "id": "WABA_ID",
                      "changes": [
                        {
                          "field": "messages",
                          "value": {
                            "messaging_product": "whatsapp",
                            "metadata": {
                              "display_phone_number": "15551902499",
                              "phone_number_id": "1229730536897494"
                            },
                            "contacts": [
                              {
                                "profile": { "name": "Test User" },
                                "wa_id": "573043370408"
                              }
                            ],
                            "messages": [
                              {
                                "from": "573043370408",
                                "id": "wamid.test-list",
                                "timestamp": "1710000000",
                                "type": "interactive",
                                "interactive": {
                                  "type": "list_reply",
                                  "list_reply": {
                                    "id": "1",
                                    "title": "Mié 4 nov",
                                    "description": "04/11/2026"
                                  }
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(whatsappMessageClient, timeout(2000)).sendTextMessage(eq("573043370408"), anyString());
    }
}
