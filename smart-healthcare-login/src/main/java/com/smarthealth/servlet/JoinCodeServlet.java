package com.smarthealth.servlet;

import com.smarthealth.model.JoinSession;
import com.smarthealth.service.AuthService;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Handles POST /join for entering an appointment, clinic, or telehealth session using an access code.
 */
@WebServlet(name = "JoinCodeServlet", urlPatterns = {"/join"})
public class JoinCodeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/login?tab=join");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String joinCode = request.getParameter("joinCode");
        String participantName = request.getParameter("participantName");

        request.setAttribute(ServletSupport.REQ_JOIN_CODE, joinCode);
        request.setAttribute(ServletSupport.REQ_PARTICIPANT_NAME, participantName);

        if (!ServletSupport.isValidCsrfToken(request)) {
            ServletSupport.forwardToLoginWithError(
                    request, response, "join",
                    "Your session expired. Please submit the access code again."
            );
            return;
        }

        AuthService authService = ServletSupport.getAuthService(getServletContext());
        AuthService.JoinResult result = authService.validateJoinCode(joinCode, participantName);

        if (!result.isSuccess()) {
            ServletSupport.forwardToLoginWithError(
                    request, response, "join",
                    result.getErrorMessage()
            );
            return;
        }

        JoinSession joinSession = result.getSession().orElseThrow();
        HttpSession session = ServletSupport.rotateSession(request);
        session.setAttribute(ServletSupport.SESSION_JOIN, joinSession);
        session.setAttribute(ServletSupport.SESSION_JOIN_PARTICIPANT, result.getParticipantName());

        response.sendRedirect(ServletSupport.buildStreamlitRedirectUrl(session));
    }
}
