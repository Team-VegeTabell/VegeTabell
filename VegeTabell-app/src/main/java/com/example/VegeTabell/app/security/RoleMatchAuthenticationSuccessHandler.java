package com.example.VegeTabell.app.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleMatchAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    // ログインフォームの買い手/売り手トグルのフィールド名。Step4はこの名前で送信すること。
    private static final String ROLE_PARAM = "role";
    private static final String ROLE_MISMATCH_REDIRECT = "/login?error=role";

    private final LoginRoleMatcher loginRoleMatcher;
    private final AuthenticationSuccessHandler delegate = new SavedRequestAwareAuthenticationSuccessHandler();

    public RoleMatchAuthenticationSuccessHandler(LoginRoleMatcher loginRoleMatcher) {
        this.loginRoleMatcher = loginRoleMatcher;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        String selectedRole = request.getParameter(ROLE_PARAM);

        if (!loginRoleMatcher.matches(authentication, selectedRole)) {
            // この時点でSpring Securityは既にセッション固定化対策済みの新セッションにSecurityContextを
            // 永続化済みだが、同一リクエスト内で即座に破棄するためクライアントには一切露出しない。
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + ROLE_MISMATCH_REDIRECT);
            return;
        }

        delegate.onAuthenticationSuccess(request, response, authentication);
    }
}
