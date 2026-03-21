package server;

import mail.AutoReplyService;
import model.AutoReplyConfig;
import model.MailRequest;
import mail.MailService;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MailServer {

    public static void main(String[] args) {

        MailService mailService = new MailService();
        AutoReplyService autoReplyService = new AutoReplyService();

        AutoReplyConfig currentConfig = new AutoReplyConfig(false, "Auto reply");

        try {
            ServerSocket server = new ServerSocket(5000);
            System.out.println("Mail Server running on port 5000...");

            autoReplyService.start(currentConfig);

            while (true) {

                Socket socket = server.accept();

                ObjectOutputStream out =
                        new ObjectOutputStream(socket.getOutputStream());

                ObjectInputStream in =
                        new ObjectInputStream(socket.getInputStream());

                Object obj = in.readObject();

                // ================= AUTO REPLY CONFIG =================
                if (obj instanceof AutoReplyConfig config) {

                    currentConfig = config;
                    autoReplyService.updateConfig(config);

                    if (config.isEnabled()) {
                        System.out.println("Auto reply ENABLED.");
                    } else {
                        System.out.println("Auto reply DISABLED.");
                    }

                    // ✅ trả response
                    out.writeObject("CONFIG_UPDATED");
                }

                // ================= SEND MAIL =================
                else if (obj instanceof MailRequest req) {

                    boolean success = false;

                    try {
                        success = mailService.sendMultipleEmails(req);
                    } catch (Exception e) {
                        success = false;
                        e.printStackTrace();
                    }

                    if (success) {
                        System.out.println("Mail sent successfully.");
                        out.writeObject("SUCCESS");
                    } else {
                        System.out.println("Mail FAILED.");
                        out.writeObject("FAILED");
                    }
                }

                socket.close();
                System.out.println("Request completed.\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}