package com.example.VegeTabell.app.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRootRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    void unauthenticatedSellerPathRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/seller/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    void loginPageIsNotBlockedByAuthentication() throws Exception {
        // .loginPage("/login") を明示指定するとSpring Securityの自動生成ログインページは無効になるため、
        // Controller未実装の現時点（Step4前）では404が正しい。もしpermitAllが効いていなければ
        // 認証要求で /login 自身にリダイレクトされ続けるはずなので、404はSecurity層を通過した証拠になる。
        // Step4でログイン画面のControllerを実装したら、この期待値は isOk() に更新すること。
        mockMvc.perform(get("/login"))
                .andExpect(status().isNotFound());
    }
}
