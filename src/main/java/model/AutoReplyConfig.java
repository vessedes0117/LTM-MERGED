package model;

import java.io.Serializable;

public class AutoReplyConfig implements Serializable {

    private boolean enabled;
    private String replyTemplate;

    public AutoReplyConfig(boolean enabled, String replyTemplate) {
        this.enabled = enabled;
        this.replyTemplate = replyTemplate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getReplyTemplate() {
        // tránh null gây lỗi
        if (replyTemplate == null || replyTemplate.trim().isEmpty()) {
            return "Auto reply message.";
        }
        return replyTemplate;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setReplyTemplate(String t) {
        this.replyTemplate = t;
    }
}