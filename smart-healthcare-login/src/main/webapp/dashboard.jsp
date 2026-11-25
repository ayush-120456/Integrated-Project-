<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.smarthealth.integration.PythonClient" %>
<%@ page import="com.smarthealth.model.JoinSession" %>
<%@ page import="com.smarthealth.model.User" %>
<%@ page import="com.smarthealth.servlet.ServletSupport" %>
<%@ page import="com.smarthealth.util.HtmlUtil" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%
    User currentUser = (User) session.getAttribute(ServletSupport.SESSION_USER);
    JoinSession activeJoin = (JoinSession) session.getAttribute(ServletSupport.SESSION_JOIN);
    String participantName = (String) session.getAttribute(ServletSupport.SESSION_JOIN_PARTICIPANT);

    if (currentUser == null && activeJoin == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }

    String ctx = request.getContextPath();
    String csrfToken = ServletSupport.ensureCsrfToken(session);
    String handoffToken = ServletSupport.createHandoffToken(session);
    String streamlitUrl = PythonClient.resolveStreamlitUrl()
            + "/?token=" + URLEncoder.encode(handoffToken, StandardCharsets.UTF_8);

    String enteredSymptoms = HtmlUtil.escape((String) request.getAttribute("enteredSymptoms"));
    String pythonResult = (String) request.getAttribute("pythonResult");
    String pythonError = (String) request.getAttribute("pythonError");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Portal Dashboard | Smart Healthcare System</title>
    <link rel="stylesheet" href="<%= ctx %>/css/auth.css">
</head>
<body>
<main class="dashboard-card">
    <% if (currentUser != null) { %>
        <span class="status-pill">Authenticated Account</span>
        <h1>Welcome, <%= HtmlUtil.escape(currentUser.getFullName()) %></h1>
        <p class="panel-sub">You are signed in to the Smart Healthcare System portal.</p>

        <div class="meta-grid">
            <div class="meta-item">
                <span>User ID</span>
                <strong><%= HtmlUtil.escape(currentUser.getId()) %></strong>
            </div>
            <div class="meta-item">
                <span>Role</span>
                <strong><%= HtmlUtil.escape(currentUser.getRole().getDisplayName()) %></strong>
            </div>
            <div class="meta-item">
                <span>Username</span>
                <strong><%= HtmlUtil.escape(currentUser.getUsername()) %></strong>
            </div>
        </div>
    <% } else if (activeJoin != null) { %>
        <span class="status-pill">Joined via Access Code</span>
        <h1><%= HtmlUtil.escape(activeJoin.getTitle()) %></h1>
        <p class="panel-sub">
            Joined as <strong><%= HtmlUtil.escape(participantName) %></strong>.
        </p>

        <div class="meta-grid">
            <div class="meta-item">
                <span>Access Code</span>
                <strong><%= HtmlUtil.escape(activeJoin.getCode()) %></strong>
            </div>
            <div class="meta-item">
                <span>Department</span>
                <strong><%= HtmlUtil.escape(activeJoin.getDepartment()) %></strong>
            </div>
            <div class="meta-item">
                <span>Attending Doctor</span>
                <strong><%= HtmlUtil.escape(activeJoin.getDoctorName()) %></strong>
            </div>
        </div>
    <% } %>

    <section class="integration-box">
        <h2>Python &amp; Streamlit Integration</h2>
        <p class="panel-sub">
            Open your Streamlit app with a signed single-sign-on token, or test a direct Java-to-Python backend call.
        </p>

        <div class="action-row">
            <a class="primary-btn secondary-btn link-btn" href="<%= HtmlUtil.escape(streamlitUrl) %>" target="_blank" rel="noopener">
                Launch Streamlit App
            </a>
        </div>

        <form method="post" action="<%= ctx %>/python-analyze" class="python-form">
            <input type="hidden" name="csrfToken" value="<%= HtmlUtil.escape(csrfToken) %>">
            <div class="field">
                <label for="symptoms">Test Java &rarr; Python Service Call (<code>/analyze</code>)</label>
                <input type="text"
                       id="symptoms"
                       name="symptoms"
                       value="<%= enteredSymptoms %>"
                       placeholder="e.g. mild fever and cough for 2 days"
                       required>
            </div>
            <button type="submit" class="primary-btn">Send to Python Service</button>
        </form>

        <% if (pythonError != null && !pythonError.isBlank()) { %>
        <div class="alert alert-error" role="alert" style="margin-top: 14px; margin-bottom: 0;">
            <%= HtmlUtil.escape(pythonError) %>
        </div>
        <% } else if (pythonResult != null && !pythonResult.isBlank()) { %>
        <div class="alert alert-info" role="status" style="margin-top: 14px; margin-bottom: 0;">
            <strong>Python Response:</strong><br>
            <code><%= HtmlUtil.escape(pythonResult) %></code>
        </div>
        <% } %>
    </section>

    <form method="post" action="<%= ctx %>/logout" style="margin-top: 20px;">
        <button type="submit" class="primary-btn outline-btn">Sign Out</button>
    </form>
</main>
</body>
</html>
