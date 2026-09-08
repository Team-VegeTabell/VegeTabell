package com.example.VegeTabell.app.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Component
public class RoleMatchAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    // ログインフォームの買い手/売り手トグルのフィールド名。Step4はこの名前で送信すること。
    private static final String ROLE_PARAM = "role";
    private static final String ROLE_MISMATCH_REDIRECT = "/login?error=role";
    private static final String BUYER_DEFAULT_TARGET = "/";
    private static final String SELLER_DEFAULT_TARGET = "/seller/dashboard";

    // SecurityConfigの認可設定に対応するロール別パス。未認証で保存されたリクエストが
    // 認証後のロールでアクセスできないパスの場合、そのままリプレイすると403になるため破棄する。
    private static final List<String> BUYER_ONLY_PATTERNS = List.of("/", "/products/**", "/reservations/**", "/mypage");
    private static final List<String> SELLER_ONLY_PATTERNS = List.of("/seller/**");
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final LoginRoleMatcher loginRoleMatcher;
    private final RequestCache requestCache = new HttpSessionRequestCache();

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

        discardIncompatibleSavedRequest(request, response, authentication);

        // シングルトンBeanのため、共有フィールドのdefaultTargetUrlをリクエストごとに
        // 書き換えると同時ログイン時に競合する。ロールに応じたリダイレクト先の決定のため、
        // リクエストごとにローカルインスタンスを生成する（生成コストは軽微）。
        SavedRequestAwareAuthenticationSuccessHandler delegate = new SavedRequestAwareAuthenticationSuccessHandler();
        delegate.setDefaultTargetUrl(defaultTargetUrlFor(authentication));
        delegate.setAlwaysUseDefaultTargetUrl(false);
        delegate.onAuthenticationSuccess(request, response, authentication);
    }

    // 未認証で買い手/売り手専用URLへアクセスすると、そのリクエストがセッションに保存される
    // （例：「/」に未ログインでアクセス→保存→ログイン画面へ）。認証後のロールでそのURLに
    // アクセスできない場合（例：保存済みが「/」なのに売り手としてログイン）、保存済みリクエストを
    // そのままリプレイすると403 Forbiddenになってしまうため、破棄してロール別デフォルト遷移先に
    // フォールバックさせる。
    private void discardIncompatibleSavedRequest(HttpServletRequest request, HttpServletResponse response,
                                                  Authentication authentication) {
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest == null) {
            return;
        }
        String path = extractPath(savedRequest.getRedirectUrl());
        if (path == null) {
            return;
        }
        boolean isSeller = isSeller(authentication);
        List<String> incompatiblePatterns = isSeller ? BUYER_ONLY_PATTERNS : SELLER_ONLY_PATTERNS;
        if (incompatiblePatterns.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path))) {
            requestCache.removeRequest(request, response);
        }
    }

    private String extractPath(String redirectUrl) {
        try {
            return new URI(redirectUrl).getPath();
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private boolean isSeller(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SELLER"));
    }

    private String defaultTargetUrlFor(Authentication authentication) {
        return isSeller(authentication) ? SELLER_DEFAULT_TARGET : BUYER_DEFAULT_TARGET;
    }
}
