package my.cool.projects;

public class Utils {
    public static String columnToLetter(int column) {
        return Character.toString((char) ('a' + column - 1));
    }

    public static int letterToColumn(char c) {
        return c - 'a' + 1;
    }
}
