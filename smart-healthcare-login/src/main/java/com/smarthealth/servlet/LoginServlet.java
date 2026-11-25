package com.smarthealth.servlet;

import com.smarthealth.model.Role;
import com.smarthealth.model.User;
import com.smarthealth.service.AuthService;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Handles GET /login (renders login page) and POST /login (authenticates user credentials).
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession existing = request.getSession(false);
        if (existing != null && (existing.getAttribute(ServletSupport.SESSION_USER) != null
                || existing.getAttribute(ServletSupport.SESSION_JOIN) != null)) {
            response.sendRedirect(ServletSupport.buildStreamlitRedirectUrl(existing));
            return;
        }

        HttpSession session = request.getSession(true);
        ServletSupport.ensureCsrfToken(session);

        if ("1".equals(request.getParameter("loggedOut"))) {
            request.setAttribute(ServletSupport.REQ_INFO, "You have been signed out safely.");
        }
        String tab = request.getParameter("tab");
        if ("join".equalsIgnoreCase(tab)) {
            request.setAttribute(ServletSupport.REQ_ACTIVE_TAB, "join");
        } else {
            request.setAttribute(ServletSupport.REQ_ACTIVE_TAB, "login");
        }

        // Pre-fill remembered username if present
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("smarthealth_remember".equals(cookie.getName())) {
                    request.setAttribute(ServletSupport.REQ_USERNAME, cookie.getValue());
                    break;
                }
            }
        }

        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String roleParam = request.getParameter("role");
        boolean rememberMe = "on".equalsIgnoreCase(request.getParameter("rememberMe"));

        request.setAttribute(ServletSupport.REQ_USERNAME, username);
        request.setAttribute(ServletSupport.REQ_ROLE, roleParam);

        if (!ServletSupport.isValidCsrfToken(request)) {
            ServletSupport.forwardToLoginWithError(
                    request, response, "login",
                    "Your session expired. Please submit the form again."
            );
            return;
        }

        Role role = Role.fromString(roleParam);
        if (roleParam != null && !roleParam.isBlank() && role == null) {
            ServletSupport.forwardToLoginWithError(
                    request, response, "login",
                    "Please select a valid role."
            );
            return;
        }

        AuthService authService = ServletSupport.getAuthService(getServletContext());
        AuthService.AuthResult result = authService.authenticate(username, password, role);

        if (!result.isSuccess()) {
            ServletSupport.forwardToLoginWithError(
                    request, response, "login",
                    result.getErrorMessage()
            );
            return;
        }

        User authenticatedUser = result.getUser().orElseThrow();
        HttpSession session = ServletSupport.rotateSession(request);
        session.setAttribute(ServletSupport.SESSION_USER, authenticatedUser);

        updateRememberCookie(request, response, authenticatedUser.getUsername(), rememberMe);
        response.sendRedirect(ServletSupport.buildStreamlitRedirectUrl(session));
    }

    private void updateRememberCookie(
            HttpServletRequest request,
            HttpServletResponse response,
            String username,
            boolean rememberMe
    ) {
        Cookie cookie = new Cookie("smarthealth_remember", rememberMe ? username : "");
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        cookie.setMaxAge(rememberMe ? 7 * 24 * 60 * 60 : 0);
        response.addCookie(cookie);
    }
}
