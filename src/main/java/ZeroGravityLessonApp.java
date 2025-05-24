import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * The main JFrame class for the Zero Gravity Lesson application.
 * Controls navigation and keeps shared resources (avatar, progress, score).
 */

public class ZeroGravityLessonApp extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel cards;
    // private final Avatar avatar;
    private final ProgressManager progressManager;
    private final Cursor customCursor;
    private int score = 0;

    /** Constructor sets up window, cursor, screens, and shows welcome. */

    public ZeroGravityLessonApp() {
        super("Zero Gravity Lesson");

        progressManager = new ProgressManager("progress.txt");

        // Create a custom image cursor for playful UI
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image cursorImage = toolkit.getImage("final.png");
        Image scaledCursor = cursorImage.getScaledInstance(40, 35, Image.SCALE_SMOOTH);
        customCursor = toolkit.createCustomCursor(scaledCursor, new Point(0, 0), "kid");
        setCursor(customCursor);

        // CardLayout to easily switch screens
        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        // Add window panels
        cards.add(new WelcomePanel(this, customCursor), ScreenNames.WELCOME);
        cards.add(new LessonPanel(this), ScreenNames.LESSON);
        cards.add(new QuizPanel(this), ScreenNames.QUIZ);
        cards.add(new ResultPanel(this), ScreenNames.RESULT);
        cards.add(new ChatPanel(this), ScreenNames.CHAT);

        setContentPane(cards);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setVisible(true);

        showScreen(ScreenNames.WELCOME);
    }

    /**
     * Switch to a new panel by its name (constant from ScreenNames).
     * 
     * @param name The logical name of the screen to display
     */

    public void showScreen(String name) {
        cardLayout.show(cards, name);
    }

    

    /** @param score Sets the latest quiz score (persisted between screens). */
    public void setScore(int score) {
        this.score = score;
    }

    /** @return The current stored quiz score. */
    public int getScore() {
        return score;
    }

    /** @return The ProgressManager for saving progress. */
    public ProgressManager getProgressManager() {
        return progressManager;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ZeroGravityLessonApp::new);
    }
}

/** Constants for screen navigation names (avoid string typos) */
class ScreenNames {
    static final String WELCOME = "welcome";
    static final String LESSON = "lesson";
    static final String QUIZ = "quiz";
    static final String RESULT = "result";
    static final String CHAT = "chat"; // new

}

/**
 * Utility to fetch Wikipedia summaries for user questions.
 * Fetches the first paragraph of a Wikipedia article using the REST API.
 * 
 * @param topic The search term or article name.
 * @return The summary, or a fallback message if not found.
 */

class WikiFetcher {
    public static String fetchSummary(String topic) {
        try {
            String apiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/" + URLEncoder.encode(topic, "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "ZeroGravityLessonApp/1.0 (your@email.com)");
            conn.connect();

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line);
            br.close();

            // Simple JSON parsing: extract the "extract":"..." field (for summary)
            String json = sb.toString();
            int idx = json.indexOf("\"extract\":\"");
            if (idx != -1) {
                int end = json.indexOf("\",", idx + 11);
                String extract = json.substring(idx + 10, end != -1 ? end : json.length())
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"");
                return extract;
            } else {
                return "Sorry, I couldn't find info on that topic!";
            }
        } catch (Exception ex) {
            return "Sorry, I couldn't find info on that topic! (" + ex.getMessage() + ")";
        }
    }
}

/**
 * Helper class for reusable Swing UI elements.
 * Creates a styled 'Back' button that navigates to a specific app screen.
 * 
 * @param app          The main app instance (to control navigation).
 * @param targetScreen The screen name to show on click.
 * @return The ready-to-use JButton.
 */
class UIUtils {
    public static JButton createBackButton(ZeroGravityLessonApp app, String targetScreen) {
        JButton backBtn = new JButton("⬅ Back");
        backBtn.setFont(new Font("Comic Sans MS", Font.BOLD, 18));
        backBtn.setBackground(new Color(60, 60, 60));
        backBtn.setForeground(Color.YELLOW);
        backBtn.setFocusPainted(false);
        backBtn.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> app.showScreen(targetScreen));
        return backBtn;
    }
}

/**
 * Chat panel lets users ask questions about zero gravity or science topics.
 * Displays avatar, conversation, and text field for input.
 */
class ChatPanel extends JPanel {
    private final JTextArea chatArea;
    private final JTextField inputField;
    private final Map<String, String> knowledgeBase = new HashMap<>();

    public ChatPanel(ZeroGravityLessonApp app) {
        setLayout(new BorderLayout());
        setBackground(Color.DARK_GRAY);

        // Build knowledge base
        knowledgeBase.put("what is zero gravity",
                "Zero gravity (microgravity) is when you and your ship fall around Earth at the same rate, so you feel weightless!");
        knowledgeBase.put("why study zero gravity",
                "Because it teaches us how fluids, fire, and even our bodies behave when weightless.");
        knowledgeBase.put("how do astronauts eat",
                "They squeeze food from packets or use magnets on utensils so everything stays put.");

        // TOP: back button and avatar side by side
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setOpaque(false);
        topPanel.add(UIUtils.createBackButton(app, ScreenNames.LESSON));
        add(topPanel, BorderLayout.NORTH);

        // CENTER: chat area (with scroll)
        chatArea = new JTextArea(10, 40);
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBackground(new Color(30, 30, 30));
        chatArea.setForeground(Color.WHITE);
        chatArea.setFont(new Font("Comic Sans MS", Font.PLAIN, 18));

        // SOUTH: input field
        inputField = new JTextField();
        inputField.setFont(new Font("Comic Sans MS", Font.PLAIN, 18));
        inputField.setBackground(new Color(50, 50, 50));
        inputField.setForeground(Color.GRAY);
        inputField.setText("Write here...");
        inputField.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        inputField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (inputField.getText().equals("Write here...")) {
                    inputField.setText("");
                    inputField.setForeground(Color.WHITE);
                }
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                if (inputField.getText().isEmpty()) {
                    inputField.setText("Write here...");
                    inputField.setForeground(Color.GRAY);
                }
            }
        });
        inputField.addActionListener(e -> processUserInput());

        // Layout: center everything in a panel (GridBagLayout for centering)

        JPanel centerWrapper = new JPanel();
        centerWrapper.setOpaque(false);
        centerWrapper.setLayout(new GridBagLayout());

        JPanel chatBox = new JPanel(new BorderLayout());
        chatBox.setPreferredSize(new Dimension(600, 350)); // width x height
        chatBox.setBackground(new Color(30, 30, 30));
        chatBox.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        chatBox.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chatBox.add(inputField, BorderLayout.SOUTH);

        centerWrapper.add(chatBox); // This centers the chatBox panel

        add(centerWrapper, BorderLayout.CENTER);

        appendAvatarMessage("Hello! Ask me anything about zero gravity.");
    }

    /**
     * Handles input, checks if user asked a local or Wikipedia question, and
     * replies.
     */

    private void processUserInput() {
        String userText = inputField.getText().trim();
        if (userText.isEmpty() || userText.equals("Write here..."))
            return;
        appendUserMessage(userText);
        inputField.setText("");

        String key = userText.toLowerCase();

        // Rule-based answers for certain keywords
        String response = null;
        if (key.contains("zero gravity") || key.contains("microgravity")) {
            response = "Zero gravity (microgravity) is the condition in which people or objects appear to be weightless. This occurs when everything is falling together around Earth, like astronauts and their spacecraft.";
        } else if (key.contains("who are you")) {
            response = "I'm your friendly astronaut avatar, here to help you explore space and science!";
        } else if (key.contains("hello") || key.contains("hi")) {
            response = "Hello! I'm your space guide. Ask me anything about zero gravity or space!";
        } else {
            // Default: fetch from Wikipedia
            response = WikiFetcher.fetchSummary(userText);
        }

        appendAvatarMessage(response);
    }

    /**
     * Simple helper to clean up questions before sending to Wikipedia (not called
     * currently).
     * 
     * @param userText The user's question or phrase.
     * @return Cleaned question text.
     */
    private String smartenQuestion(String userText) {
        String lower = userText.toLowerCase();
        // Remove leading question words for better Wikipedia search
        if (lower.startsWith("what is "))
            return userText.substring(8);
        if (lower.startsWith("who is "))
            return userText.substring(7);
        if (lower.startsWith("tell me about "))
            return userText.substring(14);
        if (lower.startsWith("explain "))
            return userText.substring(8);
        if (lower.startsWith("define "))
            return userText.substring(7);
        // Otherwise, return as is
        return userText;
    }

    // Appends a user message in the chat area.
    private void appendUserMessage(String msg) {
        chatArea.append("You: " + msg + "\n");
    }

    // Appends an avatar reply in the chat area
    private void appendAvatarMessage(String msg) {
        chatArea.append("Avatar: " + msg + "\n\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
}

/**
 * StarCometBackgroundPanel animates a black space background with twinkling
 * stars and moving comets.
 * For use as a pretty background in main screens.
 * 
 * @param starCount  Number of stars
 * @param cometCount Number of comets
 * @param width      Preferred width of panel
 * @param height     Preferred height of panel
 */
class StarCometBackgroundPanel extends JPanel {
    private static class Star {
        float x, y;
        int size;
        float brightness, dBrightness;
    }

    private final Star[] stars;

    private static class Comet {
        float x, y, dx, dy;
        int length, tailAlpha;
        Color color;
    }

    private final Comet[] comets;
    private final Random rand = new Random();

    public StarCometBackgroundPanel(int starCount, int cometCount, int width, int height) {
        setPreferredSize(new Dimension(width, height));
        setOpaque(true);
        setBackground(Color.BLACK);
        stars = new Star[starCount];
        for (int i = 0; i < starCount; i++) {
            stars[i] = new Star();
            stars[i].x = rand.nextFloat() * width;
            stars[i].y = rand.nextFloat() * height;
            stars[i].size = 1 + rand.nextInt(3);
            stars[i].brightness = 0.6f + 0.4f * rand.nextFloat();
            stars[i].dBrightness = 0.008f * (rand.nextFloat() - 0.5f);
        }
        comets = new Comet[cometCount];
        for (int i = 0; i < cometCount; i++)
            comets[i] = makeComet(width, height);
        new Timer(40, e -> animate()).start();
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent evt) {
                int w = Math.max(1, getWidth()), h = Math.max(2, getHeight());
                for (Star s : stars) {
                    s.x = rand.nextFloat() * w;
                    s.y = rand.nextFloat() * h;
                }
                for (Comet c : comets) {
                    Comet reset = makeComet(w, h);
                    c.x = reset.x;
                    c.y = reset.y;
                    c.dx = reset.dx;
                    c.dy = reset.dy;
                    c.length = reset.length;
                    c.tailAlpha = reset.tailAlpha;
                    c.color = reset.color;
                }
            }
        });
    }

    private Comet makeComet(int w, int h) {
        w = Math.max(1, w);
        h = Math.max(2, h);
        Comet c = new Comet();
        c.x = rand.nextInt(w);
        c.y = rand.nextInt(h / 2);
        double angle = Math.PI / 4 + rand.nextDouble() * Math.PI / 3;
        float speed = 5f + rand.nextFloat() * 4f;
        c.dx = (float) (speed * Math.cos(angle));
        c.dy = (float) (speed * Math.sin(angle));
        c.length = 60 + rand.nextInt(50);
        c.tailAlpha = 60 + rand.nextInt(80);
        c.color = new Color(255, 255, 80 + rand.nextInt(90));
        return c;
    }

    private void animate() {
        for (Star s : stars) {
            s.brightness += s.dBrightness;
            if (s.brightness > 1f) {
                s.brightness = 1f;
                s.dBrightness = -s.dBrightness;
            } else if (s.brightness < 0.55f) {
                s.brightness = 0.55f;
                s.dBrightness = -s.dBrightness;
            }
        }
        int w = Math.max(1, getWidth()), h = Math.max(2, getHeight());
        for (Comet c : comets) {
            c.x += c.dx;
            c.y += c.dy;
            if (c.x > w + 40 || c.y > h + 40) {
                Comet reset = makeComet(w, h);
                c.x = -40;
                c.y = rand.nextInt(h / 2);
                c.dx = reset.dx;
                c.dy = reset.dy;
                c.length = reset.length;
                c.tailAlpha = reset.tailAlpha;
                c.color = reset.color;
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        for (Star s : stars) {
            int alpha = (int) (110 + 120 * s.brightness);
            g.setColor(new Color(255, 255, 60, alpha));
            g.fillOval(Math.round(s.x), Math.round(s.y), s.size, s.size);
        }
        for (Comet c : comets) {
            for (int i = 0; i < c.length; i += 2) {
                int alpha = Math.max(0, c.tailAlpha - i * 2);
                g.setColor(new Color(c.color.getRed(), c.color.getGreen(), c.color.getBlue(), alpha));
                int x = (int) (c.x - c.dx * i / 10.0);
                int y = (int) (c.y - c.dy * i / 10.0);
                g.fillOval(x, y, 6, 6);
            }
            g.setColor(Color.WHITE);
            g.fillOval((int) c.x - 2, (int) c.y - 2, 9, 9);
        }
    }
}

/**
 * Avatar label shows a scaled astronaut or character icon.
 * 
 * @param imgPath Path to the avatar image file
 */
class Avatar extends JLabel {
    public Avatar(String imgPath) {
        ImageIcon icon = new ImageIcon(imgPath);
        Image scaledImage = icon.getImage().getScaledInstance(140, 140, Image.SCALE_SMOOTH);
        setIcon(new ImageIcon(scaledImage));
        setPreferredSize(new Dimension(140, 140));
        setHorizontalAlignment(SwingConstants.CENTER);
    }
}

/**
 * ProgressManager handles saving user quiz progress to a file.
 * 
 * @param filePath   File to save progress data.
 * @param lessonName Lesson title.
 * @param score      User's score.
 */
class ProgressManager {
    private final String filePath;

    public ProgressManager(String filePath) {
        this.filePath = filePath;
    }

    public void saveProgress(String lessonName, int score) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(lessonName + " - Score: " + score + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/**
 * WelcomePanel shows introduction story, avatar, and guides user to lesson.
 * Animates dialogue with 'Next' button.
 * 
 * @param app          Main application instance (for navigation)
 * @param customCursor Cursor for interactive avatar
 */
class WelcomePanel extends StarCometBackgroundPanel {
    private JLabel dialogueLabel;
    private JButton nextBtn;
    private int dialogueIndex = 0;
    private final String[] dialogues = {
            "<html><b>🛰 Mission Control:</b><br>Welcome aboard, Junior Astronaut!<br>This is Mission Control speaking.</html>",
            "<html><b>🛰 Mission Control:</b><br>Today's mission: <b>Explore the wonders of zero gravity!</b></html>",
            "<html><b>🛰 Mission Control:</b><br>Before we blast off, here's your briefing:<br>In space, gravity is much weaker than on Earth. That's why astronauts feel almost <b>weightless</b>—they are not flying, but falling around Earth!</html>",
            "<html><b>🛰 Mission Control:</b><br>We'll learn how this changes everything: <br>how you eat, move, and even sleep!</html>",
            "<html><b>🛰 Mission Control:</b><br>Ready to float among the stars and become a microgravity expert?<br><b>Click Start Lesson to suit up and begin your space mission!</b></html>"
    };

    public WelcomePanel(ZeroGravityLessonApp app, Cursor customCursor) {
        super(90, 2, 1000, 700);
        setLayout(null);
        // Avatar image
        ImageIcon icon = new ImageIcon("kid.png");
        Image scaled = icon.getImage().getScaledInstance(150, 220, Image.SCALE_SMOOTH);
        JLabel avatarLabel = new JLabel(new ImageIcon(scaled));
        avatarLabel.setBounds(60, 70, 115, 180);
        avatarLabel.setCursor(customCursor);
        add(avatarLabel);

        // Speech bubble
        JPanel bubble = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(30, 46, 80, 220));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 36, 36);
            }
        };
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBounds(180, 60, 800, 200);
        bubble.setOpaque(false);

        dialogueLabel = new JLabel(dialogues[dialogueIndex]);
        dialogueLabel.setFont(new Font("Comic Sans MS", Font.PLAIN, 22));
        dialogueLabel.setForeground(Color.WHITE);
        dialogueLabel.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        bubble.add(dialogueLabel);

        add(bubble);

        nextBtn = new JButton("Next");
        nextBtn.setFont(new Font("Comic Sans MS", Font.BOLD, 24));
        nextBtn.setBackground(new Color(45, 136, 255));
        nextBtn.setForeground(Color.WHITE);
        nextBtn.setFocusPainted(false);
        nextBtn.setBorder(BorderFactory.createEmptyBorder(12, 38, 12, 38));
        nextBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextBtn.setBounds(370, 320, 200, 50);

        nextBtn.addActionListener(e -> {
            if (dialogueIndex < dialogues.length - 1) {
                dialogueIndex++;
                dialogueLabel.setText(dialogues[dialogueIndex]);
                if (dialogueIndex == dialogues.length - 1) {
                    nextBtn.setText(" 🚀 Start Lesson");
                }
            } else {
                app.showScreen(ScreenNames.LESSON);
            }
        });

        add(nextBtn);
    }
}

/**
 * LessonPanel shows main science content and lets user proceed to quiz.
 * 
 * @param app Main application instance (for navigation).
 */
class LessonPanel extends StarCometBackgroundPanel {
    public LessonPanel(ZeroGravityLessonApp app) {
        super(100, 2, 800, 600);
        setLayout(new BorderLayout());

        // Back button at top
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setOpaque(false);
        topBar.add(UIUtils.createBackButton(app, ScreenNames.WELCOME));

        JButton chatBtn = new JButton("💬 Chat with Avatar");
        chatBtn.setFont(new Font("Comic Sans MS", Font.BOLD, 18));
        chatBtn.setBackground(new Color(45, 136, 255));
        chatBtn.setForeground(Color.black);
        chatBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        chatBtn.setFocusPainted(false);
        chatBtn.addActionListener(e -> app.showScreen(ScreenNames.CHAT));
        topBar.add(chatBtn);

        add(topBar, BorderLayout.NORTH);

        int maxWidth = 650;
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));
        contentPanel.setMaximumSize(new Dimension(maxWidth, Integer.MAX_VALUE));

        String lessonHTML = "<html><div style='width:700px;'>"
                + "<span style='font-size:38px; font-weight:bold; color:#ffe257;'>What is Zero Gravity?</span><br><br>"
                + "<span style='font-size:22px;'><b>Astronaut ():</b> Welcome aboard the spaceship!<br>"
                + "Ever wondered why we float here? In space, we feel almost <b>weightless</b> due to something called <b>microgravity</b>.<br><br>"
                + "<b>What's really happening?</b><br>"
                + "Our spaceship and everything inside are actually falling around the Earth—but because we're all falling together, it feels like we're floating!<br><br>"
                + "<ul>"
                + "<li><b>Liquids</b> float in bubbles—you can't pour juice into a cup in space!</li>"
                + "<li><b>Muscles and bones</b> get weaker if astronauts don’t exercise daily.</li>"
                + "<li>Just a tiny push and you drift across the whole cabin!</li>"
                + "<li>Fire burns in a ball, not a tall flame.</li>"
                + "<li>Everyday tasks (like eating, brushing teeth, or sleeping) become a funny challenge!</li>"
                + "</ul> <br>"
                + "<b>Fun Fact:</b> Did you know astronauts sleep in bags strapped to the walls, so they don’t float away while dreaming?</span>"
                + "</div></html>";

        JLabel lessonLabel = new JLabel(lessonHTML);
        lessonLabel.setFont(new Font("Comic Sans MS", Font.PLAIN, 20));
        lessonLabel.setForeground(Color.WHITE);
        lessonLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(lessonLabel);

        contentPanel.add(Box.createVerticalStrut(36));

        JButton quizBtn = new JButton("Take Quiz");
        quizBtn.setFont(new Font("Comic Sans MS", Font.BOLD, 22));
        quizBtn.setBackground(new Color(45, 136, 255));
        quizBtn.setForeground(Color.WHITE);
        quizBtn.setFocusPainted(false);
        quizBtn.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
        quizBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        quizBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        quizBtn.addActionListener(e -> app.showScreen(ScreenNames.QUIZ));
        contentPanel.add(quizBtn);

        JPanel outer = new JPanel();
        outer.setOpaque(false);
        outer.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 40));
        outer.add(contentPanel);

        JScrollPane scrollPane = new JScrollPane(outer);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }
}

/**
 * QuizPanel presents MCQ quiz about zero gravity, manages scoring.
 * 
 * @param app Main app for navigation and updating score.
 */
class QuizPanel extends StarCometBackgroundPanel {
    private final String[][] questions = {
            { "Why do astronauts feel weightless?",
                    "Because they are free falling around Earth",
                    "Because there is no gravity in space",
                    "Because they are far from Earth",
                    "Because the ship pushes them up" },
            { "What is microgravity?",
                    "Very small gravity is still present",
                    "No gravity at all",
                    "Gravity is reversed",
                    "Gravity only on Mars" },
            { "Why do astronauts have to exercise in space?",
                    "To keep bones and muscles strong",
                    "To float better",
                    "For fun",
                    "To use equipment" }
    };
    private final int[] correct = { 0, 0, 0 };

    private int currentQuestion = 0;
    private int score = 0;
    private JPanel card;
    private JLabel feedbackLabel;
    private JButton[] optionButtons;
    private JButton nextBtn;

    public QuizPanel(ZeroGravityLessonApp app) {
        super(80, 8, 1100, 800);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setOpaque(false);
        topPanel.add(UIUtils.createBackButton(app, ScreenNames.LESSON));
        add(topPanel, BorderLayout.NORTH);

        showQuestion(app);
    }

    private void showQuestion(ZeroGravityLessonApp app) {
        if (getComponentCount() > 1)
            remove(1);

        card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(550, 400));
        card.setMaximumSize(new Dimension(650, 600));

        ImageIcon icon = new ImageIcon("solar.png");
        Image img = icon.getImage();
        int maxWidth = 350;
        int imgWidth = icon.getIconWidth();
        int imgHeight = icon.getIconHeight();

        float scale = Math.min(1.0f, (float) maxWidth / imgWidth);
        int newWidth = (int) (imgWidth * scale);
        int newHeight = (int) (imgHeight * scale);

        if (scale < 1.0f)
            img = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);

        JLabel quizImgLabel = new JLabel(new ImageIcon(img));
        quizImgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(Box.createVerticalStrut(8)); // space above
        card.add(quizImgLabel);
        card.add(Box.createVerticalStrut(20)); // space below

        JLabel progressLabel = new JLabel(String.format("Question %d of %d", currentQuestion + 1, questions.length));
        progressLabel.setFont(new Font("Comic Sans MS", Font.PLAIN, 18));
        progressLabel.setForeground(new Color(255, 232, 93));
        progressLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(progressLabel);

        card.add(Box.createVerticalStrut(14));

        JLabel questionLabel = new JLabel("<html><div style='text-align:center;'>" +
                "<b>" + questions[currentQuestion][0] + "</b></div></html>");
        questionLabel.setFont(new Font("Comic Sans MS", Font.BOLD, 24));
        questionLabel.setForeground(Color.WHITE);
        questionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        questionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(questionLabel);

        card.add(Box.createVerticalStrut(12));

        optionButtons = new JButton[4];
        for (int i = 0; i < 4; i++) {
            optionButtons[i] = new JButton(questions[currentQuestion][i + 1]);
            optionButtons[i].setFont(new Font("Comic Sans MS", Font.PLAIN, 19));
            optionButtons[i].setFocusPainted(false);
            optionButtons[i].setBackground(new Color(35, 50, 90));
            optionButtons[i].setForeground(Color.WHITE);
            optionButtons[i].setAlignmentX(Component.CENTER_ALIGNMENT);
            optionButtons[i].setCursor(new Cursor(Cursor.HAND_CURSOR));
            optionButtons[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(85, 157, 255), 2, true),
                    BorderFactory.createEmptyBorder(10, 18, 10, 18)));
            int choice = i;
            optionButtons[i].addActionListener(e -> handleAnswer(choice, app));
            card.add(Box.createVerticalStrut(8));
            card.add(optionButtons[i]);
        }
        card.add(Box.createVerticalStrut(20));

        feedbackLabel = new JLabel(" ");
        feedbackLabel.setFont(new Font("Comic Sans MS", Font.BOLD, 18));
        feedbackLabel.setForeground(Color.YELLOW);
        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        feedbackLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(feedbackLabel);

        nextBtn = new JButton("Next");
        nextBtn.setFont(new Font("Comic Sans MS", Font.BOLD, 20));
        nextBtn.setBackground(new Color(60, 165, 255));
        nextBtn.setForeground(Color.WHITE);
        nextBtn.setFocusPainted(false);
        nextBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        nextBtn.setBorder(BorderFactory.createEmptyBorder(12, 35, 12, 35));
        nextBtn.setVisible(false);
        nextBtn.addActionListener(e -> {
            currentQuestion++;
            if (currentQuestion < questions.length) {
                showQuestion(app);
            } else {
                app.setScore(score);
                app.showScreen(ScreenNames.RESULT);
            }
        });
        card.add(Box.createVerticalStrut(12));
        card.add(nextBtn);

        JScrollPane scrollPane = new JScrollPane(card);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void handleAnswer(int choice, ZeroGravityLessonApp app) {
        for (JButton btn : optionButtons)
            btn.setEnabled(false);
        if (choice == correct[currentQuestion]) {
            feedbackLabel.setText("✅ Correct!");
            score++;
        } else {
            feedbackLabel.setText("❌ Oops! That's not right.");
        }
        nextBtn.setVisible(true);
    }
}

/**
 * ResultPanel shows quiz feedback and exit button.
 */
class ResultPanel extends StarCometBackgroundPanel {
    private JLabel resultLabel;
    private JLabel medalLabel; // To display medal image

    public ResultPanel(ZeroGravityLessonApp app) {
        super(80, 2, 1100, 800);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setOpaque(false);
        topPanel.add(UIUtils.createBackButton(app, ScreenNames.QUIZ));
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        resultLabel = new JLabel();
        resultLabel.setFont(new Font("Comic Sans MS", Font.BOLD, 34));
        resultLabel.setForeground(new Color(245, 222, 54));
        resultLabel.setHorizontalAlignment(SwingConstants.CENTER);
        centerPanel.add(resultLabel);
        add(centerPanel, BorderLayout.CENTER);
        medalLabel = new JLabel();
        medalLabel.setHorizontalAlignment(SwingConstants.CENTER);
        medalLabel.setPreferredSize(new Dimension(150, 150));
        centerPanel.add(medalLabel);

        // --- Fix Exit button size and layout here! ---
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        JButton exitBtn = new JButton("Exit");
        exitBtn.setFont(new Font("Comic Sans MS", Font.PLAIN, 18));
        exitBtn.setPreferredSize(new Dimension(120, 40)); // Set width and height
        exitBtn.addActionListener(e -> System.exit(0));
        bottomPanel.add(exitBtn);

        add(bottomPanel, BorderLayout.SOUTH);

        // Update message when panel is shown
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                updateResult(app);
            }
        });
    }

    private void updateResult(ZeroGravityLessonApp app) {
        int score = app.getScore();
        String message;
        String imgPath;
        if (score == 3) {
            message = "<html><center>You scored 3/3!<br>Excellent!<br>You earned the <b>Zero-G Expert</b> badge! 🚀</center></html>";
            imgPath = "gold.png";
        } else if (score == 2) {
            message = "<html><center>You scored 2/3.<br>Great job!<br>You earned the <b>Silver Medal</b>!</center></html>";
            imgPath = "silver.png";
        } else if (score == 1) {
            message = "<html><center>You scored 1/3.<br>Good try!<br>You earned the <b>Bronze Medal</b>!</center></html>";
            imgPath = "bronze.png";
        } else {
            message = "<html><center>You scored 0/3.<br>Try again to earn a medal!</center></html>";
            imgPath = "fail.png";
        }
        resultLabel.setText(message);

        // Load and scale medal image
        ImageIcon icon = new ImageIcon(imgPath);
        Image img = icon.getImage().getScaledInstance(130, 130, Image.SCALE_SMOOTH);
        medalLabel.setIcon(new ImageIcon(img));
        revalidate();
    }
}