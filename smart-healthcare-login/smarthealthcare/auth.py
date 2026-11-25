"""Java SSO guard for the Smart Healthcare Streamlit app (authentication is handled by Java)."""

import base64
import hashlib
import hmac
import json
import os
import time
from typing import Any, Dict, Optional

import streamlit as st

DEV_FALLBACK_SECRET = "smarthealth-dev-secret-change-me"
JAVA_LOGIN_URL = os.environ.get("SMARTHEALTH_JAVA_LOGIN_URL", "http://localhost:8080/smart-healthcare/login")
JAVA_LOGOUT_URL = os.environ.get("SMARTHEALTH_JAVA_LOGOUT_URL", "http://localhost:8080/smart-healthcare/logout")


def get_shared_secret() -> str:
    secret = os.environ.get("SMARTHEALTH_SHARED_SECRET", "").strip()
    return secret if secret else DEV_FALLBACK_SECRET


def verify_signed_token(token: Optional[str], secret: Optional[str] = None) -> Optional[Dict[str, Any]]:
    """Verifies a base64url(payload).base64url(hmac_sha256) token issued by Java TokenUtil."""
    if not token or "." not in token:
        return None
    parts = token.split(".")
    if len(parts) != 2 or not parts[0] or not parts[1]:
        return None

    payload_b64, provided_sig = parts
    resolved_secret = secret if secret is not None else get_shared_secret()

    expected_raw = hmac.new(
        resolved_secret.encode("utf-8"),
        payload_b64.encode("utf-8"),
        hashlib.sha256,
    ).digest()
    expected_sig = base64.urlsafe_b64encode(expected_raw).decode("ascii").rstrip("=")

    if not hmac.compare_digest(expected_sig, provided_sig):
        return None

    try:
        padding = "=" * (-len(payload_b64) % 4)
        claims = json.loads(base64.urlsafe_b64decode(payload_b64 + padding).decode("utf-8"))
    except (ValueError, UnicodeDecodeError):
        return None

    exp = claims.get("exp")
    if not isinstance(exp, (int, float)) or time.time() > exp:
        return None

    return claims


def require_java_login(java_login_url: str = JAVA_LOGIN_URL) -> Dict[str, Any]:
    """
    Requires a valid signed token from the Java login page (?token=...).
    No login form is rendered in Python; unauthenticated users are sent to the Java login page.
    """
    token_param = st.query_params.get("token")
    if isinstance(token_param, list):
        token_param = token_param[0] if token_param else None

    if token_param:
        claims = verify_signed_token(token_param)
        if claims is not None:
            st.session_state["smarthealth_auth"] = claims
            if "token" in st.query_params:
                del st.query_params["token"]
            return claims
        st.session_state.pop("smarthealth_auth", None)

    if "smarthealth_auth" in st.session_state:
        return st.session_state["smarthealth_auth"]

    _, center, _ = st.columns([1, 1.3, 1])
    with center:
        st.title("🏥 Smart Healthcare System")
        st.warning("Please sign in or enter a Join Code on the Java Login Page to continue.")
        st.link_button("Open Java Login Page", java_login_url, type="primary", use_container_width=True)
    st.stop()
