package com.techwave.auth.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            
            // Si le texte commence par <!DOCTYPE html, on l'envoie comme HTML
            boolean isHtml = text.trim().startsWith("<!DOCTYPE html") || text.trim().startsWith("<html");
            helper.setText(text, isHtml);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Erreur envoi email: " + e.getMessage());
        }
    }

    public void sendActivationEmail(String to, String name, String code) {
        String html = buildOtpEmail(name, code);
        sendEmail(to, "🚀 Code d'activation de votre compte", html);
    }

    public void sendResetPasswordEmail(String to, String name, String link) {
        String html = buildAuthEmail(name, "Réinitialisation de mot de passe", 
            "Vous avez demandé la réinitialisation de votre mot de passe. Cliquez sur le bouton ci-dessous pour choisir un nouveau mot de passe.",
            link, "Réinitialiser mon mot de passe");
        sendEmail(to, "🔐 Réinitialisation de votre mot de passe", html);
    }

    private String buildAuthEmail(String name, String title, String description, String link, String buttonText) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>" +
                "body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f3f4f6; margin: 0; padding: 0; }" +
                ".wrapper { padding: 20px; }" +
                ".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }" +
                ".header { background: linear-gradient(135deg, #1e293b 0%, #334155 100%); padding: 40px 20px; text-align: center; color: #ffffff; }" +
                ".content { padding: 40px 30px; text-align: center; }" +
                ".footer { background-color: #f8fafc; padding: 20px; text-align: center; color: #94a3b8; font-size: 12px; }" +
                ".button { display: inline-block; background-color: #4f46e5; color: #ffffff; padding: 16px 32px; border-radius: 8px; text-decoration: none; font-weight: 600; margin: 30px 0; }" +
                "h2 { color: #0f172a; margin-top: 0; }" +
                "p { color: #4b5563; line-height: 1.6; font-size: 16px; }" +
                "</style></head><body><div class='wrapper'><div class='container'>" +
                "<div class='header'><h1>SPM - Club GI ENSPY</h1></div>" +
                "<div class='content'>" +
                "<p style='font-style: italic; color: #64748b; margin-bottom: 30px;'>" +
                "Le projet SPM (Solution de Gestion de Projets Modulaire) est une initiative open source de la Cellule Projet du Club Génie Informatique de l’ENSPY. " +
                "Il vise à fournir une alternative gratuite et sans limitation d’utilisateurs aux outils propriétaires (Jira, Trello), " +
                "exploitant les compétences locales pour créer une plateforme scalable." +
                "</p>" +
                "<h2>" + title + "</h2>" +
                "<p>Bonjour " + name + ",</p>" +
                "<p>" + description + "</p>" +
                "<a href='" + link + "' class='button'>" + buttonText + "</a>" +
                "<p style='font-size: 14px; color: #94a3b8;'>Si le bouton ne fonctionne pas, copiez et collez ce lien :<br>" + link + "</p>" +
                "</div><div class='footer'><p>© 2026 Club Génie Informatique ENSPY. Tous droits réservés.</p></div>" +
                "</div></div></body></html>";
    }

    private String buildOtpEmail(String name, String code) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>" +
                "body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f3f4f6; margin: 0; padding: 0; }" +
                ".wrapper { padding: 20px; }" +
                ".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }" +
                ".header { background: linear-gradient(135deg, #1e293b 0%, #334155 100%); padding: 40px 20px; text-align: center; color: #ffffff; }" +
                ".content { padding: 40px 30px; text-align: center; }" +
                ".footer { background-color: #f8fafc; padding: 20px; text-align: center; color: #94a3b8; font-size: 12px; }" +
                ".code-box { display: inline-block; background-color: #f1f5f9; border: 2px dashed #cbd5e1; border-radius: 8px; padding: 16px 32px; font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #0f172a; margin: 30px 0; }" +
                "h2 { color: #0f172a; margin-top: 0; }" +
                "p { color: #4b5563; line-height: 1.6; font-size: 16px; }" +
                "</style></head><body><div class='wrapper'><div class='container'>" +
                "<div class='header'><h1>SPM - Club GI ENSPY</h1></div>" +
                "<div class='content'>" +
                "<div style='text-align: left; background-color: #f8fafc; padding: 20px; border-radius: 8px; margin-bottom: 30px; border-left: 4px solid #4f46e5;'>" +
                "<p style='margin: 0; font-size: 14px; color: #475569;'>" +
                "<strong>À propos du projet :</strong> Le projet SPM (Solution de Gestion de Projets Modulaire) est une initiative open source de la Cellule Projet du Club Génie Informatique de l’ENSPY. " +
                "Il vise à fournir une alternative gratuite aux outils propriétaires (Jira, Trello), tout en formant la nouvelle génération de développeurs full-stack." +
                "</p>" +
                "</div>" +
                "<h2>Vérification de votre compte</h2>" +
                "<p>Bonjour " + name + ",</p>" +
                "<p>Bienvenue ! Pour activer votre compte et commencer à utiliser nos services, veuillez saisir le code de vérification suivant :</p>" +
                "<div class='code-box'>" + code + "</div>" +
                "<p style='font-size: 14px; color: #94a3b8;'>Ce code est valide pendant 1 heure. Ne le partagez avec personne.</p>" +
                "</div><div class='footer'><p>© 2026 Club Génie Informatique ENSPY. Tous droits réservés.</p></div>" +
                "</div></div></body></html>";
    }

    public void sendProjectInvitationEmail(String to, String inviterName, String projectName, String role, String link) {
        String html = buildAuthEmail(
                to,
                "Invitation au projet " + projectName,
                inviterName + " vous invite à rejoindre le projet <strong>" + projectName + "</strong> " +
                        "avec le rôle <strong>" + role + "</strong>. " +
                        "Cliquez sur le bouton ci-dessous pour accepter l'invitation. " +
                        "Ce lien est valable 7 jours.",
                link,
                "Accepter l'invitation"
        );
        sendEmail(to, "📩 Invitation au projet " + projectName, html);
    }

    /**
     * Email de rappel : une tâche assignée approche de son échéance.
     */
    public void sendTaskReminderEmail(String to, String name, String taskTitle, String taskKey,
                                      String dueLabel, String link) {
        String description =
                "La tâche <strong>" + taskKey + " — " + taskTitle + "</strong> arrive à échéance " + dueLabel + ". " +
                "Pensez à la mettre à jour ou à la terminer à temps.";
        String html = buildAuthEmail(name, "Rappel d'échéance", description, link, "Ouvrir la tâche");
        sendEmail(to, "⏰ Rappel : « " + taskTitle + " » arrive à échéance", html);
    }

    /**
     * Email de confirmation de suppression de compte (RGPD).
     */
    public void sendGdprDeletionConfirmation(String to) {
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>" +
                "body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f3f4f6; margin: 0; padding: 0; }" +
                ".wrapper { padding: 20px; }" +
                ".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }" +
                ".header { background: linear-gradient(135deg, #1e293b 0%, #334155 100%); padding: 40px 20px; text-align: center; color: #ffffff; }" +
                ".content { padding: 40px 30px; text-align: center; }" +
                ".footer { background-color: #f8fafc; padding: 20px; text-align: center; color: #94a3b8; font-size: 12px; }" +
                "h2 { color: #0f172a; margin-top: 0; }" +
                "p { color: #4b5563; line-height: 1.6; font-size: 16px; }" +
                "</style></head><body><div class='wrapper'><div class='container'>" +
                "<div class='header'><h1>SPM - Club GI ENSPY</h1></div>" +
                "<div class='content'>" +
                "<h2>Suppression de votre compte</h2>" +
                "<p>Votre compte SPM a été supprimé conformément à votre demande ou suite à une décision administrative.</p>" +
                "<p>Toutes vos données personnelles ont été anonymisées conformément au RGPD (Règlement Général sur la Protection des Données).</p>" +
                "<p style='font-size: 14px; color: #94a3b8;'>Si vous n'êtes pas à l'origine de cette action, veuillez contacter l'administrateur.</p>" +
                "</div><div class='footer'><p>© 2026 Club Génie Informatique ENSPY. Tous droits réservés.</p></div>" +
                "</div></div></body></html>";
        sendEmail(to, "🔒 Confirmation de suppression de votre compte SPM", html);
    }
}

