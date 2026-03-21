package mail;

import model.MailRequest;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class MailService {

    private static final String SYSTEM_EMAIL = "mangoisme03@gmail.com";
    private static final String SYSTEM_PASSWORD = "ecejffzypkzvymwn";

    // ================= SEND MAIL =================
    public static boolean sendMail(String to,
                                   String subject,
                                   String content,
                                   List<String> attachments) {

        try {
            // ❌ email rỗng
            if (to == null || to.trim().isEmpty()) {
                System.out.println("❌ Email rỗng → bỏ qua");
                return false;
            }

            Properties props = new Properties();

            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.trust", "*");

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SYSTEM_EMAIL, SYSTEM_PASSWORD);
                }
            });

            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(SYSTEM_EMAIL));

            // ✅ validate email chuẩn
            InternetAddress[] addresses = InternetAddress.parse(to, true);
            if (addresses.length == 0) {
                System.out.println("❌ Email không hợp lệ: " + to);
                return false;
            }

            message.setRecipients(Message.RecipientType.TO, addresses);
            message.setSubject(subject);

            Multipart multipart = new MimeMultipart();

            // nội dung mail
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(
                    "<h3>" + subject + "</h3><p>" + content + "</p>",
                    "text/html; charset=utf-8"
            );
            multipart.addBodyPart(textPart);

            // attachment
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

            // ✅ chỉ success khi send OK
            Transport.send(message);

            return true;

        } catch (SendFailedException e) {
            System.out.println("❌ Sai địa chỉ: " + to);
        } catch (Exception e) {
            System.out.println("❌ Lỗi gửi: " + to + " | " + e.getMessage());
        }

        return false;
    }

    // ================= READ EXCEL =================
    public List<String> readEmails(String filePath) {

        List<String> emails = new ArrayList<>();

        try {
            FileInputStream fis = new FileInputStream(filePath);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {

                if (row.getRowNum() == 0) continue;

                Cell cell = row.getCell(0);

                if (cell != null && cell.getCellType() == CellType.STRING) {

                    String email = cell.getStringCellValue().trim();

                    if (!email.isEmpty()) {
                        emails.add(email);
                    }
                }
            }

            workbook.close();

        } catch (Exception e) {
            System.out.println("❌ Lỗi đọc Excel: " + e.getMessage());
        }

        return emails;
    }

    // ================= SEND MULTIPLE =================
    public boolean sendMultipleEmails(MailRequest req) {

        List<String> emails = new ArrayList<>();

        // lấy danh sách email
        if (req.getExcelFile() != null && !req.getExcelFile().isEmpty()) {
            emails = readEmails(req.getExcelFile());
        } else {
            if (req.getTo() != null) {
                emails.add(req.getTo());
            }
        }

        System.out.println("📨 Total emails: " + emails.size());

        // ❌ không có email nào
        if (emails.isEmpty()) {
            System.out.println("❌ Không có email để gửi");
            return false;
        }

        boolean atLeastOneSuccess = false;

        for (String email : emails) {

            // ❌ skip email rỗng
            if (email == null || email.trim().isEmpty()) {
                System.out.println("❌ Skip email rỗng");
                continue;
            }

            boolean success = sendMail(
                    email,
                    req.getSubject(),
                    req.getContent(),
                    req.getAttachmentPaths()
            );

            if (success) {
                System.out.println("✅ Sent to: " + email);
                atLeastOneSuccess = true;
            } else {
                System.out.println("❌ Failed: " + email);
            }

            // delay tránh spam Gmail
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {}
        }

        // ❗ QUAN TRỌNG: trả kết quả
        return atLeastOneSuccess;
    }
}