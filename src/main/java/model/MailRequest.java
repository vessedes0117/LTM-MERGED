package model;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class MailRequest implements Serializable {

    // Thêm serialVersionUID để đảm bảo an toàn khi truyền object qua mạng
    private static final long serialVersionUID = 1L;

    private String to;
    private String subject;
    private String content;
    private String attachmentPath;
    private String excelFile;
    private List<String> attachmentPaths;

    // --- CẬP NHẬT MỚI: Thêm các trường hỗ trợ Hẹn giờ gửi (Delay/Schedule) ---
    private long delaySeconds = 0;
    private String scheduledTime = null;

    public MailRequest(String to, String subject, String content, String attachmentPath) {
        this.to = to;
        this.subject = subject;
        this.content = content;
        this.attachmentPath = attachmentPath;

        // Khởi tạo list và tự động add attachmentPath cũ vào (đảm bảo tương thích ngược)
        this.attachmentPaths = new ArrayList<>();
        if (attachmentPath != null && !attachmentPath.isEmpty()) {
            this.attachmentPaths.add(attachmentPath);
        }
    }

    // Getters & Setters cơ bản
    public String getTo() { return to; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }

    public String getAttachmentPath() { return attachmentPath; }

    public String getExcelFile() { return excelFile; }
    public void setExcelFile(String excelFile) { this.excelFile = excelFile; }

    // Getters & Setters cho danh sách file đính kèm
    public List<String> getAttachmentPaths() { return attachmentPaths; }
    public void setAttachmentPaths(List<String> paths) { this.attachmentPaths = paths; }

    // --- CẬP NHẬT MỚI: Getters & Setters cho tính năng Hẹn giờ ---
    public long getDelaySeconds() { return delaySeconds; }
    public void setDelaySeconds(long delaySeconds) { this.delaySeconds = delaySeconds; }

    public String getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(String t) { this.scheduledTime = t; }
}