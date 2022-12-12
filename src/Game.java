import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;


@SuppressWarnings("SpellCheckingInspection")
public class Game extends JFrame {
    public static void main(String[] args) {
        // Argument tells the bot whether it should guess any valid word, or if it should only guess a word that is known to be a wordle soluion
        WordleBot bot = new WordleBot(false);
        // Let the bot play the game itself and set how many moves per second it should make
        bot.autoPlay(true);
        bot.setTps(2);
        new Game("Wordle",false,bot);
    }
    private final boolean useAllWords;
    private JTextPane[][] cells = new JTextPane[6][5];
    private JTextPane[] keyboard = new JTextPane[26];
    private final Dimension SCREENSIZE = Toolkit.getDefaultToolkit().getScreenSize();
    private final Dimension WINDOWSIZE = new Dimension((int) (0.45 * SCREENSIZE.width),(int) (0.85 * SCREENSIZE.height));
    private JPanel mainPanel, rightPanel, bottomPanel;
    private final static Color MAIN_BG = Color.DARK_GRAY;
    private final static Color CELL_BG = Color.GRAY;
    private final static Color SCORE_TEXT_COLOR = Color.WHITE;
    private final static Color CELL_TEXT_COLOR = Color.WHITE;
    private final static Color CORRECT_POS = new Color(106,170,100);
    private final static Color CORRECT_LETTER = new Color(201,180,88);
    private final static Color INCORRECT_LETTER = Color.LIGHT_GRAY;
    private final static Color KEYBOARD_INCORRECT = new Color(50,50,50);
    private final static Font CELL_FONT = new Font("Arial",Font.PLAIN,75);
    private final static Font SCORES_FONT = new Font("Arial",Font.BOLD,30);
    private int letterCount = 0;
    private int letterCountUpper = 5;
    private int letterCountLower = 0;
    private final ArrayList<String> VALID_WORDS = new ArrayList<>(12972);
    private final ArrayList<String> WORDLE_WORDS = new ArrayList<>(2315);
    private Hashtable<Character, Integer> containsChars = new Hashtable<>();
    private ArrayList<Character> notPossibleChars = new ArrayList<>();
    private char[] forcedChars = new char[5];
    private ArrayList<Character>[] notHereChars = new ArrayList[5];
    private String mainWord;
    private JLabel streak, maxStreak;
    private JLabel score;
    private int gameOver;
    private JLabel cheatyWord;
    private final boolean CHEATSON = false;
    private ArrayList<WordleBot> listeningBots = new ArrayList<>();
    private double totalScore;
    private int gamesPlayed;

    public Game(String name, boolean useAllWords, WordleBot bot) {
        this(name,useAllWords);
        addListeningBot(bot);
        notifyBots();
    }

    public Game(String name, boolean useAllWords) {
        super(name);
        this.useAllWords = useAllWords;
        setResizable(false);
        setLayout(new BorderLayout(10,10));
        setPreferredSize(WINDOWSIZE);
        getContentPane().setBackground(MAIN_BG);
        setLocation(SCREENSIZE.width/2 - WINDOWSIZE.width/2, (int) (SCREENSIZE.height/2 - WINDOWSIZE.height * 0.55));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        loadValidWords();
        setMainWord();

        for (int i = 0; i < notHereChars.length; i++)
            notHereChars[i] = new ArrayList<>();

        KeyListener keyListener = new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }
            public void keyPressed(KeyEvent e) {
            }
            public void keyReleased(KeyEvent e) {
                letterTyped(Character.toLowerCase(e.getKeyChar()));
            }
        };
        addKeyListener(keyListener);

        setFocusable(true);
        requestFocus();

        initMainPanel();
        add(mainPanel,BorderLayout.LINE_START);
        initRightPanel();
        add(rightPanel,BorderLayout.LINE_END);
        initBottomPanel();
        add(bottomPanel,BorderLayout.PAGE_END);

        Thread mainGameLoop = new Thread(() -> {
            int tps = 50;
            int mspt = 1000/tps;
            while (true) {
                long tickStart = System.currentTimeMillis();
                if (gameOver != 0) {
                    gameOver();
                    try {
                        TimeUnit.MILLISECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    resetGame();
                }
                else {
                    try {
                        TimeUnit.MILLISECONDS.sleep( Math.max(mspt - System.currentTimeMillis() + tickStart,0));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mainGameLoop.start();

        pack();
        setVisible(true);

        System.out.printf("%15s%15s\n","Average Score","Games Played");
    }

    private static JLabel newCenteredJLabel(Font f, Color c) {
        JLabel j = new JLabel();
        j.setFont(f);
        j.setForeground(c);
        j.setHorizontalAlignment(SwingConstants.CENTER);
        j.setPreferredSize(new Dimension(0,0));

        return j;
    }

    private void resetGame() {
        setMainWord();
        // For testing purposes only
        if (CHEATSON)
            cheatyWord.setText(mainWord);

        remove(mainPanel);
        initMainPanel();
        add(mainPanel,BorderLayout.LINE_START);
        remove(bottomPanel);
        initBottomPanel();
        add(bottomPanel,BorderLayout.PAGE_END);
        pack();



        letterCount = 0;
        letterCountUpper = 5;
        letterCountLower = 0;
        containsChars = new Hashtable<>();
        notPossibleChars = new ArrayList<>();
        forcedChars = new char[5];
        for (int i = 0; i < notHereChars.length; i++)
            notHereChars[i] = new ArrayList<>();

        notifyBots();
    }

    private void gameOver() {
        switch (gameOver) {
            case 1: // Game win
                score.setText(String.valueOf(letterCount/5));
                streak.setText(String.valueOf(Integer.parseInt(streak.getText()) + 1));
                break;
            case 2: // Game loss
                maxStreak.setText(String.valueOf(Math.max(Integer.parseInt(maxStreak.getText()),Integer.parseInt(streak.getText()))));
                score.setText("7");
                streak.setText("0");
        }
        gameOver = 0;
        saveStreak();
        totalScore += getScore();
        System.out.printf("\r%15.4f%15d", totalScore/++gamesPlayed,gamesPlayed);
    }

    public boolean isGameOver() {
        for (char c : forcedChars)
            if (c == 0)
                return false;
        return true;
    }

    private void saveStreak() {
        int maxStreak = 0;
        try (DataInputStream in = new DataInputStream(new FileInputStream("streak.dat"))) {
            maxStreak = in.readInt();
        } catch (IOException ex) {}

        maxStreak = Math.max(maxStreak, Integer.parseInt(streak.getText()));

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream("streak.dat"))) {
            out.writeInt(maxStreak);
        } catch (Exception ex) {
            System.err.println("Error writing streak to file");
        }
    }

    private void updateKnowns() {
        Hashtable<Character, Integer> wordHash = new Hashtable<Character, Integer>();
        for (char c : mainWord.toCharArray()) {
            if (wordHash.containsKey(c))
                wordHash.put(c,wordHash.get(c) + 1);
            else
                wordHash.put(c,1);
        }
        Hashtable<Character, Integer> newContainsChars = new Hashtable<>();
        int wordNum = (letterCount-1)/5;
        char[] guessChars = new char[5];
        for (int i = 0; i < 5; i++)
            guessChars[i] = cells[wordNum][i].getText().charAt(0);
        for (int i = 0; i < 5; i++) {   // for each character in guessed word
            char ch = guessChars[i];
            int count = wordHash.containsKey(ch) ? wordHash.get(ch) : 0;
            // Word doesn't contain letter
            if (!wordHash.containsKey(ch)) {
                notPossibleChars.add(ch);
                setCellColor(wordNum, i, INCORRECT_LETTER);
                setKeyColor(ch,KEYBOARD_INCORRECT);
            }
            // Highlighted all valid ones already
            else if (count == 0) {
                setCellColor(wordNum, i, INCORRECT_LETTER);
                if (!notHereChars[i].contains(ch))
                    notHereChars[i].add(ch);
            }
            // Word contains letter
            else {
                // Right position
                if (ch == mainWord.charAt(i)) {
                    forcedChars[i] = guessChars[i];
                    setCellColor(wordNum,i,CORRECT_POS);
                    wordHash.put(ch,wordHash.get(ch) - 1);

                    if (newContainsChars.containsKey(ch)) {
                        newContainsChars.put(ch,newContainsChars.get(ch) + 1);
                    } else {
                        newContainsChars.put(ch,1);
                    }
                }
                // Right letter wrong position
                else {
                    if (!notHereChars[i].contains(ch))
                        notHereChars[i].add(ch);
                    boolean highlight = true;
                    for (int j = i + 1; j < 5; j++)
                        if (mainWord.charAt(j) == ch && mainWord.charAt(j) == guessChars[j])
                            highlight = false;
                    if (highlight) {
                        setCellColor(wordNum, i, CORRECT_LETTER);
                        wordHash.put(ch,wordHash.get(ch) - 1);

                        if (newContainsChars.containsKey(ch)) {
                            newContainsChars.put(ch,newContainsChars.get(ch) + 1);
                        } else {
                            newContainsChars.put(ch,1);
                        }
                    }
                    else {
                        setCellColor(wordNum, i, INCORRECT_LETTER);
                    }
                }

            }

        }
        for (Character letter : newContainsChars.keySet()) {
            if (containsChars.containsKey(letter))
                containsChars.put(letter, Math.max(containsChars.get(letter),newContainsChars.get(letter)));
            else
                containsChars.put(letter,newContainsChars.get(letter));
        }
    }

    public void letterTyped(char letter) {
        if (Character.isAlphabetic(letter) || letter == '\b' || letter == '\n') {
            switch (letter) {
                case '\b':
                    if (letterCount > letterCountLower)
                        setCellLetter(--letterCount,"");
                    break;
                case '\n':
                    if (letterCount == letterCountUpper && validWord()) {
                        updateKnowns();
                        letterCountUpper += 5;
                        letterCountLower += 5;
                        if (isGameOver()) {
                            gameOver = 1;
                        }
                        else if (letterCountUpper > 30) {
                            gameOver = 2;
                        }
                        else {
                            notifyBots();
                        }
                    }
                    break;
                default:
                    if (letterCount < letterCountUpper)
                        setCellLetter(letterCount++,Character.toString(letter).toUpperCase());
            }
        }
    }

    private void initMainPanel() {
        GridLayout gl = new GridLayout(6,5);
        gl.setHgap(5);
        gl.setVgap(5);
        mainPanel = new JPanel(gl);
        mainPanel.setPreferredSize(new Dimension((int) (0.65 * WINDOWSIZE.width),(int) (0.8 * WINDOWSIZE.height)));
        mainPanel.setBackground(MAIN_BG);
        mainPanel.setFocusable(false);

        initCells();
    }

    private void initRightPanel() {
        rightPanel = new JPanel(new GridLayout(10,1));
        rightPanel.setPreferredSize(new Dimension((int) (0.35 * WINDOWSIZE.width),(int) (0.8 * WINDOWSIZE.height)));
        rightPanel.setBackground(MAIN_BG);
        rightPanel.setFocusable(false);

        // Temporary!! For testing only
        if (CHEATSON) {
            cheatyWord = newCenteredJLabel(SCORES_FONT, SCORE_TEXT_COLOR);
            cheatyWord.setText(mainWord);
            rightPanel.add(cheatyWord);
        }
        else {
            rightPanel.add(new JLabel()); // Buffer
        }
        JLabel maxStreakLabel = newCenteredJLabel(SCORES_FONT,SCORE_TEXT_COLOR);
        maxStreakLabel.setText("Best Streak");
        rightPanel.add(maxStreakLabel);

        maxStreak = newCenteredJLabel(SCORES_FONT,SCORE_TEXT_COLOR);
        try (DataInputStream in = new DataInputStream(new FileInputStream("streak.dat"))) {
            maxStreak.setText(String.valueOf(in.readInt()));
        } catch (FileNotFoundException e) {
            maxStreak.setText("0");
        } catch (IOException e) {
            System.err.println("Error reading streak from file");
        }
        rightPanel.add(maxStreak);

        rightPanel.add(new JLabel()); // Buffer

        JLabel streakLabel = newCenteredJLabel(SCORES_FONT,SCORE_TEXT_COLOR);
        streakLabel.setText("Win Streak");
        rightPanel.add(streakLabel);

        streak = newCenteredJLabel(SCORES_FONT,SCORE_TEXT_COLOR);
        streak.setText("0");
        rightPanel.add(streak);

        rightPanel.add(new JLabel()); // Buffer

        JLabel scoreLabel = newCenteredJLabel(SCORES_FONT,SCORE_TEXT_COLOR);
        scoreLabel.setText("Score");
        rightPanel.add(scoreLabel);

        score = newCenteredJLabel(SCORES_FONT,SCORE_TEXT_COLOR);
        score.setText("0");
        rightPanel.add(score);

        for (Component c : rightPanel.getComponents()) {
            c.setFocusable(false);
        }
    }

    private void initBottomPanel() {
        GridLayout gl = new GridLayout(2,13);
        gl.setHgap(2);
        gl.setVgap(2);
        bottomPanel = new JPanel(gl);
        bottomPanel.setPreferredSize(new Dimension(WINDOWSIZE.width,(int) (0.12 * WINDOWSIZE.height)));
        bottomPanel.setBackground(MAIN_BG);
        bottomPanel.setFocusable(false);

        for (int i = 0; i < 26; i++) {
            JTextPane t = new JTextPane();
            t.setEditable(false);
            t.setFocusable(false);
            t.setBackground(CELL_BG);
            t.setFont(new Font("Arial",Font.PLAIN,30));
            t.setForeground(CELL_TEXT_COLOR);

            StyledDocument doc = t.getStyledDocument();
            SimpleAttributeSet center = new SimpleAttributeSet();
            StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
            doc.setParagraphAttributes(0, doc.getLength(), center, false);

            t.setBorder(new LineBorder(Color.BLACK,2));
            t.setPreferredSize(new Dimension(
                     WINDOWSIZE.width/2/13,
                    WINDOWSIZE.height*4/5/2
            ));

            t.setText(String.valueOf((char) (i + 'A')));

            keyboard[i] = t;
            bottomPanel.add(t);
        }
    }

    private void initCells() {
        for (int i = 0; i < cells.length; i++) {
            for (int j = 0; j < cells[i].length; j++) {
                JTextPane t = new JTextPane();
                t.setEditable(false);
                t.setFocusable(false);
                t.setBackground(CELL_BG);
                t.setFont(CELL_FONT);
                t.setForeground(CELL_TEXT_COLOR);

                StyledDocument doc = t.getStyledDocument();
                SimpleAttributeSet center = new SimpleAttributeSet();
                StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
                doc.setParagraphAttributes(0, doc.getLength(), center, false);

                t.setBorder(new LineBorder(Color.BLACK,2));
                t.setPreferredSize(new Dimension(
                        WINDOWSIZE.width/2/5,
                        WINDOWSIZE.height/6
                ));
                cells[i][j] = t;
            }
        }
        addCellsToPanel();
    }

    private void addCellsToPanel() {
        for (int i = 0; i < cells.length; i++) {
            for (int j = 0; j < cells[i].length; j++) {
                mainPanel.add(cells[i][j]);
            }
        }
    }

    public void addListeningBot(WordleBot bot) {
        bot.setGame(this);
        bot.initLetterDistribution();
        listeningBots.add(bot);
    }

    private void notifyBots() {
        for (WordleBot bot : listeningBots)
            bot.enterWord();
    }

    private void setCellColor(int word, int letter, Color c) {
        cells[word][letter].setBackground(c);
    }

    private void setCellLetter(int pos, String s) {
        int word = pos / 5;
        int letter = pos % 5;
        cells[word][letter].setText(s);
    }

    private void setKeyColor(char ch, Color c) {
        keyboard[ch - 'A'].setBackground(c);
    }

    private boolean validWord() {
        char[] data = new char[5];
        for (int i = 0; i < 5; i++)
            data[i] = cells[(letterCount-1)/5][i].getText().charAt(0);
        String word = String.valueOf(data);
        if (VALID_WORDS.contains(word)) {
            return true;
        } else
            return false;
    }

    private void loadValidWords() {
        try {
            Scanner in = new Scanner(new File("src/wordle-allowed-guesses.txt"));
            while (in.hasNext())
                VALID_WORDS.add(in.next().toUpperCase());
            in = new Scanner(new File("src/wordle-answers-alphabetical.txt"));
            while (in.hasNext()) {
                String s = in.next().toUpperCase();
                VALID_WORDS.add(s);
                WORDLE_WORDS.add(s);
            }
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
        }
    }

    private void setMainWord() {
        if (useAllWords)
            mainWord = VALID_WORDS.get((int) (Math.random() * VALID_WORDS.size()));
        else
            mainWord = WORDLE_WORDS.get((int) (Math.random() * WORDLE_WORDS.size()));
    }

    public void setMainWord(String word) {
        if (word.length() == 5)
            mainWord = word.toUpperCase();
    }

    public ArrayList<String> getVALID_WORDS() {
        return VALID_WORDS;
    }

    public ArrayList<String> getWORDLE_WORDS() {
        return WORDLE_WORDS;
    }

    public char[] getForcedChars() {
        return forcedChars;
    }

    public ArrayList<Character>[] getNotHereChars() {
        return notHereChars;
    }

    public Hashtable<Character, Integer> getContainsChars() {
        return containsChars;
    }

    public ArrayList<Character> getNotPossibleChars() {
        return notPossibleChars;
    }

    public int getStreak() {
        return Integer.parseInt(streak.getText());
    }

    public int getScore() {
        return Integer.parseInt(score.getText());
    }

    public String getMainWord() {
        return mainWord;
    }
}
