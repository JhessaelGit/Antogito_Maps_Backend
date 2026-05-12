package com.antojito.maps_backend.service;

import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.antojito.maps_backend.model.Complaint;
import com.antojito.maps_backend.model.ComplaintStatus;
import com.antojito.maps_backend.model.Restaurante;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@antojitos.maps}")
    private String fromAddress;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.testmail.namespace:}")
    private String testmailNamespace;

    @Value("${app.testmail.tag:complaints}")
    private String testmailTag;

    /**
     * Envía notificación de resultado de queja al cliente.
     * Se ejecuta de forma asíncrona para no bloquear la respuesta del admin.
     */
    @Async
    public void sendComplaintResolutionEmail(
            String clientEmail,
            String clientName,
            Complaint complaint,
            Restaurante restaurante) {

        if (!mailEnabled || clientEmail == null || clientEmail.isBlank()) {
            log.info("[EmailService] Mail deshabilitado o email de cliente no disponible — omitiendo envío.");
            return;
        }

        try {
            String subject = buildSubject(complaint.getStatus());
            String body    = buildHtmlBody(clientName, complaint, restaurante);

            sendHtmlEmail(clientEmail, subject, body);

            // Copia de auditoría al inbox de testmail.app si está configurado
            if (!testmailNamespace.isBlank()) {
                String tag = (testmailTag != null && !testmailTag.isBlank()) ? testmailTag : "complaints";
                String auditEmail = testmailNamespace + "." + tag + "@inbox.testmail.app";
                sendHtmlEmail(auditEmail, "[AUDIT] " + subject, body);
                log.info("[EmailService] Copia de auditoría enviada a {}", auditEmail);
            }

        } catch (Exception e) {
            log.error("[EmailService] Error al enviar email de resolución de queja: {}", e.getMessage(), e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) throws Exception {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(msg);
        log.info("[EmailService] Email enviado a {}: {}", to, subject);
    }

    private String buildSubject(ComplaintStatus status) {
        return status == ComplaintStatus.ACCEPTED
                ? "✅ Tu reporte fue aceptado — Antojitos Maps"
                : "❌ Tu reporte fue revisado — Antojitos Maps";
    }

    private String buildHtmlBody(String clientName, Complaint complaint, Restaurante restaurante) {
        String statusText = complaint.getStatus() == ComplaintStatus.ACCEPTED
                ? "fue <strong style='color:#2da854'>aceptado</strong>. El restaurante ha sido <strong>vetado temporalmente</strong> del mapa."
                : "fue <strong style='color:#c43232'>rechazado</strong>. No encontramos una violación suficiente para tomar acción.";

        String targetName = (restaurante != null) ? restaurante.getName() : complaint.getTargetUuid().toString();
        String fechaStr   = (complaint.getCreatedAt() != null)
                ? complaint.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "N/A";

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"><title>Resultado de tu reporte</title></head>
            <body style="margin:0;padding:0;background:#f5f2ee;font-family:'Helvetica Neue',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f2ee;padding:40px 0;">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0"
                         style="background:#02332d;border-radius:16px;overflow:hidden;box-shadow:0 8px 32px rgba(0,0,0,0.2);">
                    <!-- Header -->
                    <tr>
                      <td style="padding:32px 40px 24px;border-bottom:1px solid rgba(191,152,97,0.2);">
                        <h1 style="margin:0;font-size:22px;font-weight:900;color:#dacfbd;letter-spacing:-0.5px;">
                          Antojitos<span style="color:#bf9861;">Maps</span>
                        </h1>
                        <p style="margin:8px 0 0;font-size:12px;color:rgba(218,207,189,0.4);text-transform:uppercase;letter-spacing:1.2px;">
                          Resultado de tu reporte
                        </p>
                      </td>
                    </tr>
                    <!-- Body -->
                    <tr>
                      <td style="padding:32px 40px;">
                        <p style="margin:0 0 16px;font-size:16px;color:#dacfbd;">
                          Hola, <strong>%s</strong>.
                        </p>
                        <p style="margin:0 0 24px;font-size:14px;color:rgba(218,207,189,0.7);line-height:1.7;">
                          Tu reporte sobre el restaurante <strong style="color:#bf9861;">%s</strong> %s
                        </p>
                        <!-- Info card -->
                        <table width="100%%" cellpadding="0" cellspacing="0"
                               style="background:rgba(255,255,255,0.05);border:1px solid rgba(191,152,97,0.15);border-radius:12px;margin-bottom:24px;">
                          <tr>
                            <td style="padding:20px 24px;">
                              <p style="margin:0 0 8px;font-size:10px;color:rgba(218,207,189,0.35);text-transform:uppercase;letter-spacing:0.8px;">
                                Tu reporte
                              </p>
                              <p style="margin:0 0 16px;font-size:13px;color:rgba(218,207,189,0.6);line-height:1.6;">
                                "%s"
                              </p>
                              <p style="margin:0;font-size:11px;color:rgba(218,207,189,0.3);">
                                Enviado el %s
                              </p>
                            </td>
                          </tr>
                        </table>
                        <p style="margin:0;font-size:13px;color:rgba(218,207,189,0.45);line-height:1.6;">
                          Gracias por ayudarnos a mantener Antojitos Maps libre de contenido inapropiado.
                        </p>
                      </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                      <td style="padding:20px 40px;border-top:1px solid rgba(191,152,97,0.1);text-align:center;">
                        <p style="margin:0;font-size:11px;color:rgba(218,207,189,0.25);">
                          © Antojitos Maps · Este es un mensaje automático, no respondas este correo.
                        </p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(
                clientName != null ? clientName : "Cliente",
                targetName,
                statusText,
                complaint.getDescription(),
                fechaStr
        );
    }
}
