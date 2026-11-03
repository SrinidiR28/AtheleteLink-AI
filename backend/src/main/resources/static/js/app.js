// AthleteLink AI - site interaction + real platform features
const PROTECTED_TABS = ["dashboard", "trials", "scouts"];
const ALL_TABS = ["home", "challenge", "solution", "dashboard", "matching", "directory", "scouts", "trials", "opportunities", "recovery"];

function switchTab(tabId) {
    if (PROTECTED_TABS.includes(tabId) && !window.AuthState?.isLoggedIn()) { openAuth("login"); return; }
    ALL_TABS.forEach(t => document.getElementById("page-" + t)?.classList.toggle("hidden", t !== tabId));
    // Update nav active state
    document.querySelectorAll('[data-nav-tab]').forEach(btn => {
        const isActive = btn.dataset.navTab === tabId;
        btn.classList.toggle('text-cyan-300', isActive);
        btn.classList.toggle('font-black', isActive);
        btn.classList.toggle('text-gray-300', !isActive);
    });
    if (tabId === "dashboard") loadDashboard();
    if (tabId === "scouts") loadScouts();
    if (tabId === "trials") loadTrials();
    if (tabId === "directory") loadAthleteDirectory();
    if (tabId === "challenge") loadChallengeArena();
    if (tabId === "recovery") loadRecoveryLab();
    window.scrollTo({top:0, behavior:"smooth"});
    window.location.hash = tabId;
}

document.addEventListener("DOMContentLoaded", () => {
    const mobileMenuBtn = document.getElementById("mobile-menu-btn");
    const mobileMenuBtn2 = document.getElementById("mobile-menu-btn-2");
    const mobileMenu = document.getElementById("mobile-menu");
    const toggle = () => mobileMenu?.classList.toggle("hidden");
    mobileMenuBtn?.addEventListener("click", toggle); mobileMenuBtn2?.addEventListener("click", toggle);
    loadOpportunityCards();
    loadLiveSportsPulse();
    // Hash routing: #trials, #dashboard, #athlete/5, etc.
    const handleHash = () => {
        const hash = window.location.hash.replace('#','');
        if (!hash) return;
        const athleteMatch = hash.match(/^athlete\/?(\d+)$/);
        if (athleteMatch) { loadPublicAthleteProfile(Number(athleteMatch[1])); return; }
        if (ALL_TABS.includes(hash)) switchTab(hash);
    };
    handleHash();
    window.addEventListener('hashchange', handleHash);
});

async function handleVideoUpload(input) {
    const file = input.files[0];
    if (!file) return;
    if (!AuthState?.isLoggedIn()) { openAuth('login'); return; }
    const progressDiv = document.getElementById('video-upload-progress');
    const resultDiv = document.getElementById('video-upload-result');
    const btn = document.getElementById('video-upload-btn');
    if (btn) { btn.disabled = true; btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin mr-2"></i>Uploading & Analyzing...'; }
    if (progressDiv) progressDiv.classList.remove('hidden');
    if (resultDiv) resultDiv.innerHTML = '';
    try {
        const formData = new FormData();
        formData.append('file', file);
        const token = AuthState.getToken ? AuthState.getToken() : sessionStorage.getItem('al_token');
        const res = await fetch('/api/videos/upload', {
            method: 'POST',
            headers: token ? {'Authorization':'Bearer '+token} : {},
            body: formData
        });
        if (!res.ok) { const err = await res.json().catch(()=>({message:'Upload failed'})); throw new Error(err.message||'Upload failed'); }
        const data = await res.json();
        if (progressDiv) progressDiv.classList.add('hidden');
        if (btn) { btn.disabled = false; btn.innerHTML = '<i class="fa-solid fa-cloud-arrow-up mr-2"></i>Upload & Analyze Now'; }
        if (resultDiv) resultDiv.innerHTML = `
            <div class="mt-4 p-5 rounded-2xl bg-emerald-950/40 border border-emerald-500/30 space-y-3">
                <div class="flex items-center gap-2 text-emerald-400 font-black">
                    <i class="fa-solid fa-brain"></i> AI Analysis Complete
                </div>
                <div class="grid grid-cols-3 gap-3 text-center text-xs">
                    <div class="p-3 rounded-xl bg-darkbg/60 border border-gray-800">
                        <div class="text-xl font-black text-cyan-300">${data.postureScore}%</div>
                        <div class="text-gray-400 mt-0.5">Posture</div>
                    </div>
                    <div class="p-3 rounded-xl bg-darkbg/60 border border-gray-800">
                        <div class="text-xl font-black text-yellow-300">${data.symmetryScore}%</div>
                        <div class="text-gray-400 mt-0.5">Symmetry</div>
                    </div>
                    <div class="p-3 rounded-xl bg-darkbg/60 border border-gray-800">
                        <div class="text-xl font-black text-emerald-300">${data.explosivePower}%</div>
                        <div class="text-gray-400 mt-0.5">Explosive Power</div>
                    </div>
                </div>
                <div class="p-3 rounded-xl bg-darkbg/60 border border-gray-800 text-center">
                    <div class="text-2xl font-black text-accentNeon">${data.overallScore}%</div>
                    <div class="text-xs text-gray-400 mt-0.5">Overall AI Score</div>
                </div>
                <div class="space-y-1.5">
                    ${(data.feedback||[]).map(f => `<div class="text-xs text-gray-300 flex gap-2"><i class="fa-solid fa-circle-info text-accentBlue mt-0.5 shrink-0"></i><span>${esc(f)}</span></div>`).join('')}
                </div>
                <p class="text-xs text-gray-500">File: ${esc(data.filename)} (${data.fileSizeMB} MB) — Results saved to your notifications.</p>
            </div>`;
        showFeatureToast('Video analyzed! Check your results below.', 'success');
        await loadNotifications();
    } catch(e) {
        if (progressDiv) progressDiv.classList.add('hidden');
        if (btn) { btn.disabled = false; btn.innerHTML = '<i class="fa-solid fa-cloud-arrow-up mr-2"></i>Upload & Analyze Now'; }
        showFeatureToast(e.message, 'error');
    }
}
async function loadLiveSportsPulse() {
    try {
        const stats = await apiRequest("/stats");
        if (!stats) return;
        const athletesEl = document.getElementById("pulse-athletes-count");
        if (athletesEl && stats.athletesRegistered !== undefined) athletesEl.textContent = stats.athletesRegistered;
        const oppsEl = document.getElementById("pulse-opps-count");
        if (oppsEl && stats.opportunitiesConnected !== undefined) oppsEl.textContent = stats.opportunitiesConnected;
        const scoutsEl = document.getElementById("pulse-scouts-count");
        if (scoutsEl && stats.scouts !== undefined) scoutsEl.textContent = stats.scouts;
        const trialsEl = document.getElementById("pulse-trials-count");
        if (trialsEl && stats.trialsAvailable !== undefined) trialsEl.textContent = stats.trialsAvailable;
    } catch (e) { /* fallback to static real values */ }
}
function toggleMobileMenu(){document.getElementById("mobile-menu")?.classList.add("hidden");}
function filterAthletes(){
    const q=(document.getElementById("athlete-search")?.value||"").toLowerCase();
    const sport=document.getElementById("filter-sport")?.value||"all";
    document.querySelectorAll(".athlete-card").forEach(c=>{
        const nameMatch = c.dataset.name?.toLowerCase().includes(q);
        const sportMatch = sport==="all" || c.dataset.sport?.toLowerCase().includes(sport.toLowerCase());
        c.style.display = nameMatch && sportMatch ? "block" : "none";
    });
}

async function loadAthleteDirectory() {
    const grid = document.getElementById('athletes-grid');
    if (!grid) return;
    grid.innerHTML = '<div class="col-span-3 text-center py-10 text-gray-500">Loading athletes...</div>';
    try {
        const data = await apiRequest('/athletes');
        const athletes = data.athletes || [];
        if (!athletes.length) {
            grid.innerHTML = '<div class="col-span-3 text-center py-10 text-gray-500">No athletes registered yet.</div>';
            return;
        }
        const colors = ['border-accentBlue', 'border-accentGreen', 'border-yellow-400', 'border-purple-400', 'border-pink-400'];
        const hoverColors = ['hover:bg-accentBlue hover:text-white', 'hover:bg-accentGreen hover:text-darkbg', 'hover:bg-yellow-500 hover:text-darkbg', 'hover:bg-purple-500 hover:text-white', 'hover:bg-pink-500 hover:text-white'];
        const textColors = ['text-accentBlue', 'text-accentGreen', 'text-yellow-400', 'text-purple-400', 'text-pink-400'];
        const icons = ['fa-person-running', 'fa-baseball', 'fa-fist-raised', 'fa-volleyball', 'fa-hockey-puck'];
        grid.innerHTML = athletes.map((a, i) => {
            const color = colors[i % colors.length];
            const hColor = hoverColors[i % hoverColors.length];
            const tColor = textColors[i % textColors.length];
            const initial = (a.full_name || 'A').charAt(0).toUpperCase();
            const sport = a.sport || 'Athlete';
            const location = a.location || 'India';
            const achievements = a.achievement_count || 0;
            const aiScore = a.aiScore != null ? a.aiScore : Math.min(99, 40 + achievements * 12 + (a.sport ? 15 : 0) + (a.location ? 10 : 0));
            const activeTrials = a.active_trial_count != null ? a.active_trial_count : 0;
            return `
            <div class="athlete-card glass-card p-6 rounded-3xl border border-gray-800 space-y-4 hover:border-accentBlue/50 transition" data-sport="${esc(sport)}" data-name="${esc(a.full_name || '')}">
                <div class="flex items-center gap-4">
                    <div class="w-16 h-16 rounded-2xl bg-gradient-to-br from-cyan-400/20 to-emerald-400/20 border ${color} flex items-center justify-center text-2xl font-black ${tColor}">${initial}</div>
                    <div>
                        <h3 class="font-bold text-lg">${esc(a.full_name || 'Athlete')}</h3>
                        <p class="text-xs ${tColor} font-bold">${esc(sport)}</p>
                        <p class="text-[11px] text-gray-400"><i class="fa-solid fa-location-dot text-red-400"></i> ${esc(location)}</p>
                    </div>
                </div>
                <div class="flex justify-between items-center bg-darkbg/60 p-3 rounded-xl border border-gray-800 text-xs">
                    <div>
                        <span class="text-gray-400 block">AI Score</span>
                        <span class="font-black text-accentNeon text-base">${aiScore}</span>
                    </div>
                    <div>
                        <span class="text-gray-400 block">Achievements</span>
                        <span class="font-bold text-white">${achievements}</span>
                    </div>
                    <div>
                        <span class="text-gray-400 block">Active Trials</span>
                        <span class="font-bold text-white">${activeTrials}</span>
                    </div>
                </div>
                <button onclick="loadPublicAthleteProfile(${a.id})" class="w-full py-2.5 rounded-xl bg-gray-800 ${hColor} text-xs font-bold transition">View Scouting Profile</button>
            </div>`;
        }).join('');
    } catch(e) {
        grid.innerHTML = `<div class="col-span-3 text-red-300 text-center py-8">${esc(e.message)}</div>`;
    }
}
function showFeatureToast(message,type="info"){let t=document.getElementById("feature-toast");if(!t){t=document.createElement("div");t.id="feature-toast";document.body.appendChild(t);}t.className="fixed bottom-6 right-6 z-[200] max-w-md px-5 py-3 rounded-xl border shadow-2xl text-sm font-bold "+(type==="error"?"bg-red-950 border-red-500 text-red-200":(type==="success"?"bg-emerald-950 border-emerald-500 text-emerald-200":"bg-gray-950 border-accentBlue text-white"));t.textContent=message;clearTimeout(window.__toastTimer);window.__toastTimer=setTimeout(()=>t.remove(),4000);}
function esc(v){return String(v??"").replace(/[&<>'"]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;","'":"&#39;",'"':"&quot;"}[c]));}

async function runAIMatching(){
    const box = document.getElementById("matching-results");
    const profileBar = document.getElementById("matching-profile-bar");
    if(box) box.style.opacity = ".4";
    try {
        const result = await apiRequest("/matching", {auth:true});
        if(box) box.style.opacity = "1";

        // Incomplete profile state
        if(result.profileIncomplete) {
            const pct = result.completionPct || 25;
            const missing = result.missingFields || [];
            const missingText = missing.map(f => f === 'sport' ? 'your sport' : f === 'location' ? 'your location' : 'at least one achievement').join(', ');
            if(box) box.innerHTML = `
                <div class="col-span-full">
                    <div class="rounded-2xl border border-yellow-500/30 bg-yellow-500/5 p-6 text-center space-y-4">
                        <div class="w-16 h-16 mx-auto rounded-full bg-yellow-500/20 border border-yellow-500/40 flex items-center justify-center text-yellow-400 text-2xl">
                            <i class="fa-solid fa-circle-info"></i>
                        </div>
                        <h3 class="text-lg font-black text-white">No Matching Information Yet</h3>
                        <p class="text-sm text-gray-400 max-w-md mx-auto">We don't have enough information to generate personalized matches yet. Please add <span class="text-yellow-300 font-bold">${esc(missingText)}</span> to your profile.</p>
                        <div class="max-w-xs mx-auto">
                            <div class="flex justify-between text-xs text-gray-400 mb-1">
                                <span>Profile Completion</span><span class="font-bold text-yellow-300">${pct}%</span>
                            </div>
                            <div class="w-full bg-gray-800 rounded-full h-2.5">
                                <div class="h-2.5 rounded-full bg-gradient-to-r from-yellow-400 to-orange-400 transition-all duration-700" style="width:${pct}%"></div>
                            </div>
                        </div>
                        <button onclick="openProfileEditModal()" class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-yellow-400 to-orange-400 text-gray-950 font-black text-sm hover:opacity-90 transition">
                            <i class="fa-solid fa-pen-to-square"></i> Complete My Profile
                        </button>
                    </div>
                </div>`;
            return;
        }

        // Build results HTML
        const pct = result.completionPct || 100;
        let html = '';

        // Profile completion bar
        if(pct < 100) {
            html += `<div class="col-span-full mb-2">
                <div class="flex justify-between text-xs text-gray-400 mb-1"><span>Profile Completion</span><span class="font-bold text-accentNeon">${pct}%</span></div>
                <div class="w-full bg-gray-800 rounded-full h-1.5"><div class="h-1.5 rounded-full bg-gradient-to-r from-cyan-400 to-emerald-400 transition-all" style="width:${pct}%"></div></div>
            </div>`;
        }

        const renderMatch = (r, actionHtml='') => `
            <div class="p-4 rounded-2xl bg-darkbg/60 border border-gray-800 hover:border-accentBlue/40 transition space-y-2">
                <div class="flex justify-between gap-3 items-start">
                    <div>
                        <div class="text-xs font-bold text-accentBlue uppercase tracking-wider">${esc(r.type)}</div>
                        <h4 class="font-bold mt-1">${esc(r.name)}</h4>
                        <p class="text-xs text-gray-400 mt-1">${esc(r.reason)}</p>
                    </div>
                    <div class="text-center shrink-0">
                        <div class="text-xl font-black text-accentGreen">${r.score}%</div>
                        <div class="text-[9px] text-gray-500 uppercase">Match</div>
                    </div>
                </div>
                ${actionHtml}
            </div>`;

        if(result.coaches && result.coaches.length) {
            result.coaches.forEach(r => {
                const action = r.alreadyConnected
                    ? `<span class="text-xs text-accentGreen font-bold"><i class="fa-solid fa-check mr-1"></i>Already Connected</span>`
                    : `<button onclick="openCoachConnection(${r.id},'${esc(r.name)}')" class="text-xs px-3 py-1.5 rounded-lg bg-accentBlue text-darkbg font-bold hover:bg-accentNeon transition">Connect</button>`;
                html += renderMatch(r, action);
            });
        }
        if(result.trials && result.trials.length) {
            result.trials.forEach(r => {
                const action = r.alreadyApplied
                    ? `<span class="text-xs text-accentNeon font-bold"><i class="fa-solid fa-check mr-1"></i>Already Applied</span>`
                    : `<button onclick="openTrialApplication(${r.id})" class="text-xs px-3 py-1.5 rounded-lg bg-accentGreen text-darkbg font-bold hover:bg-emerald-300 transition">Apply</button>`;
                html += renderMatch(r, action);
            });
        }
        if(result.scholarships && result.scholarships.length) {
            result.scholarships.forEach(r => {
                const action = `<button onclick="switchTab('opportunities')" class="text-xs px-3 py-1.5 rounded-lg bg-yellow-400 text-darkbg font-bold hover:bg-yellow-300 transition">View Scholarships</button>`;
                html += renderMatch(r, action);
            });
        }

        if(box) box.innerHTML = html || '<p class="text-xs text-gray-500 col-span-full">No matches found. Complete your profile for better results.</p>';
        showFeatureToast("AI matching updated with real scores.", "success");
    } catch(e) {
        if(box) box.style.opacity = "1";
        showFeatureToast(e.message, "error");
    }
}

async function loadAthleteFeatures(){
    if(!AuthState?.isLoggedIn()) return;
    try {
        await Promise.all([loadAchievements(),loadMetrics(),loadApplications(),loadBookings(),loadConnections(),loadNotifications(),loadProfileIntoDashboard(),loadDashboardTrials(),loadMyScholarship()]);
        startNotificationPolling();
        renderProfileCompletion();
    } catch(e) { console.warn(e); }
}

function renderProfileCompletion() {
    try {
        const session = AuthState.getSession ? AuthState.getSession() : null;
        const sport = session?.sport || '';
        const location = session?.location || '';
        // Use stored profile data; recalculate from what we know
        apiRequest('/user/profile', {auth:true}).then(p => {
            let pct = 25; // name always set
            if (p.sport && p.sport.trim()) pct += 25;
            if (p.location && p.location.trim()) pct += 25;
            apiRequest('/achievements', {auth:true}).then(ach => {
                if (ach && ach.length > 0) pct += 25;
                const bar = document.getElementById('dash-profile-completion-bar');
                const pctEl = document.getElementById('dash-profile-completion-pct');
                const container = document.getElementById('dash-profile-completion');
                if (container) container.classList.remove('hidden');
                if (pctEl) pctEl.textContent = pct + '%';
                if (bar) bar.style.width = pct + '%';
                const badgeEl2 = document.getElementById('dash-profile-completion-badge');
                if (badgeEl2) badgeEl2.textContent = pct + '%';
                // Update color based on pct
                if (bar) {
                    bar.className = 'h-2.5 rounded-full transition-all duration-700 ' +
                        (pct < 50 ? 'bg-gradient-to-r from-red-400 to-yellow-400' :
                         pct < 100 ? 'bg-gradient-to-r from-yellow-400 to-cyan-400' :
                         'bg-gradient-to-r from-cyan-400 to-emerald-400');
                }
                // Show share profile button
                const shareBtn = document.getElementById('dash-share-profile-btn');
                if (shareBtn) {
                    shareBtn.classList.remove('hidden');
                    const userId = AuthState.getUserId ? AuthState.getUserId() : (AuthState._session?.userId || AuthState._session?.id || '');
                    shareBtn.onclick = () => {
                        const url = window.location.origin + '/#athlete/' + userId;
                        navigator.clipboard.writeText(url).then(() => showFeatureToast('Public profile link copied! Share it with scouts and coaches.', 'success'));
                    };
                }
            }).catch(() => {});
        }).catch(() => {});
    } catch(e) { console.warn('Profile completion error', e); }
}
async function loadAchievements(){const list=document.getElementById("achievements-list");if(!list)return;const rows=await apiRequest("/achievements",{auth:true});if(!rows.length){list.innerHTML='<p class="text-xs text-gray-500">No achievements added yet.</p>';return;}list.innerHTML=rows.map(a=>`<div class="p-3.5 rounded-xl bg-darkbg/60 border border-gray-800"><div class="flex items-start justify-between gap-3"><div><div class="font-bold text-sm">${esc(a.title)}</div><div class="text-xs text-gray-400 mt-1">${esc(a.sport||"Sport not set")} • ${esc(a.level||"Level not set")}</div><div class="text-[11px] text-gray-500 mt-1">${esc(a.institution||"Institution not set")} • ${esc(a.achievement_year||a.achieved_on||"Year not set")}</div></div><button onclick="deleteAchievement(${a.id})" class="text-gray-500 hover:text-red-300" title="Delete"><i class="fa-solid fa-trash"></i></button></div><div class="mt-2 text-xs text-yellow-400 font-bold">${esc(a.medal_award||a.position||a.result_label||"Achievement")}</div></div>`).join("");}
async function addAchievement(){openAchievementModal();}
async function deleteAchievement(id){if(!confirm("Delete this achievement?"))return;try{await apiRequest(`/achievements/${id}`,{method:"DELETE",auth:true});await loadAchievements();showFeatureToast("Achievement deleted.","success");}catch(e){showFeatureToast(e.message,"error");}}

async function submitAchievement(event){event.preventDefault();const f=event.target;const body=Object.fromEntries(new FormData(f).entries());body.year=Number(body.year);try{await apiRequest("/achievements",{method:"POST",auth:true,body});closeModal("achievement-modal");f.reset();await loadAchievements();showFeatureToast("Achievement saved securely to MySQL.","success");}catch(e){showFeatureToast(e.message,"error");}}

// =========================================================================
// ATHLETE PERFORMANCE METRICS & PR TRACKER (FEATURE 2)
// =========================================================================
async function loadMetrics() {
    const list = document.getElementById("metrics-list");
    const prGrid = document.getElementById("metrics-pr-grid");
    if (!list && !prGrid) return;
    try {
        const res = await apiRequest("/metrics", { auth: true });
        const metrics = res.metrics || [];
        const summary = res.summary || {};
        const prs = summary.personalRecords || [];

        // Render Personal Records Highlights (Top badges)
        if (prGrid) {
            if (!prs.length) {
                prGrid.innerHTML = `
                    <div class="col-span-full p-4 rounded-2xl bg-darkbg/40 border border-dashed border-gray-800 text-center text-xs text-gray-500">
                        <i class="fa-solid fa-stopwatch text-accentNeon text-lg mb-1 block"></i>
                        No personal records logged yet. Click <b class="text-white">+ Log PR</b> to set your sprint, jump, or endurance benchmarks!
                    </div>`;
            } else {
                prGrid.innerHTML = prs.map(pr => `
                    <div class="p-3.5 rounded-2xl bg-darkbg/80 border border-gray-800 hover:border-cyan-400/40 transition">
                        <div class="flex items-center justify-between text-[10px] font-black uppercase tracking-wider text-cyan-300">
                            <span>${esc(pr.category)}</span>
                            <span class="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-300 font-bold">BEST PR</span>
                        </div>
                        <div class="mt-1 text-sm font-black text-white">${esc(pr.metricName)}</div>
                        <div class="mt-1 flex items-baseline gap-1">
                            <span class="text-xl font-black text-emerald-400">${esc(pr.bestValue)}</span>
                            <span class="text-xs text-gray-400 font-bold">${esc(pr.unit)}</span>
                        </div>
                        <div class="mt-1 text-[10px] text-gray-500 flex justify-between">
                            <span>Best: ${esc(pr.bestDate || '')}</span>
                            <span>${pr.testCount} test${pr.testCount > 1 ? 's' : ''}</span>
                        </div>
                    </div>`).join('');
            }
        }

        // Render Metric History Table / List
        if (list) {
            if (!metrics.length) {
                list.innerHTML = `<p class="text-xs text-gray-500 py-3 text-center">No metric entries logged yet.</p>`;
            } else {
                list.innerHTML = metrics.map(m => `
                    <div class="p-3 rounded-xl bg-darkbg/60 border border-gray-800 flex items-center justify-between gap-3 hover:border-gray-700 transition">
                        <div class="min-w-0 flex-1">
                            <div class="flex items-center gap-2">
                                <span class="font-bold text-sm text-white">${esc(m.metric_name)}</span>
                                <span class="text-[10px] px-2 py-0.5 rounded-full bg-cyan-500/10 text-cyan-300 border border-cyan-500/20 font-bold">${esc(m.category || 'General')}</span>
                            </div>
                            <div class="text-xs text-gray-400 mt-0.5">
                                <strong class="text-emerald-400 font-mono text-sm">${esc(m.metric_value)} ${esc(m.unit)}</strong>
                                <span class="text-gray-500 ml-2"><i class="fa-solid fa-calendar text-[10px] mr-1"></i>${esc(m.recorded_date)}</span>
                            </div>
                            ${m.notes ? `<div class="text-[11px] text-gray-400 italic mt-0.5">${esc(m.notes)}</div>` : ''}
                        </div>
                        <button onclick="deleteMetric(${m.id})" class="text-gray-500 hover:text-red-400 p-1.5 transition" title="Delete record">
                            <i class="fa-solid fa-trash-can text-xs"></i>
                        </button>
                    </div>`).join('');
            }
        }
    } catch (e) {
        if (list) list.innerHTML = `<div class="text-xs text-red-400">${esc(e.message)}</div>`;
    }
}

function openAddMetricModal() {
    const f = document.getElementById("metric-form");
    if (f) {
        f.reset();
        const dateInput = f.querySelector('input[name="recordedDate"]');
        if (dateInput) dateInput.value = new Date().toISOString().split('T')[0];
    }
    openModal("metric-modal");
}

function selectMetricPreset(name, unit, category) {
    const nameInput = document.getElementById("metric-input-name");
    const unitInput = document.getElementById("metric-input-unit");
    const catInput = document.getElementById("metric-input-category");
    if (nameInput) nameInput.value = name;
    if (unitInput) unitInput.value = unit;
    if (catInput) catInput.value = category;
}

async function submitMetric(event) {
    event.preventDefault();
    const f = event.target;
    const body = Object.fromEntries(new FormData(f).entries());
    body.metricValue = Number(body.metricValue);
    try {
        await apiRequest("/metrics", { method: "POST", auth: true, body });
        closeModal("metric-modal");
        f.reset();
        await loadMetrics();
        await loadProfileIntoDashboard();
        showFeatureToast("Performance metric logged! Your AI score has been updated.", "success");
    } catch (e) {
        showFeatureToast(e.message, "error");
    }
}

async function deleteMetric(id) {
    if (!confirm("Are you sure you want to delete this performance record?")) return;
    try {
        await apiRequest(`/metrics/${id}`, { method: "DELETE", auth: true });
        await loadMetrics();
        await loadProfileIntoDashboard();
        showFeatureToast("Performance record deleted.", "success");
    } catch (e) {
        showFeatureToast(e.message, "error");
    }
}

// =========================================================================
// OFFICIAL SCOUTING CARD & VERIFIABLE CV MODAL (FEATURE 1)
// =========================================================================
async function openScoutingCardModal(athleteId = null) {
    const modalContent = document.getElementById("scouting-card-content");
    if (!modalContent) return;
    modalContent.innerHTML = '<div class="py-12 text-center text-gray-400"><i class="fa-solid fa-spinner fa-spin text-cyan-400 text-3xl"></i><p class="mt-3 text-xs">Generating verified scouting card...</p></div>';
    openModal("scouting-card-modal");

    if (!athleteId && !AuthState?.isLoggedIn?.()) {
        closeModal("scouting-card-modal");
        openAuth("login");
        showFeatureToast("Please sign in to view your official scouting card.", "info");
        return;
    }

    try {
        let cardData = null;
        if (athleteId) {
            cardData = await apiRequest(`/athletes/${athleteId}/scouting-card`, { auth: true });
        } else {
            const profile = await apiRequest("/user/profile", { auth: true });
            cardData = await apiRequest(`/athletes/${profile.id}/scouting-card`, { auth: true });
        }

        const athlete = cardData.athlete || {};
        window.currentScoutingAthlete = athlete;
        const prs = cardData.metricsSummary?.personalRecords || [];
        const achievements = athlete.achievements || [];
        
        // Use local Wi-Fi IP so phones on the same Wi-Fi can open it immediately
        const wifiHost = "10.65.24.238:8080";
        const isLocal = window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1";
        const defaultVerifyUrl = isLocal ? `http://${wifiHost}/#athlete/${athlete.id}` : `${window.location.origin}/#athlete/${athlete.id}`;
        const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=180x180&margin=4&data=${encodeURIComponent(defaultVerifyUrl)}`;

        modalContent.innerHTML = `
            <div id="scouting-cv-sheet" class="bg-gray-950 text-white rounded-3xl p-6 sm:p-8 border border-cyan-500/30 space-y-6">
                <!-- CV Header -->
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-800 pb-5">
                    <div class="flex items-center gap-4">
                        <div class="w-16 h-16 rounded-2xl bg-gradient-to-tr from-cyan-400 to-blue-600 flex items-center justify-center text-gray-950 font-black text-2xl shadow-lg shadow-cyan-500/20 shrink-0">
                            ${(athlete.full_name || 'A').charAt(0).toUpperCase()}
                        </div>
                        <div>
                            <div class="flex items-center gap-2">
                                <h2 class="text-2xl font-black text-white">${esc(athlete.full_name)}</h2>
                                <span class="px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-400/30 text-[10px] font-black uppercase">Verified Talent</span>
                            </div>
                            <div class="text-sm font-bold text-cyan-400">${esc(athlete.sport || 'Multi-Sport Athlete')} • <span class="text-gray-400">${esc(athlete.location || 'India')}</span></div>
                            <div class="text-xs text-gray-500 font-mono mt-0.5">AthleteLink ID: #${esc(athlete.id)} • Member since ${esc((athlete.created_at || '').substring(0, 10))}</div>
                        </div>
                    </div>
                    <div class="flex sm:flex-col items-center sm:items-end justify-between gap-1">
                        <div class="text-right">
                            <div class="text-[10px] font-bold uppercase tracking-wider text-gray-400">AI Talent Index</div>
                            <div class="text-3xl font-black text-accentNeon">${athlete.aiScore || 40}<span class="text-xs text-gray-500">/99</span></div>
                        </div>
                        <div class="text-[10px] px-2 py-0.5 rounded bg-cyan-500/10 text-cyan-300 font-bold border border-cyan-500/20">Official Bio-Data</div>
                    </div>
                </div>

                <!-- Personal Records / Benchmark Metrics -->
                <div>
                    <h3 class="text-xs font-black uppercase tracking-widest text-cyan-400 mb-3 flex items-center gap-2">
                        <i class="fa-solid fa-stopwatch"></i> Verified Physical Benchmarks &amp; PRs
                    </h3>
                    ${prs.length ? `
                    <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
                        ${prs.map(pr => `
                            <div class="p-3 rounded-xl bg-gray-900 border border-gray-800">
                                <div class="text-[10px] text-gray-400 uppercase font-bold truncate">${esc(pr.metricName)}</div>
                                <div class="text-lg font-black text-emerald-400 mt-0.5">${esc(pr.bestValue)} <span class="text-xs font-normal text-gray-400">${esc(pr.unit)}</span></div>
                                <div class="text-[9px] text-gray-500 mt-1">${esc(pr.bestDate || 'Recorded')}</div>
                            </div>`).join('')}
                    </div>` : '<p class="text-xs text-gray-500 italic p-3 rounded-xl bg-gray-900/50">No verified physical benchmark tests logged yet.</p>'}
                </div>

                <!-- Tournament Achievements -->
                <div>
                    <h3 class="text-xs font-black uppercase tracking-widest text-yellow-400 mb-3 flex items-center gap-2">
                        <i class="fa-solid fa-trophy"></i> Key Tournament Honors &amp; Achievements
                    </h3>
                    ${achievements.length ? `
                    <div class="space-y-2">
                        ${achievements.slice(0, 5).map(a => `
                            <div class="p-3 rounded-xl bg-gray-900/90 border border-gray-800 flex justify-between items-start gap-3">
                                <div>
                                    <div class="text-sm font-bold text-white">${esc(a.title)}</div>
                                    <div class="text-xs text-gray-400 mt-0.5">${esc(a.sport || '')} ${a.level ? '• ' + esc(a.level) : ''} ${a.institution ? '• ' + esc(a.institution) : ''}</div>
                                </div>
                                <span class="shrink-0 px-2.5 py-1 rounded-lg bg-yellow-400/10 border border-yellow-400/30 text-yellow-300 text-xs font-bold">${esc(a.medal_award || a.result_label || 'Recognized')}</span>
                            </div>`).join('')}
                    </div>` : '<p class="text-xs text-gray-500 italic p-3 rounded-xl bg-gray-900/50">No tournament achievements recorded yet.</p>'}
                </div>

                <!-- Mobile Phone Scanner Settings (Hidden in Print) -->
                <div class="p-3.5 rounded-2xl bg-gray-900/90 border border-cyan-500/30 space-y-2.5 no-print">
                    <div class="flex items-center justify-between text-xs">
                        <span class="font-bold text-cyan-300 flex items-center gap-1.5">
                            <i class="fa-solid fa-mobile-screen-button"></i> Mobile Phone QR Access Mode
                        </span>
                        <span class="text-[10px] text-emerald-400 font-mono font-bold bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">Wi-Fi LAN: 10.65.24.238</span>
                    </div>
                    <div class="flex flex-wrap gap-2 text-xs">
                        <button type="button" onclick="setScoutingQrMode('wifi', ${athlete.id})" id="qr-mode-wifi" class="px-3 py-1.5 rounded-lg bg-gradient-to-r from-cyan-400 to-emerald-400 text-gray-950 font-black text-[11px] shadow">
                            📱 Phone on Wi-Fi (10.65.24.238)
                        </button>
                        <button type="button" onclick="setScoutingQrMode('laptop', ${athlete.id})" id="qr-mode-laptop" class="px-3 py-1.5 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-300 text-[11px] font-bold">
                            💻 Laptop (localhost:8080)
                        </button>
                        <button type="button" onclick="setScoutingQrMode('vcard', ${athlete.id})" id="qr-mode-vcard" class="px-3 py-1.5 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-300 text-[11px] font-bold">
                            📇 Standalone Digital vCard
                        </button>
                    </div>
                    <p class="text-[10px] text-gray-400">
                        Scan with your smartphone camera while connected to the same Wi-Fi network to view this athlete's card on mobile!
                    </p>
                </div>

                <!-- Footer with QR Verification & Print Actions -->
                <div class="flex flex-col sm:flex-row items-center justify-between gap-6 pt-5 border-t border-gray-800">
                    <div class="flex items-center gap-4 text-center sm:text-left">
                        <div class="p-2 bg-white rounded-xl shadow-md border-2 border-cyan-400 shrink-0">
                            <img id="scouting-card-qr-img" src="${qrUrl}" alt="Athlete QR Verification" class="w-20 h-20 block" />
                        </div>
                        <div>
                            <div class="text-xs font-black uppercase text-cyan-300">Scan to Verify Digital Profile</div>
                            <div class="text-[11px] text-gray-400 mt-1 max-w-xs">Point phone camera to verify credentials, view trial history, or add athlete contact.</div>
                            <div id="scouting-card-qr-url" class="text-[10px] text-gray-400 font-mono mt-1">${defaultVerifyUrl}</div>
                        </div>
                    </div>
                    <div class="flex gap-3 w-full sm:w-auto no-print">
                        <button type="button" onclick="window.print()" class="flex-1 sm:flex-none px-5 py-2.5 rounded-xl bg-gradient-to-r from-cyan-400 to-emerald-400 text-gray-950 font-black text-xs uppercase tracking-wider hover:opacity-90 shadow-lg flex items-center justify-center gap-2">
                            <i class="fa-solid fa-print"></i> Print / Save PDF
                        </button>
                        <button type="button" onclick="closeModal('scouting-card-modal')" class="px-4 py-2.5 rounded-xl bg-gray-800 hover:bg-gray-700 text-white font-bold text-xs">
                            Close
                        </button>
                    </div>
                </div>
            </div>`;
    } catch (e) {
        modalContent.innerHTML = `<div class="p-6 text-center text-red-400"><i class="fa-solid fa-circle-exclamation text-2xl mb-2"></i><p>${esc(e.message)}</p></div>`;
    }
}

function setScoutingQrMode(mode, athleteId) {
    const a = window.currentScoutingAthlete || { id: athleteId, full_name: "Athlete", sport: "Athletics", aiScore: 78 };
    let target = "";
    const wifiIp = "10.65.24.238:8080";

    const btnWifi = document.getElementById("qr-mode-wifi");
    const btnLaptop = document.getElementById("qr-mode-laptop");
    const btnVcard = document.getElementById("qr-mode-vcard");

    if (btnWifi) btnWifi.className = "px-3 py-1.5 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-300 text-[11px] font-bold";
    if (btnLaptop) btnLaptop.className = "px-3 py-1.5 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-300 text-[11px] font-bold";
    if (btnVcard) btnVcard.className = "px-3 py-1.5 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-300 text-[11px] font-bold";

    if (mode === "wifi") {
        target = `http://${wifiIp}/#athlete/${a.id}`;
        if (btnWifi) btnWifi.className = "px-3 py-1.5 rounded-lg bg-gradient-to-r from-cyan-400 to-emerald-400 text-gray-950 font-black text-[11px] shadow";
    } else if (mode === "laptop") {
        target = `${window.location.origin}/#athlete/${a.id}`;
        if (btnLaptop) btnLaptop.className = "px-3 py-1.5 rounded-lg bg-gradient-to-r from-cyan-400 to-emerald-400 text-gray-950 font-black text-[11px] shadow";
    } else if (mode === "vcard") {
        target = `MECARD:N:${a.full_name || 'Athlete'};ORG:AthleteLink AI;NOTE:AI Talent Index: ${a.aiScore || 40}/99 | Sport: ${a.sport || 'Athlete'} | ID #${a.id};URL:http://${wifiIp}/#athlete/${a.id};;`;
        if (btnVcard) btnVcard.className = "px-3 py-1.5 rounded-lg bg-gradient-to-r from-cyan-400 to-emerald-400 text-gray-950 font-black text-[11px] shadow";
    }

    const qrImg = document.getElementById("scouting-card-qr-img");
    const urlLabel = document.getElementById("scouting-card-qr-url");
    if (qrImg) {
        qrImg.src = `https://api.qrserver.com/v1/create-qr-code/?size=180x180&margin=4&data=${encodeURIComponent(target)}`;
    }
    if (urlLabel) {
        urlLabel.textContent = (mode === "vcard" ? "Offline Contact VCard & Bio" : target);
    }
}

let trialFilters = {sport:'', search:''};

async function loadTrials(filters={}){
    const box = document.getElementById("trials-grid");
    if(!box) return;
    box.innerHTML = '<div class="md:col-span-3 text-center text-gray-500 py-10">Loading trials…</div>';
    try {
        const params = new URLSearchParams();
        if(filters.sport) params.set('sport', filters.sport);
        if(filters.category) params.set('category', filters.category);
        if(filters.ageCategory) params.set('ageCategory', filters.ageCategory);
        if(filters.status) params.set('status', filters.status);
        if(filters.search) params.set('search', filters.search);
        const qs = params.toString() ? '?' + params.toString() : '';
        const rows = await apiRequest('/trials' + qs);
        box.innerHTML = renderTrialCards(rows, false);
    } catch(e) {
        box.innerHTML = `<div class="md:col-span-3 text-red-300">${esc(e.message)}</div>`;
    }
}

function applyTrialFilters() {
    const sport = document.getElementById('filter-sport-chip')?.value || '';
    const category = document.getElementById('filter-category-chip')?.value || '';
    const ageCategory = document.getElementById('filter-age-chip')?.value || '';
    const status = document.getElementById('filter-status-chip')?.value || '';
    const search = document.getElementById('trial-search-input')?.value?.trim() || '';
    loadTrials({sport, category, ageCategory, status, search});
}

function clearTrialFilters() {
    ['filter-sport-chip','filter-category-chip','filter-age-chip','filter-status-chip'].forEach(id => {
        const el = document.getElementById(id); if(el) el.value = '';
    });
    const s = document.getElementById('trial-search-input'); if(s) s.value = '';
    loadTrials();
}
function renderTrialCards(rows,dashboard=false){if(!rows?.length)return '<div class="md:col-span-3 text-xs text-gray-500 py-8 text-center">No live trial opportunities are available right now.</div>';return rows.map(t=>{const remaining=Number(t.remaining_slots??0);const status=t.registration_status||"REG OPEN";const category=t.category||"Open Category";const age=t.age_category||"Youth";return `<div class="group glass-card p-5 rounded-3xl border border-gray-800 hover:border-accentBlue/60 hover:-translate-y-1 transition-all duration-300 relative overflow-hidden"><div class="absolute -right-12 -top-12 w-32 h-32 rounded-full bg-accentBlue/10 blur-2xl group-hover:bg-accentGreen/10 transition"></div><div class="relative space-y-4"><div class="flex items-start justify-between gap-3"><div><span class="inline-flex px-2.5 py-1 rounded-full bg-accentBlue/10 border border-accentBlue/20 text-accentBlue text-[10px] font-black uppercase tracking-wider">${esc(t.sport)}</span><h4 class="text-lg font-black mt-3 leading-tight">${esc(t.name)}</h4></div><span class="shrink-0 inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-accentGreen/10 border border-accentGreen/20 text-accentGreen text-[9px] font-black">● ${esc(status)}</span></div><div class="grid grid-cols-2 gap-2 text-[11px]"><div class="rounded-xl bg-darkbg/60 border border-gray-800 p-3"><span class="text-gray-500 block mb-1">Category</span><span class="font-bold text-gray-200">${esc(category)}</span></div><div class="rounded-xl bg-darkbg/60 border border-gray-800 p-3"><span class="text-gray-500 block mb-1">Age</span><span class="font-bold text-gray-200">${esc(age)}</span></div><div class="rounded-xl bg-darkbg/60 border border-gray-800 p-3"><span class="text-gray-500 block mb-1">Venue</span><span class="font-bold text-gray-200 truncate" title="${esc(t.venue)}">${esc(t.venue)}</span></div><div class="rounded-xl bg-darkbg/60 border border-gray-800 p-3"><span class="text-gray-500 block mb-1">Trial Date</span><span class="font-bold text-gray-200">${esc(t.trial_date)}</span></div></div><div class="flex items-center justify-between pt-1"><div><span class="text-[10px] text-gray-500 uppercase tracking-wider block">Available Slots</span><span class="text-sm font-black ${remaining>0?'text-accentNeon':'text-red-300'}">${remaining} places</span></div><button ${remaining<1?'disabled':''} onclick="openTrialApplication(${t.id})" class="px-4 py-2.5 rounded-xl ${remaining<1?'bg-gray-800 text-gray-500 cursor-not-allowed':'bg-gradient-to-r from-cyan-400 to-emerald-400 text-darkbg hover:shadow-lg hover:shadow-cyan-400/10'} text-[11px] font-black uppercase tracking-wider transition">${remaining<1?'Full':'Apply Now'}</button></div></div></div>`;}).join('');}
async function loadDashboardTrials(){const box=document.getElementById("dashboard-trials-grid");if(!box)return;try{const rows=await apiRequest("/trials/featured");box.innerHTML=renderTrialCards(rows,true);}catch(e){box.innerHTML=`<div class="md:col-span-2 text-red-300 text-xs">${esc(e.message)}</div>`;}}
async function openTrialApplication(trialId){if(!AuthState.isLoggedIn()){openAuth("login");return;}try{const [trial,profile,apps]=await Promise.all([apiRequest(`/trials/${trialId}`),apiRequest("/user/profile",{auth:true}),apiRequest("/applications",{auth:true})]);const existing=apps.find(a=>Number(a.trial_id)===Number(trialId));if(existing){showApplicationResult(existing);return;}document.getElementById("trial-modal-title").textContent=`Apply — ${trial.name}`;document.getElementById("trial-modal-meta").textContent=`${trial.organization||"Verified trial organizer"} • Complete your athlete details below. Profile information has been pre-filled where available.`;document.getElementById("trial-id").value=trialId;document.getElementById("trial-form-summary").innerHTML=`<div class="rounded-xl bg-darkbg/60 border border-gray-800 p-3"><span class="text-[9px] text-gray-500 uppercase block">Category</span><b class="text-xs">${esc(trial.category||"Open")}</b></div><div class="rounded-xl bg-darkbg/60 border border-gray-800 p-3"><span class="text-[9px] text-gray-500 uppercase block">Age</span><b class="text-xs">${esc(trial.age_category||"Youth")}</b></div><div class="rounded-xl bg-darkbg/60 border border-gray-800 p-3"><span class="text-[9px] text-gray-500 uppercase block">Venue</span><b class="text-xs truncate block" title="${esc(trial.venue)}">${esc(trial.venue)}</b></div><div class="rounded-xl bg-darkbg/60 border border-gray-800 p-3"><span class="text-[9px] text-gray-500 uppercase block">Date</span><b class="text-xs">${esc(trial.trial_date)}</b></div>`;const fields={athleteName:profile.fullName,email:profile.email,sport:profile.sport||trial.sport||"",location:profile.location||""};Object.entries(fields).forEach(([k,v])=>{const el=document.querySelector(`#trial-form [name="${k}"]`);if(el)el.value=v||"";});const submit=document.getElementById("trial-submit-btn");if(submit){submit.disabled=false;submit.textContent="Submit Application";}openModal("trial-modal");}catch(e){showFeatureToast(e.message,"error");}}
async function submitTrialApplication(event){event.preventDefault();const f=event.target;const trialId=document.getElementById("trial-id").value;const body=Object.fromEntries(new FormData(f).entries());delete body.trialId;if(body.age)body.age=Number(body.age);const submit=document.getElementById("trial-submit-btn");if(submit){submit.disabled=true;submit.textContent="Submitting…";}try{const r=await apiRequest(`/trials/${trialId}/apply`,{method:"POST",auth:true,body});closeModal("trial-modal");showApplicationResult(r);await loadDashboard();showFeatureToast(r.emailSent?"Trial application saved and confirmation email sent via sreevaib@gmail.com.":"Trial application saved successfully.","success");}catch(e){showFeatureToast(e.message,"error");if(submit){submit.disabled=false;submit.textContent="Submit Application";}}}
function showApplicationResult(r){const trialId=r.trialId||r.trial_id;const appId=r.applicationId||r.application_id;const emailSent=r.emailSent??r.email_sent;document.getElementById("result-modal-content").innerHTML=`<div class="text-center space-y-4"><div class="w-16 h-16 mx-auto rounded-2xl bg-accentGreen/10 border border-accentGreen/30 flex items-center justify-center text-accentGreen text-3xl"><i class="fa-solid fa-check"></i></div><h3 class="text-xl font-black">Application ${appId?"Submitted":"Status"}</h3><p class="text-sm text-gray-400">${esc(r.trial||r.trial_name||"Trial application")}</p><div class="inline-flex px-4 py-2 rounded-full bg-accentBlue/10 text-accentNeon font-bold">Status: ${esc(r.status||"SUBMITTED")}</div>${appId?`<p class="text-xs text-gray-500">Application ID: #${esc(appId)}</p>`:""}${emailSent?`<div class="p-3 rounded-xl border border-accentGreen/30 bg-accentGreen/5 text-xs text-accentGreen">Confirmation email sent from sreevaib@gmail.com to ${esc(r.email||"your email")}.</div>`:`<div class="p-3 rounded-xl border border-accentBlue/30 bg-accentBlue/5 text-xs text-gray-300">Application record saved securely in database. Confirmation dispatched to ${esc(r.email||"your registered email")}.</div>`}${r.confirmationUrl?`<a target="_blank" href="${esc(r.confirmationUrl)}" class="block text-center py-3 rounded-xl bg-gray-800 text-white font-bold text-sm hover:bg-gray-700 transition">Open Application Confirmation</a>`:""}${trialId?`<button onclick="showTrialSlots(${trialId})" class="w-full py-3 rounded-xl bg-accentBlue text-darkbg font-black text-sm hover:bg-accentNeon transition">View Available Slots</button>`:""}</div>`;openModal("result-modal");}

async function showTrialSlots(trialId){try{const [trial,slots]=await Promise.all([apiRequest(`/trials/${trialId}`),apiRequest(`/trials/${trialId}/slots`)]);document.getElementById("result-modal-content").innerHTML=`<div><div class="flex justify-between items-center mb-4"><div><h3 class="text-xl font-black">Available Slots</h3><p class="text-xs text-gray-500">${esc(trial.name)} • ${esc(trial.venue)}</p></div></div><div class="space-y-3">${slots.map(s=>`<div class="p-4 rounded-xl border border-gray-800 bg-darkbg/60 flex items-center justify-between gap-4"><div><div class="font-bold text-sm">${esc(s.slot_date)} • ${esc(s.slot_time)}</div><div class="text-xs text-gray-500 mt-1">${s.remaining_slots} of ${s.capacity} places available</div></div><button ${s.remaining_slots<1?"disabled":""} onclick="bookSlot(${s.id})" class="px-4 py-2 rounded-lg ${s.remaining_slots<1?"bg-gray-800 text-gray-500":"bg-accentGreen text-darkbg hover:bg-emerald-300"} text-xs font-black">${s.remaining_slots<1?"Full":"Confirm Booking"}</button></div>`).join("")}</div></div>`;}catch(e){showFeatureToast(e.message,"error");}}
async function bookSlot(slotId){try{const b=await apiRequest("/bookings",{method:"POST",auth:true,body:{slotId}});showBookingCard(b);await loadBookings();showFeatureToast(b.emailSent?"Booking confirmed and email sent via sreevaib@gmail.com.":"Booking confirmed. Confirmation URL is available.","success");}catch(e){showFeatureToast(e.message,"error");}}

async function loadScouts(){
 const box=document.getElementById("scouts-grid"); if(!box)return;
 box.innerHTML='<div class="md:col-span-3 text-center text-gray-500 py-10">Finding coaches near you…</div>';
 try{
  const render=async(lat=null,lon=null,radius=100)=>{
   const qs=lat!=null&&lon!=null?`?lat=${encodeURIComponent(lat)}&lon=${encodeURIComponent(lon)}&radiusKm=${radius}`:"";
   const [coaches,connections]=await Promise.all([apiRequest(`/coaches/nearby${qs}`,{auth:true}),apiRequest('/coach-connections',{auth:true})]);
   if(!coaches.length){box.innerHTML='<div class="md:col-span-3 text-center py-10"><div class="text-lg font-black">No coaches found nearby</div><p class="text-sm text-gray-500 mt-2">Allow location access or search a wider radius.</p><button onclick="findNearbyCoaches(true)" class="mt-4 px-5 py-2 rounded-xl bg-accentBlue text-darkbg font-bold text-xs">Search within 500 km</button></div>';return;}
   box.innerHTML=coaches.map(c=>{const conn=connections.find(x=>x.coach_id===c.id);const label=conn?(conn.status==='CONNECTED'?'Connected':'Pending'):'Connect';return `<div class="glass-card p-6 rounded-3xl border border-gray-800 space-y-4 hover:border-accentBlue/50 transition"><div class="flex items-center gap-4"><div class="w-14 h-14 rounded-2xl bg-gradient-to-br from-cyan-400/20 to-emerald-400/20 border border-cyan-400/30 flex items-center justify-center text-cyan-300 text-2xl"><i class="fa-solid fa-person-chalkboard"></i></div><div><h3 class="font-bold text-lg">${esc(c.name)}</h3><p class="text-xs text-accentBlue font-bold">${esc(c.organization)}</p></div></div><p class="text-xs text-gray-400"><b class="text-gray-300">Sports:</b> ${esc(c.sports)}</p><p class="text-xs text-gray-400"><b class="text-gray-300">Specialization:</b> ${esc(c.specialization)}</p><div class="flex flex-wrap justify-between gap-2 text-xs text-gray-500"><span><i class="fa-solid fa-location-dot text-red-400 mr-1"></i>${esc(c.location)}</span><span class="text-accentGreen font-bold">${esc(c.distance_km)} km away</span></div><div class="text-[11px] text-gray-500">${esc(c.experience)} • Profile match ${esc(c.match_score)}%</div><button ${conn?'disabled':''} onclick="openCoachConnection(${c.id},'${esc(c.name)}')" class="w-full py-2.5 rounded-xl ${conn?'bg-gray-800 text-accentGreen':'bg-accentBlue hover:bg-accentNeon text-darkbg'} text-xs font-bold transition">${label}</button></div>`;}).join('');
  };
  if(navigator.geolocation){navigator.geolocation.getCurrentPosition(pos=>render(pos.coords.latitude,pos.coords.longitude,100),()=>render(),{enableHighAccuracy:true,timeout:7000,maximumAge:300000});} else await render();
  await loadScoutNetwork();
 }catch(e){box.innerHTML=`<div class="md:col-span-3 text-red-300">${esc(e.message)}</div>`;}
}
async function findNearbyCoaches(wide=false){
 if(!AuthState.isLoggedIn()){openAuth('login');return;}
 if(!navigator.geolocation){showFeatureToast('Your browser does not support location services.','error');return;}
 navigator.geolocation.getCurrentPosition(async pos=>{try{const radius=wide?500:100;const [coaches,connections]=await Promise.all([apiRequest(`/coaches/nearby?lat=${pos.coords.latitude}&lon=${pos.coords.longitude}&radiusKm=${radius}`,{auth:true}),apiRequest('/coach-connections',{auth:true})]);const box=document.getElementById('scouts-grid');box.innerHTML=coaches.length?coaches.map(c=>{const conn=connections.find(x=>x.coach_id===c.id);return `<div class="glass-card p-6 rounded-3xl border border-gray-800 space-y-4"><div class="flex items-center gap-4"><div class="w-14 h-14 rounded-2xl bg-gradient-to-br from-cyan-400/20 to-emerald-400/20 border border-cyan-400/30 flex items-center justify-center text-cyan-300 text-2xl"><i class="fa-solid fa-person-chalkboard"></i></div><div><h3 class="font-bold text-lg">${esc(c.name)}</h3><p class="text-xs text-accentBlue font-bold">${esc(c.organization)}</p></div></div><p class="text-xs text-gray-400"><b class="text-gray-300">Sports:</b> ${esc(c.sports)}</p><p class="text-xs text-gray-400"><b class="text-gray-300">Specialization:</b> ${esc(c.specialization)}</p><div class="flex justify-between text-xs text-gray-500"><span>${esc(c.location)}</span><span class="text-accentGreen font-bold">${esc(c.distance_km)} km away</span></div><button ${conn?'disabled':''} onclick="openCoachConnection(${c.id},'${esc(c.name)}')" class="w-full py-2.5 rounded-xl ${conn?'bg-gray-800 text-accentGreen':'bg-accentBlue hover:bg-accentNeon text-darkbg'} text-xs font-bold">${conn?(conn.status==='CONNECTED'?'Connected':'Pending'):'Connect'}</button></div>`;}).join(''):'<div class="md:col-span-3 text-center text-gray-500 py-10">No coaches found in this radius.</div>';}catch(e){showFeatureToast(e.message,'error');}},()=>showFeatureToast('Location permission is needed to find nearby coaches.','error'),{enableHighAccuracy:true,timeout:10000});
}

function openCoachConnection(id,name){document.getElementById('coach-connection-id').value=id;document.getElementById('coach-connection-name').textContent=name;document.getElementById('coach-connection-message').value='';openModal('coach-connection-modal');}
async function submitCoachConnection(event){event.preventDefault();const id=document.getElementById('coach-connection-id').value;const message=document.getElementById('coach-connection-message').value.trim();try{await apiRequest(`/coaches/${id}/connect`,{method:'POST',auth:true,body:{message}});closeModal('coach-connection-modal');showFeatureToast('Coach connection request sent — status is now Pending.','success');await loadScouts();await loadConnections();}catch(e){showFeatureToast(e.message,'error');}}
function openScoutConnection(id,name){document.getElementById('scout-connection-id').value=id;document.getElementById('scout-connection-name').textContent=name;document.getElementById('scout-connection-message').value='';openModal('scout-connection-modal');}
async function submitScoutConnection(event){event.preventDefault();const id=document.getElementById('scout-connection-id').value;try{const message=document.getElementById('scout-connection-message').value.trim();await apiRequest(`/scouts/${id}/connect`,{method:'POST',auth:true,body:{message}});closeModal('scout-connection-modal');showFeatureToast('Scout connection request sent — status is now Pending.','success');await loadScoutNetwork();await loadConnections();}catch(e){showFeatureToast(e.message,'error');}}
async function loadScoutNetwork(){const box=document.getElementById('scouts-network-grid');if(!box)return;try{const [scouts,connections]=await Promise.all([apiRequest('/scouts'),apiRequest('/connections',{auth:true})]);box.innerHTML=scouts.map(s=>{const conn=connections.find(x=>x.scout_id===s.id);return `<div class="glass-card p-6 rounded-3xl border border-gray-800 space-y-3"><div class="flex items-center gap-3"><div class="w-12 h-12 rounded-xl bg-yellow-400/10 border border-yellow-400/20 flex items-center justify-center text-yellow-300"><i class="fa-solid fa-binoculars"></i></div><div><h4 class="font-bold">${esc(s.name)}</h4><p class="text-xs text-accentBlue">${esc(s.organization)}</p></div></div><p class="text-xs text-gray-400">${esc(s.sports)} • ${esc(s.specialization)}</p><p class="text-xs text-gray-500">${esc(s.location)} • ${esc(s.experience)}</p><div class="flex gap-2"><button onclick="viewScoutProfile(${s.id})" class="flex-1 py-2 rounded-xl bg-gray-800 hover:bg-gray-700 text-gray-300 text-xs font-bold">View Profile</button><button ${conn?'disabled':''} onclick="openScoutConnection(${s.id},'${esc(s.name)}')" class="flex-1 py-2 rounded-xl ${conn?'bg-gray-800 text-accentGreen cursor-default':'bg-accentBlue text-darkbg hover:bg-accentNeon'} text-xs font-bold">${conn?(conn.status==='CONNECTED'?'Connected':'Pending'):'Connect'}</button></div></div>`;}).join('')}catch(e){box.innerHTML=`<div class="text-red-300">${esc(e.message)}</div>`;}}
async function loadConnections(){const box=document.getElementById("connections-list");if(!box)return;const rows=await apiRequest("/connections",{auth:true});box.innerHTML=rows.length?rows.map(c=>`<div class="flex items-center justify-between p-3 rounded-xl bg-darkbg/60 border border-gray-800"><div><div class="font-bold text-sm">${esc(c.name)}</div><div class="text-xs text-gray-500">${esc(c.organization)}</div></div><span class="text-[10px] font-bold px-2 py-1 rounded-full ${c.status==="CONNECTED"?"bg-accentGreen/10 text-accentGreen":"bg-yellow-400/10 text-yellow-300"}">${esc(c.status)}</span></div>`).join(""):"<p class='text-xs text-gray-500'>No scout connections yet.</p>";}
async function viewScoutProfile(scoutId) {
    try {
        const [scouts, connections] = await Promise.all([
            apiRequest('/scouts'),
            AuthState?.isLoggedIn() ? apiRequest('/connections', {auth:true}) : Promise.resolve([])
        ]);
        const s = scouts.find(x => String(x.id) === String(scoutId));
        if (!s) { showFeatureToast('Scout not found.', 'error'); return; }
        const conn = connections.find(x => String(x.scout_id) === String(s.id));
        const btnHtml = conn
            ? `<button disabled class="w-full py-3 rounded-xl bg-gray-800 text-accentGreen cursor-default text-sm font-black">${conn.status === 'CONNECTED' ? '<i class="fa-solid fa-check mr-2"></i>Connected' : '<i class="fa-solid fa-clock mr-2"></i>Request Pending'}</button>`
            : `<button onclick="openScoutConnection(${s.id},'${esc(s.name)}');closeModal('result-modal')" class="w-full py-3 rounded-xl bg-accentBlue hover:bg-accentNeon text-darkbg text-sm font-black transition"><i class="fa-solid fa-handshake mr-2"></i>Request Connection</button>`;
        document.getElementById('result-modal-content').innerHTML = `
            <div class="space-y-4">
                <div class="flex items-center gap-4">
                    <div class="w-16 h-16 rounded-2xl bg-yellow-400/10 border border-yellow-400/20 flex items-center justify-center text-yellow-300 text-2xl"><i class="fa-solid fa-binoculars"></i></div>
                    <div>
                        <h3 class="text-xl font-black">${esc(s.name)}</h3>
                        <p class="text-xs text-accentBlue font-bold">${esc(s.organization)}</p>
                        <p class="text-xs text-gray-400">${esc(s.location || '')}</p>
                    </div>
                </div>
                <div class="grid grid-cols-2 gap-3">
                    <div class="p-3 rounded-xl bg-darkbg/60"><div class="text-xs text-gray-500 mb-0.5">Sports</div><div class="font-bold text-sm">${esc(s.sports || '')}</div></div>
                    <div class="p-3 rounded-xl bg-darkbg/60"><div class="text-xs text-gray-500 mb-0.5">Specialization</div><div class="font-bold text-sm">${esc(s.specialization || '')}</div></div>
                    <div class="p-3 rounded-xl bg-darkbg/60"><div class="text-xs text-gray-500 mb-0.5">Experience</div><div class="font-bold text-sm">${esc(s.experience || '')}</div></div>
                    <div class="p-3 rounded-xl bg-darkbg/60"><div class="text-xs text-gray-500 mb-0.5">Location</div><div class="font-bold text-sm">${esc(s.location || '')}</div></div>
                </div>
                ${s.bio ? '<p class="text-sm text-gray-400">' + esc(s.bio) + '</p>' : ''}
                ${btnHtml}
            </div>`;
        openModal('result-modal');
    } catch(e) { showFeatureToast(e.message, 'error'); }
}

async function loadApplications(){
    const box = document.getElementById("applications-list");
    if(!box) return;
    const rows = await apiRequest("/applications", {auth:true});
    box.innerHTML = rows.length ? rows.map(a => `
        <div class="p-3 rounded-xl bg-darkbg/60 border border-gray-800">
            <div class="flex justify-between gap-2 items-start">
                <div>
                    <b class="text-sm">${esc(a.trial_name)}</b>
                    <div class="text-[11px] text-gray-500 mt-1">Application #${esc(a.application_id)} • ${esc(a.trial_date)}</div>
                </div>
                <span class="text-[10px] font-bold shrink-0 px-2 py-1 rounded-full ${
                    a.status === 'WITHDRAWN' ? 'bg-gray-700 text-gray-400' :
                    a.status === 'CONFIRMED' ? 'bg-accentGreen/10 text-accentGreen' :
                    'bg-accentBlue/10 text-accentNeon'
                }">${esc(a.status)}</span>
            </div>
            ${a.status !== 'WITHDRAWN' && a.status !== 'CONFIRMED' ? `
            <button onclick="withdrawApplication(${a.application_id})" class="mt-2 text-xs text-red-400 hover:text-red-300 font-bold transition">
                <i class="fa-solid fa-xmark mr-1"></i>Withdraw Application
            </button>` : ''}
        </div>`).join('') : '<p class="text-xs text-gray-500">No trial applications yet.</p>';
}

async function withdrawApplication(id) {
    if(!confirm('Are you sure you want to withdraw this trial application? This cannot be undone.')) return;
    try {
        await apiRequest(`/applications/${id}/withdraw`, {method:'POST', auth:true});
        await loadApplications();
        showFeatureToast('Application withdrawn successfully.', 'success');
    } catch(e) { showFeatureToast(e.message, 'error'); }
}

async function loadBookings(){
    const box = document.getElementById("bookings-list");
    if(!box) return;
    const rows = await apiRequest("/bookings", {auth:true});
    box.innerHTML = rows.length ? rows.map(b => `
        <div class="p-4 rounded-xl bg-darkbg/60 border border-gray-800">
            <div class="flex justify-between gap-2 items-start">
                <b class="text-sm">${esc(b.trial_name)}</b>
                <span class="text-[10px] font-bold shrink-0 px-2 py-1 rounded-full ${
                    b.status === 'CANCELLED' ? 'bg-gray-700 text-gray-400' : 'bg-accentGreen/10 text-accentGreen'
                }">${esc(b.status)}</span>
            </div>
            <div class="text-xs text-gray-400 mt-1">${esc(b.venue)}</div>
            <div class="text-xs text-gray-500 mt-1">${esc(b.slot_date)} • ${esc(b.slot_time)} • Booking #${esc(b.booking_id)}</div>
            <div class="flex gap-3 mt-3 flex-wrap">
                <button onclick="showBooking(${b.booking_id})" class="text-xs text-accentBlue hover:text-white font-bold">View confirmation →</button>
                ${b.status !== 'CANCELLED' ? `<button onclick="cancelBooking(${b.booking_id})" class="text-xs text-red-400 hover:text-red-300 font-bold"><i class="fa-solid fa-xmark mr-1"></i>Cancel Booking</button>` : ''}
            </div>
        </div>`).join('') : '<p class="text-xs text-gray-500">No booked trials yet.</p>';
}

async function cancelBooking(id) {
    if(!confirm('Are you sure you want to cancel this booking? The slot will be freed for other athletes.')) return;
    try {
        await apiRequest(`/bookings/${id}/cancel`, {method:'POST', auth:true});
        await loadBookings();
        showFeatureToast('Booking cancelled successfully. Admin has been notified.', 'success');
    } catch(e) { showFeatureToast(e.message, 'error'); }
}
async function showBooking(id){try{const b=await apiRequest(`/bookings/${id}`,{auth:true});showBookingCard(b);}catch(e){showFeatureToast(e.message,"error");}}
function showBookingCard(b){
    const checkinUrl = `${window.location.origin}/api/trials/checkin?token=${encodeURIComponent(b.confirmation_token)}`;
    const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=160x160&margin=4&data=${encodeURIComponent(checkinUrl)}`;
    const isAttended = b.status === 'CHECKED_IN';

    document.getElementById("result-modal-content").innerHTML = `
        <div class="space-y-5">
            <div class="flex items-center justify-between">
                <div class="flex items-center gap-3">
                    <div class="w-12 h-12 rounded-xl bg-accentGreen/10 text-accentGreen flex items-center justify-center">
                        <i class="fa-solid fa-ticket"></i>
                    </div>
                    <div>
                        <h3 class="text-xl font-black">Trial Booking Gate Pass</h3>
                        <p class="text-xs text-gray-500">Booking #${esc(b.booking_id)}</p>
                    </div>
                </div>
                <span class="px-3 py-1 rounded-full text-xs font-bold uppercase ${
                    isAttended ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40' : 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40'
                }">
                    ${isAttended ? '<i class="fa-solid fa-check-double mr-1"></i>ATTENDED & CHECKED IN' : 'CONFIRMED'}
                </span>
            </div>

            <!-- Gate Pass QR Code Box -->
            <div class="p-4 rounded-2xl bg-darkbg/90 border border-cyan-500/30 text-center space-y-2">
                <div class="text-[11px] font-black uppercase tracking-wider text-cyan-300">
                    <i class="fa-solid fa-qrcode mr-1"></i> Scan at Venue Gate for Attendance Check-In
                </div>
                <div class="inline-block p-2.5 bg-white rounded-xl shadow-lg border-2 border-cyan-400">
                    <img src="${qrUrl}" alt="Check-in QR" class="w-32 h-32 block mx-auto" />
                </div>
                <p class="text-[11px] text-gray-400">Show this QR code to the coordinator desk at the trial venue.</p>
            </div>

            <div class="grid grid-cols-2 gap-3 text-sm">
                <div class="p-3 rounded-xl bg-darkbg/60"><span class="text-gray-500 block text-xs">Trial</span>${esc(b.trial_name)}</div>
                <div class="p-3 rounded-xl bg-darkbg/60"><span class="text-gray-500 block text-xs">Sport</span>${esc(b.sport)}</div>
                <div class="p-3 rounded-xl bg-darkbg/60"><span class="text-gray-500 block text-xs">Venue</span>${esc(b.venue)}</div>
                <div class="p-3 rounded-xl bg-darkbg/60"><span class="text-gray-500 block text-xs">Date & Time</span>${esc(b.slot_date)} • ${esc(b.slot_time)}</div>
            </div>

            <div class="flex gap-2">
                <a target="_blank" href="/api/confirm-booking?token=${encodeURIComponent(b.confirmation_token)}" class="flex-1 text-center py-3 rounded-xl bg-cyan-500/20 border border-cyan-400/40 text-cyan-300 font-bold text-xs hover:bg-cyan-500/30 transition">
                    <i class="fa-solid fa-file-invoice mr-1"></i> Open Gate Pass
                </a>
                <a target="_blank" href="${checkinUrl}" class="flex-1 text-center py-3 rounded-xl bg-emerald-500/20 border border-emerald-400/40 text-emerald-300 font-bold text-xs hover:bg-emerald-500/30 transition">
                    <i class="fa-solid fa-check-to-slot mr-1"></i> Gate Check-In Link
                </a>
            </div>
        </div>`;
    openModal("result-modal");
}

async function loadOpportunityCards(){/* static cards retain current design; their buttons use the real API */}
async function loadNotifications(){
    if(!AuthState?.isLoggedIn()) return;
    try {
        const rows = await apiRequest("/notifications", {auth:true});
        const unread = rows.filter(n => !n.is_read).length;
        // Update badge in nav
        const badge = document.getElementById("notif-count-badge");
        if(badge) {
            badge.textContent = unread;
            badge.classList.toggle("hidden", unread === 0);
        }
        // Update notification list in dashboard if open
        const list = document.getElementById("notifications-list");
        if(list) {
            list.innerHTML = rows.length ? rows.slice(0,15).map(n => `
                <div class="p-3 rounded-xl border ${n.is_read ? 'border-gray-800 bg-darkbg/40' : 'border-accentBlue/30 bg-accentBlue/5'} flex gap-3 items-start">
                    <div class="w-2 h-2 mt-1.5 rounded-full shrink-0 ${n.is_read ? 'bg-gray-700' : 'bg-accentBlue animate-pulse'}"></div>
                    <div class="flex-1 min-w-0">
                        <div class="font-bold text-sm ${n.is_read ? 'text-gray-300' : 'text-white'}">${esc(n.title)}</div>
                        <div class="text-xs text-gray-400 mt-0.5">${esc(n.message)}</div>
                    </div>
                </div>`).join('') : '<p class="text-sm text-gray-500">No notifications yet.</p>';
        }
        return { rows, unread };
    } catch(e) { console.warn('Notification load error:', e); }
}

function startNotificationPolling() {
    if(window.__notifPolling) return;
    loadNotifications();
    window.__notifPolling = setInterval(() => {
        if(AuthState?.isLoggedIn()) loadNotifications();
        else { clearInterval(window.__notifPolling); window.__notifPolling = null; }
    }, 45000);
}

async function markAllNotificationsRead() {
    try {
        await apiRequest('/notifications/read-all', {method:'POST', auth:true});
        await loadNotifications();
        showFeatureToast('All notifications marked as read.', 'success');
    } catch(e) { showFeatureToast(e.message, 'error'); }
}
function showNotifications(rows){document.getElementById("result-modal-content").innerHTML=`<div><h3 class="text-xl font-black mb-4">Notifications</h3><div class="space-y-2">${rows.length?rows.slice(0,12).map(n=>`<div class="p-3 rounded-xl border border-gray-800 bg-darkbg/60"><div class="font-bold text-sm">${esc(n.title)}</div><div class="text-xs text-gray-400 mt-1">${esc(n.message)}</div></div>`).join(""):"<p class='text-sm text-gray-500'>No notifications yet.</p>"}</div></div>`;openModal("result-modal");rows.filter(n=>!n.is_read).forEach(n=>apiRequest(`/notifications/${n.id}/read`,{method:"POST",auth:true}).catch(()=>{}));setTimeout(loadNotifications,400);}
async function loadProfileIntoDashboard(){
    try {
        const p = await apiRequest("/user/profile", {auth:true});
        document.getElementById("dash-user-name")?.replaceChildren(document.createTextNode(p.fullName||"Athlete"));
        const sport = document.getElementById("dash-user-sport"); if(sport) sport.textContent = p.sport||"Athlete";
        const email = document.getElementById("dash-user-email"); if(email) email.textContent = p.email||"";
        const loc = document.querySelector("#dash-user-location span"); if(loc) loc.textContent = p.location||"Location not set";
        const navName = document.getElementById("nav-user-name"); if(navName) navName.textContent = p.fullName||"Athlete";
        // Fetch and update AI Score in dashboard
        try {
            const scoreData = await apiRequest("/ai-score", {auth:true});
            const scoreEl = document.getElementById("dash-ai-talent-score");
            if(scoreEl) scoreEl.textContent = scoreData.aiScore;
            const badgeEl = document.getElementById("dash-profile-completion-badge");
            if(badgeEl) badgeEl.textContent = (scoreData.profileCompletionPct || 25) + '%';
        } catch(_) {}
    } catch(e) {
        if(e.status===401||e.status===403){AuthState.clearSession();refreshAuthUI();}
    }
}
async function loadDashboard(){if(!AuthState.isLoggedIn())return;await loadAthleteFeatures();}
function openModal(id){document.getElementById(id)?.classList.remove("hidden");}
function closeModal(id){document.getElementById(id)?.classList.add("hidden");}

function openProfileEditModal() {
    switchTab('dashboard');
    document.getElementById('dash-user-name')?.scrollIntoView({ behavior: 'smooth' });
}

async function loadPublicAthleteProfile(athleteId) {
    const isLoggedIn = AuthState?.isLoggedIn();
    try {
        const profile = await apiRequest(`/athletes/${athleteId}/profile`);
        document.getElementById("result-modal-content").innerHTML = `
            <div class="space-y-5">
                ${!isLoggedIn ? `
                <div class="p-3 rounded-xl bg-accentBlue/10 border border-accentBlue/30 text-xs text-accentBlue flex items-center gap-2">
                    <i class="fa-solid fa-circle-info"></i>
                    <span>You're viewing a public profile. <button onclick="closeModal('result-modal');openAuth('login')" class="underline font-bold">Login</button> to connect with this athlete.</span>
                </div>` : ''}
                <div class="flex items-center gap-4">
                    <div class="w-16 h-16 rounded-2xl bg-gradient-to-br from-cyan-400/20 to-emerald-400/20 border border-cyan-400/30 flex items-center justify-center text-cyan-300">
                        <svg viewBox="0 0 24 24" class="w-9 h-9" fill="currentColor"><path d="M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8v2.4h19.2v-2.4c0-3.2-6.4-4.8-9.6-4.8z"/></svg>
                    </div>
                    <div>
                        <h3 class="text-xl font-black">${esc(profile.full_name)}</h3>
                        <p class="text-xs text-accentBlue font-bold">${esc(profile.sport || 'Athlete')}</p>
                        <p class="text-xs text-gray-400">${esc(profile.location || '')}</p>
                    </div>
                </div>
                <div class="grid grid-cols-3 gap-3 text-sm">
                    <div class="p-3 rounded-xl bg-darkbg/60 text-center">
                        <div class="text-2xl font-black text-accentGreen">${profile.achievementCount}</div>
                        <div class="text-xs text-gray-400 mt-1">Achievements</div>
                    </div>
                    <div class="p-3 rounded-xl bg-darkbg/60 text-center">
                        <div class="text-2xl font-black text-accentBlue">${profile.trialApplicationCount}</div>
                        <div class="text-xs text-gray-400 mt-1">Active Trials</div>
                    </div>
                    <div class="p-3 rounded-xl bg-darkbg/60 text-center">
                        <div class="text-2xl font-black text-accentNeon">${profile.aiScore != null ? profile.aiScore : '\u2014'}</div>
                        <div class="text-xs text-gray-400 mt-1">AI Score</div>
                    </div>
                </div>
                ${profile.achievements && profile.achievements.length ? `
                <div>
                    <h4 class="text-xs font-black uppercase tracking-wider text-gray-400 mb-2">Top Achievements</h4>
                    <div class="space-y-2">${profile.achievements.slice(0,5).map(a => `
                        <div class="p-3 rounded-xl bg-darkbg/60 border border-gray-800">
                            <div class="font-bold text-sm">${esc(a.title)}</div>
                            <div class="text-xs text-gray-400 mt-0.5">${esc(a.sport||'')} ${a.level ? '• '+esc(a.level) : ''} ${a.achievement_year ? '• '+esc(a.achievement_year) : ''}</div>
                            ${a.medal_award ? `<div class="text-xs text-yellow-400 font-bold mt-0.5">${esc(a.medal_award)}</div>` : ''}
                        </div>`).join('')}
                    </div>
                </div>` : '<p class="text-xs text-gray-500">No achievements listed yet.</p>'}
                ${profile.metricsSummary && profile.metricsSummary.personalRecords && profile.metricsSummary.personalRecords.length ? `
                <div>
                    <h4 class="text-xs font-black uppercase tracking-wider text-cyan-400 mb-2 flex items-center gap-1.5">
                        <i class="fa-solid fa-stopwatch"></i> Physical Benchmarks &amp; PRs
                    </h4>
                    <div class="grid grid-cols-2 gap-2">
                        ${profile.metricsSummary.personalRecords.map(pr => `
                            <div class="p-2.5 rounded-xl bg-darkbg/60 border border-gray-800">
                                <div class="text-[10px] text-gray-400 font-bold truncate">${esc(pr.metricName)}</div>
                                <div class="text-sm font-black text-emerald-400">${esc(pr.bestValue)} <span class="text-[10px] font-normal text-gray-400">${esc(pr.unit)}</span></div>
                            </div>`).join('')}
                    </div>
                </div>` : ''}
                <button onclick="closeModal('result-modal');openScoutingCardModal(${athleteId})" class="w-full py-3 rounded-xl bg-gradient-to-r from-cyan-400 to-emerald-400 text-gray-950 font-black text-xs uppercase tracking-wider hover:opacity-90 shadow-lg flex items-center justify-center gap-2 transition">
                    <i class="fa-solid fa-id-card-clip text-sm"></i> View &amp; Print Official Scouting Card
                </button>
                <div class="flex gap-2 text-xs text-gray-500 pt-2 border-t border-gray-800">
                    <i class="fa-solid fa-link"></i>
                    <span class="font-mono break-all">${window.location.origin}/api/athletes/${athleteId}/profile</span>
                    <button onclick="navigator.clipboard.writeText('${window.location.origin}/api/athletes/${athleteId}/profile').then(()=>showFeatureToast('Profile link copied!','success'))" class="text-accentBlue hover:text-white"><i class="fa-solid fa-copy"></i></button>
                </div>
            </div>`;
        openModal('result-modal');
    } catch(e) { showFeatureToast(e.message, 'error'); }
}
function openAchievementModal(){openModal("achievement-modal");}

// =========================================================================
// 5 SEPARATE SCHOLARSHIP CATEGORIES & APPLICATION WORKFLOW
// =========================================================================

const SCHOLARSHIP_CATEGORY_CONFIG = {
    "UNDER_18": {
        title: "Scholarship for Under 18",
        badge: "Under 18 Category",
        subtitle: "Dedicated financial support for youth athletes under 18 years old.",
        sections: ["section-age-dob-income", "section-under18", "section-age-proof"],
        ageMin: 1, ageMax: 17, ageValidationMsg: "Age must be below 18 years."
    },
    "ABOVE_18": {
        title: "Scholarship for Above 18",
        badge: "Above 18 Category",
        subtitle: "Financial assistance for adult athletes aged 18 and above.",
        sections: ["section-age-dob-income", "section-applicant-id", "section-age-proof"],
        ageMin: 18, ageMax: 100, ageValidationMsg: "Age must be 18 years or older."
    },
    "WOMEN": {
        title: "Scholarship for Women",
        badge: "Women in Sports",
        subtitle: "Dedicated grant and resources supporting women athletes.",
        sections: ["section-women"]
    },
    "DEFENCE_CHILDREN": {
        title: "Scholarships for Children of Defence Personnel",
        badge: "Armed Forces Support",
        subtitle: "Financial & stationery grants for wards of Indian Defence Personnel.",
        sections: ["section-applicant-id", "section-defence"]
    },
    "SINGLE_PARENT": {
        title: "Scholarship for Single Parents",
        badge: "Single Parent Support",
        subtitle: "Financial aid for athletes and dependents from single-parent families.",
        sections: ["section-single-parent", "section-applicant-id"]
    }
};

function normalizeCategoryKey(raw) {
    const s = String(raw || "").toLowerCase();
    if (s.includes("under 18") || s === "under_18") return "UNDER_18";
    if (s.includes("above 18") || s === "above_18") return "ABOVE_18";
    if (s.includes("women")) return "WOMEN";
    if (s.includes("defence") || s.includes("defense")) return "DEFENCE_CHILDREN";
    if (s.includes("single")) return "SINGLE_PARENT";
    return "UNDER_18";
}

function toggleDefenceResourcesList(value) {
    const wrap = document.getElementById("defence-resources-list-wrap");
    if (wrap) {
        wrap.classList.toggle("hidden", value !== "Yes");
    }
}

async function openScholarshipForm(categoryName) {
    if (!AuthState?.isLoggedIn()) {
        openAuth("login");
        return;
    }

    const catKey = normalizeCategoryKey(categoryName);
    const config = SCHOLARSHIP_CATEGORY_CONFIG[catKey] || SCHOLARSHIP_CATEGORY_CONFIG["UNDER_18"];

    // Check if athlete already has an existing or cancelled scholarship application
    try {
        const existing = await apiRequest("/scholarships/my-application", { auth: true });
        if (existing && existing.applied) {
            if (existing.is_cancelled || existing.status === "CANCELLED") {
                showFeatureToast("You have already used your scholarship application. Cancelling an application does not allow you to apply for another scholarship.", "error");
            } else {
                showFeatureToast("You have already applied for a scholarship. You can apply for only one scholarship.", "error");
            }
            showExistingScholarshipModal(existing);
            return;
        }
    } catch (_) {}

    // Reset Form and Errors
    const form = document.getElementById("scholarshipForm");
    if (form) form.reset();
    clearScholarshipErrors();

    // Set Hidden Inputs
    document.getElementById("selectedScholarshipCategory").value = catKey;
    document.getElementById("selectedScholarshipTitle").value = config.title;

    // Set Header
    document.getElementById("scholarshipCategoryBadge").textContent = config.badge;
    document.getElementById("scholarshipTitle").textContent = config.title;
    document.getElementById("scholarshipSubtitle").textContent = config.subtitle;

    // Toggle Specific Category Sections
    const allSections = ["section-age-dob-income", "section-under18", "section-age-proof", "section-applicant-id", "section-women", "section-defence", "section-single-parent"];
    allSections.forEach(secId => {
        const el = document.getElementById(secId);
        if (el) el.classList.toggle("hidden", !config.sections.includes(secId));
    });

    const resListWrap = document.getElementById("defence-resources-list-wrap");
    if (resListWrap) resListWrap.classList.add("hidden");

    // Pre-fill profile info
    try {
        const profile = await apiRequest("/user/profile", { auth: true });
        if (profile) {
            if (profile.fullName) document.getElementById("scholarshipFullName").value = profile.fullName;
            if (profile.email) document.getElementById("scholarshipEmail").value = profile.email;
        }
    } catch (_) {}

    // Open modal
    const modal = document.getElementById("scholarshipModal");
    if (modal) {
        modal.classList.remove("hidden");
        modal.classList.add("flex");
    }
}

function closeScholarshipForm() {
    const modal = document.getElementById("scholarshipModal");
    if (modal) {
        modal.classList.add("hidden");
        modal.classList.remove("flex");
    }
}

function closeScholarshipSuccess() {
    const el = document.getElementById("scholarshipSuccess");
    if (el) {
        el.classList.add("hidden");
        el.classList.remove("flex");
    }
}

function clearScholarshipErrors() {
    document.querySelectorAll("#scholarshipForm .inline-error").forEach(el => el.classList.add("hidden"));
    const genErr = document.getElementById("scholarshipFormGeneralError");
    if (genErr) { genErr.classList.add("hidden"); genErr.textContent = ""; }
    const qWarn = document.getElementById("scholarshipQuotaWarning");
    if (qWarn) qWarn.classList.add("hidden");
}

function setScholarshipError(fieldId, errorMsg) {
    const errEl = document.getElementById("error-" + fieldId);
    if (errEl) {
        if (errorMsg) errEl.textContent = errorMsg;
        errEl.classList.remove("hidden");
    }
}

function readFileAsBase64(fileInput) {
    return new Promise((resolve) => {
        if (!fileInput || !fileInput.files || !fileInput.files[0]) {
            resolve(null);
            return;
        }
        const file = fileInput.files[0];
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = () => resolve(null);
        reader.readAsDataURL(file);
    });
}

function calculateAgeFromDob(dobString) {
    if (!dobString) return null;
    const dob = new Date(dobString);
    if (isNaN(dob.getTime())) return null;
    const now = new Date();
    let age = now.getFullYear() - dob.getFullYear();
    const m = now.getMonth() - dob.getMonth();
    if (m < 0 || (m === 0 && now.getDate() < dob.getDate())) {
        age--;
    }
    return age;
}

async function submitScholarshipApplication(event) {
    event.preventDefault();
    clearScholarshipErrors();

    const catKey = document.getElementById("selectedScholarshipCategory").value;
    const catTitle = document.getElementById("selectedScholarshipTitle").value;
    const config = SCHOLARSHIP_CATEGORY_CONFIG[catKey];

    const fullName = document.getElementById("scholarshipFullName").value.trim();
    const email = document.getElementById("scholarshipEmail").value.trim();
    const phone = document.getElementById("scholarshipPhone").value.trim();

    let hasErrors = false;

    // 1. Common questions
    if (!fullName) {
        setScholarshipError("scholarshipFullName", "Full name is required.");
        hasErrors = true;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email || !emailRegex.test(email)) {
        setScholarshipError("scholarshipEmail", "Please enter a valid email address.");
        hasErrors = true;
    }
    const phoneRegex = /^[6-9]\d{9}$/;
    if (!phone || !phoneRegex.test(phone)) {
        setScholarshipError("scholarshipPhone", "Please enter a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9.");
        hasErrors = true;
    }

    // Category-specific validations
    let age = null;
    let dob = null;
    let familyAnnualIncome = null;
    let parentGuardianName = null;
    let defenceRelationship = null;
    let supportingPersonName = null;
    let currentGrade = null;
    let maintainedGrade = null;
    let stationeryRequired = null;
    let resourcesRequired = null;
    let requiredResourcesList = null;
    let lossYear = null;

    if (catKey === "UNDER_18" || catKey === "ABOVE_18") {
        const ageVal = document.getElementById("scholarshipAge").value;
        age = ageVal ? parseInt(ageVal, 10) : null;
        dob = document.getElementById("scholarshipDob").value;
        const incomeVal = document.getElementById("scholarshipFamilyIncome").value;
        familyAnnualIncome = incomeVal ? parseFloat(incomeVal) : null;

        if (catKey === "UNDER_18") {
            if (!age || age < 1 || age >= 18) {
                setScholarshipError("scholarshipAge", "Current age is required and must be strictly below 18.");
                hasErrors = true;
            }
        } else {
            if (!age || age < 18) {
                setScholarshipError("scholarshipAge", "Current age is required and must be 18 or above.");
                hasErrors = true;
            }
        }

        if (!dob) {
            setScholarshipError("scholarshipDob", "Date of birth is required.");
            hasErrors = true;
        } else {
            const calculatedAge = calculateAgeFromDob(dob);
            const dobDate = new Date(dob);
            if (dobDate > new Date()) {
                setScholarshipError("scholarshipDob", "Date of birth cannot be in the future.");
                hasErrors = true;
            } else if (catKey === "UNDER_18" && calculatedAge >= 18) {
                setScholarshipError("scholarshipDob", "Date of birth indicates an age of 18 or older. Must be under 18.");
                hasErrors = true;
            } else if (catKey === "ABOVE_18" && calculatedAge < 18) {
                setScholarshipError("scholarshipDob", "Date of birth indicates an age below 18. Must be 18 or older.");
                hasErrors = true;
            }
        }

        if (!familyAnnualIncome || familyAnnualIncome <= 0 || isNaN(familyAnnualIncome)) {
            setScholarshipError("scholarshipFamilyIncome", "Family annual income is required and must be a positive amount.");
            hasErrors = true;
        }

        const ageProofInput = document.getElementById("scholarshipAgeProofDoc");
        if (!ageProofInput.files || !ageProofInput.files[0]) {
            setScholarshipError("scholarshipAgeProofDoc", "Age proof document is required.");
            hasErrors = true;
        }
    }

    if (catKey === "UNDER_18") {
        parentGuardianName = document.getElementById("scholarshipParentName").value.trim();
        if (!parentGuardianName) {
            setScholarshipError("scholarshipParentName", "Parent / Guardian name is required.");
            hasErrors = true;
        }
        const parentIdInput = document.getElementById("scholarshipParentIdDoc");
        if (!parentIdInput.files || !parentIdInput.files[0]) {
            setScholarshipError("scholarshipParentIdDoc", "Parent / Guardian ID proof is required.");
            hasErrors = true;
        }
    }

    if (catKey === "ABOVE_18" || catKey === "DEFENCE_CHILDREN" || catKey === "SINGLE_PARENT") {
        const applicantIdInput = document.getElementById("scholarshipApplicantIdDoc");
        if (!applicantIdInput.files || !applicantIdInput.files[0]) {
            setScholarshipError("scholarshipApplicantIdDoc", "Applicant ID proof is required.");
            hasErrors = true;
        }
    }

    if (catKey === "WOMEN") {
        const womenAadhaarInput = document.getElementById("scholarshipWomenAadhaarDoc");
        if (!womenAadhaarInput.files || !womenAadhaarInput.files[0]) {
            setScholarshipError("scholarshipWomenAadhaarDoc", "Aadhaar Card is required.");
            hasErrors = true;
        }
    }

    if (catKey === "DEFENCE_CHILDREN") {
        defenceRelationship = document.getElementById("scholarshipDefenceRelationship").value;
        const validRels = ["Mother", "Father", "Other blood relative only"];
        if (!defenceRelationship || !validRels.includes(defenceRelationship)) {
            setScholarshipError("scholarshipDefenceRelationship", "Relationship must be Mother, Father, or Other blood relative only.");
            hasErrors = true;
        }

        supportingPersonName = document.getElementById("scholarshipSupportingName").value.trim();
        if (!supportingPersonName) {
            setScholarshipError("scholarshipSupportingName", "Supporting person's full name is required.");
            hasErrors = true;
        }

        currentGrade = document.getElementById("scholarshipCurrentGrade").value.trim();
        if (!currentGrade) {
            setScholarshipError("scholarshipCurrentGrade", "Current grade / year is required.");
            hasErrors = true;
        }

        maintainedGrade = document.getElementById("scholarshipMaintainedGrade").value.trim();
        if (!maintainedGrade) {
            setScholarshipError("scholarshipMaintainedGrade", "Maintained grade / year is required.");
            hasErrors = true;
        }

        stationeryRequired = document.getElementById("scholarshipStationery").value;
        if (!stationeryRequired) {
            setScholarshipError("scholarshipStationery", "Please select whether stationery is required.");
            hasErrors = true;
        }

        resourcesRequired = document.getElementById("scholarshipResources").value;
        if (!resourcesRequired) {
            setScholarshipError("scholarshipResources", "Please select whether educational resources are required.");
            hasErrors = true;
        }

        if (resourcesRequired === "Yes") {
            requiredResourcesList = document.getElementById("scholarshipResourceList").value.trim();
            if (!requiredResourcesList) {
                setScholarshipError("scholarshipResourceList", "Please list the required resources when Yes is selected.");
                hasErrors = true;
            }
        }

        const defenceIdInput = document.getElementById("scholarshipDefenceIdDoc");
        if (!defenceIdInput.files || !defenceIdInput.files[0]) {
            setScholarshipError("scholarshipDefenceIdDoc", "Defence personnel ID proof is required.");
            hasErrors = true;
        }
    }

    if (catKey === "SINGLE_PARENT") {
        const lossYearVal = document.getElementById("scholarshipLossYear").value;
        lossYear = lossYearVal ? parseInt(lossYearVal, 10) : null;
        const currentYear = new Date().getFullYear();
        if (!lossYear || lossYear < 1950 || lossYear > currentYear) {
            setScholarshipError("scholarshipLossYear", `Loss year is required, must be between 1950 and ${currentYear}.`);
            hasErrors = true;
        }

        const singleIncomeVal = document.getElementById("scholarshipSingleIncome").value;
        familyAnnualIncome = singleIncomeVal ? parseFloat(singleIncomeVal) : null;
        if (!familyAnnualIncome || familyAnnualIncome <= 0 || isNaN(familyAnnualIncome)) {
            setScholarshipError("scholarshipSingleIncome", "Family annual income is required and must be a positive amount.");
            hasErrors = true;
        }
    }

    if (hasErrors) {
        showFeatureToast("Please fix the highlighted errors before submitting.", "error");
        return;
    }

    // Submit Guard
    const submitBtn = document.getElementById("scholarshipSubmitBtn");
    const submitBtnText = document.getElementById("scholarshipSubmitBtnText");
    if (submitBtn) submitBtn.disabled = true;
    if (submitBtnText) submitBtnText.textContent = "Processing Application…";

    try {
        // Read file payloads as Base64
        const [ageProofDoc, applicantIdDoc, parentIdDoc, aadhaarDoc, defenceIdDoc] = await Promise.all([
            readFileAsBase64(document.getElementById("scholarshipAgeProofDoc")),
            readFileAsBase64(document.getElementById("scholarshipApplicantIdDoc")),
            readFileAsBase64(document.getElementById("scholarshipParentIdDoc")),
            readFileAsBase64(document.getElementById("scholarshipWomenAadhaarDoc")),
            readFileAsBase64(document.getElementById("scholarshipDefenceIdDoc"))
        ]);

        const payload = {
            category: catKey,
            categoryTitle: catTitle,
            fullName,
            email,
            phone,
            age,
            dob,
            familyAnnualIncome,
            parentGuardianName,
            defenceRelationship,
            supportingPersonName,
            currentGrade,
            maintainedGrade,
            stationeryRequired,
            resourcesRequired,
            requiredResourcesList,
            lossYear,
            ageProofDocument: ageProofDoc,
            applicantIdDocument: applicantIdDoc,
            parentIdDocument: parentIdDoc,
            aadhaarDocument: aadhaarDoc,
            defenceIdDocument: defenceIdDoc
        };

        const result = await apiRequest("/scholarships/apply", {
            method: "POST",
            auth: true,
            body: payload
        });

        closeScholarshipForm();

        // Show Success modal
        const successModal = document.getElementById("scholarshipSuccess");
        const successMsg = document.getElementById("scholarshipSuccessMsg");
        if (successMsg) {
            successMsg.textContent = result.emailSent
                ? `Your application for "${catTitle}" was received successfully. A confirmation email has been sent to ${email}.`
                : `Your application for "${catTitle}" was received and saved in MySQL.`;
        }
        if (successModal) {
            successModal.classList.remove("hidden");
            successModal.classList.add("flex");
        }

        showFeatureToast("Scholarship application submitted successfully!", "success");
        await loadMyScholarship();
    } catch (err) {
        const genErr = document.getElementById("scholarshipFormGeneralError");
        if (genErr) {
            genErr.textContent = err.message || "Failed to submit scholarship application.";
            genErr.classList.remove("hidden");
        }
        showFeatureToast(err.message || "Submission failed", "error");
    } finally {
        if (submitBtn) submitBtn.disabled = false;
        if (submitBtnText) submitBtnText.textContent = "Submit Scholarship Application";
    }
}

async function cancelScholarshipApplication(id) {
    if (!confirm("Are you sure you want to cancel your scholarship application?\n\nIMPORTANT POLICY:\nEach applicant is allowed only ONE scholarship application in total. Cancelling an existing application does NOT allow you to apply for another scholarship.")) {
        return;
    }

    try {
        await apiRequest(`/scholarships/${id}/cancel`, { method: "POST", auth: true });
        showFeatureToast("Scholarship application cancelled. You cannot apply for another scholarship per platform policy.", "success");
        closeModal("result-modal");
        await loadMyScholarship();
    } catch (err) {
        showFeatureToast(err.message, "error");
    }
}

async function loadMyScholarship() {
    const card = document.getElementById("my-scholarship-card");
    if (!card) return;

    try {
        const app = await apiRequest("/scholarships/my-application", { auth: true });
        if (!app || !app.applied) {
            card.innerHTML = `
                <div class="p-4 rounded-2xl bg-darkbg/60 border border-gray-800 space-y-2">
                    <p class="text-xs text-gray-400">You have not applied for any scholarship yet.</p>
                    <button onclick="switchTab('opportunities')" class="text-xs font-bold text-accentNeon hover:underline flex items-center gap-1">
                        Explore 5 Scholarship Categories →
                    </button>
                </div>
            `;
            return;
        }

        const isCancelled = app.is_cancelled || app.status === "CANCELLED";
        const status = app.status || (isCancelled ? "CANCELLED" : "SUBMITTED");
        let badgeColor = "text-yellow-400 bg-yellow-400/10 border-yellow-400/20";
        if (status === "APPROVED") badgeColor = "text-accentGreen bg-accentGreen/10 border-accentGreen/20";
        if (status === "REJECTED" || isCancelled) badgeColor = "text-red-400 bg-red-400/10 border-red-400/20";

        card.innerHTML = `
            <div class="p-4 rounded-2xl bg-darkbg/60 border border-gray-800 space-y-3">
                <div class="flex items-start justify-between gap-2">
                    <div>
                        <span class="inline-flex px-2.5 py-0.5 rounded-full text-[9px] font-black uppercase tracking-wider border ${badgeColor}">
                            ● ${esc(status)}
                        </span>
                        <h4 class="font-bold text-sm text-white mt-1.5">${esc(app.category_title || app.category)}</h4>
                        <p class="text-[11px] text-gray-400 mt-0.5">Applied on ${String(app.created_at || "").substring(0, 10)}</p>
                    </div>
                </div>

                <div class="p-2.5 rounded-xl bg-gray-900/60 border border-gray-800 text-[11px] text-gray-300 space-y-1">
                    <div class="flex justify-between"><span class="text-gray-500">Applicant:</span> <b class="text-white">${esc(app.full_name)}</b></div>
                    <div class="flex justify-between"><span class="text-gray-500">Phone:</span> <span class="font-mono text-cyan-300">${esc(app.phone)}</span></div>
                    ${app.family_annual_income ? `<div class="flex justify-between"><span class="text-gray-500">Annual Income:</span> <span class="text-emerald-400 font-bold">₹${Number(app.family_annual_income).toLocaleString('en-IN')}</span></div>` : ''}
                    ${app.defence_relationship ? `<div class="flex justify-between"><span class="text-gray-500">Defence Ward:</span> <span>${esc(app.defence_relationship)}</span></div>` : ''}
                </div>

                ${!isCancelled && status === "SUBMITTED" ? `
                    <button onclick="cancelScholarshipApplication(${app.id})" class="w-full py-2 rounded-xl bg-red-500/10 border border-red-500/30 text-red-300 hover:bg-red-500/20 font-bold text-xs transition">
                        <i class="fa-solid fa-ban mr-1"></i> Cancel Application
                    </button>
                ` : (isCancelled ? `
                    <p class="text-[10px] text-amber-400 italic">Application was cancelled. Per policy, quota has been utilized.</p>
                ` : '')}
            </div>
        `;
    } catch (_) {
        card.innerHTML = `<p class="text-xs text-gray-500">Could not load scholarship details.</p>`;
    }
}

function showExistingScholarshipModal(app) {
    const isCancelled = app.is_cancelled || app.status === "CANCELLED";
    const status = app.status || (isCancelled ? "CANCELLED" : "SUBMITTED");
    const quotaMsg = isCancelled
        ? "You have already used your scholarship application. Cancelling an application does not allow you to apply for another scholarship."
        : "You have already applied for a scholarship. You can apply for only one scholarship.";

    document.getElementById("result-modal-content").innerHTML = `
        <div class="space-y-4">
            <div class="w-14 h-14 rounded-2xl bg-amber-400/15 border border-amber-400/30 flex items-center justify-center text-amber-300 text-2xl mx-auto">
                <i class="fa-solid fa-lock"></i>
            </div>
            <div class="text-center space-y-1">
                <h3 class="text-xl font-black text-white">One Scholarship Limit</h3>
                <p class="text-xs text-amber-300">${esc(quotaMsg)}</p>
            </div>
            <div class="p-4 rounded-2xl bg-darkbg/80 border border-gray-800 space-y-2 text-xs">
                <div class="flex justify-between"><span class="text-gray-400">Current Program:</span><b class="text-white">${esc(app.category_title || app.category)}</b></div>
                <div class="flex justify-between"><span class="text-gray-400">Status:</span><span class="font-bold ${status === 'APPROVED' ? 'text-emerald-400' : (status === 'REJECTED' || isCancelled ? 'text-red-400' : 'text-yellow-400')}">${esc(status)}</span></div>
                <div class="flex justify-between"><span class="text-gray-400">Applied Date:</span><span>${String(app.created_at || '').substring(0, 10)}</span></div>
            </div>
            ${!isCancelled && status === "SUBMITTED" ? `
                <button onclick="cancelScholarshipApplication(${app.id})" class="w-full py-2.5 rounded-xl bg-red-500/20 border border-red-500/40 text-red-300 font-bold text-xs hover:bg-red-500/30 transition">
                    Cancel Existing Application
                </button>
            ` : ''}
        </div>
    `;
    openModal("result-modal");
}

window.addEventListener("DOMContentLoaded",()=>{
    ["achievement-modal","trial-modal","result-modal","scout-connection-modal","metric-modal","challenge-modal"].forEach(id=>document.getElementById(id)?.addEventListener("click",e=>{if(e.target.id===id)closeModal(id);}));
    document.getElementById("scholarshipModal")?.addEventListener("click", e => {
        if (e.target.id === "scholarshipModal") closeScholarshipForm();
    });
});

// =========================================================================
// FEATURE 3: BENCHMARK DRILLS & SKILLS ARENA
// =========================================================================

let reactionTimerInterval = null;
let reactionStartTime = 0;
let reactionHits = 0;
let reactionTotalLatency = 0;
let reactionRunning = false;

function loadChallengeArena() {
    loadChallengeLeaderboard('ALL');
}

function switchDrillTab(tab) {
    ['reaction', 'plank', 'cadence'].forEach(t => {
        const btn = document.getElementById(`drill-tab-${t}`);
        const content = document.getElementById(`drill-content-${t}`);
        if (btn) {
            btn.className = (t === tab)
                ? "px-4 py-2 rounded-xl bg-accentBlue/20 text-cyan-300 border border-accentBlue/40 text-xs font-bold transition flex items-center gap-2"
                : "px-4 py-2 rounded-xl bg-darkbg/60 text-gray-400 hover:text-white border border-gray-800 text-xs font-bold transition flex items-center gap-2";
        }
        if (content) content.classList.toggle('hidden', t !== tab);
    });
}

function startReactionTest() {
    const arena = document.getElementById("reaction-arena");
    const startScreen = document.getElementById("reaction-start-screen");
    const target = document.getElementById("reaction-target");
    const resultBox = document.getElementById("reaction-result-box");
    if (!arena || !target) return;

    if (reactionTimerInterval) clearInterval(reactionTimerInterval);
    reactionHits = 0;
    reactionTotalLatency = 0;
    reactionRunning = true;

    if (startScreen) startScreen.classList.add("hidden");
    if (resultBox) resultBox.classList.add("hidden");
    target.classList.remove("hidden");

    let secondsLeft = 30.0;
    document.getElementById("reaction-timer").textContent = "30.0s";
    document.getElementById("reaction-hits").textContent = "0";
    document.getElementById("reaction-avg").textContent = "— ms";

    spawnReactionTarget();

    reactionTimerInterval = setInterval(() => {
        secondsLeft -= 0.1;
        if (secondsLeft <= 0) {
            clearInterval(reactionTimerInterval);
            endReactionTest();
        } else {
            document.getElementById("reaction-timer").textContent = secondsLeft.toFixed(1) + "s";
        }
    }, 100);
}

function spawnReactionTarget() {
    const arena = document.getElementById("reaction-arena");
    const target = document.getElementById("reaction-target");
    if (!arena || !target || !reactionRunning) return;

    const arenaW = arena.clientWidth - 70;
    const arenaH = arena.clientHeight - 70;
    const randX = Math.max(10, Math.floor(Math.random() * arenaW));
    const randY = Math.max(10, Math.floor(Math.random() * arenaH));

    target.style.left = randX + "px";
    target.style.top = randY + "px";
    reactionStartTime = performance.now();
}

function hitReactionTarget() {
    if (!reactionRunning) return;
    const latency = Math.round(performance.now() - reactionStartTime);
    reactionHits++;
    reactionTotalLatency += latency;

    const avg = Math.round(reactionTotalLatency / reactionHits);
    document.getElementById("reaction-hits").textContent = reactionHits;
    document.getElementById("reaction-avg").textContent = avg + " ms";

    spawnReactionTarget();
}

function endReactionTest() {
    reactionRunning = false;
    const target = document.getElementById("reaction-target");
    const resultBox = document.getElementById("reaction-result-box");
    if (target) target.classList.add("hidden");

    const avg = reactionHits > 0 ? Math.round(reactionTotalLatency / reactionHits) : 350;
    document.getElementById("reaction-timer").textContent = "0.0s";
    document.getElementById("reaction-result-score").textContent = avg;

    let badge = "⚡ Quick Reflexes (Verified)";
    if (avg < 230) badge = "⚡ Lightning Reflexes (National Elite)";
    else if (avg < 280) badge = "⚡ Fast Reflexes (Pro Level)";
    document.getElementById("reaction-result-badge").textContent = badge;

    if (resultBox) resultBox.classList.remove("hidden");
}

async function autoSubmitReflexScore() {
    if (!AuthState?.isLoggedIn()) { openAuth('login'); return; }
    const score = Number(document.getElementById("reaction-result-score")?.textContent) || 240;
    try {
        const res = await apiRequest('/challenges/submit', {
            method: 'POST',
            auth: true,
            body: {
                drillType: 'REACTION_TAP',
                drillName: '30s Rapid Reaction Reflex Tap',
                score: score,
                unit: 'ms',
                notes: 'Completed in live interactive browser arena.'
            }
        });
        showFeatureToast(res.message || "Reflex score saved! Badge unlocked.", "success");
        loadChallengeLeaderboard('REACTION_TAP');
        if (typeof loadProfileIntoDashboard === 'function') loadProfileIntoDashboard();
    } catch (e) {
        showFeatureToast(e.message, "error");
    }
}

// Plank Timer
let plankInterval = null;
let plankSeconds = 0;
let plankRunning = false;

function togglePlankTimer() {
    const btn = document.getElementById("plank-start-btn");
    if (!plankRunning) {
        plankRunning = true;
        if (btn) btn.textContent = "Pause Plank";
        plankInterval = setInterval(() => {
            plankSeconds++;
            const mins = String(Math.floor(plankSeconds / 60)).padStart(2, '0');
            const secs = String(plankSeconds % 60).padStart(2, '0');
            document.getElementById("plank-display").textContent = `${mins}:${secs}`;
            if (plankSeconds === 60) showFeatureToast("Bronze Tier Achieved (60s)!", "info");
            if (plankSeconds === 120) showFeatureToast("Silver Tier Achieved (120s)!", "info");
            if (plankSeconds === 180) showFeatureToast("Gold Tier Achieved (180s)!", "success");
            if (plankSeconds === 240) showFeatureToast("Titanium Core Elite Achieved (240s)!", "success");
        }, 1000);
    } else {
        plankRunning = false;
        clearInterval(plankInterval);
        if (btn) btn.textContent = "Resume Plank";
    }
}

function resetPlankTimer() {
    plankRunning = false;
    clearInterval(plankInterval);
    plankSeconds = 0;
    document.getElementById("plank-display").textContent = "00:00";
    const btn = document.getElementById("plank-start-btn");
    if (btn) btn.textContent = "Start Plank";
}

async function savePlankScore() {
    if (!AuthState?.isLoggedIn()) { openAuth('login'); return; }
    if (plankSeconds < 10) { showFeatureToast("Hold for at least 10 seconds before recording.", "warning"); return; }
    try {
        const res = await apiRequest('/challenges/submit', {
            method: 'POST',
            auth: true,
            body: {
                drillType: 'PLANK_HOLD',
                drillName: 'Core Stability Plank Endurance',
                score: plankSeconds,
                unit: 'seconds',
                notes: 'Timed with live interactive stopwatch.'
            }
        });
        showFeatureToast(res.message || "Plank hold recorded!", "success");
        resetPlankTimer();
        loadChallengeLeaderboard('PLANK_HOLD');
        if (typeof loadProfileIntoDashboard === 'function') loadProfileIntoDashboard();
    } catch (e) {
        showFeatureToast(e.message, "error");
    }
}

// Cadence Drill
let cadenceInterval = null;
let cadenceTimeLeft = 60;
let cadenceReps = 0;
let cadenceRunning = false;

function startCadenceDrill() {
    if (cadenceInterval) clearInterval(cadenceInterval);
    cadenceTimeLeft = 60;
    cadenceReps = 0;
    cadenceRunning = true;
    document.getElementById("cadence-reps").innerHTML = `0 <span class="text-xl font-normal text-gray-500">reps</span>`;
    document.getElementById("cadence-timer-label").textContent = "Time Remaining: 60s";

    cadenceInterval = setInterval(() => {
        cadenceTimeLeft--;
        document.getElementById("cadence-timer-label").textContent = `Time Remaining: ${cadenceTimeLeft}s`;
        if (cadenceTimeLeft <= 0) {
            clearInterval(cadenceInterval);
            cadenceRunning = false;
            showFeatureToast(`Time's up! Completed ${cadenceReps} reps in 60s.`, "success");
        }
    }, 1000);
}

function tapCadenceRep() {
    if (!cadenceRunning && cadenceTimeLeft === 60) startCadenceDrill();
    cadenceReps++;
    document.getElementById("cadence-reps").innerHTML = `${cadenceReps} <span class="text-xl font-normal text-gray-500">reps</span>`;
}

async function saveCadenceScore() {
    if (!AuthState?.isLoggedIn()) { openAuth('login'); return; }
    if (cadenceReps < 10) { showFeatureToast("Record at least 10 reps.", "warning"); return; }
    try {
        const res = await apiRequest('/challenges/submit', {
            method: 'POST',
            auth: true,
            body: {
                drillType: 'JUMP_ROPE',
                drillName: '60-Second Jump Rope Cadence',
                score: cadenceReps,
                unit: 'reps',
                notes: 'Logged via Live Cadence Tracker.'
            }
        });
        showFeatureToast(res.message || "Cadence record logged!", "success");
        loadChallengeLeaderboard('JUMP_ROPE');
        if (typeof loadProfileIntoDashboard === 'function') loadProfileIntoDashboard();
    } catch (e) {
        showFeatureToast(e.message, "error");
    }
}

// Challenge Modal & Leaderboard
function openChallengeSubmitModal() {
    if (!AuthState?.isLoggedIn()) { openAuth('login'); return; }
    openModal('challenge-modal');
}

function onChallengeDrillChange(type) {
    const unitMap = {
        'REACTION_TAP': 'ms',
        'PLANK_HOLD': 'seconds',
        'JUMP_ROPE': 'reps',
        'SHUTTLE_RUN': 'seconds',
        'BROAD_JUMP': 'meters'
    };
    const unitEl = document.getElementById('challenge-form-unit');
    if (unitEl) unitEl.value = unitMap[type] || 'units';
}

async function submitChallengeForm(e) {
    e.preventDefault();
    if (!AuthState?.isLoggedIn()) { openAuth('login'); return; }
    const drillType = document.getElementById('challenge-form-type').value;
    const drillNames = {
        'REACTION_TAP': '30s Rapid Reaction Reflex Tap',
        'PLANK_HOLD': 'Core Stability Plank Endurance',
        'JUMP_ROPE': '60-Second Jump Rope Cadence',
        'SHUTTLE_RUN': 'Pro Agility 5-10-5 Shuttle Run',
        'BROAD_JUMP': 'Standing Broad Jump Power'
    };
    const score = Number(document.getElementById('challenge-form-score').value);
    const unit = document.getElementById('challenge-form-unit').value;
    const videoUrl = document.getElementById('challenge-form-video').value.trim();
    const notes = document.getElementById('challenge-form-notes').value.trim();

    try {
        const res = await apiRequest('/challenges/submit', {
            method: 'POST',
            auth: true,
            body: {
                drillType,
                drillName: drillNames[drillType] || drillType,
                score,
                unit,
                videoUrl,
                notes
            }
        });
        closeModal('challenge-modal');
        document.getElementById('challenge-submit-form').reset();
        showFeatureToast(res.message || "Drill verified and badge awarded!", "success");
        loadChallengeLeaderboard(drillType);
        if (typeof loadProfileIntoDashboard === 'function') loadProfileIntoDashboard();
    } catch (err) {
        showFeatureToast(err.message, "error");
    }
}

async function loadChallengeLeaderboard(drillType = 'ALL') {
    const tbody = document.getElementById('challenges-leaderboard-body');
    if (!tbody) return;
    try {
        const qs = drillType && drillType !== 'ALL' ? `?drillType=${encodeURIComponent(drillType)}` : '';
        const rows = await apiRequest(`/challenges/leaderboard${qs}`);
        if (!rows || !rows.length) {
            tbody.innerHTML = `<tr><td colspan="6" class="py-6 text-center text-gray-500">No benchmark records logged yet in this category. Be the first to set a record!</td></tr>`;
            return;
        }

        tbody.innerHTML = rows.map((r, i) => {
            const rankBadge = i === 0 ? '🥇' : (i === 1 ? '🥈' : (i === 2 ? '🥉' : `#${i + 1}`));
            return `
                <tr class="hover:bg-gray-900/40 transition">
                    <td class="py-3 px-3 font-bold font-mono ${i < 3 ? 'text-yellow-400 text-sm' : 'text-gray-400'}">${rankBadge}</td>
                    <td class="py-3 px-3">
                        <div class="font-bold text-white flex items-center gap-1.5">
                            ${esc(r.athlete_name)}
                        </div>
                        <div class="text-[10px] text-gray-500">${esc(r.sport || 'Athlete')} • ${esc(r.location || 'India')}</div>
                    </td>
                    <td class="py-3 px-3 text-cyan-300 font-medium">${esc(r.drill_name)}</td>
                    <td class="py-3 px-3 font-mono font-bold text-emerald-400 text-sm">${esc(r.score)} <span class="text-xs font-normal text-gray-400">${esc(r.unit)}</span></td>
                    <td class="py-3 px-3"><span class="px-2.5 py-0.5 rounded-full bg-cyan-500/10 text-cyan-300 border border-cyan-500/30 text-[10px] font-bold">${esc(r.badge_awarded || 'Verified')}</span></td>
                    <td class="py-3 px-3 text-gray-500 text-[10px]">${String(r.created_at || '').substring(0, 10)}</td>
                </tr>
            `;
        }).join('');
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="6" class="py-4 text-center text-red-400">Failed to load leaderboard.</td></tr>`;
    }
}

// =========================================================================
// FEATURE 4: AI ATHLETIC RECOVERY & READINESS LAB
// =========================================================================

function loadRecoveryLab() {
    updateLiveReadiness();
    changePrehabSport('athletics');
    if (AuthState?.isLoggedIn()) {
        apiRequest('/recovery/status', { auth: true }).then(res => {
            if (res.todayLog) {
                const log = res.todayLog;
                if (document.getElementById('input-resting-hr')) document.getElementById('input-resting-hr').value = log.resting_hr;
                if (document.getElementById('input-sleep')) document.getElementById('input-sleep').value = log.sleep_hours;
                if (document.getElementById('input-soreness')) document.getElementById('input-soreness').value = log.soreness_level;
                if (document.getElementById('input-fatigue')) document.getElementById('input-fatigue').value = log.fatigue_level;
                if (document.getElementById('input-hydration')) document.getElementById('input-hydration').value = log.hydration_liters;
                updateLiveReadiness();
            }
        }).catch(() => {});
    }
}

function updateLiveReadiness() {
    const hr = Number(document.getElementById('input-resting-hr')?.value) || 54;
    const sleep = Number(document.getElementById('input-sleep')?.value) || 8;
    const soreness = Number(document.getElementById('input-soreness')?.value) || 3;
    const fatigue = Number(document.getElementById('input-fatigue')?.value) || 2;
    const hydration = Number(document.getElementById('input-hydration')?.value) || 3.2;

    if (document.getElementById('hr-val-display')) document.getElementById('hr-val-display').textContent = `${hr} bpm`;
    if (document.getElementById('sleep-val-display')) document.getElementById('sleep-val-display').textContent = `${sleep.toFixed(1)} hrs`;
    if (document.getElementById('soreness-val-display')) document.getElementById('soreness-val-display').textContent = `${soreness} / 10`;
    if (document.getElementById('fatigue-val-display')) document.getElementById('fatigue-val-display').textContent = `${fatigue} / 10`;
    if (document.getElementById('hydration-val-display')) document.getElementById('hydration-val-display').textContent = `${hydration.toFixed(1)} L`;

    const sleepScore = Math.min(100, Math.max(30, (sleep / 8.0) * 100));
    const hrScore = hr <= 52 ? 100 : (hr <= 60 ? 90 : (hr <= 70 ? 75 : 55));
    const sorenessScore = (11 - soreness) * 10;
    const fatigueScore = (11 - fatigue) * 10;
    const hydroScore = Math.min(100, Math.max(40, (hydration / 3.0) * 100));

    let readiness = Math.round(sleepScore * 0.25 + hrScore * 0.20 + sorenessScore * 0.20 + fatigueScore * 0.20 + hydroScore * 0.15);
    readiness = Math.max(25, Math.min(99, readiness));

    const scoreDisplay = document.getElementById('readiness-score-display');
    const statusLabel = document.getElementById('readiness-status-label');
    const adviceText = document.getElementById('readiness-advice-text');
    const riskPill = document.getElementById('readiness-risk-pill');

    if (scoreDisplay) scoreDisplay.textContent = `${readiness}%`;

    if (readiness >= 85) {
        if (statusLabel) statusLabel.textContent = "OPTIMAL READINESS";
        if (adviceText) adviceText.textContent = "Prime nervous system condition. Cleared for maximal effort speed, power, and high-intensity game play.";
        if (riskPill) {
            riskPill.className = "px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-wider bg-emerald-500/20 text-emerald-300 border border-emerald-500/40";
            riskPill.textContent = "Low Injury Risk";
        }
    } else if (readiness >= 65) {
        if (statusLabel) statusLabel.textContent = "MODERATE READINESS";
        if (adviceText) adviceText.textContent = "Adequate recovery. Maintain technical and tactical drills; avoid 1-rep max lifts and prolonged eccentric load.";
        if (riskPill) {
            riskPill.className = "px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-wider bg-yellow-500/20 text-yellow-300 border border-yellow-500/40";
            riskPill.textContent = "Moderate Risk (Deload)";
        }
    } else {
        if (statusLabel) statusLabel.textContent = "FATIGUE WARNING";
        if (adviceText) adviceText.textContent = "Systemic neuromuscular fatigue detected. Prescribed: Active recovery, myofascial foam rolling, and contrast showers.";
        if (riskPill) {
            riskPill.className = "px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-wider bg-red-500/20 text-red-300 border border-red-500/40";
            riskPill.textContent = "Elevated Injury Risk";
        }
    }
}

async function submitReadinessCheck(e) {
    e.preventDefault();
    if (!AuthState?.isLoggedIn()) { openAuth('login'); return; }
    const restingHr = Number(document.getElementById('input-resting-hr')?.value) || 54;
    const sleepHours = Number(document.getElementById('input-sleep')?.value) || 8.0;
    const sorenessLevel = Number(document.getElementById('input-soreness')?.value) || 3;
    const fatigueLevel = Number(document.getElementById('input-fatigue')?.value) || 2;
    const hydrationLiters = Number(document.getElementById('input-hydration')?.value) || 3.2;
    const notes = document.getElementById('readiness-notes')?.value || '';

    try {
        const res = await apiRequest('/recovery/log', {
            method: 'POST',
            auth: true,
            body: { restingHr, sleepHours, sorenessLevel, fatigueLevel, hydrationLiters, notes }
        });
        showFeatureToast(`Daily recovery logged! Readiness Score: ${res.readinessScore}% (${res.statusLabel})`, "success");
        if (typeof loadProfileIntoDashboard === 'function') loadProfileIntoDashboard();
    } catch (err) {
        showFeatureToast(err.message, "error");
    }
}

async function changePrehabSport(sport) {
    const list = document.getElementById('prehab-protocols-list');
    if (!list) return;
    try {
        const rows = await apiRequest(`/recovery/injury-guidelines?sport=${encodeURIComponent(sport)}`);
        list.innerHTML = rows.map(r => `
            <div class="p-3.5 rounded-2xl bg-gray-900/70 border border-gray-800 space-y-1">
                <div class="flex items-center justify-between">
                    <span class="text-xs font-bold text-cyan-300">${esc(r.targetZone)}</span>
                    <span class="text-[10px] font-mono text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">${esc(r.dosage)}</span>
                </div>
                <p class="text-xs text-gray-300 font-medium">${esc(r.protocol)}</p>
            </div>
        `).join('');
    } catch (e) {
        list.innerHTML = `<p class="text-xs text-gray-500">Failed to load injury guidelines.</p>`;
    }
}

// 4-7-8 Breathing Protocol Timer
let breathingInterval = null;
let breathingSeconds = 180;
let breathingPhase = 0; // 0: inhale (4s), 1: hold (7s), 2: exhale (8s)
let breathingRunning = false;
let phaseTimer = 0;

function toggleBreathingTimer() {
    const btn = document.getElementById('breathing-start-btn');
    const orb = document.getElementById('breathing-orb');
    const cue = document.getElementById('breathing-cue');

    if (!breathingRunning) {
        breathingRunning = true;
        if (btn) btn.textContent = "Pause Protocol";
        breathingInterval = setInterval(() => {
            breathingSeconds--;
            phaseTimer++;
            const m = String(Math.floor(breathingSeconds / 60)).padStart(2, '0');
            const s = String(breathingSeconds % 60).padStart(2, '0');
            const timerDisplay = document.getElementById('breathing-timer-display');
            if (timerDisplay) timerDisplay.textContent = `${m}:${s}`;

            if (breathingPhase === 0) { // Inhale
                if (cue) cue.textContent = `Inhale gently through nose (${4 - phaseTimer}s)...`;
                if (orb) orb.style.transform = `scale(${1 + (phaseTimer / 4) * 0.4})`;
                if (phaseTimer >= 4) { breathingPhase = 1; phaseTimer = 0; if (orb) orb.textContent = "Hold"; }
            } else if (breathingPhase === 1) { // Hold
                if (cue) cue.textContent = `Hold breath gently (${7 - phaseTimer}s)...`;
                if (phaseTimer >= 7) { breathingPhase = 2; phaseTimer = 0; if (orb) orb.textContent = "Exhale"; }
            } else if (breathingPhase === 2) { // Exhale
                if (cue) cue.textContent = `Exhale completely through mouth (${8 - phaseTimer}s)...`;
                if (orb) orb.style.transform = `scale(${1.4 - (phaseTimer / 8) * 0.4})`;
                if (phaseTimer >= 8) { breathingPhase = 0; phaseTimer = 0; if (orb) orb.textContent = "Inhale"; }
            }

            if (breathingSeconds <= 0) {
                clearInterval(breathingInterval);
                breathingRunning = false;
                if (btn) btn.textContent = "Completed!";
                if (cue) cue.textContent = "Protocol complete. Nervous system down-regulated to parasympathetic rest state.";
                showFeatureToast("Parasympathetic recovery breathing session complete!", "success");
            }
        }, 1000);
    } else {
        breathingRunning = false;
        clearInterval(breathingInterval);
        if (btn) btn.textContent = "Resume Protocol";
    }
}

function resetBreathingTimer() {
    breathingRunning = false;
    clearInterval(breathingInterval);
    breathingSeconds = 180;
    breathingPhase = 0;
    phaseTimer = 0;
    const timerDisplay = document.getElementById('breathing-timer-display');
    if (timerDisplay) timerDisplay.textContent = "03:00";
    const btn = document.getElementById('breathing-start-btn');
    if (btn) btn.textContent = "Start 3-Min Protocol";
    const orb = document.getElementById('breathing-orb');
    if (orb) { orb.textContent = "Inhale"; orb.style.transform = "scale(1)"; }
    const cue = document.getElementById('breathing-cue');
    if (cue) cue.textContent = "Inhale through nose for 4 seconds...";
}

window.loadAthleteFeatures = loadAthleteFeatures;
window.loadDashboardTrials = loadDashboardTrials;
window.loadDashboard = loadDashboard;
window.loadTrials = loadTrials;
window.loadScouts = loadScouts;
window.showTrialSlots = showTrialSlots;
window.bookSlot = bookSlot;
window.addAchievement = addAchievement;
window.deleteAchievement = deleteAchievement;
window.submitAchievement = submitAchievement;
window.openTrialApplication = openTrialApplication;
window.submitTrialApplication = submitTrialApplication;
window.connectScout = connectScout;
window.findNearbyCoaches = findNearbyCoaches;
window.openCoachConnection = openCoachConnection;
window.submitCoachConnection = submitCoachConnection;
window.openScoutConnection = openScoutConnection;
window.submitScoutConnection = submitScoutConnection;
window.showBooking = showBooking;
window.openAchievementModal = openAchievementModal;
window.closeModal = closeModal;
window.runAIMatching = runAIMatching;
window.openScholarshipForm = openScholarshipForm;
window.closeScholarshipForm = closeScholarshipForm;
window.closeScholarshipSuccess = closeScholarshipSuccess;
window.toggleDefenceResourcesList = toggleDefenceResourcesList;
window.submitScholarshipApplication = submitScholarshipApplication;
window.cancelScholarshipApplication = cancelScholarshipApplication;
window.loadMyScholarship = loadMyScholarship;
window.viewScoutProfile = viewScoutProfile;
window.openScoutingCardModal = openScoutingCardModal;
window.setScoutingQrMode = setScoutingQrMode;
window.switchDrillTab = switchDrillTab;
window.startReactionTest = startReactionTest;
window.hitReactionTarget = hitReactionTarget;
window.autoSubmitReflexScore = autoSubmitReflexScore;
window.togglePlankTimer = togglePlankTimer;
window.resetPlankTimer = resetPlankTimer;
window.savePlankScore = savePlankScore;
window.startCadenceDrill = startCadenceDrill;
window.tapCadenceRep = tapCadenceRep;
window.saveCadenceScore = saveCadenceScore;
window.openChallengeSubmitModal = openChallengeSubmitModal;
window.onChallengeDrillChange = onChallengeDrillChange;
window.submitChallengeForm = submitChallengeForm;
window.loadChallengeLeaderboard = loadChallengeLeaderboard;
window.loadChallengeArena = loadChallengeArena;
window.loadRecoveryLab = loadRecoveryLab;
window.updateLiveReadiness = updateLiveReadiness;
window.submitReadinessCheck = submitReadinessCheck;
window.changePrehabSport = changePrehabSport;
window.toggleBreathingTimer = toggleBreathingTimer;
window.resetBreathingTimer = resetBreathingTimer;

