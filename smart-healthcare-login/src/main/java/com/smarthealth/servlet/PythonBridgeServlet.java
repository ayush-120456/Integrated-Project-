package com.smarthealth.servlet;

import com.smarthealth.integration.PythonClient;
import com.smarthealth.model.JoinSession;
import com.smarthealth.model.User;
import com.smarthealth.util.TokenUtil;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Proxies authenticated requests from the Java portal to the Python backend service.
 */
@WebServlet(name = "PythonBridgeServlet", urlPatterns = {"/python-analyze"})
public class PythonBridgeServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        User user = (User) session.getAttribute(ServletSupport.SESSION_USER);
        JoinSession joinSession = (JoinSession) session.getAttribute(ServletSupport.SESSION_JOIN);
        String participantName = (String) session.getAttribute(ServletSupport.SESSION_JOIN_PARTICIPANT);

        if (user == null && joinSession == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        if (!ServletSupport.isValidCsrfToken(request)) {
            request.setAttribute("pythonError", "Session token expired. Please reload and try again.");
            request.getRequestDispatcher("/dashboard.jsp").forward(request, response);
            return;
        }

        String symptoms = request.getParameter("symptoms");
        request.setAttribute("enteredSymptoms", symptoms);
        if (symptoms == null || symptoms.isBlank()) {
            request.setAttribute("pythonError", "Please enter symptoms or notes to send to Python.");
            request.getRequestDispatcher("/dashboard.jsp").forward(request, response);
            return;
        }

        String token = ServletSupport.createHandoffToken(session);

        PythonClient client = new PythonClient();
        try {
            PythonClient.PythonResponse pyRes = client.analyzeSymptoms(symptoms.trim(), token);
            if (pyRes.isOk()) {
                request.setAttribute("pythonResult", pyRes.body());
            } else {
                request.setAttribute("pythonError",
                        "Python service returned HTTP " + pyRes.statusCode() + ": " + pyRes.body());
            }
        } catch (IOException ex) {
            request.setAttribute("pythonError",
                    "Could not reach Python service at " + PythonClient.resolvePythonApiUrl()
                            + ". Start python_integration/python_service.py first.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            request.setAttribute("pythonError", "Request to Python service was interrupted.");
        }

        request.getRequestDispatcher("/dashboard.jsp").forward(request, response);
    }
}
