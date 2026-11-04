package com.ocnilentes.mediagenda.bootstrap;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LegalPagesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void privacyIsPublicHtml() throws Exception {
        mockMvc.perform(get("/privacy").header("User-Agent", facebookCrawler()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Política de privacidad")));
    }

    @Test
    void privacyTrailingSlashIsPublicHtml() throws Exception {
        mockMvc.perform(get("/privacy/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    void deletionAliasesArePublicHtml() throws Exception {
        for (String path : new String[] {"/datadeletion", "/eliminar-datos", "/data-deletion", "/datadeletion/"}) {
            mockMvc.perform(get(path).header("User-Agent", facebookCrawler()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(content().string(containsString("Eliminación de datos")));
        }
    }

    @Test
    void termsAndRobotsArePublic() throws Exception {
        mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Términos de servicio")));
        mockMvc.perform(get("/robots.txt").header("User-Agent", facebookCrawler()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("Allow: /")));
    }

    @Test
    void legalPagesAcceptHead() throws Exception {
        mockMvc.perform(head("/privacy")).andExpect(status().isOk());
        mockMvc.perform(head("/datadeletion")).andExpect(status().isOk());
        mockMvc.perform(head("/robots.txt")).andExpect(status().isOk());
    }

    @Test
    void metaCallbackReturnsJsonContract() throws Exception {
        mockMvc.perform(post("/datadeletion")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("signed_request", "test.payload"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.url").value(containsString("/datadeletion?id=")))
                .andExpect(jsonPath("$.confirmation_code").isString());
    }

    @Test
    void unknownPathIsNotFoundInsteadOfServerError() throws Exception {
        mockMvc.perform(get("/this-path-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private static String facebookCrawler() {
        return "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)";
    }
}
