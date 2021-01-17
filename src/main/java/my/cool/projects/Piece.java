package my.cool.projects;

import java.util.Objects;

public abstract class Piece {
    Color color;
    BoardLocation boardLocation;
    PieceType pieceType;

    public Piece(Color color, BoardLocation boardLocation) {
        switch (color) {
            case WHITE:
            case BLACK:
                this.color = color;
                break;
            default:
                throw new IllegalArgumentException("Invalid piece color");
        }
        this.boardLocation = boardLocation;
    }

    public enum PieceType {PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING};

    public enum Color {WHITE, BLACK};

    public abstract boolean validMove(Piece[][] board, int currentRow, int currentColumn, int moveToRow, int moveToColumn, boolean capture, boolean printErrors);

    protected boolean validateInput(Piece[][] board, int currentRow, int currentColumn, int moveToRow, int moveToColumn) {
        if(board == null) {
            System.err.println("Null board input");
            return false;
        }
        if(board.length != 9 || board[1].length != 9) {
            System.err.println("Board must be 9 x 9");
            return false;
        }
        if(currentRow < 1 || currentRow > 8 || currentColumn < 1 || currentColumn > 8 ||moveToRow < 1 || moveToRow > 8 || moveToColumn < 1 || moveToColumn > 8) {
            System.err.println("Invalid current or intended location input. Must be between 1 and 8");
            return false;
        }
        if(board[currentRow][currentColumn] == null) {
            System.err.println("No piece at [" + currentRow + ", " + currentColumn + "]");
            return false;
        }
        return true;
    }

    protected String shorthand() {
        return toString().substring(0, 2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Piece piece = (Piece) o;
        return color == piece.color &&
                boardLocation.equals(piece.boardLocation) &&
                pieceType.equals(piece.pieceType);
    }

    @Override
    public int hashCode() {
        return ((Objects.hash(color, boardLocation, pieceType) * 11) + 5) % 31;
    }

    @Override
    public String toString() {
        String postfix = (color == Color.WHITE) ? "w" : "b";
        String type;
        switch (pieceType) {
            case KING:
                type = "K";
                break;
            case PAWN:
                type = "P";
                break;
            case ROOK:
                type = "R";
                break;
            case QUEEN:
                type = "Q";
                break;
            case BISHOP:
                type = "B";
                break;
            case KNIGHT:
                type = "N";
                break;
            default:
                throw new IllegalArgumentException("Illegal piece type");
        }
        return type + postfix + boardLocation.chessLingo;
    }
}