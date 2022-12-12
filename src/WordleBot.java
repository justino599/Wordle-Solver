import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;

@SuppressWarnings("SpellCheckingInspection")
public class WordleBot {
    private Game game;
    private boolean useAllWords;
    private Hashtable<Character,Double> letterDistribution;
    private boolean autoPlay;
    private int tps = 50, mspt;
    private long lastMove;

    /**
     *
     * @param useAllWords Should the bot use the list of all valid words, or only use those which are known to be answers to a wordle puzzle
     */
    public WordleBot(boolean useAllWords) {
        this.useAllWords = useAllWords;
        this.lastMove = System.currentTimeMillis();
    }

    public void enterWord() {
        String bestGuess = getBestGuess();
        if (!autoPlay)
            System.out.println(bestGuess);
        else {
            for (char letter : bestGuess.toCharArray())
                game.letterTyped(letter);

            try {
                TimeUnit.MILLISECONDS.sleep(mspt);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            game.letterTyped('\n');
        }
    }

    public String getBestGuess() {
        /* Average Score   Games Played    Method
         * ---------------------------------------
         *        3.7827          13844    Letter Distribution
         *        4.0824           9568    Random Selection
         */

        ArrayList<String> possibleGuesses = getPossibleGuesses();
        System.out.println(possibleGuesses);
        String bestWord = "";

        switch (1) {
            case 1:
                // Letter Distribution method
                TreeMap<String,Double> guessScores = new TreeMap<>();

                for (String word : possibleGuesses) {
                    // Count letters in word
                    Hashtable<Character, Boolean> wordChars = new Hashtable<>();
                    for (char letter : word.toCharArray())
                        wordChars.put(letter,true);
                    // Add up score of word
                    double score = 0;
                    for (Character letter : wordChars.keySet())
                        score += letterDistribution.get(letter);
                    guessScores.put(word,score);
                }

                double maxScore = 0;
                for (String word : guessScores.keySet()) {
                    if (guessScores.get(word) > maxScore) {
                        bestWord = word;
                        maxScore = guessScores.get(word);
                    }
                }
                break;
            case 2:
                bestWord = possibleGuesses.get((int) (Math.random() * possibleGuesses.size()));
        }

        return bestWord;
    }

    private ArrayList<String> getPossibleGuesses() {
        ArrayList<String> guesses = new ArrayList<>();
        Hashtable<Character,Integer> containsCharacters = game.getContainsChars();
        char[] forcedCharacters = game.getForcedChars();
        ArrayList<Character>[] notHereChars = game.getNotHereChars();
        ArrayList<Character> notPossibleChars = game.getNotPossibleChars();

//        System.out.println(containsCharacters);
//        System.out.println(forcedCharacters);
//        System.out.println("notHereChars: " + Arrays.toString(notHereChars));
//        System.out.println("notPossibleChars: " + notPossibleChars);

        word: for (String word : useAllWords ? game.getVALID_WORDS() : game.getWORDLE_WORDS()) {
            char[] cWord = word.toCharArray();
            // Check that all forced characters are present
            for (int i = 0; i < forcedCharacters.length; i++)
                // still a potential valid word if this forced is blank or if letter is in right pos
                if (forcedCharacters[i] != 0 && cWord[i] != forcedCharacters[i] || notPossibleChars.contains(cWord[i]) || notHereChars[i].contains(cWord[i]))
                    continue word;
            // Check that all contained characters are present
            for (Character letter : containsCharacters.keySet())
                if (countChars(letter, word) < containsCharacters.get(letter))
                    continue word;
            // Word is a valid guess
            guesses.add(word);
        }

        return guesses;
    }

    private int countChars(char c, String word) {
        int count = 0;
        for (char ch : word.toCharArray())
            if (ch == c)
                count++;
        return count;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void initLetterDistribution() {
        letterDistribution = new Hashtable<>(26);
        double[] temp = new double[26];
        ArrayList<String> words = useAllWords ? game.getVALID_WORDS() : game.getWORDLE_WORDS();
        int count = words.size();
        for (String word : words) {
            char[] charArray = word.toCharArray();
            for (int i = 0; i < 5; i++)
                temp[charArray[i] - 'A'] += 1;
        }
        for (int i = 0; i < 26; i++)
            letterDistribution.put((char)('A' + i),temp[i]/count);
    }

    public void autoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
    }

    /**
     * @param tps The number of actions per second the bot is allowed to make
     */
    public void setTps(int tps) {
        this.tps = tps;
        this.mspt = 1000/tps;
    }

    public int getTps() {
        return tps;
    }
}
