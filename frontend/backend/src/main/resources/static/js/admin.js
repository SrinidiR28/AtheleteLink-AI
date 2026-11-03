// =========================================================================
// AthleteLink AI - Professional Admin Dashboard Controller
// =========================================================================

let currentAdminTab = "dashboard";
let sportsChartInstance = null;
let appsChartInstance = null;

// Cache storage for table filtering
let allUsersData = [];
let allAthletesData = [];
let allApplicationsData = { trialApplications: [], opportunityApplications: [] };
let allScholarshipAppsData = [];
let allAchievementsData = [];

document.addEventListener("DOMContentLoaded", () => {
    initSidebarToggle();
    checkAdminAuth();
});

function initSidebarToggle() {
    const btn = document.getElementById("sidebar-toggle-btn");
    const sidebar = document.getElementById("admin-sidebar");
    if (btn && sidebar) {
        btn.addEventListener("click", () => {
            sidebar.classList.toggle("-translate-x-full");
        });
    }
}

// -------------------------------------------------------------------------
// AUTHENTICATION & ACCESS CONTROL
// -------------------------------------------------------------------------
function checkAdminAuth() {
    const token = localStorage.getItem("athletelink_token");
    const userRaw = localStorage.getItem("athletelink_user");
    let user = null;
    try { user = userRaw ? JSON.parse(userRaw) : null; } catch (_) {}

    if (!token || !user || (user.role !== "ADMIN" && user.email !== "admin@athletelink.ai")) {
        // Show Admin Login modal
        openAdminModal("modal-admin-login");
        return;
    }

    // Set admin email badge in header
    const emailBadge = document.getElementById("admin-header-email");
    if (emailBadge && user) emailBadge.textContent = user.email || "admin@athletelink.ai";

    closeAdminModal("modal-admin-login");
    loadAdminStats();
}

async function handleAdminLogin(e) {
    e.preventDefault();
    const email = document.getElementById("admin-login-email").value.trim();
    const password = document.getElementById("admin-login-password").value;
    const errorEl = document.getElementById("admin-login-error");
    if (errorEl) errorEl.classList.add("hidden");

    try {
        const res = await fetch((window.APP_CONFIG?.API_BASE_URL || "/api") + "/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, password })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Invalid admin credentials");

        if (data.role !== "ADMIN" && data.email !== "admin@athletelink.ai") {
            throw new Error("Access denied: Your account does not have platform administrator privileges.");
        }

        localStorage.setItem("athletelink_token", data.token);
        localStorage.setItem("athletelink_user", JSON.stringify({
            id: data.userId,
            fullName: data.fullName,
            email: data.email,
            role: data.role || "ADMIN"
        }));

        showAdminToast("Welcome back, Administrator!", "success");
        closeAdminModal("modal-admin-login");
        
        const emailBadge = document.getElementById("admin-header-email");
        if (emailBadge) emailBadge.textContent = data.email;

        loadAdminStats();
    } catch (err) {
        if (errorEl) {
            errorEl.textContent = err.message;
            errorEl.classList.remove("hidden");
        }
    }
}

function quickDemoLogin() {
    document.getElementById("admin-login-email").value = "admin@athletelink.ai";
    document.getElementById("admin-login-password").value = "Admin@123";
    handleAdminLogin(new Event("submit"));
}

function adminLogout() {
    localStorage.removeItem("athletelink_token");
    localStorage.removeItem("athletelink_user");
    window.location.href = "/";
}

// -------------------------------------------------------------------------
// LOW-LEVEL ADMIN API HELPER
// -------------------------------------------------------------------------
async function adminFetch(endpoint, { method = "GET", body = null } = {}) {
    const token = localStorage.getItem("athletelink_token");
    const headers = { "Content-Type": "application/json" };
    if (token) headers["Authorization"] = "Bearer " + token;

    const base = window.APP_CONFIG?.API_BASE_URL || "/api";
    const res = await fetch(base + endpoint, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined
    });

    if (res.status === 401 || res.status === 403) {
        openAdminModal("modal-admin-login");
        throw new Error("Admin session expired. Please sign in.");
    }

    const data = await res.json().catch(() => null);
    if (!res.ok) {
        throw new Error(data?.message || "Operation failed (" + res.status + ")");
    }
    return data;
}

// -------------------------------------------------------------------------
// TAB NAVIGATION
// -------------------------------------------------------------------------
const ADMIN_TABS = ["dashboard", "users", "athletes", "coaches", "scouts", "opportunities", "trials", "scholarships", "applications", "achievements", "announcements", "settings"];

function switchAdminTab(tabId) {
    currentAdminTab = tabId;

    // Update active state in sidebar
    document.querySelectorAll(".nav-item").forEach((el, idx) => {
        const itemTab = ADMIN_TABS[idx];
        el.classList.toggle("active", itemTab === tabId);
    });

    // Toggle tab sections
    ADMIN_TABS.forEach(t => {
        const sec = document.getElementById("tab-" + t);
        if (sec) sec.classList.toggle("hidden", t !== tabId);
    });

    // Close mobile sidebar on navigation
    document.getElementById("admin-sidebar")?.classList.add("-translate-x-full");

    // Load tab-specific data
    switch (tabId) {
        case "dashboard": loadAdminStats(); break;
        case "users": loadUsers(); break;
        case "athletes": loadAthletes(); break;
        case "coaches": loadCoaches(); break;
        case "scouts": loadScouts(); break;
        case "opportunities": loadOpportunities(); break;
        case "trials": loadTrials(); break;
        case "scholarships": loadScholarships(); loadScholarshipApplications(); break;
        case "applications": loadApplications(); break;
        case "achievements": loadAchievements(); break;
        case "announcements": loadAnnouncements(); break;
        case "settings": loadSystemStatus(); break;
    }
}

// -------------------------------------------------------------------------
// 1. DASHBOARD OVERVIEW & ANALYTICS
// -------------------------------------------------------------------------
async function loadAdminStats() {
    try {
        const stats = await adminFetch("/admin/stats");
        
        // Populate KPIs
        document.getElementById("kpi-users").textContent = stats.totalUsers ?? 0;
        document.getElementById("kpi-athletes").textContent = stats.totalAthletes ?? 0;
        document.getElementById("kpi-coaches").textContent = stats.totalCoaches ?? 0;
        document.getElementById("kpi-scouts").textContent = stats.totalScouts ?? 0;
        document.getElementById("kpi-opportunities").textContent = stats.totalOpportunities ?? 0;
        document.getElementById("kpi-trials").textContent = stats.totalTrials ?? 0;
        document.getElementById("kpi-scholarships").textContent = stats.totalScholarships ?? 0;
        document.getElementById("kpi-applications").textContent = stats.totalApplications ?? 0;

        // Render Charts
        renderSportsChart(stats.sportsDistribution || []);
        renderAppsChart(stats.applicationStatus || []);

        // Render Recent Activity Feed
        renderActivityFeed(stats.recentActivity || []);
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

function renderSportsChart(data) {
    const ctx = document.getElementById("chart-sports")?.getContext("2d");
    if (!ctx) return;
    if (sportsChartInstance) sportsChartInstance.destroy();

    const labels = data.length ? data.map(d => d.sport_name) : ["Athletics", "Cricket", "Hockey", "Boxing", "Badminton"];
    const values = data.length ? data.map(d => Number(d.athlete_count)) : [12, 8, 7, 5, 4];

    sportsChartInstance = new Chart(ctx, {
        type: "doughnut",
        data: {
            labels,
            datasets: [{
                data: values,
                backgroundColor: [
                    "#0ea5e9", "#10b981", "#22d3ee", "#f59e0b", "#a855f7", "#ec4899"
                ],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: "right", labels: { color: "#9ca3af", font: { family: "Outfit", size: 11 } } }
            },
            cutout: "68%"
        }
    });
}

function renderAppsChart(data) {
    const ctx = document.getElementById("chart-applications")?.getContext("2d");
    if (!ctx) return;
    if (appsChartInstance) appsChartInstance.destroy();

    const labels = ["SUBMITTED", "UNDER REVIEW", "SHORTLISTED", "APPROVED", "REJECTED"];
    const values = labels.map(l => {
        const item = data.find(d => String(d.status).toUpperCase() === l);
        return item ? Number(item.count) : 0;
    });

    appsChartInstance = new Chart(ctx, {
        type: "bar",
        data: {
            labels: ["Submitted", "Reviewing", "Shortlisted", "Approved", "Rejected"],
            datasets: [{
                label: "Applications",
                data: values,
                backgroundColor: ["#0ea5e9", "#f59e0b", "#a855f7", "#10b981", "#ef4444"],
                borderRadius: 8
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: { grid: { display: false }, ticks: { color: "#9ca3af", font: { family: "Outfit", size: 10 } } },
                y: { grid: { color: "rgba(255,255,255,0.05)" }, ticks: { color: "#9ca3af", stepSize: 1 } }
            },
            plugins: { legend: { display: false } }
        }
    });
}

function renderActivityFeed(items) {
    const container = document.getElementById("dashboard-activity-feed");
    if (!container) return;
    if (!items.length) {
        container.innerHTML = '<div class="text-xs text-gray-500 py-4 text-center">No platform activity recorded yet.</div>';
        return;
    }

    container.innerHTML = items.map(act => {
        let icon = '<i class="fa-solid fa-bell text-cyan-300"></i>';
        let title = act.full_name || act.athlete_name || "Platform Event";
        let sub = act.created_at || "Just now";
        let badge = '<span class="badge badge-cyan">Update</span>';

        if (act.activity_type === "USER_REGISTERED") {
            icon = '<i class="fa-solid fa-user-plus text-emerald-400"></i>';
            title = `New user joined: <b>${escapeHtml(act.full_name)}</b> (${escapeHtml(act.sport || "General")})`;
            badge = '<span class="badge badge-green">Signup</span>';
        } else if (act.activity_type === "TRIAL_APPLICATION") {
            icon = '<i class="fa-solid fa-stopwatch text-cyan-400"></i>';
            title = `<b>${escapeHtml(act.athlete_name)}</b> applied for <i>${escapeHtml(act.target_name || "Trial")}</i>`;
            badge = `<span class="badge badge-yellow">${escapeHtml(act.status || "SUBMITTED")}</span>`;
        }

        return `
            <div class="p-3 rounded-xl bg-darkbg/60 border border-gray-800/80 flex items-center justify-between gap-4">
                <div class="flex items-center gap-3">
                    <div class="w-8 h-8 rounded-lg bg-white/5 flex items-center justify-center text-sm">${icon}</div>
                    <div>
                        <div class="text-xs text-gray-200">${title}</div>
                        <div class="text-[10px] text-gray-500 mt-0.5">${sub}</div>
                    </div>
                </div>
                <div>${badge}</div>
            </div>
        `;
    }).join("");
}

// -------------------------------------------------------------------------
// 2. USERS MANAGEMENT
// -------------------------------------------------------------------------
async function loadUsers() {
    const tbody = document.getElementById("users-table-body");
    if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="p-6 text-center text-gray-500">Loading users from MySQL…</td></tr>';
    try {
        allUsersData = await adminFetch("/admin/users");
        filterUsersTable();
    } catch (err) {
        if (tbody) tbody.innerHTML = `<tr><td colspan="7" class="p-6 text-center text-red-400">${escapeHtml(err.message)}</td></tr>`;
    }
}

function filterUsersTable() {
    const q = (document.getElementById("users-search")?.value || "").toLowerCase();
    const roleFilter = document.getElementById("users-filter-role")?.value || "ALL";
    const statusFilter = document.getElementById("users-filter-status")?.value || "ALL";

    const filtered = allUsersData.filter(u => {
        const matchQ = (u.full_name || "").toLowerCase().includes(q) || (u.email || "").toLowerCase().includes(q) || (u.sport || "").toLowerCase().includes(q);
        const matchRole = roleFilter === "ALL" || (u.role || "ATHLETE").toUpperCase() === roleFilter;
        const matchStatus = statusFilter === "ALL" || (u.status || "ACTIVE").toUpperCase() === statusFilter;
        return matchQ && matchRole && matchStatus;
    });

    const tbody = document.getElementById("users-table-body");
    if (!tbody) return;

    if (!filtered.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="p-6 text-center text-gray-500">No users found matching filters.</td></tr>';
        return;
    }

    tbody.innerHTML = filtered.map(u => {
        const isSelf = u.email === "admin@athletelink.ai";
        const isActive = (u.status || "ACTIVE").toUpperCase() === "ACTIVE";
        const role = (u.role || "ATHLETE").toUpperCase();

        return `
            <tr class="hover:bg-white/[0.02] transition">
                <td class="p-4">
                    <div class="font-bold text-white">${escapeHtml(u.full_name)}</div>
                    <div class="text-[10px] text-gray-500">ID: #${u.id}</div>
                </td>
                <td class="p-4 text-cyan-300">${escapeHtml(u.email)}</td>
                <td class="p-4">
                    <div class="text-gray-300 font-semibold">${escapeHtml(u.sport || "Not specified")}</div>
                    <div class="text-[10px] text-gray-500">${escapeHtml(u.location || "Location not set")}</div>
                </td>
                <td class="p-4">
                    <span class="badge ${role === 'ADMIN' ? 'badge-purple' : 'badge-cyan'}">${role}</span>
                </td>
                <td class="p-4">
                    <button onclick="toggleUserStatus(${u.id}, '${isActive ? 'INACTIVE' : 'ACTIVE'}')" ${isSelf ? 'disabled' : ''} class="badge ${isActive ? 'badge-green hover:bg-emerald-500/20' : 'badge-red hover:bg-red-500/20'} cursor-pointer transition">
                        ${isActive ? '● Active' : '○ Inactive'}
                    </button>
                </td>
                <td class="p-4 text-gray-400 text-[11px]">${escapeHtml(u.created_at ? String(u.created_at).substring(0, 10) : "-")}</td>
                <td class="p-4 text-right space-x-1">
                    <button onclick="editUser(${u.id})" class="p-1.5 rounded-lg bg-white/5 hover:bg-cyan-400/20 text-cyan-300" title="Edit User"><i class="fa-solid fa-pen"></i></button>
                    ${!isSelf ? `<button onclick="deleteUser(${u.id})" class="p-1.5 rounded-lg bg-white/5 hover:bg-red-400/20 text-red-300" title="Delete User"><i class="fa-solid fa-trash"></i></button>` : ''}
                </td>
            </tr>
        `;
    }).join("");
}

function openAddUserModal() {
    document.getElementById("modal-user-title").textContent = "Add User";
    document.getElementById("user-form-id").value = "";
    document.getElementById("user-form-name").value = "";
    document.getElementById("user-form-email").value = "";
    document.getElementById("user-form-password").value = "";
    document.getElementById("user-form-pass-hint").textContent = "(Default: Password@123)";
    document.getElementById("user-form-sport").value = "";
    document.getElementById("user-form-location").value = "";
    document.getElementById("user-form-role").value = "ATHLETE";
    document.getElementById("user-form-status").value = "ACTIVE";
    openAdminModal("modal-user");
}

function editUser(id) {
    const u = allUsersData.find(x => x.id === id);
    if (!u) return;
    document.getElementById("modal-user-title").textContent = "Edit User";
    document.getElementById("user-form-id").value = u.id;
    document.getElementById("user-form-name").value = u.full_name || "";
    document.getElementById("user-form-email").value = u.email || "";
    document.getElementById("user-form-password").value = "";
    document.getElementById("user-form-pass-hint").textContent = "(Leave blank to keep current)";
    document.getElementById("user-form-sport").value = u.sport || "";
    document.getElementById("user-form-location").value = u.location || "";
    document.getElementById("user-form-role").value = (u.role || "ATHLETE").toUpperCase();
    document.getElementById("user-form-status").value = (u.status || "ACTIVE").toUpperCase();
    openAdminModal("modal-user");
}

async function saveUser(e) {
    e.preventDefault();
    const id = document.getElementById("user-form-id").value;
    const body = {
        fullName: document.getElementById("user-form-name").value.trim(),
        email: document.getElementById("user-form-email").value.trim(),
        password: document.getElementById("user-form-password").value,
        sport: document.getElementById("user-form-sport").value.trim(),
        location: document.getElementById("user-form-location").value.trim(),
        role: document.getElementById("user-form-role").value,
        status: document.getElementById("user-form-status").value
    };

    try {
        if (id) {
            await adminFetch(`/admin/users/${id}`, { method: "PUT", body });
            showAdminToast("User updated successfully", "success");
        } else {
            await adminFetch("/admin/users", { method: "POST", body });
            showAdminToast("User created successfully", "success");
        }
        closeAdminModal("modal-user");
        loadUsers();
        loadAdminStats();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

async function toggleUserStatus(id, newStatus) {
    try {
        await adminFetch(`/admin/users/${id}/status`, { method: "PATCH", body: { status: newStatus } });
        showAdminToast("User status changed to " + newStatus, "success");
        loadUsers();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

async function deleteUser(id) {
    if (!confirm("Are you sure you want to delete this user? All related submissions and records will be removed.")) return;
    try {
        await adminFetch(`/admin/users/${id}`, { method: "DELETE" });
        showAdminToast("User deleted from platform", "success");
        loadUsers();
        loadAdminStats();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

// -------------------------------------------------------------------------
// 3. ATHLETES MANAGEMENT
// -------------------------------------------------------------------------
async function loadAthletes() {
    const tbody = document.getElementById("athletes-table-body");
    if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="p-6 text-center text-gray-500">Loading athlete profiles…</td></tr>';
    try {
        allAthletesData = await adminFetch("/admin/athletes");
        populateSportFilter(allAthletesData);
        filterAthletesTable();
    } catch (err) {
        if (tbody) tbody.innerHTML = `<tr><td colspan="7" class="p-6 text-center text-red-400">${escapeHtml(err.message)}</td></tr>`;
    }
}

function populateSportFilter(athletes) {
    const sel = document.getElementById("athletes-filter-sport");
    if (!sel) return;
    const sports = [...new Set(athletes.map(a => a.sport).filter(Boolean))];
    sel.innerHTML = '<option value="ALL">All Sports</option>' + sports.map(s => `<option value="${escapeHtml(s)}">${escapeHtml(s)}</option>`).join("");
}

function filterAthletesTable() {
    const q = (document.getElementById("athletes-search")?.value || "").toLowerCase();
    const sportFilter = document.getElementById("athletes-filter-sport")?.value || "ALL";

    const filtered = allAthletesData.filter(a => {
        const matchQ = (a.full_name || "").toLowerCase().includes(q) || (a.email || "").toLowerCase().includes(q) || (a.location || "").toLowerCase().includes(q);
        const matchSport = sportFilter === "ALL" || a.sport === sportFilter;
        return matchQ && matchSport;
    });

    const tbody = document.getElementById("athletes-table-body");
    if (!tbody) return;

    if (!filtered.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="p-6 text-center text-gray-500">No athletes found matching criteria.</td></tr>';
        return;
    }

    tbody.innerHTML = filtered.map(a => {
        const initials = (a.full_name || "A").split(/\s+/).map(x => x[0]).join("").toUpperCase().substring(0, 2);
        return `
            <tr class="hover:bg-white/[0.02] transition">
                <td class="p-4">
                    <div class="flex items-center gap-3">
                        <div class="w-9 h-9 rounded-xl bg-gradient-to-tr from-cyan-400/20 to-emerald-400/20 border border-cyan-400/30 flex items-center justify-center font-bold text-cyan-300 text-xs">
                            ${initials}
                        </div>
                        <div>
                            <div class="font-bold text-white">${escapeHtml(a.full_name)}</div>
                            <div class="text-[10px] text-gray-500">${escapeHtml(a.email)}</div>
                        </div>
                    </div>
                </td>
                <td class="p-4"><span class="badge badge-cyan">${escapeHtml(a.sport || "General")}</span></td>
                <td class="p-4 text-gray-300 font-medium">${escapeHtml(a.location || "Location not set")}</td>
                <td class="p-4 text-center">
                    <span class="font-bold text-emerald-400">${a.verified_achievements_count || 0}</span> / <span class="text-gray-500">${a.achievements_count || 0}</span>
                </td>
                <td class="p-4 text-center font-bold text-yellow-300">${a.applications_count || 0}</td>
                <td class="p-4 text-center font-bold text-cyan-300">${a.bookings_count || 0}</td>
                <td class="p-4 text-right">
                    <button onclick="viewAthleteProfile(${a.id})" class="px-3 py-1.5 rounded-lg bg-cyan-400/10 hover:bg-cyan-400/20 border border-cyan-400/30 text-cyan-300 font-bold text-[11px] transition">
                        View Profile →
                    </button>
                </td>
            </tr>
        `;
    }).join("");
}

async function viewAthleteProfile(id) {
    try {
        const data = await adminFetch(`/admin/athletes/${id}`);
        const p = data.profile || {};
        
        document.getElementById("athlete-modal-name").textContent = p.full_name || "Athlete";
        document.getElementById("athlete-modal-email").textContent = p.email || "";
        document.getElementById("athlete-modal-sport").textContent = p.sport || "Sport not set";
        document.getElementById("athlete-modal-loc").textContent = p.location || "Location not set";
        
        const initials = (p.full_name || "A").split(/\s+/).map(x => x[0]).join("").toUpperCase().substring(0, 2);
        document.getElementById("athlete-modal-avatar").textContent = initials;

        const achHtml = (data.achievements || []).map(a => `
            <div class="p-3 rounded-xl bg-darkbg border border-gray-800 flex justify-between items-start">
                <div>
                    <div class="font-bold text-white text-xs">${escapeHtml(a.title)}</div>
                    <div class="text-[11px] text-gray-400 mt-0.5">${escapeHtml(a.sport || "")} • ${escapeHtml(a.level || "")} • ${escapeHtml(a.achievement_year || a.achieved_on || "")}</div>
                    <div class="text-[11px] text-yellow-400 mt-1 font-semibold">${escapeHtml(a.medal_award || a.position || "")}</div>
                </div>
                <span class="badge ${a.verification_status === 'VERIFIED' ? 'badge-green' : (a.verification_status === 'REJECTED' ? 'badge-red' : 'badge-yellow')}">${escapeHtml(a.verification_status || 'PENDING')}</span>
            </div>
        `).join("") || '<p class="text-gray-500">No achievements recorded.</p>';

        const appHtml = (data.trialApplications || []).map(ta => `
            <div class="p-3 rounded-xl bg-darkbg border border-gray-800 flex justify-between items-start">
                <div>
                    <div class="font-bold text-white text-xs">${escapeHtml(ta.trial_name)}</div>
                    <div class="text-[11px] text-gray-400 mt-0.5">${escapeHtml(ta.venue || "")} • ${escapeHtml(ta.trial_date || "")}</div>
                </div>
                <span class="badge badge-cyan">${escapeHtml(ta.status || "SUBMITTED")}</span>
            </div>
        `).join("") || '<p class="text-gray-500">No trial applications submitted.</p>';

        document.getElementById("athlete-modal-content").innerHTML = `
            <div>
                <h4 class="text-xs font-black uppercase text-cyan-300 mb-2">Registered Achievements (${(data.achievements || []).length})</h4>
                <div class="space-y-2">${achHtml}</div>
            </div>
            <div>
                <h4 class="text-xs font-black uppercase text-emerald-300 mb-2">Trial Applications (${(data.trialApplications || []).length})</h4>
                <div class="space-y-2">${appHtml}</div>
            </div>
        `;

        openAdminModal("modal-athlete-profile");
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

// -------------------------------------------------------------------------
// 4. COACHES MANAGEMENT
// -------------------------------------------------------------------------
async function loadCoaches() {
    try {
        const coaches = await adminFetch("/admin/coaches");
        const connections = await adminFetch("/admin/coaches/connections");

        document.getElementById("coaches-count-badge").textContent = `${coaches.length} Coaches`;

        const tbody = document.getElementById("coaches-table-body");
        tbody.innerHTML = coaches.map(c => `
            <tr class="hover:bg-white/[0.02] transition">
                <td class="p-4">
                    <div class="font-bold text-white">${escapeHtml(c.name)}</div>
                    <div class="text-[10px] text-cyan-300">${escapeHtml(c.organization)}</div>
                </td>
                <td class="p-4 text-gray-300 font-semibold">${escapeHtml(c.sports)}</td>
                <td class="p-4 text-gray-400">${escapeHtml(c.specialization)}</td>
                <td class="p-4 text-gray-400">${escapeHtml(c.experience)}</td>
                <td class="p-4">
                    <div class="text-gray-300">${escapeHtml(c.location)}</div>
                    <div class="text-[10px] text-gray-500 font-mono">${c.latitude}, ${c.longitude}</div>
                </td>
                <td class="p-4 text-right space-x-1">
                    <button onclick="editCoach(${c.id})" class="p-1.5 rounded-lg bg-white/5 hover:bg-cyan-400/20 text-cyan-300"><i class="fa-solid fa-pen"></i></button>
                    <button onclick="deleteCoach(${c.id})" class="p-1.5 rounded-lg bg-white/5 hover:bg-red-400/20 text-red-300"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>
        `).join("") || '<tr><td colspan="6" class="p-6 text-center text-gray-500">No coaches in directory.</td></tr>';

        // Render connection requests
        const connBody = document.getElementById("coach-connections-table-body");
        connBody.innerHTML = connections.map(conn => `
            <tr class="hover:bg-white/[0.02] transition">
                <td class="p-4">
                    <div class="font-bold text-white">${escapeHtml(conn.athlete_name)}</div>
                    <div class="text-[10px] text-gray-500">${escapeHtml(conn.athlete_email)}</div>
                </td>
                <td class="p-4 font-bold text-cyan-300">${escapeHtml(conn.coach_name)}</td>
                <td class="p-4 text-gray-400 text-[11px] max-w-xs truncate">${escapeHtml(conn.message || "Connection request")}</td>
                <td class="p-4 text-gray-500 text-[11px]">${String(conn.created_at).substring(0, 10)}</td>
                <td class="p-4"><span class="badge ${conn.status === 'CONNECTED' ? 'badge-green' : (conn.status === 'REJECTED' ? 'badge-red' : 'badge-yellow')}">${conn.status}</span></td>
                <td class="p-4 text-right space-x-1">
                    <button onclick="updateCoachConnStatus(${conn.id}, 'CONNECTED')" class="px-2 py-1 rounded-md bg-emerald-400/10 hover:bg-emerald-400/20 text-emerald-400 font-bold text-[10px]">Accept</button>
                    <button onclick="updateCoachConnStatus(${conn.id}, 'REJECTED')" class="px-2 py-1 rounded-md bg-red-400/10 hover:bg-red-400/20 text-red-400 font-bold text-[10px]">Reject</button>
                </td>
            </tr>
        `).join("") || '<tr><td colspan="6" class="p-6 text-center text-gray-500">No incoming coach connection requests.</td></tr>';

    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

let allCoachesData = [];
function openAddCoachModal() {
    document.getElementById("modal-coach-title").textContent = "Add Coach";
    document.getElementById("coach-form-id").value = "";
    document.getElementById("coach-form-name").value = "";
    document.getElementById("coach-form-org").value = "";
    document.getElementById("coach-form-sports").value = "";
    document.getElementById("coach-form-spec").value = "";
    document.getElementById("coach-form-exp").value = "";
    document.getElementById("coach-form-loc").value = "";
    document.getElementById("coach-form-lat").value = "13.0827";
    document.getElementById("coach-form-lon").value = "80.2707";
    openAdminModal("modal-coach");
}

async function editCoach(id) {
    const coaches = await adminFetch("/admin/coaches");
    const c = coaches.find(x => x.id === id);
    if (!c) return;
    document.getElementById("modal-coach-title").textContent = "Edit Coach";
    document.getElementById("coach-form-id").value = c.id;
    document.getElementById("coach-form-name").value = c.name || "";
    document.getElementById("coach-form-org").value = c.organization || "";
    document.getElementById("coach-form-sports").value = c.sports || "";
    document.getElementById("coach-form-spec").value = c.specialization || "";
    document.getElementById("coach-form-exp").value = c.experience || "";
    document.getElementById("coach-form-loc").value = c.location || "";
    document.getElementById("coach-form-lat").value = c.latitude || "";
    document.getElementById("coach-form-lon").value = c.longitude || "";
    openAdminModal("modal-coach");
}

async function saveCoach(e) {
    e.preventDefault();
    const id = document.getElementById("coach-form-id").value;
    const body = {
        name: document.getElementById("coach-form-name").value.trim(),
        organization: document.getElementById("coach-form-org").value.trim(),
        sports: document.getElementById("coach-form-sports").value.trim(),
        specialization: document.getElementById("coach-form-spec").value.trim(),
        experience: document.getElementById("coach-form-exp").value.trim(),
        location: document.getElementById("coach-form-loc").value.trim(),
        latitude: document.getElementById("coach-form-lat").value,
        longitude: document.getElementById("coach-form-lon").value
    };

    try {
        if (id) {
            await adminFetch(`/admin/coaches/${id}`, { method: "PUT", body });
            showAdminToast("Coach updated successfully", "success");
        } else {
            await adminFetch("/admin/coaches", { method: "POST", body });
            showAdminToast("Coach added to directory", "success");
        }
        closeAdminModal("modal-coach");
        loadCoaches();
        loadAdminStats();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

async function deleteCoach(id) {
    if (!confirm("Delete this coach?")) return;
    try {
        await adminFetch(`/admin/coaches/${id}`, { method: "DELETE" });
        showAdminToast("Coach deleted", "success");
        loadCoaches();
        loadAdminStats();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

async function updateCoachConnStatus(id, status) {
    try {
        await adminFetch(`/admin/coaches/connections/${id}/status`, { method: "PATCH", body: { status } });
        showAdminToast("Connection marked as " + status, "success");
        loadCoaches();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

// -------------------------------------------------------------------------
// 5. SCOUTS MANAGEMENT
// -------------------------------------------------------------------------
async function loadScouts() {
    try {
        const scouts = await adminFetch("/admin/scouts");
        const connections = await adminFetch("/admin/scouts/connections");

        document.getElementById("scouts-count-badge").textContent = `${scouts.length} Scouts`;

        const tbody = document.getElementById("scouts-table-body");
        tbody.innerHTML = scouts.map(s => `
            <tr class="hover:bg-white/[0.02] transition">
                <td class="p-4">
                    <div class="font-bold text-white">${escapeHtml(s.name)}</div>
                    <div class="text-[10px] text-yellow-400">${escapeHtml(s.organization)}</div>
                </td>
                <td class="p-4 text-gray-300 font-semibold">${escapeHtml(s.sports)}</td>
                <td class="p-4 text-gray-400">${escapeHtml(s.specialization)}</td>
                <td class="p-4 text-gray-400">${escapeHtml(s.experience)}</td>
                <td class="p-4 text-gray-300">${escapeHtml(s.location)}</td>
                <td class="p-4 text-right space-x-1">
                    <button onclick="editScout(${s.id})" class="p-1.5 rounded-lg bg-white/5 hover:bg-yellow-400/20 text-yellow-300"><i class="fa-solid fa-pen"></i></button>
                    <button onclick="deleteScout(${s.id})" class="p-1.5 rounded-lg bg-white/5 hover:bg-red-400/20 text-red-300"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>
        `).join("") || '<tr><td colspan="6" class="p-6 text-center text-gray-500">No scouts in directory.</td></tr>';

        // Render scout requests
        const connBody = document.getElementById("scout-connections-table-body");
        connBody.innerHTML = connections.map(conn => `
            <tr class="hover:bg-white/[0.02] transition">
                <td class="p-4">
                    <div class="font-bold text-white">${escapeHtml(conn.athlete_name)}</div>
                    <div class="text-[10px] text-gray-500">${escapeHtml(conn.athlete_email)}</div>
                </td>
                <td class="p-4 font-bold text-yellow-300">${escapeHtml(conn.scout_name)}</td>
                <td class="p-4 text-gray-400 text-[11px] max-w-xs truncate">${escapeHtml(conn.message || "Scout request")}</td>
                <td class="p-4 text-gray-500 text-[11px]">${String(conn.created_at).substring(0, 10)}</td>
                <td class="p-4"><span class="badge ${conn.status === 'CONNECTED' ? 'badge-green' : (conn.status === 'REJECTED' ? 'badge-red' : 'badge-yellow')}">${conn.status}</span></td>
                <td class="p-4 text-right space-x-1">
                    <button onclick="updateScoutConnStatus(${conn.id}, 'CONNECTED')" class="px-2 py-1 rounded-md bg-emerald-400/10 hover:bg-emerald-400/20 text-emerald-400 font-bold text-[10px]">Accept</button>
                    <button onclick="updateScoutConnStatus(${conn.id}, 'REJECTED')" class="px-2 py-1 rounded-md bg-red-400/10 hover:bg-red-400/20 text-red-400 font-bold text-[10px]">Reject</button>
                </td>
            </tr>
        `).join("") || '<tr><td colspan="6" class="p-6 text-center text-gray-500">No incoming scout requests.</td></tr>';

    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

function openAddScoutModal() {
    document.getElementById("modal-scout-title").textContent = "Add Scout";
    document.getElementById("scout-form-id").value = "";
    document.getElementById("scout-form-name").value = "";
    document.getElementById("scout-form-org").value = "";
    document.getElementById("scout-form-sports").value = "";
    document.getElementById("scout-form-spec").value = "";
    document.getElementById("scout-form-exp").value = "";
    document.getElementById("scout-form-loc").value = "";
    openAdminModal("modal-scout");
}

async function editScout(id) {
    const scouts = await adminFetch("/admin/scouts");
    const s = scouts.find(x => x.id === id);
    if (!s) return;
    document.getElementById("modal-scout-title").textContent = "Edit Scout";
    document.getElementById("scout-form-id").value = s.id;
    document.getElementById("scout-form-name").value = s.name || "";
    document.getElementById("scout-form-org").value = s.organization || "";
    document.getElementById("scout-form-sports").value = s.sports || "";
    document.getElementById("scout-form-spec").value = s.specialization || "";
    document.getElementById("scout-form-exp").value = s.experience || "";
    document.getElementById("scout-form-loc").value = s.location || "";
    openAdminModal("modal-scout");
}

async function saveScout(e) {
    e.preventDefault();
    const id = document.getElementById("scout-form-id").value;
    const body = {
        name: document.getElementById("scout-form-name").value.trim(),
        organization: document.getElementById("scout-form-org").value.trim(),
        sports: document.getElementById("scout-form-sports").value.trim(),
        specialization: document.getElementById("scout-form-spec").value.trim(),
        experience: document.getElementById("scout-form-exp").value.trim(),
        location: document.getElementById("scout-form-loc").value.trim()
    };

    try {
        if (id) {
            await adminFetch(`/admin/scouts/${id}`, { method: "PUT", body });
            showAdminToast("Scout updated successfully", "success");
        } else {
            await adminFetch("/admin/scouts", { method: "POST", body });
            showAdminToast("Scout added to directory", "success");
        }
        closeAdminModal("modal-scout");
        loadScouts();
        loadAdminStats();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

async function deleteScout(id) {
    if (!confirm("Delete this scout?")) return;
    try {
        await adminFetch(`/admin/scouts/${id}`, { method: "DELETE" });
        showAdminToast("Scout deleted", "success");
        loadScouts();
        loadAdminStats();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

async function updateScoutConnStatus(id, status) {
    try {
        await adminFetch(`/admin/scouts/connections/${id}/status`, { method: "PATCH", body: { status } });
        showAdminToast("Scout connection marked as " + status, "success");
        loadScouts();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

// -------------------------------------------------------------------------
// 6. OPPORTUNITIES MANAGEMENT
// -------------------------------------------------------------------------
async function loadOpportunities() {
    try {
        const opps = await adminFetch("/admin/opportunities");
        const tbody = document.getElementById("opportunities-table-body");
        tbody.innerHTML = opps.map(o => `
            <tr class="hover:bg-white/[0.02] transition">
                <td class="p-4">
                    <div class="font-bold text-white">${escapeHtml(o.name)}</div>
                    <div class="text-[10px] text-gray-500 max-w-sm truncate">${escapeHtml(o.description || "")}</div>
                </td>
                <td class="p-4"><span class="badge badge-cyan">${escapeHtml(o.type)}</span></td>
                <td class="p-4 font-mono text-gray-400 text-[11px]">${escapeHtml(o.slug)}</td>
                <td class="p-4 text-gray-300 font-semibold">${escapeHtml(o.deadline ? String(o.deadline).substring(0, 10) : "Open")}</td>
                <td class="p-4">
                    <button onclick="toggleOppStatus(${o.id}, ${!o.is_active})" class="badge ${o.is_active ? 'badge-green' : 'badge-red'} cursor-pointer">
                        ${o.is_active ? '● Active' : '○ Inactive'}
                    </button>
                </td>
                <td class="p-4 text-center font-bold text-cyan-300">${o.applications_count || 0}</td>
                <td class="p-4 text-right space-x-1">
                    <button onclick="editOpportunity(${o.id})" class="p-1.5 rounded-lg bg-white/5 hover:bg-cyan-400/20 text-cyan-300"><i class="fa-solid fa-pen"></i></button>
                    <button onclick="deleteOpportunity(${o.id})" class="p-1.5 rounded-lg bg-white/5 hover:bg-red-400/20 text-red-300"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>
        `).join("") || '<tr><td colspan="7" class="p-6 text-center text-gray-500">No opportunities created.</td></tr>';
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

function openAddOpportunityModal() {
    document.getElementById("modal-opp-title").textContent = "Add Opportunity";
    document.getElementById("opp-form-id").value = "";
    document.getElementById("opp-form-name").value = "";
    document.getElementById("opp-form-slug").value = "";
    document.getElementById("opp-form-type").value = "Academy & Training";
    document.getElementById("opp-form-deadline").value = "";
    document.getElementById("opp-form-desc").value = "";
    openAdminModal("modal-opportunity");
}

async function editOpportunity(id) {
    const opps = await adminFetch("/admin/opportunities");
    const o = opps.find(x => x.id === id);
    if (!o) return;
    document.getElementById("modal-opp-title").textContent = "Edit Opportunity";
    document.getElementById("opp-form-id").value = o.id;
    document.getElementById("opp-form-name").value = o.name || "";
    document.getElementById("opp-form-slug").value = o.slug || "";
    document.getElementById("opp-form-type").value = o.type || "Academy & Training";
    document.getElementById("opp-form-deadline").value = o.deadline ? String(o.deadline).substring(0, 10) : "";
    document.getElementById("opp-form-desc").value = o.description || "";
    openAdminModal("modal-opportunity");
}

async function saveOpportunity(e) {
    e.preventDefault();
    const id = document.getElementById("opp-form-id").value;
    const body = {
        name: document.getElementById("opp-form-name").value.trim(),
        slug: document.getElementById("opp-form-slug").value.trim(),
        type: document.getElementById("opp-form-type").value,
        deadline: document.getElementById("opp-form-deadline").value || null,
        description: document.getElementById("opp-form-desc").value.trim(),
        isActive: true
    };

    try {
        if (id) {
            await adminFetch(`/admin/opportunities/${id}`, { method: "PUT", body });
            showAdminToast("Opportunity updated", "success");
        } else {
            await adminFetch("/admin/opportunities", { method: "POST", body });
            showAdminToast("Opportunity published", "success");
        }
        closeAdminModal("modal-opportunity");
        loadOpportunities();
        loadAdminStats();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

async function toggleOppStatus(id, isActive) {
    try {
        await adminFetch(`/admin/opportunities/${id}/status`, { method: "PATCH", body: { isActive } });
        showAdminToast("Opportunity status updated", "success");
        loadOpportunities();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

async function deleteOpportunity(id) {
    if (!confirm("Delete this opportunity?")) return;
    try {
        await adminFetch(`/admin/opportunities/${id}`, { method: "DELETE" });
        showAdminToast("Opportunity deleted", "success");
        loadOpportunities();
        loadAdminStats();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

// -------------------------------------------------------------------------
// 7. TRIALS MANAGEMENT
// -------------------------------------------------------------------------
async function loadTrials() {
    try {
        const trials = await adminFetch("/admin/trials");
        const tbody = document.getElementById("trials-table-body");
        tbody.innerHTML = trials.map(t => `
            <tr class="hover:bg-white/[0.02] transition">
                <td class="p-4">
                    <div class="font-bold text-white">${escapeHtml(t.name)}</div>
                    ${t.featured ? '<span class="badge badge-yellow mt-1 text-[9px]">★ Featured</span>' : ''}
                </td>
                <td class="p-4">
                    <div class="font-bold text-cyan-300">${escapeHtml(t.sport)}</div>
                    <div class="text-[10px] text-gray-500">${escapeHtml(t.organization)}</div>
                </td>
                <td class="p-4">
                    <div class="text-gray-200 font-semibold">${escapeHtml(t.venue)}</div>
                    <div class="text-[10px] text-emerald-400 font-bold mt-0.5"><i class="fa-solid fa-calendar mr-1"></i>${String(t.trial_date).substring(0, 10)}</div>
                </td>
                <td class="p-4">
                    <div class="text-gray-300 font-medium">${escapeHtml(t.category || "Open")}</div>
                    <div class="text-[10px] text-gray-500">${escapeHtml(t.age_category || "Youth")}</div>
                </td>
                <td class="p-4">
                    <span class="badge ${t.registration_status === 'REG OPEN' ? 'badge-green' : 'badge-red'}">${escapeHtml(t.registration_status || "REG OPEN")}</span>
                </td>
                <td class="p-4">
                    <div class="font-bold text-cyan-300">${t.booked_count || 0} / ${t.total_capacity || 0} Booked</div>
                    <div class="text-[10px] text-gray-400 font-semibold">${t.remaining_slots || 0} slots left</div>
                </td>
                <td class="p-4 text-right space-x-1">
                    <button onclick="editTrial(${t.id})" class="p-1.5 rounded-lg bg-white/5 hover:bg-cyan-400/20 text-cyan-300"><i class="fa-solid fa-pen"></i></button>
                    <button onclick="deleteTrial(${t.id})" class="p-1.5 rounded-lg bg-white/5 hover:bg-red-400/20 text-red-300"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>
        `).join("") || '<tr><td colspan="7" class="p-6 text-center text-gray-500">No trials recorded.</td></tr>';
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

function openAddTrialModal() {
    document.getElementById("modal-trial-title").textContent = "Add Trial";
    document.getElementById("trial-form-id").value = "";
    document.getElementById("trial-form-name").value = "";
    document.getElementById("trial-form-sport").value = "";
    document.getElementById("trial-form-org").value = "";
    document.getElementById("trial-form-venue").value = "";
    document.getElementById("trial-form-date").value = "";
    document.getElementById("trial-form-category").value = "Men's";
    document.getElementById("trial-form-age").value = "U19";
    document.getElementById("trial-form-reg-status").value = "REG OPEN";
    document.getElementById("trial-form-capacity").value = "30";
    document.getElementById("trial-form-featured").checked = true;
    openAdminModal("modal-trial");
}

async function editTrial(id) {
    const trials = await adminFetch("/admin/trials");
    const t = trials.find(x => x.id === id);
    if (!t) return;
    document.getElementById("modal-trial-title").textContent = "Edit Trial";
    document.getElementById("trial-form-id").value = t.id;
    document.getElementById("trial-form-name").value = t.name || "";
    document.getElementById("trial-form-sport").value = t.sport || "";
    document.getElementById("trial-form-org").value = t.organization || "";
    document.getElementById("trial-form-venue").value = t.venue || "";
    document.getElementById("trial-form-date").value = t.trial_date ? String(t.trial_date).substring(0, 10) : "";
    document.getElementById("trial-form-category").value = t.category || "Open";
    document.getElementById("trial-form-age").value = t.age_category || "Youth";
    document.getElementById("trial-form-reg-status").value = t.registration_status || "REG OPEN";
    document.getElementById("trial-form-capacity").value = t.total_capacity || "30";
    document.getElementById("trial-form-featured").checked = !!t.featured;
    openAdminModal("modal-trial");
}

async function saveTrial(e) {
    e.preventDefault();
    const id = document.getElementById("trial-form-id").value;
    const body = {
        name: document.getElementById("trial-form-name").value.trim(),
        sport: document.getElementById("trial-form-sport").value.trim(),
        organization: document.getElementById("trial-form-org").value.trim(),
        venue: document.getElementById("trial-form-venue").value.trim(),
        trialDate: document.getElementById("trial-form-date").value,
        category: document.getElementById("trial-form-category").value,
        ageCategory: document.getElementById("trial-form-age").value,
        registrationStatus: document.getElementById("trial-form-reg-status").value,
        capacity: Number(document.getElementById("trial-form-capacity").value || 30),
        featured: document.getElementById("trial-form-featured").checked
    };

    try {
        if (id) {
            await adminFetch(`/admin/trials/${id}`, { method: "PUT", body });
            showAdminToast("Trial updated successfully", "success");
        } else {
            await adminFetch("/admin/trials", { method: "POST", body });
            showAdminToast("Trial published with automated slot setup", "success");
        }
        closeAdminModal("modal-trial");
        loadTrials();
        loadAdminStats();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

async function deleteTrial(id) {
    if (!confirm("Delete this trial and all its booked slots?")) return;
    try {
        await adminFetch(`/admin/trials/${id}`, { method: "DELETE" });
        showAdminToast("Trial deleted", "success");
        loadTrials();
        loadAdminStats();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

// -------------------------------------------------------------------------
// 8. SCHOLARSHIPS MANAGEMENT
// -------------------------------------------------------------------------
async function loadScholarships() {
    try {
        const scholarships = await adminFetch("/admin/scholarships");
        const tbody = document.getElementById("scholarships-table-body");
        tbody.innerHTML = scholarships.map(s => `
            <tr class="hover:bg-white/[0.02] transition">
                <td class="p-4">
                    <div class="font-bold text-white">${escapeHtml(s.title)}</div>
                    <div class="text-[10px] text-cyan-300 font-semibold">${escapeHtml(s.organization)}</div>
                </td>
                <td class="p-4"><span class="badge badge-purple">${escapeHtml(s.category)}</span></td>
                <td class="p-4 text-gray-300 text-[11px] max-w-xs truncate" title="${escapeHtml(s.eligibility || '')}">${escapeHtml(s.eligibility || "Standard merit")}</td>
                <td class="p-4 font-bold text-emerald-400 text-xs">${escapeHtml(s.amount_details || "")}</td>
                <td class="p-4 text-gray-400 text-xs font-semibold">${s.deadline ? String(s.deadline).substring(0, 10) : "Rolling"}</td>
                <td class="p-4">
                    <button onclick="toggleScholarshipStatus(${s.id}, ${!s.is_active})" class="badge ${s.is_active ? 'badge-green' : 'badge-red'} cursor-pointer">
                        ${s.is_active ? '● Active' : '○ Inactive'}
                    </button>
                </td>
                <td class="p-4 text-right space-x-1">
                    <button onclick="editScholarship(${s.id})" class="p-1.5 rounded-lg bg-white/5 hover:bg-cyan-400/20 text-cyan-300"><i class="fa-solid fa-pen"></i></button>
                    <button onclick="deleteScholarship(${s.id})" class="p-1.5 rounded-lg bg-white/5 hover:bg-red-400/20 text-red-300"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>
        `).join("") || '<tr><td colspan="7" class="p-6 text-center text-gray-500">No scholarships created.</td></tr>';
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

function openAddScholarshipModal() {
    document.getElementById("modal-scholarship-title").textContent = "Add Scholarship";
    document.getElementById("scholarship-form-id").value = "";
    document.getElementById("scholarship-form-title").value = "";
    document.getElementById("scholarship-form-org").value = "";
    document.getElementById("scholarship-form-category").value = "General";
    document.getElementById("scholarship-form-deadline").value = "";
    document.getElementById("scholarship-form-amount").value = "";
    document.getElementById("scholarship-form-elig").value = "";
    openAdminModal("modal-scholarship");
}

async function editScholarship(id) {
    const scholarships = await adminFetch("/admin/scholarships");
    const s = scholarships.find(x => x.id === id);
    if (!s) return;
    document.getElementById("modal-scholarship-title").textContent = "Edit Scholarship";
    document.getElementById("scholarship-form-id").value = s.id;
    document.getElementById("scholarship-form-title").value = s.title || "";
    document.getElementById("scholarship-form-org").value = s.organization || "";
    document.getElementById("scholarship-form-category").value = s.category || "General";
    document.getElementById("scholarship-form-deadline").value = s.deadline ? String(s.deadline).substring(0, 10) : "";
    document.getElementById("scholarship-form-amount").value = s.amount_details || "";
    document.getElementById("scholarship-form-elig").value = s.eligibility || "";
    openAdminModal("modal-scholarship");
}

async function saveScholarship(e) {
    e.preventDefault();
    const id = document.getElementById("scholarship-form-id").value;
    const body = {
        title: document.getElementById("scholarship-form-title").value.trim(),
        organization: document.getElementById("scholarship-form-org").value.trim(),
        category: document.getElementById("scholarship-form-category").value,
        deadline: document.getElementById("scholarship-form-deadline").value || null,
        amountDetails: document.getElementById("scholarship-form-amount").value.trim(),
        eligibility: document.getElementById("scholarship-form-elig").value.trim(),
        isActive: true
    };

    try {
        if (id) {
            await adminFetch(`/admin/scholarships/${id}`, { method: "PUT", body });
            showAdminToast("Scholarship updated", "success");
        } else {
            await adminFetch("/admin/scholarships", { method: "POST", body });
            showAdminToast("Scholarship published", "success");
        }
        closeAdminModal("modal-scholarship");
        loadScholarships();
        loadAdminStats();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

async function toggleScholarshipStatus(id, isActive) {
    try {
        await adminFetch(`/admin/scholarships/${id}/status`, { method: "PATCH", body: { isActive } });
        showAdminToast("Scholarship status updated", "success");
        loadScholarships();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

async function deleteScholarship(id) {
    if (!confirm("Delete this scholarship?")) return;
    try {
        await adminFetch(`/admin/scholarships/${id}`, { method: "DELETE" });
        showAdminToast("Scholarship deleted", "success");
        loadScholarships();
        loadAdminStats();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

// -------------------------------------------------------------------------
// 8.1 SCHOLARSHIP APPLICATIONS (5 CATEGORIES) REVIEW & APPROVAL
// -------------------------------------------------------------------------
async function loadScholarshipApplications() {
    try {
        allScholarshipAppsData = await adminFetch("/admin/scholarship-applications");
        filterScholarshipAppsTable();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

function filterScholarshipAppsTable() {
    const catFilter = document.getElementById("scholarship-apps-filter-cat")?.value || "ALL";
    const statusFilter = document.getElementById("scholarship-apps-filter-status")?.value || "ALL";

    let filtered = allScholarshipAppsData || [];
    if (catFilter !== "ALL") {
        filtered = filtered.filter(a => a.category === catFilter);
    }
    if (statusFilter !== "ALL") {
        filtered = filtered.filter(a => (a.status || "SUBMITTED").toUpperCase() === statusFilter);
    }

    const tbody = document.getElementById("scholarship-apps-table-body");
    if (!tbody) return;

    if (!filtered.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="p-6 text-center text-gray-500">No scholarship applications matching filters.</td></tr>';
        return;
    }

    tbody.innerHTML = filtered.map(app => {
        const isCancelled = app.is_cancelled || app.status === "CANCELLED";
        const status = (app.status || (isCancelled ? "CANCELLED" : "SUBMITTED")).toUpperCase();
        let badgeClass = "badge-yellow";
        if (status === "APPROVED") badgeClass = "badge-green";
        if (status === "REJECTED" || isCancelled) badgeClass = "badge-red";

        // Summary of key answers
        let summary = [];
        if (app.age) summary.push(`Age: ${app.age}`);
        if (app.dob) summary.push(`DOB: ${String(app.dob).substring(0, 10)}`);
        if (app.family_annual_income) summary.push(`Income: ₹${Number(app.family_annual_income).toLocaleString('en-IN')}`);
        if (app.parent_guardian_name) summary.push(`Parent: ${app.parent_guardian_name}`);
        if (app.defence_relationship) summary.push(`Ward: ${app.defence_relationship}`);
        if (app.supporting_person_name) summary.push(`Personnel: ${app.supporting_person_name}`);
        if (app.current_grade) summary.push(`Grade: ${app.current_grade}`);
        if (app.loss_year) summary.push(`Loss Year: ${app.loss_year}`);
        const summaryText = summary.join(" • ") || "Standard Application";

        // Document count / badges
        let docBadges = [];
        if (app.age_proof_document) docBadges.push("Age Proof");
        if (app.applicant_id_document) docBadges.push("Applicant ID");
        if (app.parent_id_document) docBadges.push("Parent ID");
        if (app.aadhaar_document) docBadges.push("Aadhaar");
        if (app.defence_id_document) docBadges.push("Defence ID");

        return `
            <tr class="hover:bg-white/[0.02] transition">
                <td class="p-4">
                    <div class="font-bold text-white">${escapeHtml(app.full_name || "Applicant")}</div>
                    <div class="text-[10px] text-cyan-300 font-mono">${escapeHtml(app.phone || "")}</div>
                    <div class="text-[10px] text-gray-500">${escapeHtml(app.email || "")}</div>
                </td>
                <td class="p-4">
                    <span class="badge badge-purple text-[9px] mb-1">${escapeHtml(app.category)}</span>
                    <div class="font-bold text-white text-xs">${escapeHtml(app.category_title || app.category)}</div>
                </td>
                <td class="p-4">
                    <div class="text-gray-300 text-xs">${escapeHtml(summaryText)}</div>
                </td>
                <td class="p-4">
                    <div class="flex flex-wrap gap-1">
                        ${docBadges.map(b => `<span class="badge badge-cyan text-[8px]">${b}</span>`).join("") || '<span class="text-gray-500 text-[10px]">No docs</span>'}
                    </div>
                </td>
                <td class="p-4 text-gray-500 text-[11px]">${String(app.created_at || "").substring(0, 10)}</td>
                <td class="p-4"><span class="badge ${badgeClass}">${status}</span></td>
                <td class="p-4 text-right space-x-1 whitespace-nowrap">
                    <button onclick="viewScholarshipAppDetails(${app.id})" class="px-2 py-1 rounded bg-white/10 hover:bg-white/20 text-gray-200 font-bold text-[10px]" title="Inspect All Answers">View All</button>
                    ${!isCancelled && status === "SUBMITTED" ? `
                        <button onclick="approveScholarshipApp(${app.id})" class="px-2.5 py-1 rounded bg-emerald-400/20 hover:bg-emerald-400/30 text-emerald-300 font-bold text-[10px]" title="Approve & Trigger Email"><i class="fa-solid fa-check mr-1"></i> Approve</button>
                        <button onclick="rejectScholarshipApp(${app.id})" class="px-2 py-1 rounded bg-red-400/20 hover:bg-red-400/30 text-red-300 font-bold text-[10px]" title="Reject"><i class="fa-solid fa-xmark mr-1"></i> Reject</button>
                    ` : ''}
                </td>
            </tr>
        `;
    }).join("");
}

async function approveScholarshipApp(id) {
    if (!confirm("Approve this scholarship application?\n\nAn automated approval confirmation email will be triggered to the applicant.")) {
        return;
    }

    try {
        const res = await adminFetch(`/admin/scholarship-applications/${id}/approve`, { method: "POST" });
        showAdminToast(res.message || "Scholarship application approved & email sent.", "success");
        await loadScholarshipApplications();
        loadAdminStats();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

async function rejectScholarshipApp(id) {
    if (!confirm("Reject this scholarship application?")) {
        return;
    }

    try {
        const res = await adminFetch(`/admin/scholarship-applications/${id}/reject`, { method: "POST" });
        showAdminToast(res.message || "Scholarship application rejected.", "info");
        await loadScholarshipApplications();
        loadAdminStats();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

function viewScholarshipAppDetails(id) {
    const app = (allScholarshipAppsData || []).find(a => a.id === id);
    if (!app) return;

    document.getElementById("sch-detail-badge").textContent = app.category;
    document.getElementById("sch-detail-title").textContent = app.full_name + " — " + (app.category_title || app.category);
    document.getElementById("sch-detail-applied").textContent = `Applied on: ${String(app.created_at || "").substring(0, 19)}`;

    let fields = [
        { label: "Full Name", value: app.full_name },
        { label: "Email", value: app.email },
        { label: "Phone", value: app.phone }
    ];

    if (app.age) fields.push({ label: "Current Age", value: `${app.age} years` });
    if (app.dob) fields.push({ label: "Date of Birth", value: String(app.dob).substring(0, 10) });
    if (app.family_annual_income) fields.push({ label: "Family Annual Income", value: `₹${Number(app.family_annual_income).toLocaleString('en-IN')}` });
    if (app.parent_guardian_name) fields.push({ label: "Parent / Guardian Name", value: app.parent_guardian_name });
    if (app.defence_relationship) fields.push({ label: "Defence Relationship", value: app.defence_relationship });
    if (app.supporting_person_name) fields.push({ label: "Supporting Person Name", value: app.supporting_person_name });
    if (app.current_grade) fields.push({ label: "Current Grade / Year", value: app.current_grade });
    if (app.maintained_grade) fields.push({ label: "Maintained Grade / Year", value: app.maintained_grade });
    if (app.stationery_required) fields.push({ label: "Stationery Required", value: app.stationery_required });
    if (app.resources_required) fields.push({ label: "Other Resources Required", value: app.resources_required });
    if (app.required_resources_list) fields.push({ label: "List of Required Resources", value: app.required_resources_list });
    if (app.loss_year) fields.push({ label: "Parent Loss Year", value: app.loss_year });
    fields.push({ label: "Application Status", value: app.status || "SUBMITTED" });

    // Documents
    let docs = [];
    if (app.age_proof_document) docs.push({ name: "Age Proof Document", url: app.age_proof_document });
    if (app.applicant_id_document) docs.push({ name: "Applicant ID Proof", url: app.applicant_id_document });
    if (app.parent_id_document) docs.push({ name: "Parent / Guardian ID Proof", url: app.parent_id_document });
    if (app.aadhaar_document) docs.push({ name: "Aadhaar Card", url: app.aadhaar_document });
    if (app.defence_id_document) docs.push({ name: "Defence Personnel ID Proof", url: app.defence_id_document });

    const contentBox = document.getElementById("sch-detail-content");
    contentBox.innerHTML = `
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 p-4 rounded-2xl bg-darkbg/70 border border-gray-800">
            ${fields.map(f => `
                <div>
                    <span class="text-gray-500 block text-[10px] uppercase font-bold">${escapeHtml(f.label)}</span>
                    <span class="font-bold text-gray-200">${escapeHtml(f.value)}</span>
                </div>
            `).join("")}
        </div>

        <div class="p-4 rounded-2xl bg-darkbg/70 border border-gray-800 space-y-2">
            <h4 class="font-bold text-xs text-white uppercase tracking-wider">Uploaded Verification Documents</h4>
            ${docs.length ? `
                <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    ${docs.map(d => `
                        <a href="${escapeHtml(d.url)}" target="_blank" download="${escapeHtml(d.name)}" class="p-2.5 rounded-xl bg-white/5 hover:bg-cyan-400/10 border border-gray-700 flex items-center justify-between text-cyan-300 font-bold text-xs transition">
                            <span class="truncate"><i class="fa-solid fa-file-pdf mr-1 text-red-400"></i> ${escapeHtml(d.name)}</span>
                            <i class="fa-solid fa-arrow-up-right-from-square text-[10px]"></i>
                        </a>
                    `).join("")}
                </div>
            ` : '<p class="text-gray-500 text-xs">No documents uploaded.</p>'}
        </div>
    `;

    const actionsBox = document.getElementById("sch-detail-actions");
    const isPending = (app.status || "SUBMITTED").toUpperCase() === "SUBMITTED" && !app.is_cancelled;
    actionsBox.innerHTML = `
        <button type="button" onclick="closeAdminModal('modal-scholarship-app-details')" class="px-5 py-2.5 rounded-xl bg-gray-800 text-white font-bold text-xs">Close</button>
        ${isPending ? `
            <button onclick="approveScholarshipApp(${app.id}); closeAdminModal('modal-scholarship-app-details');" class="px-6 py-2.5 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-400 text-darkbg font-black text-xs"><i class="fa-solid fa-check mr-1"></i> Approve & Send Email</button>
            <button onclick="rejectScholarshipApp(${app.id}); closeAdminModal('modal-scholarship-app-details');" class="px-5 py-2.5 rounded-xl bg-red-500/20 text-red-300 font-bold text-xs hover:bg-red-500/30"><i class="fa-solid fa-xmark mr-1"></i> Reject</button>
        ` : ''}
    `;

    openAdminModal("modal-scholarship-app-details");
}

// -------------------------------------------------------------------------
// 9. APPLICATIONS MANAGEMENT (TRIALS & OPPORTUNITIES)
// -------------------------------------------------------------------------
async function loadApplications() {
    try {
        allApplicationsData = await adminFetch("/admin/applications");
        filterApplicationsTable();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

function filterApplicationsTable() {
    const typeFilter = document.getElementById("applications-filter-type")?.value || "ALL";
    const statusFilter = document.getElementById("applications-filter-status")?.value || "ALL";

    let combined = [];
    if (typeFilter === "ALL" || typeFilter === "TRIAL") {
        combined.push(...(allApplicationsData.trialApplications || []));
    }
    if (typeFilter === "ALL" || typeFilter === "OPPORTUNITY") {
        combined.push(...(allApplicationsData.opportunityApplications || []));
    }

    if (statusFilter !== "ALL") {
        combined = combined.filter(a => (a.status || "SUBMITTED").toUpperCase() === statusFilter);
    }

    const tbody = document.getElementById("applications-table-body");
    if (!tbody) return;

    if (!combined.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="p-6 text-center text-gray-500">No applications matching filters.</td></tr>';
        return;
    }

    tbody.innerHTML = combined.map(app => {
        const isTrial = app.application_type === "TRIAL";
        const status = (app.status || "SUBMITTED").toUpperCase();
        let badgeClass = "badge-yellow";
        if (status === "APPROVED") badgeClass = "badge-green";
        if (status === "REJECTED") badgeClass = "badge-red";
        if (status === "SHORTLISTED") badgeClass = "badge-purple";
        if (status === "UNDER REVIEW") badgeClass = "badge-cyan";

        return `
            <tr class="hover:bg-white/[0.02] transition">
                <td class="p-4">
                    <div class="font-bold text-white">${escapeHtml(app.athlete_name || "Athlete")}</div>
                    <div class="text-[10px] text-gray-500">${escapeHtml(app.email || "")}</div>
                </td>
                <td class="p-4">
                    <span class="badge ${isTrial ? 'badge-cyan' : 'badge-purple'} text-[9px] mb-1">${isTrial ? 'Trial' : 'Opportunity'}</span>
                    <div class="font-bold text-white text-xs">${escapeHtml(app.trial_name || app.opportunity_name || "Program")}</div>
                    ${app.confirmed_trial_date ? `<div class="text-[10px] text-emerald-400 font-bold mt-0.5"><i class="fa-solid fa-calendar-check mr-1"></i>Booked: ${escapeHtml(app.confirmed_trial_date)} ${escapeHtml(app.confirmed_trial_time || '')}</div>` : ''}
                </td>
                <td class="p-4">
                    <div class="text-gray-300 font-semibold">${escapeHtml(app.sport || "")}</div>
                    <div class="text-[10px] text-gray-500">${escapeHtml(app.position_role || "Athlete")}</div>
                </td>
                <td class="p-4">
                    <div class="text-[11px] text-gray-400 max-w-xs truncate" title="${escapeHtml(app.introduction || app.experience || '')}">${escapeHtml(app.introduction || app.experience || 'Submitted')}</div>
                </td>
                <td class="p-4 text-gray-500 text-[11px]">${String(app.created_at || "").substring(0, 10)}</td>
                <td class="p-4"><span class="badge ${badgeClass}">${status}</span></td>
                <td class="p-4 text-right space-x-1 whitespace-nowrap">
                    ${isTrial ? `
                        <button onclick="openTrialBookingModal(${JSON.stringify(app).replace(/"/g, '&quot;')})" class="px-2.5 py-1 rounded bg-cyan-400/20 hover:bg-cyan-400/30 text-cyan-300 font-bold text-[10px]" title="Assign Date & Slot and Send Confirmation"><i class="fa-solid fa-calendar-plus mr-1"></i> Book Trial Date/Time</button>
                    ` : `
                        <button onclick="updateApplicationStatus(${app.id}, 'opportunities', 'APPROVED')" class="px-2 py-1 rounded bg-emerald-400/15 hover:bg-emerald-400/25 text-emerald-400 font-bold text-[10px]" title="Approve">Approve</button>
                    `}
                    <button onclick="updateApplicationStatus(${app.id}, '${isTrial ? 'trials' : 'opportunities'}', 'SHORTLISTED')" class="px-2 py-1 rounded bg-purple-400/15 hover:bg-purple-400/25 text-purple-300 font-bold text-[10px]" title="Shortlist">Shortlist</button>
                    <button onclick="updateApplicationStatus(${app.id}, '${isTrial ? 'trials' : 'opportunities'}', 'REJECTED')" class="px-2 py-1 rounded bg-red-400/15 hover:bg-red-400/25 text-red-300 font-bold text-[10px]" title="Reject">Reject</button>
                </td>
            </tr>
        `;
    }).join("");
}

function openTrialBookingModal(app) {
    document.getElementById("trial-booking-app-id").value = app.id;
    document.getElementById("trial-booking-athlete").value = app.athlete_name || "Athlete";
    document.getElementById("trial-booking-trial-name").value = app.trial_name || "Trial Program";
    document.getElementById("trial-booking-date").value = app.trial_date ? String(app.trial_date).substring(0, 10) : new Date().toISOString().substring(0, 10);
    document.getElementById("trial-booking-time").value = "09:30 AM - 11:30 AM";
    openAdminModal("modal-trial-booking");
}

async function submitAdminTrialBooking(e) {
    e.preventDefault();
    const id = document.getElementById("trial-booking-app-id").value;
    const confirmedDate = document.getElementById("trial-booking-date").value;
    const confirmedTime = document.getElementById("trial-booking-time").value.trim();

    const btn = document.getElementById("trial-booking-btn");
    if (btn) btn.disabled = true;

    try {
        const res = await adminFetch(`/admin/trial-applications/${id}/confirm-booking`, {
            method: "POST",
            body: { confirmedDate, confirmedTime }
        });
        showAdminToast(res.message || "Trial booking confirmed & email sent to athlete!", "success");
        closeAdminModal("modal-trial-booking");
        await loadApplications();
        loadAdminStats();
    } catch (err) {
        showAdminToast(err.message, "error");
    } finally {
        if (btn) btn.disabled = false;
    }
}

async function updateApplicationStatus(id, type, status) {
    try {
        await adminFetch(`/admin/applications/${type}/${id}/status`, { method: "PATCH", body: { status } });
        showAdminToast(`Application marked as ${status}`, "success");
        loadApplications();
        loadAdminStats();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

// -------------------------------------------------------------------------
// 10. ACHIEVEMENTS VERIFICATION
// -------------------------------------------------------------------------
async function loadAchievements() {
    try {
        allAchievementsData = await adminFetch("/admin/achievements");
        filterAchievementsTable();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

function filterAchievementsTable() {
    const statusFilter = document.getElementById("achievements-filter-status")?.value || "ALL";

    const filtered = allAchievementsData.filter(a => {
        const vStatus = (a.verification_status || "PENDING").toUpperCase();
        return statusFilter === "ALL" || vStatus === statusFilter;
    });

    const tbody = document.getElementById("achievements-table-body");
    if (!tbody) return;

    if (!filtered.length) {
        tbody.innerHTML = '<tr><td colspan="8" class="p-6 text-center text-gray-500">No achievements matching filter.</td></tr>';
        return;
    }

    tbody.innerHTML = filtered.map(a => {
        const vStatus = (a.verification_status || "PENDING").toUpperCase();
        let badgeClass = "badge-yellow";
        if (vStatus === "VERIFIED") badgeClass = "badge-green";
        if (vStatus === "REJECTED") badgeClass = "badge-red";

        return `
            <tr class="hover:bg-white/[0.02] transition">
                <td class="p-4">
                    <div class="font-bold text-white">${escapeHtml(a.athlete_name || "Athlete")}</div>
                    <div class="text-[10px] text-gray-500">${escapeHtml(a.athlete_email || "")}</div>
                </td>
                <td class="p-4">
                    <div class="font-bold text-white">${escapeHtml(a.title)}</div>
                    <div class="text-[10px] text-cyan-300">${escapeHtml(a.achievement_type || "Tournament")}</div>
                </td>
                <td class="p-4 text-gray-300 font-semibold">${escapeHtml(a.sport || "")} • ${escapeHtml(a.level || "")}</td>
                <td class="p-4 text-yellow-400 font-bold text-xs">${escapeHtml(a.medal_award || a.position || "-")}</td>
                <td class="p-4 text-gray-400 text-xs">${escapeHtml(a.institution || "-")} (${a.achievement_year || ""})</td>
                <td class="p-4">
                    ${a.certificate_url ? `<a href="${escapeHtml(a.certificate_url)}" target="_blank" class="text-cyan-400 hover:underline font-bold text-xs flex items-center gap-1"><i class="fa-solid fa-arrow-up-right-from-square text-[10px]"></i> View</a>` : (a.certificate_details ? `<span class="text-gray-400 text-[11px]">${escapeHtml(a.certificate_details)}</span>` : '<span class="text-gray-600 text-[10px]">No cert</span>')}
                </td>
                <td class="p-4"><span class="badge ${badgeClass}">${vStatus}</span></td>
                <td class="p-4 text-right space-x-1">
                    <button onclick="verifyAchievement(${a.id}, 'VERIFIED')" class="px-2.5 py-1.5 rounded-lg bg-emerald-400/15 hover:bg-emerald-400/25 text-emerald-400 font-bold text-[11px]">Verify</button>
                    <button onclick="verifyAchievement(${a.id}, 'REJECTED')" class="px-2.5 py-1.5 rounded-lg bg-red-400/15 hover:bg-red-400/25 text-red-300 font-bold text-[11px]">Reject</button>
                </td>
            </tr>
        `;
    }).join("");
}

async function verifyAchievement(id, status) {
    const notes = prompt(`Enter optional note for athlete (${status}):`, status === "VERIFIED" ? "Certificate verified by platform admin" : "Insufficient proof");
    if (notes === null) return;

    try {
        await adminFetch(`/admin/achievements/${id}/verify`, { method: "PATCH", body: { status, adminNotes: notes } });
        showAdminToast(`Achievement marked as ${status}`, "success");
        loadAchievements();
        loadAdminStats();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

// -------------------------------------------------------------------------
// 11. ANNOUNCEMENTS MANAGEMENT
// -------------------------------------------------------------------------
async function loadAnnouncements() {
    try {
        const announcements = await adminFetch("/admin/announcements");
        const tbody = document.getElementById("announcements-table-body");
        tbody.innerHTML = announcements.map(a => `
            <tr class="hover:bg-white/[0.02] transition">
                <td class="p-4 font-bold text-white">${escapeHtml(a.title)}</td>
                <td class="p-4"><span class="badge badge-cyan">${escapeHtml(a.category)}</span></td>
                <td class="p-4"><span class="badge ${a.priority === 'URGENT' ? 'badge-red' : (a.priority === 'HIGH' ? 'badge-yellow' : 'badge-green')}">${escapeHtml(a.priority)}</span></td>
                <td class="p-4 text-gray-300 text-xs max-w-sm truncate">${escapeHtml(a.content)}</td>
                <td class="p-4 text-gray-500 text-xs">${String(a.created_at || "").substring(0, 10)}</td>
                <td class="p-4 text-right space-x-1">
                    <button onclick="editAnnouncement(${a.id})" class="p-1.5 rounded-lg bg-white/5 hover:bg-cyan-400/20 text-cyan-300"><i class="fa-solid fa-pen"></i></button>
                    <button onclick="deleteAnnouncement(${a.id})" class="p-1.5 rounded-lg bg-white/5 hover:bg-red-400/20 text-red-300"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>
        `).join("") || '<tr><td colspan="6" class="p-6 text-center text-gray-500">No announcements posted.</td></tr>';
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

function openAddAnnouncementModal() {
    document.getElementById("modal-announcement-title").textContent = "New Platform Announcement";
    document.getElementById("announcement-form-id").value = "";
    document.getElementById("announcement-form-title").value = "";
    document.getElementById("announcement-form-category").value = "Important Announcement";
    document.getElementById("announcement-form-priority").value = "NORMAL";
    document.getElementById("announcement-form-content").value = "";
    openAdminModal("modal-announcement");
}

async function editAnnouncement(id) {
    const announcements = await adminFetch("/admin/announcements");
    const a = announcements.find(x => x.id === id);
    if (!a) return;
    document.getElementById("modal-announcement-title").textContent = "Edit Announcement";
    document.getElementById("announcement-form-id").value = a.id;
    document.getElementById("announcement-form-title").value = a.title || "";
    document.getElementById("announcement-form-category").value = a.category || "Important Announcement";
    document.getElementById("announcement-form-priority").value = a.priority || "NORMAL";
    document.getElementById("announcement-form-content").value = a.content || "";
    openAdminModal("modal-announcement");
}

async function saveAnnouncement(e) {
    e.preventDefault();
    const id = document.getElementById("announcement-form-id").value;
    const body = {
        title: document.getElementById("announcement-form-title").value.trim(),
        category: document.getElementById("announcement-form-category").value,
        priority: document.getElementById("announcement-form-priority").value,
        content: document.getElementById("announcement-form-content").value.trim(),
        isActive: true
    };

    try {
        if (id) {
            await adminFetch(`/admin/announcements/${id}`, { method: "PUT", body });
            showAdminToast("Announcement updated", "success");
        } else {
            await adminFetch("/admin/announcements", { method: "POST", body });
            showAdminToast("Announcement broadcasted to athletes", "success");
        }
        closeAdminModal("modal-announcement");
        loadAnnouncements();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

async function deleteAnnouncement(id) {
    if (!confirm("Delete this announcement?")) return;
    try {
        await adminFetch(`/admin/announcements/${id}`, { method: "DELETE" });
        showAdminToast("Announcement removed", "success");
        loadAnnouncements();
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

// -------------------------------------------------------------------------
// 12. SETTINGS & SYSTEM STATUS
// -------------------------------------------------------------------------
async function loadSystemStatus() {
    try {
        const sys = await adminFetch("/admin/system/status");
        document.getElementById("sys-jvm").textContent = sys.jvmVersion || "Java 17";
        document.getElementById("sys-memory").textContent = `${sys.freeMemoryMB || 0} MB Free / ${sys.totalMemoryMB || 0} MB Total`;
        document.getElementById("sys-tables-count").textContent = `${(sys.tables || []).length} Active Tables`;

        const grid = document.getElementById("sys-tables-grid");
        if (grid) {
            grid.innerHTML = (sys.tables || []).map(t => `
                <div class="p-3 rounded-xl bg-darkbg/80 border border-gray-800/80 hover:border-cyan-400/30 transition">
                    <div class="text-[10px] text-gray-500 font-mono">TABLE</div>
                    <div class="font-bold text-white text-xs mt-0.5 truncate" title="${escapeHtml(t.TABLE_NAME || t.table_name)}">${escapeHtml(t.TABLE_NAME || t.table_name)}</div>
                    <div class="text-[10px] text-emerald-400 mt-1 font-semibold">${t.TABLE_ROWS || t.table_rows || 0} rows</div>
                </div>
            `).join("") || '<div class="text-gray-500">Connected to athletelink_db</div>';
        }
    } catch (err) {
        showAdminToast(err.message, "error");
    }
}

// -------------------------------------------------------------------------
// MODAL & UTILITY HELPERS
// -------------------------------------------------------------------------
function openAdminModal(id) {
    document.getElementById(id)?.classList.remove("hidden");
}

function closeAdminModal(id) {
    document.getElementById(id)?.classList.add("hidden");
}

function showAdminToast(message, type = "info") {
    let container = document.getElementById("admin-toast-container");
    if (!container) return;

    const toast = document.createElement("div");
    toast.className = `fixed bottom-6 right-6 z-50 px-5 py-3 rounded-xl border shadow-2xl text-xs font-bold transition-all duration-300 ${
        type === "error" ? "bg-red-950/90 border-red-500 text-red-200" : (type === "success" ? "bg-emerald-950/90 border-emerald-500 text-emerald-200" : "bg-gray-900/90 border-cyan-400 text-white")
    }`;
    toast.innerHTML = `<i class="fa-solid ${type === 'error' ? 'fa-triangle-exclamation' : (type === 'success' ? 'fa-circle-check' : 'fa-info')} mr-2"></i> ${escapeHtml(message)}`;
    
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = "0";
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

function escapeHtml(str) {
    return String(str ?? "").replace(/[&<>'"]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" }[c]));
}
