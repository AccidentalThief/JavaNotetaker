import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection; // For making HTTP requests
import java.net.URL;               // For handling URLs
import java.nio.charset.StandardCharsets; // For encoding/decoding strings
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NoteTakerApp extends JFrame {

    private JTextArea noteTextArea;
    private File currentFile;

    // Define the local directory for notes
    private static final String NOTES_DIRECTORY = "my_notes";
    private static final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

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
            // Start the file chooser in the designated notes directory
            JFileChooser fileChooser = new JFileChooser(NOTES_DIRECTORY);
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

        if (noteContent.trim().isEmpty()) {
            JOptionPane.showMessageDialog(noteTextArea.getTopLevelAncestor(),
                    "Cannot save an empty note.",
                    "Save Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Generate Title using Gemini API!!!!!!!
        String noteTitle = "untitled_note"; // Default title in case API call 
        
        if (currentFile != null) {
            // If editing an existing note, use its current name (without .txt extension)
            String existingFileName = currentFile.getName();
            int dotIndex = existingFileName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < existingFileName.length() - 1) {
                noteTitle = existingFileName.substring(0, dotIndex);
            } else {
                noteTitle = existingFileName; // No extension found, use full name
            }
            System.out.println("DEBUG: Editing existing note. Using current filename as title: " + noteTitle);
        } else {
            // If it's a new note, generate title using Gemini API
            try {
                noteTitle = generateTitleWithGemini(noteContent);
                if (noteTitle.isEmpty()) { // Fallback if sanitization makes it empty
                    noteTitle = "note_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                }
            } catch (IOException | InterruptedException ex) {
                System.err.println("Error generating title with Gemini: " + ex.getMessage());
                JOptionPane.showMessageDialog(noteTextArea.getTopLevelAncestor(),
                        "Could not generate title. Saving with a default name.\nError: " + ex.getMessage(),
                        "Title Generation Failed", JOptionPane.WARNING_MESSAGE);
            }
        }

        try {
            // Ensure the notes directory exists
            File notesDir = new File(NOTES_DIRECTORY);
            if (!notesDir.exists()) {
                notesDir.mkdirs();
            }
            
            File fileToSave;
            if (currentFile != null) {
                fileToSave = currentFile;
            }
            else {
                // Use the generated title for the filename
                String fileName = noteTitle + ".txt";
                fileToSave = new File(notesDir, fileName);

                // Add a counter to the filename if a file with the same name already exists
                int counter = 1;
                while (fileToSave.exists()) {
                    fileName = noteTitle + "_" + counter + ".txt";
                    fileToSave = new File(notesDir, fileName);
                    counter++;
                }
            }
            
            // Write the note content to the file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                writer.write(noteContent);
                JOptionPane.showMessageDialog(noteTextArea.getTopLevelAncestor(),
                        "Note saved successfully to: " + fileToSave.getAbsolutePath(),
                        "Save Success", JOptionPane.INFORMATION_MESSAGE);

                JFrame editorFrame = (JFrame) noteTextArea.getTopLevelAncestor();
                if (editorFrame != null) {
                    editorFrame.dispose();
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(noteTextArea.getTopLevelAncestor(),
                    "Error saving note: " + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private String generateTitleWithGemini(String content) throws IOException, InterruptedException {
        if (GEMINI_API_KEY.isEmpty() || GEMINI_API_KEY.equals("YOUR_GEMINI_API_KEY")) {
            System.err.println("DEBUG: Gemini API Key is not set or is default placeholder.");
            throw new IOException("Gemini API Key is not set. Please replace 'YOUR_GEMINI_API_KEY' with your actual key.");
        }

        URL url = new URL(GEMINI_API_URL + GEMINI_API_KEY);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        String jsonInputString = "{\"contents\": [{\"parts\": [{\"text\": \"Your task is to create a title for these notes, following the word-word format. This means the title should consist of two words separated by a hyphen. For example, a grocery list might be grocery-list, while someone's dream journal might be dream-journal. Generate a title that accurately reflects the content of the notes. Only respond with the title of the notes. Notes: "
                               + escapeJson(content) + "\"}]}]}";

        System.out.println("DEBUG: Sending JSON to Gemini API: " + jsonInputString);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        System.out.println("DEBUG: Gemini API Response Code: " + responseCode);

        if (responseCode != HttpURLConnection.HTTP_OK) {
            InputStream errorStream = connection.getErrorStream();
            String errorResponse = "";
            if (errorStream != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse += line.trim();
                    }
                }
            }
            System.err.println("DEBUG: Gemini API Error Response Body: " + errorResponse);
            throw new IOException("Gemini API call failed with HTTP error code: " + responseCode + ". Response: " + errorResponse);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        } finally {
            connection.disconnect();
        }

        String responseString = response.toString();
        System.out.println("DEBUG: Raw Gemini API Response: " + responseString);

        // IDK how else to do it, so here's the manual JSON parsing
        int textStartIndex = responseString.indexOf("\"text\": \"");
        if (textStartIndex != -1) {
            textStartIndex += "\"text\": \"".length();
            int textEndIndex = responseString.indexOf("\"", textStartIndex);
            if (textEndIndex != -1) {
                String generatedText = responseString.substring(textStartIndex, textEndIndex);
                // Remove newlines and trim whitespace from the generated text
                String cleanedText = generatedText.replace("\\n", "").trim();
                System.out.println("DEBUG: Parsed and Cleaned Title: " + cleanedText);
                return cleanedText;
            }
        }
        System.out.println("DEBUG: Failed to parse 'text' field from Gemini API response. Falling back to default title.");
        return "untitled_note"; //fallback if bad parsing
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NoteTakerApp app = new NoteTakerApp();
            app.setVisible(true);
        });
    }
}
