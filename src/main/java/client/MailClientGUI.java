package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import model.AutoReplyConfig;
import model.MailRequest;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

public class MailClientGUI extends Application {

    private List<String> filePaths = new ArrayList<>();
    private String excelPath = "";

    private static final String PRIMARY    = "#1a73e8";
    private static final String BG         = "#f6f8fc";
    private static final String CARD       = "#ffffff";
    private static final String BORDER     = "#e0e0e0";
    private static final String TEXT_MAIN  = "#202124";
    private static final String TEXT_MUTE  = "#5f6368";
    private static final String SUCCESS    = "#1e8e3e";
    private static final String ERROR      = "#d93025";
    private static final String DANGER_BG  = "#fce8e6";

    private static final Set<String> IMAGE_EXTS =
            new HashSet<>(Arrays.asList("png","jpg","jpeg","gif","bmp","webp"));

    private VBox    historyList;
    private Label   autoReplyDot;
    private boolean autoReplyActive = false;

    @Override
    public void start(Stage stage) {

        // ══════════════════════════════
        // HEADER
        // ══════════════════════════════
        HBox header = new HBox();
        header.setPadding(new Insets(14, 24, 14, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color:" + CARD + ";" +
                        "-fx-border-color:" + BORDER + ";" +
                        "-fx-border-width:0 0 1 0;"
        );

        Label logo = new Label("✉  Auto Mailer");
        logo.setStyle("-fx-font-size:17px; -fx-font-weight:bold; -fx-text-fill:" + TEXT_MAIN + ";");

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        autoReplyDot = new Label("⬤  Tự động trả lời: TẮT");
        autoReplyDot.setStyle("-fx-font-size:12px; -fx-text-fill:" + TEXT_MUTE + ";");

        header.getChildren().addAll(logo, hSpacer, autoReplyDot);

        // ══════════════════════════════
        // CARD: SOẠN THƯ
        // ══════════════════════════════
        VBox composeCard = new VBox(12);
        composeCard.setPadding(new Insets(20));
        composeCard.setStyle(cardStyle());

        Label composeTitle = sectionTitle("Soạn thư");

        TextField txtTo      = styledField("Người nhận");
        TextField txtSubject = styledField("Tiêu đề");

        TextArea txtContent = new TextArea();
        txtContent.setPromptText("Nội dung...");
        txtContent.setPrefHeight(110);
        txtContent.setStyle(areaStyle());

        // ── FILE ĐÍNH KÈM (MULTIPLE FILES) ──
        Label attachLabel = subLabel("Tệp đính kèm");

        // Danh sách file đã chọn
        VBox fileListBox = new VBox(6);
        fileListBox.setVisible(false);
        fileListBox.setManaged(false);

        Button btnChoose = outlineBtn("📎  Thêm tệp");

        FileChooser chooser = new FileChooser();

        btnChoose.setOnAction(e -> {
            List<File> files = chooser.showOpenMultipleDialog(stage);
            if (files == null || files.isEmpty()) return;

            for (File file : files) {
                // Tránh thêm trùng
                if (filePaths.contains(file.getAbsolutePath())) continue;

                filePaths.add(file.getAbsolutePath());

                // Tạo 1 row cho mỗi file
                HBox fileRow = new HBox(10);
                fileRow.setAlignment(Pos.CENTER_LEFT);
                fileRow.setPadding(new Insets(8, 10, 8, 10));
                fileRow.setStyle(
                        "-fx-background-color:#f1f3f4;" +
                                "-fx-background-radius:8;" +
                                "-fx-border-color:" + BORDER + ";" +
                                "-fx-border-radius:8;"
                );

                // Icon hoặc preview ảnh
                String ext = getExt(file.getName());
                if (IMAGE_EXTS.contains(ext)) {
                    ImageView iv = new ImageView(new Image(file.toURI().toString()));
                    iv.setFitHeight(36);
                    iv.setFitWidth(36);
                    iv.setPreserveRatio(true);
                    fileRow.getChildren().add(iv);
                } else {
                    Label icon = new Label(fileEmoji(ext));
                    icon.setStyle("-fx-font-size:24px;");
                    fileRow.getChildren().add(icon);
                }

                // Tên + dung lượng
                VBox info = new VBox(2);
                Label lName = new Label(file.getName());
                lName.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:" + TEXT_MAIN + ";");
                Label lSize = new Label(formatSize(file.length()));
                lSize.setStyle("-fx-font-size:11px; -fx-text-fill:" + TEXT_MUTE + ";");
                info.getChildren().addAll(lName, lSize);

                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);

                // Nút xóa từng file
                Button btnDel = new Button("✕");
                btnDel.setStyle(
                        "-fx-background-color:" + DANGER_BG + ";" +
                                "-fx-text-fill:" + ERROR + ";" +
                                "-fx-font-weight:bold;" +
                                "-fx-background-radius:20;" +
                                "-fx-padding:4 8 4 8;" +
                                "-fx-cursor:hand;"
                );

                String absPath = file.getAbsolutePath();
                btnDel.setOnAction(ev -> {
                    filePaths.remove(absPath);
                    fileListBox.getChildren().remove(fileRow);
                    if (fileListBox.getChildren().isEmpty()) {
                        fileListBox.setVisible(false);
                        fileListBox.setManaged(false);
                    }
                });

                fileRow.getChildren().addAll(info, sp, btnDel);
                fileListBox.getChildren().add(fileRow);
            }

            fileListBox.setVisible(true);
            fileListBox.setManaged(true);
        });

        // ── Danh sách email (Excel) ──
        Label excelLabel2 = subLabel("Danh sách email (Excel)");

        HBox excelRow = new HBox(10);
        excelRow.setAlignment(Pos.CENTER_LEFT);

        Button btnExcel = outlineBtn("📊  Tải danh sách");
        Label  excelNameLabel = new Label("Chưa chọn file");
        excelNameLabel.setStyle("-fx-font-size:12px; -fx-text-fill:" + TEXT_MUTE + ";");

        Button btnRemoveExcel = new Button("✕");
        btnRemoveExcel.setStyle(
                "-fx-background-color:" + DANGER_BG + ";" +
                        "-fx-text-fill:" + ERROR + ";" +
                        "-fx-font-weight:bold;" +
                        "-fx-background-radius:20;" +
                        "-fx-padding:4 8 4 8;" +
                        "-fx-cursor:hand;"
        );
        btnRemoveExcel.setVisible(false);
        btnRemoveExcel.setManaged(false);

        Tooltip excelTip = new Tooltip(
                "Định dạng file Excel:\n" +
                        "• Cột A chứa danh sách email\n" +
                        "• Dòng 1: tiêu đề (bỏ qua)\n" +
                        "• Từ dòng 2: mỗi ô 1 địa chỉ email"
        );
        Tooltip.install(btnExcel, excelTip);

        FileChooser excelChooser = new FileChooser();
        excelChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel (.xlsx)", "*.xlsx")
        );

        btnExcel.setOnAction(e -> {
            File file = excelChooser.showOpenDialog(stage);
            if (file == null) return;
            excelPath = file.getAbsolutePath();
            excelNameLabel.setText(file.getName());
            excelNameLabel.setStyle("-fx-font-size:12px; -fx-text-fill:" + SUCCESS + "; -fx-font-weight:bold;");
            txtTo.setDisable(true);
            txtTo.setPromptText("(Dùng danh sách từ Excel)");
            btnRemoveExcel.setVisible(true);
            btnRemoveExcel.setManaged(true);
            btnExcel.setText("📊  Đổi file");
        });

        btnRemoveExcel.setOnAction(e -> {
            excelPath = "";
            excelNameLabel.setText("Chưa chọn file");
            excelNameLabel.setStyle("-fx-font-size:12px; -fx-text-fill:" + TEXT_MUTE + ";");
            txtTo.setDisable(false);
            txtTo.setPromptText("Người nhận");
            btnRemoveExcel.setVisible(false);
            btnRemoveExcel.setManaged(false);
            btnExcel.setText("📊  Tải danh sách");
        });

        excelRow.getChildren().addAll(btnExcel, excelNameLabel, btnRemoveExcel);

        Label excelHint = new Label("ℹ  Cột A từ dòng 2 trở đi, mỗi ô 1 địa chỉ email");
        excelHint.setStyle("-fx-font-size:11px; -fx-text-fill:" + TEXT_MUTE + "; -fx-padding:0 0 0 2;");

        // Cập nhật giao diện composeCard
        composeCard.getChildren().addAll(
                composeTitle,
                txtTo, txtSubject, txtContent,
                attachLabel, btnChoose, fileListBox,
                excelLabel2, excelRow, excelHint
        );

        // ══════════════════════════════
        // CARD: TỰ ĐỘNG TRẢ LỜI
        // ══════════════════════════════
        VBox autoReplyCard = new VBox(0);
        autoReplyCard.setStyle(cardStyle());

        HBox arHeader = new HBox(12);
        arHeader.setPadding(new Insets(16, 20, 16, 20));
        arHeader.setAlignment(Pos.CENTER_LEFT);

        VBox arTitleGroup = new VBox(2);
        Label arTitle = new Label("Tự động trả lời");
        arTitle.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + TEXT_MAIN + ";");
        Label arSubtitle = new Label("Tự động reply khi nhận được email từ người đã gửi");
        arSubtitle.setStyle("-fx-font-size:11px; -fx-text-fill:" + TEXT_MUTE + ";");
        arTitleGroup.getChildren().addAll(arTitle, arSubtitle);

        Region arSpacer = new Region();
        HBox.setHgrow(arSpacer, Priority.ALWAYS);

        ToggleButton arToggle = new ToggleButton("TẮT");
        arToggle.setStyle(toggleOff());
        arHeader.getChildren().addAll(arTitleGroup, arSpacer, arToggle);

        Separator arDivider = new Separator();
        arDivider.setStyle("-fx-background-color:" + BORDER + ";");
        arDivider.setVisible(false);
        arDivider.setManaged(false);

        VBox arBody = new VBox(12);
        arBody.setPadding(new Insets(0, 20, 16, 20));
        arBody.setVisible(false);
        arBody.setManaged(false);

        HBox arInfoRow = new HBox(8);
        arInfoRow.setAlignment(Pos.CENTER_LEFT);
        arInfoRow.setPadding(new Insets(10));
        arInfoRow.setStyle(
                "-fx-background-color:#e8f0fe;" +
                        "-fx-background-radius:8;"
        );
        Label arInfoIcon = new Label("ℹ");
        arInfoIcon.setStyle("-fx-font-size:13px; -fx-text-fill:" + PRIMARY + ";");
        Label arInfoText = new Label("Hệ thống sẽ kiểm tra hòm thư mỗi 30 giây và tự động trả lời.");
        arInfoText.setStyle("-fx-font-size:11px; -fx-text-fill:#1a73e8; -fx-wrap-text:true;");
        arInfoText.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(arInfoText, Priority.ALWAYS);
        arInfoRow.getChildren().addAll(arInfoIcon, arInfoText);

        Label templateLabel = subLabel("Nội dung trả lời tự động");

        TextArea txtTemplate = new TextArea();
        txtTemplate.setPromptText("Ví dụ: Cảm ơn bạn đã liên hệ! Chúng tôi sẽ phản hồi trong vòng 24 giờ.");
        txtTemplate.setPrefHeight(90);
        txtTemplate.setStyle(areaStyle());

        Label arStatus = new Label();
        arStatus.setStyle("-fx-font-size:11px;");
        arStatus.setMaxWidth(Double.MAX_VALUE);

        Button btnApply = new Button("Áp dụng cài đặt");
        btnApply.setStyle(primaryBtnStyle());
        btnApply.setMaxWidth(Double.MAX_VALUE);

        btnApply.setOnAction(e -> {
            try {
                Socket socket = new Socket("localhost", 5000);
                AutoReplyConfig cfg = new AutoReplyConfig(
                        arToggle.isSelected(), txtTemplate.getText()
                );
                new ObjectOutputStream(socket.getOutputStream()).writeObject(cfg);
                socket.close();

                autoReplyActive = arToggle.isSelected();
                refreshDot();

                arStatus.setStyle("-fx-font-size:11px; -fx-text-fill:" + SUCCESS + ";");
                arStatus.setText("✓  Đã lưu cài đặt thành công");

                fade(arStatus);

            } catch (Exception ex) {
                arStatus.setStyle("-fx-font-size:11px; -fx-text-fill:" + ERROR + ";");
                arStatus.setText("✗  Không kết nối được server");
            }
        });

        arBody.getChildren().addAll(arInfoRow, templateLabel, txtTemplate, btnApply, arStatus);

        // --- ĐÃ CẬP NHẬT Ở ĐÂY: Logic bật/tắt Toggle Button ---
        arToggle.setOnAction(e -> {
            boolean on = arToggle.isSelected();
            arToggle.setText(on ? "BẬT" : "TẮT");
            arToggle.setStyle(on ? toggleOn() : toggleOff());
            arDivider.setVisible(on);
            arDivider.setManaged(on);
            arBody.setVisible(on);
            arBody.setManaged(on);

            // Khi TẮT → gửi lệnh tắt lên Server ngay lập tức
            if (!on) {
                new Thread(() -> {
                    try {
                        Socket socket = new Socket("localhost", 5000);
                        AutoReplyConfig cfg = new AutoReplyConfig(false, "");
                        new ObjectOutputStream(socket.getOutputStream()).writeObject(cfg);
                        socket.close();

                        Platform.runLater(() -> {
                            autoReplyActive = false;
                            refreshDot();
                        });

                        System.out.println("Auto reply disabled.");

                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            // Báo lỗi nhẹ trên dot ở header
                            autoReplyDot.setStyle("-fx-font-size:12px; -fx-text-fill:" + ERROR + ";");
                            autoReplyDot.setText("⬤  Lỗi kết nối server");
                        });
                    }
                }).start();
            }
        });

        autoReplyCard.getChildren().addAll(arHeader, arDivider, arBody);

        // ══════════════════════════════
        // NÚT GỬI + PROGRESS
        // ══════════════════════════════
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(3);
        progressBar.setVisible(false);
        progressBar.setStyle("-fx-accent:" + PRIMARY + ";");

        Button btnSend = new Button("  Gửi email  ");
        btnSend.setMaxWidth(Double.MAX_VALUE);
        btnSend.setStyle(primaryBtnStyle());

        Label sendStatus = new Label();
        sendStatus.setMaxWidth(Double.MAX_VALUE);
        sendStatus.setAlignment(Pos.CENTER);
        sendStatus.setStyle("-fx-font-size:12px;");

        btnSend.setOnAction(e -> {

            // ✅ VALIDATE TRƯỚC KHI GỬI (QUAN TRỌNG)
            if ((txtTo.getText() == null || txtTo.getText().trim().isEmpty())
                    && (excelPath == null || excelPath.isEmpty())) {

                sendStatus.setStyle("-fx-font-size:12px; -fx-text-fill:" + ERROR + ";");
                sendStatus.setText("✗  Vui lòng nhập người nhận hoặc chọn file Excel");
                return;
            }
            if (txtTo.getText().trim().isEmpty() && excelPath.isEmpty()) {
                sendStatus.setStyle("-fx-text-fill:" + ERROR + ";");
                sendStatus.setText("✗  Chưa nhập người nhận");
                return;
            }

            if (!txtTo.getText().contains("@") && excelPath.isEmpty()) {
                sendStatus.setStyle("-fx-text-fill:" + ERROR + ";");
                sendStatus.setText("✗  Email sai định dạng");
                return;
            }

            btnSend.setDisable(true);
            btnSend.setText("Đang gửi...");
            progressBar.setVisible(true);
            progressBar.setProgress(-1);
            sendStatus.setText("");

            new Thread(() -> {
                try {
                    Socket socket = new Socket("localhost", 5000);

                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                    MailRequest req = new MailRequest(
                            txtTo.getText(),
                            txtSubject.getText(),
                            txtContent.getText(),
                            filePaths.isEmpty() ? "" : filePaths.get(0)
                    );

                    req.setAttachmentPaths(new ArrayList<>(filePaths));
                    req.setExcelFile(excelPath);

                    out.writeObject(req);

                    // 🔥 NHẬN RESPONSE TỪ SERVER
                    String response = (String) in.readObject();

                    socket.close();

                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        btnSend.setDisable(false);
                        btnSend.setText("  Gửi email  ");

                        if ("SUCCESS".equals(response)) {

                            sendStatus.setStyle("-fx-font-size:12px; -fx-text-fill:" + SUCCESS + ";");
                            sendStatus.setText("✓  Gửi thành công");

                            addHistory(txtTo.getText(), txtSubject.getText());

                        } else {

                            sendStatus.setStyle("-fx-font-size:12px; -fx-text-fill:" + ERROR + ";");
                            sendStatus.setText("✗  Email sai hoặc gửi thất bại");
                        }

                        fade(sendStatus);
                    });

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        btnSend.setDisable(false);
                        btnSend.setText("  Gửi email  ");
                        sendStatus.setStyle("-fx-font-size:12px; -fx-text-fill:" + ERROR + ";");
                        sendStatus.setText("✗  Lỗi kết nối server");
                    });
                }
            }).start();
        });
        VBox sendBox = new VBox(6, progressBar, btnSend, sendStatus);

        // ══════════════════════════════
        // CARD: LỊCH SỬ GỬI
        // ══════════════════════════════
        VBox historyCard = new VBox(10);
        historyCard.setPadding(new Insets(20));
        historyCard.setStyle(cardStyle());

        HBox histHeader = new HBox();
        histHeader.setAlignment(Pos.CENTER_LEFT);
        Label histTitle = sectionTitle("Lịch sử gửi");
        Region hst = new Region(); HBox.setHgrow(hst, Priority.ALWAYS);
        Button btnClearHist = new Button("Xóa tất cả");
        btnClearHist.setStyle(
                "-fx-background-color:transparent;" +
                        "-fx-text-fill:" + TEXT_MUTE + ";" +
                        "-fx-font-size:11px;" +
                        "-fx-cursor:hand;" +
                        "-fx-padding:4 8 4 8;"
        );
        histHeader.getChildren().addAll(histTitle, hst, btnClearHist);

        historyList = new VBox(6);

        Label emptyLabel = new Label("Chưa có email nào được gửi.");
        emptyLabel.setStyle("-fx-font-size:12px; -fx-text-fill:" + TEXT_MUTE + ";");
        historyList.getChildren().add(emptyLabel);

        btnClearHist.setOnAction(e -> {
            historyList.getChildren().clear();
            historyList.getChildren().add(emptyLabel);
        });

        ScrollPane histScroll = new ScrollPane(historyList);
        histScroll.setFitToWidth(true);
        histScroll.setPrefHeight(140);
        histScroll.setStyle(
                "-fx-background:transparent;" +
                        "-fx-background-color:transparent;" +
                        "-fx-border-color:transparent;"
        );

        historyCard.getChildren().addAll(histHeader, histScroll);

        // ══════════════════════════════
        // ROOT
        // ══════════════════════════════
        VBox content = new VBox(16,
                composeCard,
                autoReplyCard,
                sendBox,
                historyCard
        );
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color:" + BG + ";");

        ScrollPane root = new ScrollPane(content);
        root.setFitToWidth(true);
        root.setStyle("-fx-background-color:" + BG + "; -fx-border-color:transparent;");

        BorderPane base = new BorderPane();
        base.setTop(header);
        base.setCenter(root);
        base.setStyle("-fx-background-color:" + BG + ";");

        Scene scene = new Scene(base, 500, 700);
        stage.setTitle("Auto Mailer");
        stage.setScene(scene);
        stage.show();
    }

    // ══════════════════════════════
    // HELPERS
    // ══════════════════════════════

    private void addHistory(String to, String subject) {
        historyList.getChildren().removeIf(
                n -> n instanceof Label && ((Label) n).getText().equals("Chưa có email nào được gửi.")
        );

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setStyle("-fx-background-color:#f1f3f4; -fx-background-radius:6;");

        Label lTime = new Label(time);
        lTime.setStyle("-fx-font-size:11px; -fx-text-fill:" + TEXT_MUTE + "; -fx-min-width:34;");

        String toShort  = to.length()      > 24 ? to.substring(0, 24) + "…" : to;
        String subShort = subject.length() > 22 ? subject.substring(0, 22) + "…" : subject;

        Label lTo  = new Label(toShort);
        lTo.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:" + TEXT_MAIN + ";");
        Label lSub = new Label(" — " + subShort);
        lSub.setStyle("-fx-font-size:12px; -fx-text-fill:" + TEXT_MUTE + ";");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label tick = new Label("✓");
        tick.setStyle("-fx-font-size:12px; -fx-text-fill:" + SUCCESS + ";");

        row.getChildren().addAll(lTime, lTo, lSub, sp, tick);
        historyList.getChildren().add(0, row);
        if (historyList.getChildren().size() > 10)
            historyList.getChildren().remove(10);

        FadeTransition ft = new FadeTransition(Duration.millis(350), row);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void refreshDot() {
        if (autoReplyActive) {
            autoReplyDot.setStyle("-fx-font-size:12px; -fx-text-fill:" + SUCCESS + ";");
            autoReplyDot.setText("⬤  Tự động trả lời: BẬT");
        } else {
            autoReplyDot.setStyle("-fx-font-size:12px; -fx-text-fill:" + TEXT_MUTE + ";");
            autoReplyDot.setText("⬤  Tự động trả lời: TẮT");
        }
    }

    private void fade(Label lbl) {
        PauseTransition p = new PauseTransition(Duration.seconds(4));
        p.setOnFinished(e -> lbl.setText(""));
        p.play();
    }

    private String getExt(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i + 1).toLowerCase() : "";
    }

    private String fileEmoji(String ext) {
        switch (ext) {
            case "pdf":  return "📕";
            case "doc": case "docx": return "📝";
            case "xls": case "xlsx": return "📊";
            case "zip": case "rar":  return "🗜";
            case "mp4": case "mov":  return "🎬";
            case "mp3": case "wav":  return "🎵";
            default: return "📄";
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    // ══════════════════════════════
    // STYLE FACTORIES
    // ══════════════════════════════

    private Label sectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:" + TEXT_MUTE + ";");
        return l;
    }

    private Label subLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:12px; -fx-text-fill:" + TEXT_MUTE + "; -fx-padding:4 0 0 0;");
        return l;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle(
                "-fx-background-color:white;" +
                        "-fx-border-color:" + BORDER + ";" +
                        "-fx-border-radius:6; -fx-background-radius:6;" +
                        "-fx-padding:8 12 8 12; -fx-font-size:13px;"
        );
        return tf;
    }

    private String areaStyle() {
        return "-fx-background-color:white;" +
                "-fx-border-color:" + BORDER + ";" +
                "-fx-border-radius:6; -fx-background-radius:6;" +
                "-fx-font-size:13px; -fx-padding:8;";
    }

    private String cardStyle() {
        return "-fx-background-color:" + CARD + ";" +
                "-fx-border-color:" + BORDER + ";" +
                "-fx-border-radius:10; -fx-background-radius:10;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);";
    }

    private String primaryBtnStyle() {
        return "-fx-background-color:" + PRIMARY + ";" +
                "-fx-text-fill:white; -fx-font-size:13px;" +
                "-fx-font-weight:bold; -fx-padding:10 20 10 20;" +
                "-fx-background-radius:6; -fx-cursor:hand;";
    }

    private Button outlineBtn(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color:white;" +
                        "-fx-text-fill:" + TEXT_MAIN + "; -fx-font-size:12px;" +
                        "-fx-padding:7 14 7 14; -fx-background-radius:6;" +
                        "-fx-border-color:" + BORDER + "; -fx-border-radius:6;" +
                        "-fx-cursor:hand;"
        );
        return b;
    }

    private String toggleOn() {
        return "-fx-background-color:" + PRIMARY + ";" +
                "-fx-text-fill:white; -fx-font-size:12px;" +
                "-fx-font-weight:bold; -fx-background-radius:20;" +
                "-fx-padding:5 14 5 14; -fx-cursor:hand;";
    }

    private String toggleOff() {
        return "-fx-background-color:#e8eaed;" +
                "-fx-text-fill:" + TEXT_MUTE + "; -fx-font-size:12px;" +
                "-fx-font-weight:bold; -fx-background-radius:20;" +
                "-fx-padding:5 14 5 14; -fx-cursor:hand;";
    }

    public static void main(String[] args) { launch(); }
}