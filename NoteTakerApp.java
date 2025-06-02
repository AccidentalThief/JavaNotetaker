import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class NoteTakerApp extends JFrame {

    private JTextArea noteTextArea;
    private File currentFile;

    public NoteTakerApp() {
        setTitle("Java Note Taker");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));

        JButton createNewNoteButton = new JButton("Create New Note");
        createNewNoteButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        createNewNoteButton.addActionListener(e -> openNoteEditor(null));

        JButton editExistingNoteButton = new JButton("Edit Existing Note");
        editExistingNoteButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        editExistingNoteButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Note to Edit");

            int userSelection = fileChooser.showOpenDialog(NoteTakerApp.this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToOpen = fileChooser.getSelectedFile();

                if (fileToOpen.exists() && fileToOpen.isFile()) {
                    openNoteEditor(fileToOpen);
                } else {
                    JOptionPane.showMessageDialog(NoteTakerApp.this,
                            "Selected file does not exist or is not a valid file.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        mainPanel.add(createNewNoteButton);
        mainPanel.add(editExistingNoteButton);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void openNoteEditor(File file) {
        JFrame editorFrame = new JFrame("Note Editor");
        editorFrame.setSize(700, 500);
        editorFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        editorFrame.setLocationRelativeTo(this);

        noteTextArea = new JTextArea();
        noteTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        noteTextArea.setLineWrap(true);
        noteTextArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(noteTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JButton saveButton = new JButton("Save Note");
        saveButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        saveButton.addActionListener(e -> saveNote());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveButton);

        editorFrame.add(scrollPane, BorderLayout.CENTER);
        editorFrame.add(buttonPanel, BorderLayout.SOUTH);

        currentFile = file;

        if (file != null) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
                noteTextArea.setText(content);
                editorFrame.setTitle("Note Editor - " + file.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(editorFrame,
                        "Error loading note: " + ex.getMessage(),
                        "Load Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        } else {
            noteTextArea.setText("");
            editorFrame.setTitle("Note Editor - New Note");
        }

        editorFrame.setVisible(true);
    }

    private void saveNote() {
        String noteContent = noteTextArea.getText();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Note As");

        if (currentFile != null) {
            fileChooser.setSelectedFile(currentFile);
        } else {
            fileChooser.setSelectedFile(new File("untitled_note.txt"));
        }

        int userSelection = fileChooser.showSaveDialog(noteTextArea.getTopLevelAncestor());

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            if (!fileToSave.getName().toLowerCase().endsWith(".txt")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".txt");
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                writer.write(noteContent);
                currentFile = fileToSave;
                JOptionPane.showMessageDialog(noteTextArea.getTopLevelAncestor(),
                        "Note saved successfully to: " + fileToSave.getAbsolutePath(),
                        "Save Success", JOptionPane.INFORMATION_MESSAGE);
                ((JFrame) noteTextArea.getTopLevelAncestor()).setTitle("Note Editor - " + fileToSave.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(noteTextArea.getTopLevelAncestor(),
                        "Error saving note: " + ex.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NoteTakerApp app = new NoteTakerApp();
            app.setVisible(true);
        });
    }
}
