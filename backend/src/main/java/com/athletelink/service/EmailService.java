package com.athletelink.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {
    private final JavaMailSender sender;
    private final String host;
    private final String from;

    public EmailService(JavaMailSender sender,
                        @Value("${spring.mail.host:}") String host,
                        @Value("${athletelink.mail.from:${MAIL_FROM:${spring.mail.username:}}}") String from) {
        this.sender = sender;
        this.host = host;
        this.from = from;
    }

    public boolean sendScholarshipSubmissionEmail(String email, String name, String categoryTitle) {
        if (!smtpConfigured()) {
            System.out.println("[SMTP NOT CONFIGURED] Scholarship submitted for " + name + " (" + email + ") - Category: " + categoryTitle);
            return false;
        }

        String subject = "🎓 AthleteLink AI - Scholarship Application Received: " + categoryTitle;
        String plainText = "Hi " + name + ",\n\n" +
                "Your scholarship application for " + categoryTitle + " has been successfully submitted and is awaiting review.\n\n" +
                "Status: SUBMITTED (Under Committee Review)\n\n" +
                "Best regards,\nAthleteLink AI Scholarship Team";

        Map<String, String> details = new LinkedHashMap<>();
        details.put("Applicant Name", name);
        details.put("Scholarship Program", categoryTitle);
        details.put("Status", "SUBMITTED (Awaiting Review)");
        details.put("Verification Stage", "Document & Eligibility Verification");

        List<String> checklist = List.of(
                "Ensure all uploaded age, identity, and income documents are valid.",
                "Keep your registered email and phone number active for updates.",
                "Our verification team will review your application within 2-3 business days."
        );

        String htmlContent = buildHtmlEmail(
                "Scholarship Application Received",
                "SUBMISSION RECEIVED",
                "#0284c7",
                name,
                "Your scholarship application has been successfully received by AthleteLink AI. Our sports scholarship committee is currently reviewing your profile and credentials.",
                details,
                "Open AthleteLink AI Dashboard",
                "http://localhost:8080/#dashboard",
                "Next Steps & Verification Guidelines",
                checklist
        );

        sendMimeEmail(email, subject, plainText, htmlContent);
        return true;
    }

    public boolean sendScholarshipApprovalEmail(String email, String name, String categoryTitle) {
        if (!smtpConfigured()) {
            System.out.println("[SMTP NOT CONFIGURED] Scholarship APPROVED for " + name + " (" + email + ") - Category: " + categoryTitle);
            return false;
        }

        String subject = "🎉 AthleteLink AI - Congratulations! Scholarship Approved: " + categoryTitle;
        String plainText = "Hi " + name + ",\n\n" +
                "Congratulations! Your scholarship application for " + categoryTitle + " has been Approved!\n\n" +
                "Status: APPROVED\n\n" +
                "Best regards,\nAthleteLink AI Scholarship Committee";

        Map<String, String> details = new LinkedHashMap<>();
        details.put("Recipient Name", name);
        details.put("Scholarship Program", categoryTitle);
        details.put("Decision Status", "APPROVED 🎉");
        details.put("Committee Clearance", "Verified & Cleared for Disbursement");

        List<String> checklist = List.of(
                "Our disbursement and sports grant coordinator will contact you directly.",
                "Check your AthleteLink AI Dashboard for official sanction letters.",
                "Maintain your athletic training records and academic reports."
        );

        String htmlContent = buildHtmlEmail(
                "Scholarship Application Approved!",
                "APPROVED 🎉",
                "#10b981",
                name,
                "We are thrilled to inform you that your scholarship application has been officially APPROVED by the AthleteLink AI National Review Committee.",
                details,
                "View Scholarship in Dashboard",
                "http://localhost:8080/#dashboard",
                "Important Information for Approved Athletes",
                checklist
        );

        sendMimeEmail(email, subject, plainText, htmlContent);
        return true;
    }

<<<<<<< HEAD
    public boolean sendCancellationEmail(String email, String name, String trialName, String type) {
        if (!smtpConfigured()) {
            System.out.println("[SMTP NOT CONFIGURED] Cancellation notification for " + name + " (" + email + ") - " + type + ": " + trialName);
            return false;
        }
        String subject = "AthleteLink AI - " + type + " Cancelled: " + trialName;
        String plainText = "Hi " + name + ",\n\nYour " + type.toLowerCase() + " for " + trialName + " has been cancelled.\n\nBest regards,\nAthleteLink AI Team";
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Athlete Name", name);
        details.put("Trial Event", trialName);
        details.put("Action", type + " Cancelled");
        details.put("Status", "CANCELLED");
        List<String> checklist = List.of(
                "The freed slot may be taken by another athlete.",
                "Visit AthleteLink AI to browse and apply for other available trials.",
                "Contact our support team if this cancellation was made in error."
        );
        String htmlContent = buildHtmlEmail(
                type + " Cancelled", "CANCELLED", "#ef4444", name,
                "Your " + type.toLowerCase() + " for <strong>" + escapeHtml(trialName) + "</strong> has been successfully cancelled. The slot has been freed for other athletes.",
                details, "Browse Open Trials", "http://localhost:8080/#trials",
                "What Happens Next", checklist);
        sendMimeEmail(email, subject, plainText, htmlContent);
        return true;
    }

=======
>>>>>>> 1bdcedfdb8d69998a11cf16735225a5f5770ab40
    public boolean sendTrialAdminBookingEmail(String email, String name, String trialName, String venue, String confirmedDate, String confirmedTime) {
        if (!smtpConfigured()) {
            System.out.println("[SMTP NOT CONFIGURED] Trial booked by Admin for " + name + " (" + email + ") - Trial: " + trialName + " on " + confirmedDate + " at " + confirmedTime);
            return false;
        }

        String subject = "🏆 AthleteLink AI - Official Trial Slot Confirmed: " + trialName;
        String plainText = "Hi " + name + ",\n\n" +
                "Your trial slot for " + trialName + " has been confirmed by the Admin team.\n\n" +
                "Venue: " + venue + "\n" +
                "Confirmed Date: " + confirmedDate + "\n" +
                "Confirmed Time: " + confirmedTime + "\n\n" +
                "Best regards,\nAthleteLink AI Trials Team";

        Map<String, String> details = new LinkedHashMap<>();
        details.put("Athlete Name", name);
        details.put("Trial Event", trialName);
        details.put("Venue Location", venue);
        details.put("Confirmed Date", confirmedDate);
        details.put("Confirmed Time", confirmedTime);
        details.put("Booking Status", "CONFIRMED BY ADMIN");

        List<String> checklist = List.of(
                "Carry a valid Government / School Photo ID card and Age Proof.",
                "Wear proper athletic apparel, turf/court shoes, and bring protective sports gear.",
                "Report to the venue registration desk at least 30 minutes prior to your time slot.",
                "Stay well hydrated and warm up before the trial session begins."
        );

        String htmlContent = buildHtmlEmail(
                "Trial Slot Confirmed by Admin",
                "SLOT CONFIRMED",
                "#10b981",
                name,
                "Your trial appointment has been confirmed by the official trial coordinator. Please review your slot details below and prepare for the evaluation.",
                details,
                "Open AthleteLink AI Dashboard",
                "http://localhost:8080/#dashboard",
                "Trial Day Attendance Guidelines",
                checklist
        );

        sendMimeEmail(email, subject, plainText, htmlContent);
        return true;
    }

    public boolean sendTrialApplicationConfirmation(String email, String name, Map<String, Object> trial, long applicationId, String link) {
        if (!smtpConfigured()) {
            System.out.println("[SMTP NOT CONFIGURED] Trial application confirmation URL: " + link);
            return false;
        }

        String trialName = Objects.toString(trial.get("name"), "Trial Event");
        String sport = Objects.toString(trial.get("sport"), "Sports");
        String category = Objects.toString(trial.get("category"), "-");
        String ageCategory = Objects.toString(trial.get("age_category"), "-");
        String venue = Objects.toString(trial.get("venue"), "Venue TBA");
        String trialDate = Objects.toString(trial.get("trial_date"), "Date TBA");

        String subject = "⚡ AthleteLink AI - Trial Application Received: " + trialName + " (#" + applicationId + ")";
        String plainText = "Hi " + name + ",\n\n" +
                "Your AthleteLink AI trial application has been received.\n\n" +
                "Trial: " + trialName + "\n" +
                "Sport: " + sport + "\n" +
                "Category: " + category + " (" + ageCategory + ")\n" +
                "Venue: " + venue + "\n" +
                "Trial Date: " + trialDate + "\n" +
                "Application ID: #" + applicationId + "\n" +
                "Confirmation Link: " + link + "\n\n" +
                "Best regards,\nAthleteLink AI Team";

        Map<String, String> details = new LinkedHashMap<>();
        details.put("Application ID", "#" + applicationId);
        details.put("Trial Event", trialName);
        details.put("Sport", sport);
        details.put("Category", category + " (" + ageCategory + ")");
        details.put("Venue", venue);
        details.put("Trial Date", trialDate);
        details.put("Application Status", "SUBMITTED & VERIFIED");

        List<String> checklist = List.of(
                "Click the verification button below to view and verify your application pass.",
                "You can now book an available time slot for this trial on AthleteLink AI.",
                "Carry your official identification and sports gear when attending."
        );

        String htmlContent = buildHtmlEmail(
                "Trial Application Received",
                "APPLICATION SUBMITTED",
                "#0284c7",
                name,
                "Thank you for applying! Your application for <strong>" + escapeHtml(trialName) + "</strong> has been registered in the AthleteLink AI Talent Identification network.",
                details,
                "⚡ Confirm & View Application Pass",
                link,
                "Trial Instructions & Next Step",
                checklist
        );

        sendMimeEmail(email, subject, plainText, htmlContent);
        return true;
    }

    public boolean sendBookingConfirmation(String email, String name, Map<String, Object> slot, long bookingId, String link) {
        if (!smtpConfigured()) {
            System.out.println("[SMTP NOT CONFIGURED] Booking confirmation URL: " + link);
            return false;
        }

        String trialName = Objects.toString(slot.get("trial_name"), "Trial Event");
        String sport = Objects.toString(slot.get("sport"), "Sports");
        String venue = Objects.toString(slot.get("venue"), "Venue TBA");
        String slotDate = Objects.toString(slot.get("slot_date"), "Date TBA");
        String slotTime = Objects.toString(slot.get("slot_time"), "Time TBA");

        String subject = "🎟️ AthleteLink AI - Trial Booking Confirmed: " + trialName + " (#" + bookingId + ")";
        String plainText = "Hi " + name + ",\n\n" +
                "Your trial slot booking is confirmed!\n\n" +
                "Trial: " + trialName + "\n" +
                "Sport: " + sport + "\n" +
                "Venue: " + venue + "\n" +
                "Date: " + slotDate + "\n" +
                "Time: " + slotTime + "\n" +
                "Booking ID: #" + bookingId + "\n" +
                "Confirmation Link: " + link + "\n\n" +
                "Best regards,\nAthleteLink AI Team";

        Map<String, String> details = new LinkedHashMap<>();
        details.put("Booking ID", "#" + bookingId);
        details.put("Trial Event", trialName);
        details.put("Sport", sport);
        details.put("Venue Location", venue);
        details.put("Confirmed Date", slotDate);
        details.put("Confirmed Time", slotTime);
        details.put("Booking Status", "CONFIRMED & ACTIVE");

        List<String> checklist = List.of(
                "Present your confirmation pass on your phone or carry a printed copy.",
                "Carry original photo ID (Aadhaar, Passport, or Student ID).",
                "Arrive at the venue registration desk at least 30 minutes prior to your slot.",
                "Bring your personal playing kit, shoes, and required protective gear."
        );

        String htmlContent = buildHtmlEmail(
                "Trial Slot Booking Confirmed!",
                "BOOKING CONFIRMED",
                "#10b981",
                name,
                "Your slot for <strong>" + escapeHtml(trialName) + "</strong> is officially locked in! Your trial pass details are shown below.",
                details,
                "🎟️ View & Verify Booking Pass",
                link,
                "Trial Day Checklist & Guidelines",
                checklist
        );

        sendMimeEmail(email, subject, plainText, htmlContent);
        return true;
    }

    private void sendMimeEmail(String toEmail, String subject, String plainText, String htmlContent) {
        CompletableFuture.runAsync(() -> {
            try {
                MimeMessage mimeMessage = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                String fromAddress = (from != null && !from.isBlank()) ? from : "sreevaib@gmail.com";
                helper.setFrom(fromAddress, "AthleteLink AI");
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(plainText, htmlContent);

                sender.send(mimeMessage);
                System.out.println("[SMTP SUCCESS] Rich HTML email delivered to " + toEmail + " | Subject: " + subject);
            } catch (Exception e) {
                System.err.println("[SMTP ERROR] Failed sending email to " + toEmail + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private String buildHtmlEmail(
            String title,
            String badgeText,
            String badgeColor,
            String recipientName,
            String messageBody,
            Map<String, String> details,
            String ctaText,
            String ctaUrl,
            String checklistTitle,
            List<String> checklistItems) {

        StringBuilder detailsRows = new StringBuilder();
        if (details != null) {
            for (Map.Entry<String, String> entry : details.entrySet()) {
                detailsRows.append("<tr>")
                        .append("<td style=\"padding: 10px 14px; border-bottom: 1px solid #1e293b; color: #94a3b8; font-size: 13px; font-weight: 500; width: 40%;\">")
                        .append(escapeHtml(entry.getKey()))
                        .append("</td>")
                        .append("<td style=\"padding: 10px 14px; border-bottom: 1px solid #1e293b; color: #f8fafc; font-size: 13px; font-weight: 700; text-align: right;\">")
                        .append(escapeHtml(entry.getValue()))
                        .append("</td>")
                        .append("</tr>");
            }
        }

        StringBuilder checklistHtml = new StringBuilder();
        if (checklistItems != null && !checklistItems.isEmpty()) {
            checklistHtml.append("<div style=\"background-color: #0d1527; border-left: 4px solid #00f2fe; border-radius: 0 8px 8px 0; padding: 14px 16px; margin: 24px 0;\">");
            checklistHtml.append("<div style=\"color: #38bdf8; font-size: 12px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 10px;\">")
                    .append("📋 ").append(escapeHtml(checklistTitle)).append("</div>");
            checklistHtml.append("<ul style=\"margin: 0; padding-left: 18px; color: #cbd5e1; font-size: 13px; line-height: 1.6;\">");
            for (String item : checklistItems) {
                checklistHtml.append("<li style=\"margin-bottom: 6px;\">").append(escapeHtml(item)).append("</li>");
            }
            checklistHtml.append("</ul></div>");
        }

        StringBuilder ctaHtml = new StringBuilder();
        if (ctaUrl != null && !ctaUrl.isBlank() && ctaText != null && !ctaText.isBlank()) {
            ctaHtml.append("<div style=\"text-align: center; margin: 28px 0 16px 0;\">")
                    .append("<a href=\"").append(escapeHtml(ctaUrl)).append("\" target=\"_blank\" style=\"display: inline-block; background: linear-gradient(135deg, #00f2fe 0%, #0072ff 100%); color: #020617; font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; font-size: 14px; font-weight: 800; text-decoration: none; padding: 14px 30px; border-radius: 8px; text-transform: uppercase; letter-spacing: 0.5px; box-shadow: 0 4px 15px rgba(0, 242, 254, 0.4);\">")
                    .append(escapeHtml(ctaText))
                    .append("</a>")
                    .append("</div>")
                    .append("<p style=\"text-align: center; font-size: 11px; color: #64748b; margin: 0 0 20px 0;\">If the button above does not work, copy and paste this link in your browser:<br/>")
                    .append("<a href=\"").append(escapeHtml(ctaUrl)).append("\" style=\"color: #38bdf8; word-break: break-all;\">").append(escapeHtml(ctaUrl)).append("</a></p>");
        }

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>" + escapeHtml(title) + "</title>" +
                "</head>" +
                "<body style=\"margin: 0; padding: 24px 12px; background-color: #060911; font-family: 'Segoe UI', Roboto, -apple-system, BlinkMacSystemFont, Arial, sans-serif; color: #f3f4f6;\">" +
                "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width: 600px; margin: 0 auto; background-color: #0d1424; border: 1px solid #1e293b; border-radius: 12px; overflow: hidden; box-shadow: 0 15px 35px rgba(0, 0, 0, 0.6);\">" +
                // Top Glowing Accent Bar
                "<tr><td style=\"height: 4px; background: linear-gradient(90deg, #00f2fe 0%, #4facfe 50%, #10b981 100%);\"></td></tr>" +
                // Header Brand
                "<tr>" +
                "<td style=\"padding: 24px 28px 16px 28px; text-align: center; background-color: #0f182c; border-bottom: 1px solid #1e293b;\">" +
                "<div style=\"font-size: 20px; font-weight: 900; letter-spacing: 1.5px; color: #ffffff;\">" +
                "<span style=\"color: #00f2fe;\">⚡ ATHLETE</span><span style=\"color: #ffffff;\">LINK</span> <span style=\"background: linear-gradient(90deg, #00f2fe, #4facfe); -webkit-background-clip: text; color: #00f2fe;\">AI</span>" +
                "</div>" +
                "<div style=\"font-size: 10px; color: #94a3b8; font-weight: 700; text-transform: uppercase; letter-spacing: 2px; margin-top: 4px;\">National Sports Scouting & Talent Network</div>" +
                "</td>" +
                "</tr>" +
                // Main Content
                "<tr>" +
                "<td style=\"padding: 28px 28px 20px 28px;\">" +
                // Status Badge
                "<div style=\"text-align: center; margin-bottom: 16px;\">" +
                "<span style=\"display: inline-block; background-color: " + badgeColor + "22; color: " + badgeColor + "; border: 1px solid " + badgeColor + "; padding: 5px 14px; border-radius: 20px; font-size: 11px; font-weight: 800; letter-spacing: 1px; text-transform: uppercase;\">" +
                "● " + escapeHtml(badgeText) +
                "</span>" +
                "</div>" +
                // Heading
                "<h1 style=\"color: #ffffff; font-size: 20px; font-weight: 800; text-align: center; margin: 0 0 16px 0;\">" + escapeHtml(title) + "</h1>" +
                // Greeting & Message Body
                "<p style=\"font-size: 14px; color: #cbd5e1; line-height: 1.6; margin: 0 0 12px 0;\">" +
                "Hi <strong style=\"color: #ffffff;\">" + escapeHtml(recipientName) + "</strong>," +
                "</p>" +
                "<p style=\"font-size: 14px; color: #94a3b8; line-height: 1.6; margin: 0 0 20px 0;\">" +
                messageBody +
                "</p>" +
                // Details Card
                "<div style=\"margin: 20px 0;\">" +
                "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #131c30; border: 1px solid #1e293b; border-radius: 8px; border-collapse: collapse;\">" +
                detailsRows.toString() +
                "</table>" +
                "</div>" +
                // CTA Button & Link Fallback
                ctaHtml.toString() +
                // Checklist
                checklistHtml.toString() +
                "</td>" +
                "</tr>" +
                // Footer
                "<tr>" +
                "<td style=\"padding: 20px 28px 24px 28px; background-color: #0a0f1d; border-top: 1px solid #1e293b; text-align: center; font-size: 11px; color: #64748b; line-height: 1.6;\">" +
                "<p style=\"margin: 0 0 4px 0; color: #94a3b8; font-weight: 600;\">AthleteLink AI Platform • Official Notification</p>" +
                "<p style=\"margin: 0 0 6px 0;\">You received this email because an action was performed on the AthleteLink AI platform for your profile.</p>" +
                "<p style=\"margin: 0; font-size: 10px; color: #475569;\">© 2026 AthleteLink AI. All rights reserved.</p>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</body>" +
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

    public boolean smtpConfigured() {
        return host != null && !host.isBlank();
    }
}


