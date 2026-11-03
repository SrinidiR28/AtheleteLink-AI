package com.athletelink.controller;

import com.athletelink.dto.AchievementRequest;
import com.athletelink.dto.ApiMessage;
import com.athletelink.dto.AthleteMetricRequest;
import com.athletelink.dto.BookingRequest;
import com.athletelink.dto.ChallengeSubmissionRequest;
import com.athletelink.dto.CoachConnectionRequest;
import com.athletelink.dto.ReadinessLogRequest;
import com.athletelink.dto.ScholarshipApplicationRequest;
import com.athletelink.dto.TrialApplicationRequest;
import com.athletelink.service.PlatformService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class PlatformController {
    private final PlatformService service;
    private final String publicBaseUrl;

    public PlatformController(PlatformService service, @org.springframework.beans.factory.annotation.Value("${athletelink.public-base-url:http://localhost:8080}") String publicBaseUrl){
        this.service = service;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
    }
    private long id(Authentication a){return (Long)a.getPrincipal();}

    @GetMapping("/stats") public Map<String,Object> publicStats(){return service.publicStats();}

    @GetMapping("/achievements") public Object achievements(Authentication a){return service.achievements(id(a));}
    @GetMapping("/achievements/{x}") public Object achievement(Authentication a,@PathVariable long x){return service.achievement(id(a),x);}
    @PostMapping("/achievements") public Object addAchievement(Authentication a,@Valid @RequestBody AchievementRequest r){return service.addAchievement(id(a),r);}
    @DeleteMapping("/achievements/{x}") public ApiMessage delAchievement(Authentication a,@PathVariable long x){service.deleteAchievement(id(a),x);return new ApiMessage("Achievement deleted");}

    // Athlete Performance Metrics & PRs
    @GetMapping("/metrics") public Object metrics(Authentication a){return Map.of("metrics", service.metrics(id(a)), "summary", service.metricsSummary(id(a)));}
    @PostMapping("/metrics") public Object addMetric(Authentication a,@Valid @RequestBody AthleteMetricRequest r){return service.addMetric(id(a),r);}
    @DeleteMapping("/metrics/{x}") public ApiMessage delMetric(Authentication a,@PathVariable long x){service.deleteMetric(id(a),x);return new ApiMessage("Metric deleted");}
    @GetMapping("/athletes/{athleteId}/scouting-card") public Object scoutingCard(@PathVariable long athleteId){return service.scoutingCard(athleteId);}

    // Feature 3: Challenges & Skills Arena
    @GetMapping("/challenges/drills") public Object challengeDrills(){return service.challengesCatalog();}
    @GetMapping("/challenges/leaderboard") public Object challengeLeaderboard(@RequestParam(required=false) String drillType){return service.challengesLeaderboard(drillType);}
    @PostMapping("/challenges/submit") public Object submitChallenge(Authentication a,@Valid @RequestBody ChallengeSubmissionRequest r){return service.submitChallenge(id(a), r);}

    // Feature 4: AI Athletic Recovery & Readiness Lab
    @GetMapping("/recovery/status") public Object recoveryStatus(Authentication a){return service.readinessStatus(id(a));}
    @PostMapping("/recovery/log") public Object logRecovery(Authentication a,@Valid @RequestBody ReadinessLogRequest r){return service.logReadiness(id(a), r);}
    @GetMapping("/recovery/injury-guidelines") public Object injuryGuidelines(@RequestParam(required=false) String sport){return service.injuryGuidelines(sport);}

    @GetMapping("/trials") public Object trials(@RequestParam(required=false) String sport, @RequestParam(required=false) String category, @RequestParam(required=false) String ageCategory, @RequestParam(required=false) String status, @RequestParam(required=false) String search){return (sport==null&&category==null&&ageCategory==null&&status==null&&search==null)?service.trials():service.trialsFiltered(sport,category,ageCategory,status,search);}
    @GetMapping("/trials/featured") public Object featuredTrials(){return service.featuredTrials();}
    @GetMapping("/trials/{x}") public Object trial(@PathVariable long x){return service.trial(x);}
    @GetMapping("/trials/{x}/slots") public Object slots(@PathVariable long x){return service.slots(x);}
    @PostMapping("/trials/{x}/apply") public Object applyTrial(Authentication a,@PathVariable long x,@Valid @RequestBody TrialApplicationRequest r){return service.applyTrial(id(a),x,r);}

    @GetMapping("/applications") public Object applications(Authentication a){return service.applications(id(a));}
    @GetMapping("/applications/{x}") public Object application(Authentication a,@PathVariable long x){return service.application(id(a),x);}

    // Scholarship Applications
    @PostMapping("/scholarships/apply") public Object applyScholarship(Authentication a, @RequestBody ScholarshipApplicationRequest r) {
        return service.applyScholarship(id(a), r);
    }
    @GetMapping("/scholarships/my-application") public Object getMyScholarship(Authentication a) {
        return service.getMyScholarshipApplication(id(a));
    }
    @GetMapping("/scholarships/applications") public Object getScholarshipApplications(Authentication a) {
        return service.scholarshipApplications(id(a));
    }
    @PostMapping("/scholarships/{x}/cancel") public Object cancelScholarship(Authentication a, @PathVariable long x) {
        return service.cancelScholarship(id(a), x);
    }

    @PostMapping("/bookings") public Object book(Authentication a,@Valid @RequestBody BookingRequest r){return service.book(id(a),r);}
    @GetMapping("/bookings") public Object bookings(Authentication a){return service.bookings(id(a));}
    @GetMapping("/bookings/{x}") public Object booking(Authentication a,@PathVariable long x){return service.booking(id(a),x);}
    @PostMapping("/bookings/{x}/cancel") public Object cancelBooking(Authentication a, @PathVariable long x){return service.cancelBooking(id(a),x);}
    @PostMapping("/applications/{x}/withdraw") public Object withdrawApplication(Authentication a, @PathVariable long x){return service.withdrawApplication(id(a),x);}

    @GetMapping("/scouts") public Object scouts(){return service.scouts();}
    @PostMapping("/scouts/{x}/connect") public Object connect(Authentication a,@PathVariable long x,@Valid @RequestBody(required=false) CoachConnectionRequest r){return service.connect(id(a),x,r==null?new CoachConnectionRequest():r);}
    @GetMapping("/connections") public Object connections(Authentication a){return service.connections(id(a));}

    @GetMapping("/notifications") public Object notifications(Authentication a){return service.notifications(id(a));}
    @PostMapping("/notifications/{x}/read") public ApiMessage read(Authentication a,@PathVariable long x){service.markNotificationRead(id(a),x);return new ApiMessage("Notification marked read");}
    @PostMapping("/notifications/read-all") public ApiMessage readAll(Authentication a){service.markAllNotificationsRead(id(a));return new ApiMessage("All notifications marked as read");}

    @GetMapping("/dashboard") public Object dashboard(Authentication a){return service.dashboard(id(a));}
    @GetMapping("/matching") public Object matching(Authentication a){return service.matching(id(a));}
    @GetMapping("/ai-score") public Object aiScore(Authentication a){return service.getAIScore(id(a));}
    @GetMapping("/athletes") public Object athletes(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size){return service.athleteDirectory(page,size);}
    @GetMapping("/athletes/{athleteId}/profile") public Object publicProfile(@PathVariable long athleteId){return service.publicAthleteProfile(athleteId);}
    @PostMapping("/videos/upload") public Object uploadVideo(Authentication a, @RequestParam("file") org.springframework.web.multipart.MultipartFile file){return service.uploadVideo(id(a),file);}
    @GetMapping("/coaches") public Object coaches(){return service.coaches();}
    @GetMapping("/coaches/nearby") public Object nearbyCoaches(Authentication a, @RequestParam(required=false) Double lat, @RequestParam(required=false) Double lon, @RequestParam(defaultValue="75") double radiusKm){return service.nearbyCoaches(id(a),lat,lon,radiusKm);}
    @PostMapping("/coaches/{x}/connect") public Object connectCoach(Authentication a,@PathVariable long x,@Valid @RequestBody(required=false) CoachConnectionRequest r){return service.connectCoach(id(a),x,r==null?new CoachConnectionRequest():r);}
    @GetMapping("/coach-connections") public Object coachConnections(Authentication a){return service.coachConnections(id(a));}
    @GetMapping("/opportunities") public Object opportunities(){return service.opportunities();}
    @GetMapping("/announcements") public Object announcements(){return service.announcements();}
    @PostMapping("/opportunities/{x}/apply") public Object opportunity(Authentication a,@PathVariable String x){return service.apply(id(a),x);}
    @GetMapping("/confirm-booking")
    public Object confirm(@RequestParam String token,
                          @RequestHeader(value = "Accept", required = false) String acceptHeader,
                          @RequestParam(value = "format", required = false) String format) {
        Map<String, Object> data = service.confirm(token);
        if ("json".equalsIgnoreCase(format) || (acceptHeader != null && acceptHeader.contains("application/json") && !acceptHeader.contains("text/html"))) {
            if (data == null) {
                return org.springframework.http.ResponseEntity.status(404).body(Map.of("error", "Invalid or expired confirmation token"));
            }
            return data;
        }
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.TEXT_HTML)
                .body(renderBookingConfirmationHtml(data, token));
    }

    @GetMapping("/trials/applications/confirm")
    public Object confirmTrialApplication(@RequestParam String token,
                                          @RequestHeader(value = "Accept", required = false) String acceptHeader,
                                          @RequestParam(value = "format", required = false) String format) {
        Map<String, Object> data = service.confirmTrialApplication(token);
        if ("json".equalsIgnoreCase(format) || (acceptHeader != null && acceptHeader.contains("application/json") && !acceptHeader.contains("text/html"))) {
            if (data == null) {
                return org.springframework.http.ResponseEntity.status(404).body(Map.of("error", "Invalid or expired confirmation token"));
            }
            return data;
        }
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.TEXT_HTML)
                .body(renderTrialApplicationConfirmationHtml(data, token));
    }

    @GetMapping("/trials/checkin")
    public Object checkinTrial(@RequestParam String token,
                               @RequestHeader(value = "Accept", required = false) String acceptHeader,
                               @RequestParam(value = "format", required = false) String format) {
        Map<String, Object> data = service.checkin(token);
        if ("json".equalsIgnoreCase(format) || (acceptHeader != null && acceptHeader.contains("application/json") && !acceptHeader.contains("text/html"))) {
            if (data == null) {
                return org.springframework.http.ResponseEntity.status(404).body(Map.of("error", "Invalid or expired check-in token"));
            }
            return data;
        }
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.TEXT_HTML)
                .body(renderTrialCheckinHtml(data, token));
    }

    private String renderBookingConfirmationHtml(Map<String, Object> data, String token) {
        if (data == null) {
            return renderErrorHtml("Invalid or Expired Booking Token", "The requested booking confirmation token could not be found or has expired.");
        }

        String bookingId = Objects.toString(data.get("booking_id"), "-");
        String athleteName = Objects.toString(data.get("athlete_name"), "Athlete");
        String email = Objects.toString(data.get("email"), "-");
        String trialName = Objects.toString(data.get("trial_name"), "Trial Event");
        String sport = Objects.toString(data.get("sport"), "-");
        String venue = Objects.toString(data.get("venue"), "Venue TBA");
        String slotDate = Objects.toString(data.get("slot_date"), "Date TBA");
        String slotTime = Objects.toString(data.get("slot_time"), "Time TBA");
        String orgName = Objects.toString(data.get("organization"), "AthleteLink Official");
        String wifiCheckinUrl = "http://10.65.24.238:8080/api/trials/checkin?token=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
        String checkinUrl = publicBaseUrl.contains("localhost") ? wifiCheckinUrl : (publicBaseUrl + "/api/trials/checkin?token=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8));
        String qrImgUrl = "https://api.qrserver.com/v1/create-qr-code/?size=180x180&margin=6&data=" + java.net.URLEncoder.encode(checkinUrl, java.nio.charset.StandardCharsets.UTF_8);

        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\" class=\"dark\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Trial Booking Confirmed – AthleteLink AI</title>\n" +
                "    <script src=\"https://cdn.tailwindcss.com\"></script>\n" +
                "    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css\">\n" +
                "    <link href=\"https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800;900&display=swap\" rel=\"stylesheet\">\n" +
                "    <style>\n" +
                "        body { font-family: 'Outfit', sans-serif; background-color: #060911; color: #f3f4f6; min-height: 100vh; }\n" +
                "        .glow-card { background: rgba(17, 24, 39, 0.92); border: 1px solid rgba(31, 41, 55, 0.8); backdrop-filter: blur(16px); box-shadow: 0 20px 40px -15px rgba(0, 0, 0, 0.8), 0 0 30px rgba(16, 185, 129, 0.15); }\n" +
                "        .badge-glow { box-shadow: 0 0 20px rgba(16, 185, 129, 0.4); }\n" +
                "        @media print {\n" +
                "            @page { size: A4 portrait; margin: 8mm 10mm; }\n" +
                "            .no-print { display: none !important; }\n" +
                "            body { background: white !important; color: #0f172a !important; padding: 0 !important; margin: 0 !important; }\n" +
                "            .glow-card { border: 2px solid #0284c7 !important; border-radius: 12px !important; box-shadow: none !important; background: white !important; color: #0f172a !important; margin: 0 auto !important; max-width: 100% !important; padding: 20px !important; page-break-inside: avoid !important; break-inside: avoid !important; }\n" +
                "            .text-white { color: #0f172a !important; }\n" +
                "            .text-gray-300, .text-gray-400 { color: #334155 !important; }\n" +
                "            .bg-gray-900, .bg-gray-800 { background: #f8fafc !important; border-color: #cbd5e1 !important; }\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body class=\"flex items-center justify-center p-4 sm:p-6 lg:p-10 relative overflow-x-hidden\">\n" +
                "    <!-- Background Radial Gradients -->\n" +
                "    <div class=\"fixed inset-0 pointer-events-none opacity-20 no-print\">\n" +
                "        <div class=\"absolute -top-40 -left-40 w-96 h-96 bg-cyan-500 rounded-full blur-3xl\"></div>\n" +
                "        <div class=\"absolute -bottom-40 -right-40 w-96 h-96 bg-emerald-500 rounded-full blur-3xl\"></div>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"w-full max-w-2xl glow-card rounded-2xl p-6 sm:p-10 relative z-10 my-8\">\n" +
                "        <!-- Brand Header -->\n" +
                "        <div class=\"flex items-center justify-between border-b border-gray-800 pb-5 mb-6\">\n" +
                "            <div class=\"flex items-center space-x-3\">\n" +
                "                <div class=\"w-10 h-10 rounded-xl bg-gradient-to-tr from-cyan-500 to-blue-600 flex items-center justify-center text-white font-black text-lg shadow-lg shadow-cyan-500/30\">\n" +
                "                    <i class=\"fa-solid fa-bolt\"></i>\n" +
                "                </div>\n" +
                "                <div>\n" +
                "                    <div class=\"text-xl font-black tracking-wider text-white\">ATHLETE<span class=\"text-cyan-400\">LINK</span> <span class=\"text-xs bg-cyan-500/20 text-cyan-300 px-2 py-0.5 rounded-full border border-cyan-500/40 ml-1\">AI</span></div>\n" +
                "                    <div class=\"text-xs text-gray-400 font-medium\">Official Digital Pass & Verification</div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "            <span class=\"px-3 py-1 bg-emerald-500/10 border border-emerald-500/40 text-emerald-400 rounded-full text-xs font-bold uppercase tracking-wider flex items-center gap-1.5\">\n" +
                "                <span class=\"w-2 h-2 rounded-full bg-emerald-400 animate-ping\"></span> Verified & Active\n" +
                "            </span>\n" +
                "        </div>\n" +
                "\n" +
                "        <!-- Centerpiece Verified Hero -->\n" +
                "        <div class=\"text-center mb-8\">\n" +
                "            <div class=\"w-20 h-20 mx-auto rounded-full bg-emerald-500/20 border-2 border-emerald-400 flex items-center justify-center text-emerald-400 text-3xl mb-4 badge-glow\">\n" +
                "                <i class=\"fa-solid fa-check\"></i>\n" +
                "            </div>\n" +
                "            <h1 class=\"text-2xl sm:text-3xl font-black text-white mb-2 tracking-tight\">Trial Booking Confirmed!</h1>\n" +
                "            <p class=\"text-sm text-gray-300 max-w-md mx-auto\">Your slot for <span class=\"text-cyan-400 font-semibold\">" + escapeHtml(trialName) + "</span> is confirmed in the AthleteLink AI system.</p>\n" +
                "        </div>\n" +
                "\n" +
                "        <!-- Pass Details Box -->\n" +
                "        <div class=\"bg-gray-900/80 rounded-xl p-5 border border-gray-800 space-y-4 mb-6\">\n" +
                "            <div class=\"flex items-center justify-between pb-3 border-b border-gray-800/80 text-xs font-semibold text-gray-400 uppercase tracking-wider\">\n" +
                "                <span>Booking Reference</span>\n" +
                "                <span class=\"text-cyan-400 font-mono text-sm font-bold\">#" + escapeHtml(bookingId) + "</span>\n" +
                "            </div>\n" +
                "            <div class=\"grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm\">\n" +
                "                <div>\n" +
                "                    <span class=\"text-xs text-gray-400 block mb-0.5\">Athlete Name</span>\n" +
                "                    <span class=\"font-bold text-white flex items-center gap-2\"><i class=\"fa-solid fa-user text-cyan-400 text-xs\"></i> " + escapeHtml(athleteName) + "</span>\n" +
                "                </div>\n" +
                "                <div>\n" +
                "                    <span class=\"text-xs text-gray-400 block mb-0.5\">Registered Email</span>\n" +
                "                    <span class=\"font-bold text-white truncate block\"><i class=\"fa-solid fa-envelope text-cyan-400 text-xs mr-1\"></i> " + escapeHtml(email) + "</span>\n" +
                "                </div>\n" +
                "                <div>\n" +
                "                    <span class=\"text-xs text-gray-400 block mb-0.5\">Sport & Organization</span>\n" +
                "                    <span class=\"font-bold text-white\"><i class=\"fa-solid fa-trophy text-yellow-400 text-xs mr-1\"></i> " + escapeHtml(sport) + " • " + escapeHtml(orgName) + "</span>\n" +
                "                </div>\n" +
                "                <div>\n" +
                "                    <span class=\"text-xs text-gray-400 block mb-0.5\">Venue Location</span>\n" +
                "                    <span class=\"font-bold text-white\"><i class=\"fa-solid fa-location-dot text-red-400 text-xs mr-1\"></i> " + escapeHtml(venue) + "</span>\n" +
                "                </div>\n" +
                "                <div class=\"sm:col-span-2 bg-gray-800/60 rounded-lg p-3 border border-gray-700/60 flex flex-wrap items-center justify-between gap-2\">\n" +
                "                    <div class=\"flex items-center gap-2 text-cyan-300 font-semibold\">\n" +
                "                        <i class=\"fa-solid fa-calendar-check text-cyan-400\"></i>\n" +
                "                        <span>" + escapeHtml(slotDate) + "</span>\n" +
                "                    </div>\n" +
                "                    <div class=\"flex items-center gap-2 text-emerald-300 font-bold bg-emerald-500/10 px-3 py-1 rounded-md border border-emerald-500/30\">\n" +
                "                        <i class=\"fa-solid fa-clock text-emerald-400\"></i>\n" +
                "                        <span>" + escapeHtml(slotTime) + "</span>\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "\n" +
                "        <!-- Official Gate Pass QR Code -->\n" +
                "        <div class=\"bg-gray-900/90 rounded-2xl p-5 border border-cyan-500/30 text-center space-y-3 mb-6 shadow-xl\">\n" +
                "            <div class=\"flex items-center justify-center gap-2 text-xs font-bold uppercase tracking-wider text-cyan-300\">\n" +
                "                <i class=\"fa-solid fa-qrcode text-base text-cyan-400\"></i> Official Gate Check-In QR Pass\n" +
                "            </div>\n" +
                "            <div class=\"inline-block p-3.5 bg-white rounded-2xl shadow-xl shadow-cyan-500/10 border-2 border-cyan-400\">\n" +
                "                <img src=\"" + qrImgUrl + "\" alt=\"Trial Check-in QR Code\" class=\"w-36 h-36 mx-auto block\" />\n" +
                "            </div>\n" +
                "            <p class=\"text-xs text-gray-300 max-w-sm mx-auto\">\n" +
                "                Scan with any smartphone camera at the venue reception to verify your trial pass and mark your attendance.\n" +
                "            </p>\n" +
                "            <div class=\"pt-1\">\n" +
                "                <a href=\"" + checkinUrl + "\" target=\"_blank\" class=\"inline-flex items-center gap-1.5 text-xs text-cyan-400 hover:text-cyan-300 underline font-semibold\">\n" +
                "                    <i class=\"fa-solid fa-arrow-up-right-from-square text-[10px]\"></i> Direct Gate Check-In Verification Link\n" +
                "                </a>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "\n" +
                "        <!-- Attendance Guidelines -->\n" +
                "        <div class=\"bg-cyan-950/20 border border-cyan-800/40 rounded-xl p-4 mb-8 text-xs text-cyan-200/90 space-y-2\">\n" +
                "            <div class=\"font-bold text-cyan-300 uppercase tracking-wider flex items-center gap-2 text-xs\">\n" +
                "                <i class=\"fa-solid fa-clipboard-check\"></i> Attendance & Trial Day Checklist\n" +
                "            </div>\n" +
                "            <ul class=\"list-disc list-inside space-y-1 text-gray-300 pl-1\">\n" +
                "                <li>Present this digital pass or mention Booking ID <strong class=\"text-white\">#" + escapeHtml(bookingId) + "</strong> at registration.</li>\n" +
                "                <li>Carry a valid photo identity card (Aadhaar / Passport / Student ID) and age proof.</li>\n" +
                "                <li>Arrive at the venue registration desk <strong class=\"text-cyan-300\">30 minutes before</strong> your scheduled slot.</li>\n" +
                "                <li>Wear appropriate sports gear, turf/court footwear, and bring personal hydration.</li>\n" +
                "            </ul>\n" +
                "        </div>\n" +
                "\n" +
                "        <!-- Action Buttons -->\n" +
                "        <div class=\"space-y-3 no-print\">\n" +
                "            <div class=\"grid grid-cols-1 sm:grid-cols-2 gap-3\">\n" +
                "                <a href=\"/#dashboard\" class=\"flex items-center justify-center gap-2 w-full py-3.5 px-4 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-gray-950 font-black text-sm uppercase tracking-wider transition-all duration-200 shadow-lg shadow-cyan-500/25\">\n" +
                "                    <i class=\"fa-solid fa-gauge-high\"></i> Open Dashboard\n" +
                "                </a>\n" +
                "                <a href=\"/#trials\" class=\"flex items-center justify-center gap-2 w-full py-3.5 px-4 rounded-xl bg-gray-800 hover:bg-gray-700 border border-gray-700 text-white font-bold text-sm tracking-wider transition-all duration-200\">\n" +
                "                    <i class=\"fa-solid fa-futbol\"></i> Explore More Trials\n" +
                "                </a>\n" +
                "            </div>\n" +
                "            <button onclick=\"window.print()\" class=\"w-full py-2.5 rounded-lg border border-gray-800 hover:border-gray-700 text-gray-400 hover:text-white text-xs font-semibold flex items-center justify-center gap-2 transition-all\">\n" +
                "                <i class=\"fa-solid fa-print\"></i> Print / Save Pass as PDF\n" +
                "            </button>\n" +
                "        </div>\n" +
                "\n" +
                "        <!-- Footer -->\n" +
                "        <div class=\"text-center text-xs text-gray-500 mt-8 pt-4 border-t border-gray-800/80\">\n" +
                "            AthleteLink AI • India's Smart Sports Talent Network • Authenticated Token: <span class=\"font-mono text-gray-400\">" + escapeHtml(token.substring(0, Math.min(token.length(), 10))) + "...</span>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    private String renderTrialApplicationConfirmationHtml(Map<String, Object> data, String token) {
        if (data == null) {
            return renderErrorHtml("Invalid or Expired Application Token", "The requested trial application confirmation token could not be found or has expired.");
        }

        String appId = Objects.toString(data.get("application_id"), "-");
        String athleteName = Objects.toString(data.get("athlete_name"), "Athlete");
        String email = Objects.toString(data.get("email"), "-");
        String trialName = Objects.toString(data.get("trial_name"), "Trial Event");
        String sport = Objects.toString(data.get("sport"), "-");
        String venue = Objects.toString(data.get("venue"), "Venue TBA");
        String trialDate = Objects.toString(data.get("trial_date"), "Date TBA");
        String orgName = Objects.toString(data.get("organization"), "AthleteLink Official");
        String age = Objects.toString(data.get("age"), "-");
        String role = Objects.toString(data.get("position_role"), "-");

        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\" class=\"dark\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Trial Application Confirmed – AthleteLink AI</title>\n" +
                "    <script src=\"https://cdn.tailwindcss.com\"></script>\n" +
                "    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css\">\n" +
                "    <link href=\"https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800;900&display=swap\" rel=\"stylesheet\">\n" +
                "    <style>\n" +
                "        body { font-family: 'Outfit', sans-serif; background-color: #060911; color: #f3f4f6; min-height: 100vh; }\n" +
                "        .glow-card { background: rgba(17, 24, 39, 0.92); border: 1px solid rgba(31, 41, 55, 0.8); backdrop-filter: blur(16px); box-shadow: 0 20px 40px -15px rgba(0, 0, 0, 0.8), 0 0 30px rgba(14, 165, 233, 0.15); }\n" +
                "        .badge-glow { box-shadow: 0 0 20px rgba(14, 165, 233, 0.4); }\n" +
                "        @media print { .no-print { display: none !important; } body { background: white; color: black; } .glow-card { border: 1px solid #ccc; box-shadow: none; background: white; } }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body class=\"flex items-center justify-center p-4 sm:p-6 lg:p-10 relative overflow-x-hidden\">\n" +
                "    <!-- Background Radial Gradients -->\n" +
                "    <div class=\"fixed inset-0 pointer-events-none opacity-20\">\n" +
                "        <div class=\"absolute -top-40 -left-40 w-96 h-96 bg-cyan-500 rounded-full blur-3xl\"></div>\n" +
                "        <div class=\"absolute -bottom-40 -right-40 w-96 h-96 bg-blue-500 rounded-full blur-3xl\"></div>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"w-full max-w-2xl glow-card rounded-2xl p-6 sm:p-10 relative z-10 my-8\">\n" +
                "        <!-- Brand Header -->\n" +
                "        <div class=\"flex items-center justify-between border-b border-gray-800 pb-5 mb-6\">\n" +
                "            <div class=\"flex items-center space-x-3\">\n" +
                "                <div class=\"w-10 h-10 rounded-xl bg-gradient-to-tr from-cyan-500 to-blue-600 flex items-center justify-center text-white font-black text-lg shadow-lg shadow-cyan-500/30\">\n" +
                "                    <i class=\"fa-solid fa-bolt\"></i>\n" +
                "                </div>\n" +
                "                <div>\n" +
                "                    <div class=\"text-xl font-black tracking-wider text-white\">ATHLETE<span class=\"text-cyan-400\">LINK</span> <span class=\"text-xs bg-cyan-500/20 text-cyan-300 px-2 py-0.5 rounded-full border border-cyan-500/40 ml-1\">AI</span></div>\n" +
                "                    <div class=\"text-xs text-gray-400 font-medium\">Trial Application Verified</div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "            <span class=\"px-3 py-1 bg-cyan-500/10 border border-cyan-500/40 text-cyan-400 rounded-full text-xs font-bold uppercase tracking-wider flex items-center gap-1.5\">\n" +
                "                <span class=\"w-2 h-2 rounded-full bg-cyan-400 animate-ping\"></span> Submitted & Verified\n" +
                "            </span>\n" +
                "        </div>\n" +
                "\n" +
                "        <!-- Centerpiece Verified Hero -->\n" +
                "        <div class=\"text-center mb-8\">\n" +
                "            <div class=\"w-20 h-20 mx-auto rounded-full bg-cyan-500/20 border-2 border-cyan-400 flex items-center justify-center text-cyan-400 text-3xl mb-4 badge-glow\">\n" +
                "                <i class=\"fa-solid fa-id-card-clip\"></i>\n" +
                "            </div>\n" +
                "            <h1 class=\"text-2xl sm:text-3xl font-black text-white mb-2 tracking-tight\">Trial Application Verified!</h1>\n" +
                "            <p class=\"text-sm text-gray-300 max-w-md mx-auto\">Your trial profile for <span class=\"text-cyan-400 font-semibold\">" + escapeHtml(trialName) + "</span> is confirmed. You can now select an available trial slot.</p>\n" +
                "        </div>\n" +
                "\n" +
                "        <!-- Pass Details Box -->\n" +
                "        <div class=\"bg-gray-900/80 rounded-xl p-5 border border-gray-800 space-y-4 mb-6\">\n" +
                "            <div class=\"flex items-center justify-between pb-3 border-b border-gray-800/80 text-xs font-semibold text-gray-400 uppercase tracking-wider\">\n" +
                "                <span>Application Reference</span>\n" +
                "                <span class=\"text-cyan-400 font-mono text-sm font-bold\">#" + escapeHtml(appId) + "</span>\n" +
                "            </div>\n" +
                "            <div class=\"grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm\">\n" +
                "                <div>\n" +
                "                    <span class=\"text-xs text-gray-400 block mb-0.5\">Athlete Name</span>\n" +
                "                    <span class=\"font-bold text-white flex items-center gap-2\"><i class=\"fa-solid fa-user text-cyan-400 text-xs\"></i> " + escapeHtml(athleteName) + "</span>\n" +
                "                </div>\n" +
                "                <div>\n" +
                "                    <span class=\"text-xs text-gray-400 block mb-0.5\">Registered Email</span>\n" +
                "                    <span class=\"font-bold text-white truncate block\"><i class=\"fa-solid fa-envelope text-cyan-400 text-xs mr-1\"></i> " + escapeHtml(email) + "</span>\n" +
                "                </div>\n" +
                "                <div>\n" +
                "                    <span class=\"text-xs text-gray-400 block mb-0.5\">Sport & Position</span>\n" +
                "                    <span class=\"font-bold text-white\"><i class=\"fa-solid fa-trophy text-yellow-400 text-xs mr-1\"></i> " + escapeHtml(sport) + " (" + escapeHtml(role) + ")</span>\n" +
                "                </div>\n" +
                "                <div>\n" +
                "                    <span class=\"text-xs text-gray-400 block mb-0.5\">Age / Category</span>\n" +
                "                    <span class=\"font-bold text-white\"><i class=\"fa-solid fa-person-running text-cyan-400 text-xs mr-1\"></i> Age: " + escapeHtml(age) + "</span>\n" +
                "                </div>\n" +
                "                <div>\n" +
                "                    <span class=\"text-xs text-gray-400 block mb-0.5\">Trial Event</span>\n" +
                "                    <span class=\"font-bold text-white\">" + escapeHtml(trialName) + "</span>\n" +
                "                </div>\n" +
                "                <div>\n" +
                "                    <span class=\"text-xs text-gray-400 block mb-0.5\">Venue & Date</span>\n" +
                "                    <span class=\"font-bold text-white\"><i class=\"fa-solid fa-location-dot text-red-400 text-xs mr-1\"></i> " + escapeHtml(venue) + " (" + escapeHtml(trialDate) + ")</span>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "\n" +
                "        <!-- Action Buttons -->\n" +
                "        <div class=\"space-y-3 no-print\">\n" +
                "            <div class=\"grid grid-cols-1 sm:grid-cols-2 gap-3\">\n" +
                "                <a href=\"/#trials\" class=\"flex items-center justify-center gap-2 w-full py-3.5 px-4 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-gray-950 font-black text-sm uppercase tracking-wider transition-all duration-200 shadow-lg shadow-cyan-500/25\">\n" +
                "                    <i class=\"fa-solid fa-calendar-plus\"></i> Book Trial Slot Now\n" +
                "                </a>\n" +
                "                <a href=\"/#dashboard\" class=\"flex items-center justify-center gap-2 w-full py-3.5 px-4 rounded-xl bg-gray-800 hover:bg-gray-700 border border-gray-700 text-white font-bold text-sm tracking-wider transition-all duration-200\">\n" +
                "                    <i class=\"fa-solid fa-gauge-high\"></i> Open Dashboard\n" +
                "                </a>\n" +
                "            </div>\n" +
                "            <button onclick=\"window.print()\" class=\"w-full py-2.5 rounded-lg border border-gray-800 hover:border-gray-700 text-gray-400 hover:text-white text-xs font-semibold flex items-center justify-center gap-2 transition-all\">\n" +
                "                <i class=\"fa-solid fa-print\"></i> Print Application Pass\n" +
                "            </button>\n" +
                "        </div>\n" +
                "\n" +
                "        <!-- Footer -->\n" +
                "        <div class=\"text-center text-xs text-gray-500 mt-8 pt-4 border-t border-gray-800/80\">\n" +
                "            AthleteLink AI • India's Smart Sports Talent Network • Application Reference: <span class=\"font-mono text-gray-400\">#" + escapeHtml(appId) + "</span>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    private String renderTrialCheckinHtml(Map<String, Object> data, String token) {
        if (data == null) {
            return renderErrorHtml("Invalid or Expired Check-in Token", "This trial check-in pass could not be verified or the token is invalid.");
        }

        String bookingId = Objects.toString(data.get("booking_id"), "-");
        String athleteName = Objects.toString(data.get("athlete_name"), "Athlete");
        String email = Objects.toString(data.get("email"), "-");
        String trialName = Objects.toString(data.get("trial_name"), "Trial Event");
        String sport = Objects.toString(data.get("sport"), "-");
        String venue = Objects.toString(data.get("venue"), "Venue TBA");
        String slotDate = Objects.toString(data.get("slot_date"), "Date TBA");
        String slotTime = Objects.toString(data.get("slot_time"), "Time TBA");
        String orgName = Objects.toString(data.get("organization"), "AthleteLink Official");
        boolean justChecked = Boolean.TRUE.equals(data.get("justCheckedIn"));
        String checkedInAt = Objects.toString(data.get("checked_in_at"), "Just now");

        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\" class=\"dark\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Trial Attendance Verified – AthleteLink AI</title>\n" +
                "    <script src=\"https://cdn.tailwindcss.com\"></script>\n" +
                "    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css\">\n" +
                "    <link href=\"https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800;900&display=swap\" rel=\"stylesheet\">\n" +
                "    <style>\n" +
                "        body { font-family: 'Outfit', sans-serif; background-color: #060911; color: #f3f4f6; min-height: 100vh; }\n" +
                "        .glow-card { background: rgba(17, 24, 39, 0.92); border: 1px solid rgba(16, 185, 129, 0.4); backdrop-filter: blur(16px); box-shadow: 0 20px 40px -15px rgba(0, 0, 0, 0.8), 0 0 40px rgba(16, 185, 129, 0.25); }\n" +
                "        .badge-glow { box-shadow: 0 0 25px rgba(16, 185, 129, 0.5); }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body class=\"flex items-center justify-center p-4 sm:p-6 lg:p-10 relative overflow-x-hidden\">\n" +
                "    <div class=\"fixed inset-0 pointer-events-none opacity-20\">\n" +
                "        <div class=\"absolute -top-40 -left-40 w-96 h-96 bg-emerald-500 rounded-full blur-3xl\"></div>\n" +
                "        <div class=\"absolute -bottom-40 -right-40 w-96 h-96 bg-cyan-500 rounded-full blur-3xl\"></div>\n" +
                "    </div>\n" +
                "    <div class=\"w-full max-w-xl glow-card rounded-2xl p-6 sm:p-10 relative z-10 my-8\">\n" +
                "        <div class=\"flex items-center justify-between border-b border-gray-800 pb-5 mb-6\">\n" +
                "            <div class=\"flex items-center space-x-3\">\n" +
                "                <div class=\"w-10 h-10 rounded-xl bg-gradient-to-tr from-emerald-500 to-cyan-500 flex items-center justify-center text-white font-black text-lg shadow-lg shadow-emerald-500/30\">\n" +
                "                    <i class=\"fa-solid fa-qrcode\"></i>\n" +
                "                </div>\n" +
                "                <div>\n" +
                "                    <div class=\"text-xl font-black tracking-wider text-white\">ATHLETE<span class=\"text-cyan-400\">LINK</span> <span class=\"text-emerald-400\">AI</span></div>\n" +
                "                    <div class=\"text-xs text-gray-400 font-medium\">Official Trial Gate Check-In</div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "            <span class=\"px-3 py-1 bg-emerald-500/20 border border-emerald-400 text-emerald-300 rounded-full text-xs font-black uppercase tracking-wider flex items-center gap-1.5\">\n" +
                "                <i class=\"fa-solid fa-circle-check\"></i> " + (justChecked ? "CHECKED IN NOW" : "ALREADY CHECKED IN") + "\n" +
                "            </span>\n" +
                "        </div>\n" +
                "        <div class=\"text-center mb-6\">\n" +
                "            <div class=\"w-20 h-20 mx-auto rounded-full bg-emerald-500/20 border-2 border-emerald-400 flex items-center justify-center text-emerald-400 text-3xl mb-4 badge-glow\">\n" +
                "                <i class=\"fa-solid fa-user-check\"></i>\n" +
                "            </div>\n" +
                "            <h1 class=\"text-2xl font-black text-white mb-2 tracking-tight\">Attendance Verified!</h1>\n" +
                "            <p class=\"text-sm text-emerald-300 font-semibold\">Athlete <span class=\"text-white font-bold\">" + escapeHtml(athleteName) + "</span> is officially admitted for trial session.</p>\n" +
                "        </div>\n" +
                "        <div class=\"bg-gray-900/90 rounded-xl p-5 border border-gray-800 space-y-3 mb-6 text-sm\">\n" +
                "            <div class=\"flex justify-between border-b border-gray-800 pb-2 text-xs\">\n" +
                "                <span class=\"text-gray-400\">Booking ID</span>\n" +
                "                <span class=\"font-mono font-bold text-cyan-400\">#" + escapeHtml(bookingId) + "</span>\n" +
                "            </div>\n" +
                "            <div class=\"flex justify-between border-b border-gray-800 pb-2 text-xs\">\n" +
                "                <span class=\"text-gray-400\">Trial Event</span>\n" +
                "                <span class=\"font-bold text-white\">" + escapeHtml(trialName) + "</span>\n" +
                "            </div>\n" +
                "            <div class=\"flex justify-between border-b border-gray-800 pb-2 text-xs\">\n" +
                "                <span class=\"text-gray-400\">Sport & Organization</span>\n" +
                "                <span class=\"font-semibold text-gray-200\">" + escapeHtml(sport) + " (" + escapeHtml(orgName) + ")</span>\n" +
                "            </div>\n" +
                "            <div class=\"flex justify-between border-b border-gray-800 pb-2 text-xs\">\n" +
                "                <span class=\"text-gray-400\">Scheduled Slot</span>\n" +
                "                <span class=\"font-bold text-emerald-400\">" + escapeHtml(slotDate) + " at " + escapeHtml(slotTime) + "</span>\n" +
                "            </div>\n" +
                "            <div class=\"flex justify-between border-b border-gray-800 pb-2 text-xs\">\n" +
                "                <span class=\"text-gray-400\">Venue</span>\n" +
                "                <span class=\"font-semibold text-gray-200 text-right\">" + escapeHtml(venue) + "</span>\n" +
                "            </div>\n" +
                "            <div class=\"flex justify-between text-xs pt-1\">\n" +
                "                <span class=\"text-gray-400\">Verified Timestamp</span>\n" +
                "                <span class=\"font-mono text-cyan-300\">" + escapeHtml(checkedInAt) + "</span>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <div class=\"space-y-3\">\n" +
                "            <a href=\"/#dashboard\" class=\"flex items-center justify-center gap-2 w-full py-3.5 px-4 rounded-xl bg-gradient-to-r from-emerald-500 to-cyan-500 text-gray-950 font-black text-sm uppercase tracking-wider transition hover:opacity-90\">\n" +
                "                <i class=\"fa-solid fa-gauge-high\"></i> Open AthleteLink Dashboard\n" +
                "            </a>\n" +
                "        </div>\n" +
                "        <div class=\"text-center text-xs text-gray-500 mt-6 pt-4 border-t border-gray-800\">\n" +
                "            AthleteLink AI Verification Service • Token: " + escapeHtml(token.substring(0, Math.min(token.length(), 10))) + "...\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    private String renderErrorHtml(String title, String message) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\" class=\"dark\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>" + escapeHtml(title) + " – AthleteLink AI</title>\n" +
                "    <script src=\"https://cdn.tailwindcss.com\"></script>\n" +
                "    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css\">\n" +
                "    <link href=\"https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800;900&display=swap\" rel=\"stylesheet\">\n" +
                "    <style>body { font-family: 'Outfit', sans-serif; background-color: #060911; color: #f3f4f6; min-height: 100vh; }</style>\n" +
                "</head>\n" +
                "<body class=\"flex items-center justify-center p-4\">\n" +
                "    <div class=\"w-full max-w-md bg-gray-900 border border-gray-800 rounded-2xl p-8 text-center shadow-2xl\">\n" +
                "        <div class=\"w-16 h-16 mx-auto rounded-full bg-red-500/20 border border-red-500/40 flex items-center justify-center text-red-400 text-2xl mb-4\">\n" +
                "            <i class=\"fa-solid fa-triangle-exclamation\"></i>\n" +
                "        </div>\n" +
                "        <h1 class=\"text-xl font-bold text-white mb-2\">" + escapeHtml(title) + "</h1>\n" +
                "        <p class=\"text-sm text-gray-400 mb-6\">" + escapeHtml(message) + "</p>\n" +
                "        <a href=\"/\" class=\"inline-flex items-center justify-center gap-2 w-full py-3 px-4 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-gray-950 font-bold text-sm uppercase tracking-wider\">\n" +
                "            <i class=\"fa-solid fa-house\"></i> Return to AthleteLink AI Home\n" +
                "        </a>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
