<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.smarthealth.servlet.ServletSupport" %>
<%@ page import="com.smarthealth.util.HtmlUtil" %>
<%
    String csrfToken = ServletSupport.ensureCsrfToken(session);
    String activeTab = (String) request.getAttribute(ServletSupport.REQ_ACTIVE_TAB);
    if (!"join".equals(activeTab)) {
        activeTab = "login";
    }
    String errorMessage = (String) request.getAttribute(ServletSupport.REQ_ERROR);
    String infoMessage = (String) request.getAttribute(ServletSupport.REQ_INFO);

    String enteredUsername = HtmlUtil.escape((String) request.getAttribute(ServletSupport.REQ_USERNAME));
    String enteredRole = (String) request.getAttribute(ServletSupport.REQ_ROLE);
    String enteredJoinCode = HtmlUtil.escape((String) request.getAttribute(ServletSupport.REQ_JOIN_CODE));
    String enteredParticipant = HtmlUtil.escape((String) request.getAttribute(ServletSupport.REQ_PARTICIPANT_NAME));
    String ctx = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign In | Smart Healthcare System</title>
    <link rel="stylesheet" href="<%= ctx %>/css/auth.css">
</head>
<body>
<main class="auth-shell">
    <section class="brand-panel" aria-label="Smart Healthcare overview">
        <div>
            <div class="brand-badge">
                <span class="brand-icon" aria-hidden="true">+</span>
                <span>Smart Healthcare System</span>
            </div>
            <h1>Connected care for patients, doctors, and clinics.</h1>
            <p>
                Sign in with your healthcare account or join an active consultation directly
                using your clinic access code.
            </p>
            <ul class="feature-list">
                <li><span class="feature-dot" aria-hidden="true"></span>Role-based portal for Patients, Doctors, and Staff</li>
                <li><span class="feature-dot" aria-hidden="true"></span>Instant room entry with 8-character appointment codes</li>
                <li><span class="feature-dot" aria-hidden="true"></span>CSRF-protected sessions with salted password hashing</li>
            </ul>
        </div>

        <div class="demo-box">
            <strong>Demo Credentials &amp; Join Codes</strong>
            Patient: <code>patient@smarthealth.org</code> / <code>Patient123!</code><br>
            Doctor: <code>doctor@smarthealth.org</code> / <code>Doctor123!</code><br>
            Active Codes: <code>CARE-4821</code>, <code>HLTH-9034</code>
        </div>
    </section>

    <section class="form-panel" aria-label="Authentication forms">
        <div class="tab-bar" role="tablist" aria-label="Access method">
            <button type="button"
                    class="tab-btn <%= "login".equals(activeTab) ? "active" : "" %>"
                    data-tab="login"
                    role="tab"
                    aria-selected="<%= "login".equals(activeTab) %>">
                Account Sign In
            </button>
            <button type="button"
                    class="tab-btn <%= "join".equals(activeTab) ? "active" : "" %>"
                    data-tab="join"
                    role="tab"
                    aria-selected="<%= "join".equals(activeTab) %>">
                Join with Code
            </button>
        </div>

        <% if (errorMessage != null && !errorMessage.isBlank()) { %>
        <div class="alert alert-error" role="alert">
            <%= HtmlUtil.escape(errorMessage) %>
        </div>
        <% } else if (infoMessage != null && !infoMessage.isBlank()) { %>
        <div class="alert alert-info" role="status">
            <%= HtmlUtil.escape(infoMessage) %>
        </div>
        <% } %>

        <!-- Standard Account Login Form -->
        <div id="pane-login" class="tab-pane <%= "login".equals(activeTab) ? "active" : "" %>">
            <h2 class="panel-heading">Welcome back</h2>
            <p class="panel-sub">Sign in to access medical records, schedules, and telehealth.</p>

            <form method="post" action="<%= ctx %>/login" autocomplete="on">
                <input type="hidden" name="csrfToken" value="<%= HtmlUtil.escape(csrfToken) %>">

                <div class="field">
                    <label for="role">Portal Role</label>
                    <select id="role" name="role">
                        <option value="" <%= (enteredRole == null || enteredRole.isBlank()) ? "selected" : "" %>>Any Role (Auto-detect)</option>
                        <option value="PATIENT" <%= "PATIENT".equalsIgnoreCase(enteredRole) ? "selected" : "" %>>Patient</option>
                        <option value="DOCTOR" <%= "DOCTOR".equalsIgnoreCase(enteredRole) ? "selected" : "" %>>Doctor</option>
                        <option value="STAFF" <%= "STAFF".equalsIgnoreCase(enteredRole) ? "selected" : "" %>>Staff / Admin</option>
                    </select>
                </div>

                <div class="field">
                    <label for="username">Email or Username</label>
                    <input type="text"
                           id="username"
                           name="username"
                           value="<%= enteredUsername %>"
                           placeholder="e.g. patient@smarthealth.org"
                           required>
                </div>

                <div class="field">
                    <label for="password">Password</label>
                    <div class="password-wrap">
                        <input type="password"
                               id="password"
                               name="password"
                               placeholder="Enter your password"
                               required>
                        <button type="button" id="togglePassword" class="toggle-pw" aria-label="Show password">Show</button>
                    </div>
                </div>

                <div class="row-between">
                    <label class="checkbox-label">
                        <input type="checkbox" name="rememberMe">
                        <span>Remember my username</span>
                    </label>
                </div>

                <button type="submit" class="primary-btn">Sign In</button>
            </form>
        </div>

        <!-- Join with Access Code Form -->
        <div id="pane-join" class="tab-pane <%= "join".equals(activeTab) ? "active" : "" %>">
            <h2 class="panel-heading">Join with access code</h2>
            <p class="panel-sub">Enter the 8-character clinic or appointment code shared by your care team.</p>

            <form method="post" action="<%= ctx %>/join" autocomplete="off">
                <input type="hidden" name="csrfToken" value="<%= HtmlUtil.escape(csrfToken) %>">

                <div class="field">
                    <label for="joinCode">Clinic / Appointment Code</label>
                    <input type="text"
                           id="joinCode"
                           name="joinCode"
                           class="code-input"
                           value="<%= enteredJoinCode %>"
                           placeholder="CARE-4821"
                           maxlength="9"
                           pattern="[A-Za-z0-9]{4}-?[A-Za-z0-9]{4}"
                           required>
                    <div class="field-hint">Format: 8 letters or numbers (hyphen added automatically).</div>
                </div>

                <div class="field">
                    <label for="participantName">Your Full Name (Optional)</label>
                    <input type="text"
                           id="participantName"
                           name="participantName"
                           value="<%= enteredParticipant %>"
                           placeholder="e.g. Maya Patel">
                </div>

                <button type="submit" class="primary-btn secondary-btn">Join Session</button>
            </form>
        </div>
    </section>
</main>

<script src="<%= ctx %>/js/auth.js"></script>
</body>
</html>
