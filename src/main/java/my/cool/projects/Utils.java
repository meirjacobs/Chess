package my.cool.projects;

public class Utils {
    public static String columnToLetter(int column) {
        return Character.toString((char) ('a' + column - 1));
    }

    public static int letterToColumn(char c) {
        return c - 'a' + 1;
    }

    public static String pieceToAbbreviation(Piece.PieceType type) {
        return switch (type) {
            case PAWN -> "";
            case KNIGHT -> "N";
            case BISHOP -> "B";
            case ROOK -> "R";
            case QUEEN -> "Q";
            case KING -> "K";
        };
    }
}
