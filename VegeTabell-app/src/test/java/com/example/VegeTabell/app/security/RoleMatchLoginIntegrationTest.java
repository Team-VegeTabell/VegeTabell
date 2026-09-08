package com.example.VegeTabell.app.security;

import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.UserRole;
import com.example.VegeTabell.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class RoleMatchLoginIntegrationTest {

    private static final String RAW_PASSWORD = "password123";
    private static final String EMAIL = "buyer@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    private static final String SELLER_EMAIL = "seller@example.com";

    private User buyerUser() {
        User user = new User();
        user.setEmail(EMAIL);
        user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
        user.setRole(UserRole.BUYER);
        user.setDisplayName("テスト買い手");
        return user;
    }

    private User sellerUser() {
        User user = new User();
        user.setEmail(SELLER_EMAIL);
        user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
        user.setRole(UserRole.SELLER);
        user.setDisplayName("テスト店主");
        return user;
    }

    @Test
    void loginWithMismatchedRole_isRejected() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buyerUser()));

        mockMvc.perform(post("/login")
                        .param("username", EMAIL)
                        .param("password", RAW_PASSWORD)
                        .param("role", "seller")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login?error=role")));
    }

    @Test
    void loginWithMatchingRole_succeeds() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buyerUser()));

        mockMvc.perform(post("/login")
                        .param("username", EMAIL)
                        .param("password", RAW_PASSWORD)
                        .param("role", "buyer")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", not(containsString("/login"))));
    }

    @Test
    void loginAsSeller_afterBuyerOnlyPageWasSavedPreLogin_redirectsToSellerDashboardNotSavedRequest() throws Exception {
        // 未認証で買い手専用の「/」にアクセスし、リクエストをセッションに保存させる
        // （その後ログイン画面へリダイレクトされる）。
        MvcResult saved = mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) saved.getRequest().getSession();

        when(userRepository.findByEmail(SELLER_EMAIL)).thenReturn(Optional.of(sellerUser()));

        // 同じセッションのまま売り手としてログインする。保存済みリクエスト（「/」）をそのまま
        // リプレイすると403になってしまうため、売り手用のデフォルト遷移先に飛ぶべき。
        mockMvc.perform(post("/login")
                        .session(session)
                        .param("username", SELLER_EMAIL)
                        .param("password", RAW_PASSWORD)
                        .param("role", "seller")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/seller/dashboard")));
    }
}
