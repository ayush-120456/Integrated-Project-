package com.smarthealth.servlet;

import com.smarthealth.integration.PythonClient;
import com.smarthealth.model.JoinSession;
import com.smarthealth.model.User;
import com.smarthealth.service.AuthService;
import com.smarthealth.util.PasswordUtil;
import com.smarthealth.util.TokenUtil;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Shared constants, singleton AuthService lookup, and CSRF helpers for servlets and JSPs.
 */
public final class ServletSupport {
    public static final String ATTR_AUTH_SERVICE = "smarthealth.authService";
    public static final String SESSION_USER = "currentUser";
    public static final String SESSION_JOIN = "activeJoinSession";
    public static final String SESSION_JOIN_PARTICIPANT = "joinParticipantName";
    public static final String SESSION_CSRF_TOKEN = "csrfToken";

    public static final String REQ_ACTIVE_TAB = "activeTab";
    public static final String REQ_ERROR = "errorMessage";
    public static final String REQ_INFO = "infoMessage";
    public static final String REQ_USERNAME = "enteredUsername";
    public static final String REQ_ROLE = "enteredRole";
    public static final String REQ_JOIN_CODE = "enteredJoinCode";
    public static final String REQ_PARTICIPANT_NAME = "enteredParticipantName";

    private ServletSupport() {
    }

    /**
     * Returns a shared AuthService instance stored on the ServletContext.
     */
    public static AuthService getAuthService(ServletContext context) {
        Object existing = context.getAttribute(ATTR_AUTH_SERVICE);
        if (existing instanceof AuthService service) {
            return service;
        }
        synchronized (context) {
            existing = context.getAttribute(ATTR_AUTH_SERVICE);
            if (existing instanceof AuthService service) {
                return service;
            }
            AuthService created = new AuthService();
            context.setAttribute(ATTR_AUTH_SERVICE, created);
            return created;
        }
    }

    /**
     * Ensures the current session has a CSRF token and returns it.
     */
    public static String ensureCsrfToken(HttpSession session) {
        Object token = session.getAttribute(SESSION_CSRF_TOKEN);
        if (token instanceof String existing && !existing.isBlank()) {
            return existing;
        }
        String fresh = PasswordUtil.generateToken();
        session.setAttribute(SESSION_CSRF_TOKEN, fresh);
        return fresh;
    }

    /**
     * Validates the submitted CSRF token against the one stored in the session.
     */
    public static boolean isValidCsrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object expectedObj = session.getAttribute(SESSION_CSRF_TOKEN);
        String submitted = request.getParameter("csrfToken");
        if (!(expectedObj instanceof String expected) || submitted == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                submitted.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Rotates the HTTP session to prevent session fixation after successful login or join.
     */
    public static HttpSession rotateSession(HttpServletRequest request) {
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession fresh = request.getSession(true);
        fresh.setMaxInactiveInterval(30 * 60);
        ensureCsrfToken(fresh);
        return fresh;
    }

    /**
     * Builds a signed handoff token for the currently authenticated user or join-code session.
     */
    public static String createHandoffToken(HttpSession session) {
        if (session == null) {
            return null;
        }
        User user = (User) session.getAttribute(SESSION_USER);
        if (user != null) {
            return TokenUtil.createSignedToken(
                    user.getId(),
                    user.getFullName(),
                    user.getRole().name(),
                    "login",
                    ""
            );
        }
        JoinSession joinSession = (JoinSession) session.getAttribute(SESSION_JOIN);
        if (joinSession != null) {
            String participant = (String) session.getAttribute(SESSION_JOIN_PARTICIPANT);
            return TokenUtil.createSignedToken(
                    joinSession.getCode(),
                    participant != null ? participant : "Guest Patient",
                    "GUEST",
                    "join",
                    joinSession.getCode()
            );
        }
        return null;
    }

    /**
     * Builds the Streamlit redirect URL carrying a fresh signed handoff token.
     */
    public static String buildStreamlitRedirectUrl(HttpSession session) {
        String token = createHandoffToken(session);
        return PythonClient.resolveStreamlitUrl()
                + "/?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    /**
     * Forwards back to login.jsp with the given tab and error message.
     */
    public static void forwardToLoginWithError(
            HttpServletRequest request,
            HttpServletResponse response,
            String activeTab,
            String errorMessage
    ) throws ServletException, IOException {
        ensureCsrfToken(request.getSession(true));
        request.setAttribute(REQ_ACTIVE_TAB, activeTab);
        request.setAttribute(REQ_ERROR, errorMessage);
        RequestDispatcher dispatcher = request.getRequestDispatcher("/login.jsp");
        dispatcher.forward(request, response);
    }
}
