package mail;

import model.MailRequest;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class MailService {

    private static final String SYSTEM_EMAIL    = "mangoisme03@gmail.com";
    private static final String SYSTEM_PASSWORD = "ecejffzypkzvymwn";

    // Scheduler dùng cho hẹn giờ gửi
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(4);

    // ================= SEND MAIL =================
    public static boolean sendMail(String to,
                                   String subject,
                                   String content,
                                   List<String> attachments) {
        try {
            if (to == null || to.trim().isEmpty()) {
                System.out.println("❌ Email rỗng → bỏ qua");
                return false;
            }

            Properties props = new Properties();
            props.put("mail.smtp.host",            "smtp.gmail.com");
            props.put("mail.smtp.port",            "587");
            props.put("mail.smtp.auth",            "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.trust",       "*");

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SYSTEM_EMAIL, SYSTEM_PASSWORD);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SYSTEM_EMAIL));

            InternetAddress[] addresses = InternetAddress.parse(to, true);
            if (addresses.length == 0) {
                System.out.println("❌ Email không hợp lệ: " + to);
                return false;
            }

            message.setRecipients(Message.RecipientType.TO, addresses);
            message.setSubject(subject);

            Multipart multipart = new MimeMultipart();

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(
                    "<h3>" + subject + "</h3><p>" + content + "</p>",
                    "text/html; charset=utf-8"
            );
            multipart.addBodyPart(textPart);

            if (attachments != null) {
                for (String path : attachments) {
                    if (path != null && !path.isEmpty()) {
                        File file = new File(path);
                        if (file.exists()) {
                            MimeBodyPart ap = new MimeBodyPart();
                            ap.attachFile(file);
                            multipart.addBodyPart(ap);
                        } else {
                            System.out.println("⚠ File không tồn tại: " + path);
                        }
                    }
                }
            }

            message.setContent(multipart);
            Transport.send(message);
            return true;

        } catch (SendFailedException e) {
            System.out.println("❌ Sai địa chỉ: " + to);
        } catch (Exception e) {
            System.out.println("❌ Lỗi gửi: " + to + " | " + e.getMessage());
        }
        return false;
    }

    // ================= GỬI CÓ HẸN GIỜ =================
    public static void sendMailWithDelay(String to,
                                         String subject,
                                         String content,
                                         List<String> attachments,
                                         long delaySeconds) {
        System.out.printf("[Scheduler] Mail đến %s sẽ gửi sau %d giây%n", to, delaySeconds);

        SCHEDULER.schedule(() -> {
            boolean ok = sendMail(to, subject, content, attachments);
            if (ok) System.out.println("[Scheduler] ✅ Đã gửi: " + to);
            else    System.out.println("[Scheduler] ❌ Thất bại: " + to);
        }, delaySeconds, TimeUnit.SECONDS);
    }

    // ================= SEND MULTIPLE =================
    public boolean sendMultipleEmails(MailRequest req) {

        List<String> emails = new ArrayList<>();

        // lấy danh sách email
        if (req.getExcelFile() != null && !req.getExcelFile().isEmpty()) {
            emails = readEmails(req.getExcelFile());
        } else {
            if (req.getTo() != null) {
                // Hỗ trợ chia tách nhiều email bằng dấu phẩy
                String[] splitEmails = req.getTo().split(",");
                for (String e : splitEmails) {
                    if (!e.trim().isEmpty()) emails.add(e.trim());
                }
            }
        }

        System.out.println("📨 Total emails: " + emails.size());

        if (emails.isEmpty()) {
            System.out.println("❌ Không có email để gửi");
            return false;
        }

        long delay = req.getDelaySeconds();
        boolean atLeastOneSuccess = false;

        if (delay > 0) {
            // Chế độ hẹn giờ — đặt lịch, không chờ
            String label = req.getScheduledTime() != null
                    ? "lúc " + req.getScheduledTime()
                    : "sau " + delay + " giây";
            System.out.println("[Scheduler] Sẽ gửi " + emails.size() + " email " + label);

            for (int i = 0; i < emails.size(); i++) {
                final String email  = emails.get(i);
                final long   offset = delay + (i * 3L); // mỗi mail cách nhau 3s để tránh bị block
                sendMailWithDelay(email, req.getSubject(), req.getContent(),
                        req.getAttachmentPaths(), offset);
            }
            // Trả true ngay vì đã lên lịch thành công (Server không bị treo)
            return true;

        } else {
            // --- ĐÃ CẬP NHẬT: Gửi song song đa luồng (Multi-threading) ---
            System.out.println("[Executor] Đang gửi ngay lập tức (Chạy song song)...");

            List<Future<Boolean>> futures = new ArrayList<>();
            // Tạo ThreadPool tối đa 5 luồng chạy cùng lúc
            ExecutorService pool = Executors.newFixedThreadPool(Math.min(emails.size(), 5));

            for (String email : emails) {
                if (email == null || email.trim().isEmpty()) {
                    System.out.println("❌ Skip email rỗng");
                    continue;
                }

                final String finalEmail = email;

                // Submit task vào pool
                futures.add(pool.submit(() -> {
                    boolean ok = sendMail(
                            finalEmail,
                            req.getSubject(),
                            req.getContent(),
                            req.getAttachmentPaths()
                    );
                    if (ok) System.out.println("✅ Sent to: " + finalEmail);
                    else    System.out.println("❌ Failed: " + finalEmail);
                    return ok;
                }));
            }

            // Đóng pool và chờ các task hoàn thành (Tối đa 60 giây)
            pool.shutdown();
            try {
                pool.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}

            // Kiểm tra kết quả: thành công nếu ít nhất 1 mail gửi được
            for (Future<Boolean> f : futures) {
                try {
                    if (f.get()) {
                        atLeastOneSuccess = true;
                        break;
                    }
                } catch (Exception ignored) {}
            }

            return atLeastOneSuccess;
            // -------------------------------------------------------------
        }
    }

    // ================= READ EXCEL =================
    public List<String> readEmails(String filePath) {
        List<String> emails = new ArrayList<>();
        try {
            FileInputStream fis = new FileInputStream(filePath);
            Workbook workbook   = new XSSFWorkbook(fis);
            Sheet sheet         = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                Cell cell = row.getCell(0);
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    String email = cell.getStringCellValue().trim();
                    if (!email.isEmpty()) emails.add(email);
                }
            }
            workbook.close();
        } catch (Exception e) {
            System.out.println("❌ Lỗi đọc Excel: " + e.getMessage());
        }
        return emails;
    }
}