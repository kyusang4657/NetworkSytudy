package gsj_project.filetransfer;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

public class FileClientGUI extends JFrame {
    private final FileClient client = new FileClient();

    private JTextField downloadFileField;
    private JTextField uploadFileField;
    private JTextArea logArea;

    public FileClientGUI() {
        setTitle("UDP File Transfer Client");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        createUI();
    }

    private void createUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(2, 3, 8, 8));

        downloadFileField = new JTextField("");
        JButton downloadButton = new JButton("Download");

        uploadFileField = new JTextField("");
        JButton uploadButton = new JButton("Upload");

        inputPanel.add(new JLabel("Download file:"));
        inputPanel.add(downloadFileField);
        inputPanel.add(downloadButton);

        inputPanel.add(new JLabel("Upload file:"));
        inputPanel.add(uploadFileField);
        inputPanel.add(uploadButton);

        logArea = new JTextArea();
        logArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(logArea);

        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);

        downloadButton.addActionListener(e -> downloadFile());
        uploadButton.addActionListener(e -> uploadFile());
    }

    private void downloadFile() {
        String filename = downloadFileField.getText().trim();

        if(filename.isEmpty()) {
            appendLog("Download filename is empty.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File(filename));

        int result = fileChooser.showSaveDialog(this);

        if(result != JFileChooser.APPROVE_OPTION) {
            appendLog("Download canceled.");
            return;
        }

        Path outputPath = fileChooser.getSelectedFile().toPath();

        new Thread(() -> {
            appendLog("Download start: " + filename);
            client.downloadFile(filename, outputPath);
            appendLog("Download saved to: " + outputPath);
        }).start();
    }

    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if(result != JFileChooser.APPROVE_OPTION) {
            appendLog("Upload canceled.");
            return;
        }

        java.nio.file.Path selectedFile = fileChooser.getSelectedFile().toPath();

        new Thread(() -> {
            appendLog("Upload start: " + selectedFile.getFileName());
            client.uploadFile(selectedFile);
            appendLog("Upload request finished: " + selectedFile.getFileName());
        }).start();
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FileClientGUI gui = new FileClientGUI();
            gui.setVisible(true);
        });
    }
}