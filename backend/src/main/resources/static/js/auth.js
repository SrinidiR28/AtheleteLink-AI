// =========================================================
// AthleteLink AI - Auth & backend integration
//
// This is the ONLY file that talks to the Spring Boot API.
// Login/Register buttons in the header call openAuth(), which
// shows the modal below. Submitting either form makes a real
// fetch() call to the backend - there is no mock/demo login.
// =========================================================

const TOKEN_KEY = "athletelink_token";
const USER_KEY = "athletelink_user";

const AuthState = {
    getToken() {
        return localStorage.getItem(TOKEN_KEY);
    },
    getUser() {
        const raw = localStorage.getItem(USER_KEY);
        return raw ? JSON.parse(raw) : null;
    },
    isLoggedIn() {
        return !!this.getToken();
    },
    setSession(token, user) {
        localStorage.setItem(TOKEN_KEY, token);
        localStorage.setItem(USER_KEY, JSON.stringify(user));
    },
    clearSession() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
    },
};
window.AuthState = AuthState;
window.apiRequest = apiRequest;

// ---------------------------------------------------------
// Low-level API helper
// ---------------------------------------------------------
async function apiRequest(path, { method = "GET", body = null, auth = false } = {}) {
    const headers = { "Content-Type": "application/json" };

<<<<<<< HEAD
    const token = AuthState.getToken();
    if (auth || token) {
=======
    if (auth) {
        const token = AuthState.getToken();
>>>>>>> 1bdcedfdb8d69998a11cf16735225a5f5770ab40
        if (token) headers["Authorization"] = "Bearer " + token;
    }

    let response;
    try {
        response = await fetch(window.APP_CONFIG.API_BASE_URL + path, {
            method,
            headers,
            body: body ? JSON.stringify(body) : undefined,
        });
    } catch (networkErr) {
        // Backend unreachable / CORS / offline
        throw new Error(
            "Can't reach the AthleteLink server. Make sure the backend is running on " +
                window.APP_CONFIG.API_BASE_URL
        );
    }

    let data = null;
    try {
        data = await response.json();
    } catch (_) {
        // No JSON body (e.g. empty 204) - that's fine.
    }

    if (!response.ok) {
        const message = (data && data.message) || "Something went wrong. Please try again.";
        const error = new Error(message);
        error.details = data && data.details;
        error.status = response.status;
        throw error;
    }

    return data;
}

// ---------------------------------------------------------
// Modal open/close + mode switching
// ---------------------------------------------------------
function openAuth(mode) {
    document.getElementById("auth-overlay").classList.remove("hidden");
    switchAuthMode(mode || "login");
}

function closeAuth() {
    document.getElementById("auth-overlay").classList.add("hidden");
    clearAuthBanner();
    clearFieldErrors();
}

function switchAuthMode(mode) {
    const loginForm = document.getElementById("login-form");
    const registerForm = document.getElementById("register-form");
    const title = document.getElementById("auth-title");
    const subtitle = document.getElementById("auth-subtitle");

    clearAuthBanner();
    clearFieldErrors();

    if (mode === "register") {
        loginForm.classList.add("hidden");
        registerForm.classList.remove("hidden");
        title.textContent = "Join AthleteLink AI";
        subtitle.textContent = "Create your athlete profile in a minute";
    } else {
        registerForm.classList.add("hidden");
        loginForm.classList.remove("hidden");
        title.textContent = "Welcome Back";
        subtitle.textContent = "Login to access your AthleteLink dashboard";
    }
}

// Close modal when clicking the dark overlay itself (not the card)
document.addEventListener("DOMContentLoaded", () => {
    const overlay = document.getElementById("auth-overlay");
    if (overlay) {
        overlay.addEventListener("click", (e) => {
            if (e.target === overlay) closeAuth();
        });
    }
});

// ---------------------------------------------------------
// Banners & field errors
// ---------------------------------------------------------
function showAuthBanner(message, type) {
    const banner = document.getElementById("auth-banner");
    banner.textContent = message;
    banner.className = "auth-banner show " + type;
}

function clearAuthBanner() {
    const banner = document.getElementById("auth-banner");
    banner.className = "auth-banner";
    banner.textContent = "";
}

function clearFieldErrors() {
    document.querySelectorAll(".auth-field-error").forEach((el) => (el.textContent = ""));
}

function setFieldError(fieldId, message) {
    const el = document.getElementById(fieldId + "-error");
    if (el) el.textContent = message;
}

function setButtonLoading(buttonId, loading, label) {
    const btn = document.getElementById(buttonId);
    if (!btn) return;
    btn.disabled = loading;
    btn.innerHTML = loading ? '<span class="btn-spinner"></span>Please wait…' : label;
}

// ---------------------------------------------------------
// LOGIN
// ---------------------------------------------------------
async function handleLogin(event) {
    event.preventDefault();
    clearAuthBanner();
    clearFieldErrors();

    const email = document.getElementById("login-email").value.trim();
    const password = document.getElementById("login-password").value;

    if (!email) return setFieldError("login-email", "Email is required");
    if (!password) return setFieldError("login-password", "Password is required");

    setButtonLoading("login-submit-btn", true);

    try {
        const data = await apiRequest("/auth/login", {
            method: "POST",
            body: { email, password },
        });

        const role = data.role || (data.email === "admin@athletelink.ai" ? "ADMIN" : "ATHLETE");

        AuthState.setSession(data.token, {
            id: data.userId,
            fullName: data.fullName,
            email: data.email,
            role: role
        });

        showAuthBanner("Login successful! Redirecting…", "success");
        refreshAuthUI();

        setTimeout(() => {
            closeAuth();
            if (role === "ADMIN") {
                window.location.href = "/admin.html";
            } else {
                switchTab("dashboard");
            }
        }, 600);
    } catch (err) {
        showAuthBanner(err.message, "error");
    } finally {
        setButtonLoading("login-submit-btn", false, "Login");
    }

    return false;
}

// ---------------------------------------------------------
// REGISTER
// ---------------------------------------------------------
async function handleRegister(event) {
    event.preventDefault();
    clearAuthBanner();
    clearFieldErrors();

    const fullName = document.getElementById("register-name").value.trim();
    const email = document.getElementById("register-email").value.trim();
    const sport = document.getElementById("register-sport").value.trim();
    const location = document.getElementById("register-location").value.trim();
    const password = document.getElementById("register-password").value;
    const confirmPassword = document.getElementById("register-confirm-password").value;

    let hasError = false;
    if (!fullName) { setFieldError("register-name", "Full name is required"); hasError = true; }
    if (!email) { setFieldError("register-email", "Email is required"); hasError = true; }
    if (!password || password.length < 6) {
        setFieldError("register-password", "Password must be at least 6 characters");
        hasError = true;
    }
    if (password !== confirmPassword) {
        setFieldError("register-confirm-password", "Passwords do not match");
        hasError = true;
    }
    if (hasError) return false;

    setButtonLoading("register-submit-btn", true);

    try {
        const data = await apiRequest("/auth/register", {
            method: "POST",
            body: { fullName, email, password, confirmPassword, sport, location },
        });

        AuthState.setSession(data.token, {
            id: data.userId,
            fullName: data.fullName,
            email: data.email,
        });

        showAuthBanner("Account created! Taking you to your dashboard…", "success");
        refreshAuthUI();

        setTimeout(() => {
            closeAuth();
            switchTab("dashboard");
        }, 600);
    } catch (err) {
        if (err.details && Array.isArray(err.details)) {
            err.details.forEach((d) => {
                const [field, msg] = d.split(/:\s(.+)/);
                const map = {
                    fullName: "register-name",
                    email: "register-email",
                    password: "register-password",
                    confirmPassword: "register-confirm-password",
                };
                if (map[field]) setFieldError(map[field], msg || d);
            });
        }
        showAuthBanner(err.message, "error");
    } finally {
        setButtonLoading("register-submit-btn", false, "Create Account");
    }

    return false;
}

// ---------------------------------------------------------
// LOGOUT
// ---------------------------------------------------------
async function logout() {
    try {
        await apiRequest("/auth/logout", { method: "POST", auth: true });
    } catch (_) {
        // Even if the network call fails, we still clear the local session.
    }
    AuthState.clearSession();
    refreshAuthUI();
    switchTab("home");
}

// ---------------------------------------------------------
// UI sync based on login state
// ---------------------------------------------------------
function refreshAuthUI() {
    const loggedIn = AuthState.isLoggedIn();
    const user = AuthState.getUser();

    document.getElementById("auth-buttons-guest").classList.toggle("hidden", loggedIn);
    document.getElementById("auth-buttons-user").classList.toggle("flex", loggedIn);
    document.getElementById("auth-buttons-user").classList.toggle("hidden", !loggedIn);

    document.querySelectorAll(".auth-guest-only").forEach((el) => el.classList.toggle("hidden", loggedIn));
    document.querySelectorAll(".auth-user-only").forEach((el) => el.classList.toggle("hidden", !loggedIn));

    if (loggedIn && user) {
        const navName = document.getElementById("nav-user-name");
        if (navName) navName.textContent = user.fullName;

        const dashName = document.getElementById("dash-user-name");
        if (dashName) dashName.textContent = user.fullName;

        const dashId = document.getElementById("dash-user-id");
        if (dashId) dashId.textContent = "ID: IND-ATH-" + user.id;

        const adminLink = document.getElementById("nav-admin-link");
        if (adminLink) {
            const isAdmin = user.role === "ADMIN" || user.email === "admin@athletelink.ai";
            adminLink.classList.toggle("hidden", !isAdmin);
            adminLink.classList.toggle("flex", isAdmin);
        }

        loadProfileIntoDashboard();
        if (typeof loadAthleteFeatures === "function") loadAthleteFeatures();
    }
}

// Fetch the authoritative profile (sport/location) from the backend
// and reflect it on the dashboard header card.
async function loadProfileIntoDashboard() {
    try {
        const profile = await apiRequest("/user/profile", { method: "GET", auth: true });

        const sportEl = document.getElementById("dash-user-sport");
        if (sportEl) sportEl.textContent = profile.sport || "Athlete";

        const locationEl = document.getElementById("dash-user-location");
        if (locationEl) {
            const span = locationEl.querySelector("span");
            if (span) span.textContent = profile.location || "Location not set";
        }
    } catch (err) {
        // If the token has expired/is invalid, log the user out cleanly.
        if (err.status === 401 || err.status === 403) {
            AuthState.clearSession();
            refreshAuthUI();
        }
    }
}

// Run once on page load: restore session UI state if a token exists.
document.addEventListener("DOMContentLoaded", refreshAuthUI);
