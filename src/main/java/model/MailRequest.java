package model;

import java.io.Serializable;
import java.util.List;

public class MailRequest implements Serializable {

    private String to;
    private String subject;
    private String content;
    private String attachmentPath;
    private String excelFile;

    // Thêm field chứa danh sách nhiều file
    private List<String> attachmentPaths;

    public MailRequest(String to,
                       String subject,
                       String content,
                       String attachmentPath){

        this.to = to;
        this.subject = subject;
        this.content = content;
        this.attachmentPath = attachmentPath;
    }

    public String getTo(){
        return to;
    }

    public String getSubject(){
        return subject;
    }

    public String getContent(){
        return content;
    }

    public String getAttachmentPath(){
        return attachmentPath;
    }

    public String getExcelFile(){
        return excelFile;
    }

    public void setExcelFile(String excelFile){
        this.excelFile = excelFile;
    }

    // Getters & Setters cho danh sách file đính kèm
    public List<String> getAttachmentPaths() {
        return attachmentPaths;
    }

    public void setAttachmentPaths(List<String> paths) {
        this.attachmentPaths = paths;
    }
}