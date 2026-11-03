package com.athletelink.controller;

import com.athletelink.dto.ApiMessage;
import com.athletelink.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // Helper: verify admin access
    private void checkAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
        if (!isAdmin) {
            // Also check if username or email is admin
            String name = auth.getName();
            if (!"admin@athletelink.ai".equalsIgnoreCase(name)) {
                throw new org.springframework.security.access.AccessDeniedException("Access restricted to platform administrators");
            }
        }
    }

    // =========================================================================
    // OVERVIEW
    // =========================================================================
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getOverviewStats(Authentication auth) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getOverviewStats());
    }

    // =========================================================================
    // USERS
    // =========================================================================
    @GetMapping("/users")
    public ResponseEntity<?> getUsers(Authentication auth) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(Authentication auth, @PathVariable Long id) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getUser(id));
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(Authentication auth, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createUser(req));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.updateUser(id, req));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<?> toggleUserStatus(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        String status = (String) req.get("status");
        adminService.toggleUserStatus(id, status);
        return ResponseEntity.ok(new ApiMessage("User status updated to " + status));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<?> setUserRole(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        String role = (String) req.get("role");
        adminService.setUserRole(id, role);
        return ResponseEntity.ok(new ApiMessage("User role updated to " + role));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(Authentication auth, @PathVariable Long id) {
        checkAdmin(auth);
        adminService.deleteUser(id);
        return ResponseEntity.ok(new ApiMessage("User deleted successfully"));
    }

    // =========================================================================
    // ATHLETES
    // =========================================================================
    @GetMapping("/athletes")
    public ResponseEntity<?> getAthletes(Authentication auth) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getAthletes());
    }

    @GetMapping("/athletes/{id}")
    public ResponseEntity<?> getAthleteDetails(Authentication auth, @PathVariable Long id) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getAthleteDetails(id));
    }

    // =========================================================================
    // COACHES
    // =========================================================================
    @GetMapping("/coaches")
    public ResponseEntity<?> getCoaches(Authentication auth) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getCoaches());
    }

    @PostMapping("/coaches")
    public ResponseEntity<?> createCoach(Authentication auth, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createCoach(req));
    }

    @PutMapping("/coaches/{id}")
    public ResponseEntity<?> updateCoach(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.updateCoach(id, req));
    }

    @DeleteMapping("/coaches/{id}")
    public ResponseEntity<?> deleteCoach(Authentication auth, @PathVariable Long id) {
        checkAdmin(auth);
        adminService.deleteCoach(id);
        return ResponseEntity.ok(new ApiMessage("Coach deleted successfully"));
    }

    @GetMapping("/coaches/connections")
    public ResponseEntity<?> getCoachConnections(Authentication auth) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getCoachConnections());
    }

    @PatchMapping("/coaches/connections/{id}/status")
    public ResponseEntity<?> updateCoachConnectionStatus(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        String status = (String) req.get("status");
        adminService.updateCoachConnectionStatus(id, status);
        return ResponseEntity.ok(new ApiMessage("Connection status updated to " + status));
    }

    // =========================================================================
    // SCOUTS
    // =========================================================================
    @GetMapping("/scouts")
    public ResponseEntity<?> getScouts(Authentication auth) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getScouts());
    }

    @PostMapping("/scouts")
    public ResponseEntity<?> createScout(Authentication auth, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createScout(req));
    }

    @PutMapping("/scouts/{id}")
    public ResponseEntity<?> updateScout(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.updateScout(id, req));
    }

    @DeleteMapping("/scouts/{id}")
    public ResponseEntity<?> deleteScout(Authentication auth, @PathVariable Long id) {
        checkAdmin(auth);
        adminService.deleteScout(id);
        return ResponseEntity.ok(new ApiMessage("Scout deleted successfully"));
    }

    @GetMapping("/scouts/connections")
    public ResponseEntity<?> getScoutConnections(Authentication auth) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getScoutConnections());
    }

    @PatchMapping("/scouts/connections/{id}/status")
    public ResponseEntity<?> updateScoutConnectionStatus(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        String status = (String) req.get("status");
        adminService.updateScoutConnectionStatus(id, status);
        return ResponseEntity.ok(new ApiMessage("Scout connection status updated to " + status));
    }

    // =========================================================================
    // OPPORTUNITIES
    // =========================================================================
    @GetMapping("/opportunities")
    public ResponseEntity<?> getOpportunities(Authentication auth) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getOpportunities());
    }

    @PostMapping("/opportunities")
    public ResponseEntity<?> createOpportunity(Authentication auth, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createOpportunity(req));
    }

    @PutMapping("/opportunities/{id}")
    public ResponseEntity<?> updateOpportunity(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.updateOpportunity(id, req));
    }

    @DeleteMapping("/opportunities/{id}")
    public ResponseEntity<?> deleteOpportunity(Authentication auth, @PathVariable Long id) {
        checkAdmin(auth);
        adminService.deleteOpportunity(id);
        return ResponseEntity.ok(new ApiMessage("Opportunity deleted successfully"));
    }

    @PatchMapping("/opportunities/{id}/status")
    public ResponseEntity<?> toggleOpportunityStatus(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        boolean isActive = Boolean.parseBoolean(req.get("isActive").toString());
        adminService.toggleOpportunityStatus(id, isActive);
        return ResponseEntity.ok(new ApiMessage("Opportunity status updated"));
    }

    // =========================================================================
    // TRIALS
    // =========================================================================
    @GetMapping("/trials")
    public ResponseEntity<?> getTrials(Authentication auth) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getTrials());
    }

    @PostMapping("/trials")
    public ResponseEntity<?> createTrial(Authentication auth, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createTrial(req));
    }

    @PutMapping("/trials/{id}")
    public ResponseEntity<?> updateTrial(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.updateTrial(id, req));
    }

    @DeleteMapping("/trials/{id}")
    public ResponseEntity<?> deleteTrial(Authentication auth, @PathVariable Long id) {
        checkAdmin(auth);
        adminService.deleteTrial(id);
        return ResponseEntity.ok(new ApiMessage("Trial deleted successfully"));
    }

    @GetMapping("/trials/{id}/slots")
    public ResponseEntity<?> getTrialSlots(Authentication auth, @PathVariable Long id) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getTrialSlots(id));
    }

    @PostMapping("/trials/{id}/slots")
    public ResponseEntity<?> addTrialSlot(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.addTrialSlot(id, req));
    }

    @DeleteMapping("/trials/slots/{slotId}")
    public ResponseEntity<?> deleteTrialSlot(Authentication auth, @PathVariable Long slotId) {
        checkAdmin(auth);
        adminService.deleteTrialSlot(slotId);
        return ResponseEntity.ok(new ApiMessage("Trial slot deleted successfully"));
    }

    // =========================================================================
    // SCHOLARSHIPS
    // =========================================================================
    @GetMapping("/scholarships")
    public ResponseEntity<?> getScholarships(Authentication auth) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getScholarships());
    }

    @PostMapping("/scholarships")
    public ResponseEntity<?> createScholarship(Authentication auth, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createScholarship(req));
    }

    @PutMapping("/scholarships/{id}")
    public ResponseEntity<?> updateScholarship(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.updateScholarship(id, req));
    }

    @DeleteMapping("/scholarships/{id}")
    public ResponseEntity<?> deleteScholarship(Authentication auth, @PathVariable Long id) {
        checkAdmin(auth);
        adminService.deleteScholarship(id);
        return ResponseEntity.ok(new ApiMessage("Scholarship deleted successfully"));
    }

    @PatchMapping("/scholarships/{id}/status")
    public ResponseEntity<?> toggleScholarshipStatus(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        boolean isActive = Boolean.parseBoolean(req.get("isActive").toString());
        adminService.toggleScholarshipStatus(id, isActive);
        return ResponseEntity.ok(new ApiMessage("Scholarship status updated"));
    }

    // =========================================================================
    // APPLICATIONS & SCHOLARSHIP APPROVALS
    // =========================================================================
    @GetMapping("/applications")
    public ResponseEntity<?> getApplications(Authentication auth) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getAllApplications());
    }

    @GetMapping("/scholarship-applications")
    public ResponseEntity<?> getScholarshipApplications(Authentication auth) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getScholarshipApplications());
    }

    @RequestMapping(value = "/scholarship-applications/{id}/approve", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<?> approveScholarshipApplication(Authentication auth, @PathVariable Long id, @RequestBody(required=false) Map<String, Object> req) {
        checkAdmin(auth);
        String notes = req != null ? (String) req.get("adminNotes") : null;
        return ResponseEntity.ok(adminService.approveScholarshipApplication(id, notes));
    }

    @RequestMapping(value = "/scholarship-applications/{id}/reject", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<?> rejectScholarshipApplication(Authentication auth, @PathVariable Long id, @RequestBody(required=false) Map<String, Object> req) {
        checkAdmin(auth);
        String notes = req != null ? (String) req.get("adminNotes") : null;
        return ResponseEntity.ok(adminService.rejectScholarshipApplication(id, notes));
    }

    @RequestMapping(value = "/trial-applications/{id}/confirm-booking", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<?> confirmTrialBooking(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        String date = Objects.toString(req.get("confirmedDate"), "");
        String time = Objects.toString(req.get("confirmedTime"), "");
        return ResponseEntity.ok(adminService.confirmTrialBookingByAdmin(id, date, time));
    }

    @PatchMapping("/applications/trials/{id}/status")
    public ResponseEntity<?> updateTrialAppStatus(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        String status = (String) req.get("status");
        adminService.updateTrialApplicationStatus(id, status);
        return ResponseEntity.ok(new ApiMessage("Trial application status updated to " + status));
    }

    @PatchMapping("/applications/opportunities/{id}/status")
    public ResponseEntity<?> updateOppAppStatus(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        String status = (String) req.get("status");
        adminService.updateOpportunityApplicationStatus(id, status);
        return ResponseEntity.ok(new ApiMessage("Opportunity application status updated to " + status));
    }

    // =========================================================================
    // ACHIEVEMENTS
    // =========================================================================
    @GetMapping("/achievements")
    public ResponseEntity<?> getAchievements(Authentication auth) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getAchievements());
    }

    @PatchMapping("/achievements/{id}/verify")
    public ResponseEntity<?> updateAchievementVerification(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        String status = (String) req.get("status");
        String notes = (String) req.get("adminNotes");
        adminService.updateAchievementVerification(id, status, notes);
        return ResponseEntity.ok(new ApiMessage("Achievement verification status updated to " + status));
    }

    // =========================================================================
    // ANNOUNCEMENTS
    // =========================================================================
    @GetMapping("/announcements")
    public ResponseEntity<?> getAnnouncements(Authentication auth) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getAnnouncements());
    }

    @PostMapping("/announcements")
    public ResponseEntity<?> createAnnouncement(Authentication auth, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createAnnouncement(req));
    }

    @PutMapping("/announcements/{id}")
    public ResponseEntity<?> updateAnnouncement(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> req) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.updateAnnouncement(id, req));
    }

    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<?> deleteAnnouncement(Authentication auth, @PathVariable Long id) {
        checkAdmin(auth);
        adminService.deleteAnnouncement(id);
        return ResponseEntity.ok(new ApiMessage("Announcement deleted successfully"));
    }

    // =========================================================================
    // SYSTEM STATUS
    // =========================================================================
    @GetMapping("/system/status")
    public ResponseEntity<?> getSystemStatus(Authentication auth) {
        checkAdmin(auth);
        return ResponseEntity.ok(adminService.getSystemStatus());
    }
}
