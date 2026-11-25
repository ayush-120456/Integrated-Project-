package com.smarthealth.service;

import com.smarthealth.model.JoinSession;
import com.smarthealth.model.Role;
import com.smarthealth.util.HtmlUtil;
import com.smarthealth.util.PasswordUtil;
import com.smarthealth.util.TokenUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Standalone verification suite for AuthService, PasswordUtil, and HtmlUtil.
 * Run with:
 *   javac -d out src/main/java/com/smarthealth/model/*.java src/main/java/com/smarthealth/util/*.java src/main/java/com/smarthealth/service/*.java src/test/java/com/smarthealth/service/AuthServiceTest.java
 *   java -cp out com.smarthealth.service.AuthServiceTest
 */
public class AuthServiceTest {

    public static void main(String[] args) {
        testPasswordHashing();
        testValidLoginAndRoleCheck();
        testInvalidLogin();
        testJoinWithCodeValid();
        testJoinWithCodeExpiredAndInactive();
        testJoinWithCodeInvalidFormat();
        testHtmlEscaping();
        testSignedTokenRoundTrip();
        System.out.println("All AuthServiceTest checks passed (8/8).");
    }

    private static void testPasswordHashing() {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword("Secret123!", salt);
        assertTrue(PasswordUtil.verifyPassword("Secret123!", salt, hash), "Valid password must verify");
        assertTrue(!PasswordUtil.verifyPassword("Wrong123!", salt, hash), "Wrong password must fail");
    }

    private static void testValidLoginAndRoleCheck() {
        AuthService service = AuthService.inMemoryForTests();

        AuthService.AuthResult ok = service.authenticate("patient@smarthealth.org", "Patient123!", Role.PATIENT);
        assertTrue(ok.isSuccess(), "Seeded patient should log in");
        assertEquals("Maya Patel", ok.getUser().orElseThrow().getFullName(), "Full name mismatch");

        AuthService.AuthResult autoRole = service.authenticate("DOCTOR@smarthealth.org", "Doctor123!", null);
        assertTrue(autoRole.isSuccess(), "Case-insensitive username with null role should succeed");
        assertEquals(Role.DOCTOR, autoRole.getUser().orElseThrow().getRole(), "Role mismatch");

        AuthService.AuthResult wrongRole = service.authenticate("patient@smarthealth.org", "Patient123!", Role.DOCTOR);
        assertTrue(!wrongRole.isSuccess(), "Patient logging in as Doctor must fail");
    }

    private static void testInvalidLogin() {
        AuthService service = AuthService.inMemoryForTests();
        assertTrue(!service.authenticate("unknown@smarthealth.org", "Patient123!", null).isSuccess(), "Unknown user must fail");
        assertTrue(!service.authenticate("patient@smarthealth.org", "WrongPass", null).isSuccess(), "Wrong password must fail");
        assertTrue(!service.authenticate("", "Patient123!", null).isSuccess(), "Blank username must fail");
    }

    private static void testJoinWithCodeValid() {
        AuthService service = AuthService.inMemoryForTests();
        AuthService.JoinResult res = service.validateJoinCode("care4821", "Alex Rivera");
        assertTrue(res.isSuccess(), "Lowercase code without hyphen should normalize and succeed");
        assertEquals("CARE-4821", res.getSession().orElseThrow().getCode(), "Code should normalize to XXXX-XXXX");
        assertEquals("Alex Rivera", res.getParticipantName(), "Participant name should match");

        AuthService.JoinResult defaultName = service.validateJoinCode("HLTH-9034", "  ");
        assertTrue(defaultName.isSuccess(), "Blank participant name should fall back to Guest Patient");
        assertEquals("Guest Patient", defaultName.getParticipantName(), "Default participant name mismatch");
    }

    private static void testJoinWithCodeExpiredAndInactive() {
        AuthService service = AuthService.inMemoryForTests();
        AuthService.JoinResult expired = service.validateJoinCode("OLDX-1100", "Maya");
        assertTrue(!expired.isSuccess(), "Expired join code must fail");

        service.registerJoinSession(new JoinSession(
                "INAC-7777",
                "Inactive Room",
                "Neurology",
                "Dr. Lee",
                Instant.now().plus(2, ChronoUnit.HOURS),
                false
        ));
        AuthService.JoinResult inactive = service.validateJoinCode("INAC-7777", "Maya");
        assertTrue(!inactive.isSuccess(), "Inactive join code must fail");
    }

    private static void testJoinWithCodeInvalidFormat() {
        AuthService service = AuthService.inMemoryForTests();
        assertTrue(!service.validateJoinCode("ABC", "Maya").isSuccess(), "Short code must fail");
        assertTrue(!service.validateJoinCode("CARE-9999", "Maya").isSuccess(), "Unknown code must fail");
    }

    private static void testHtmlEscaping() {
        String escaped = HtmlUtil.escape("<script>alert('xss')</script>");
        assertEquals("&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;", escaped, "XSS characters must be escaped");
    }

    private static void testSignedTokenRoundTrip() {
        String secret = "unit-test-secret";
        String token = TokenUtil.createSignedToken("P-1001", "Maya \"M\" Patel", "PATIENT", "login", "", 120, secret);
        Optional<String> verified = TokenUtil.verifyToken(token, secret);
        assertTrue(verified.isPresent(), "Valid signed token must verify");
        assertTrue(verified.get().contains("\"sub\":\"P-1001\""), "Payload must contain subject");
        assertTrue(verified.get().contains("Maya \\\"M\\\" Patel"), "Payload must escape quotes safely");

        assertTrue(TokenUtil.verifyToken(token + "x", secret).isEmpty(), "Tampered token must fail");
        assertTrue(TokenUtil.verifyToken(token, "wrong-secret").isEmpty(), "Wrong secret must fail");

        String expired = TokenUtil.createSignedToken("P-1001", "Maya", "PATIENT", "login", "", -10, secret);
        assertTrue(TokenUtil.verifyToken(expired, secret).isEmpty(), "Expired token must fail");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }
}
