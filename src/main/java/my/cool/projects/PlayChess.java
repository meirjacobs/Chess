package my.cool.projects;

import java.util.*;

public class PlayChess {
    private static HashMap<Piece, Set<BoardLocation>> piecesToSquares;
    private static HashMap<BoardLocation, Set<Piece>> squaresToPieces;
    private static Piece[][] board;
    private static BoardLocation whiteKingLocation;
    private static BoardLocation blackKingLocation;
    protected static boolean whiteInCheck;
    protected static boolean blackInCheck;
    private Stack<Command> stack;

    public static void main(String[] args) {
        initializeBoard();
        initPTS();
        initSTP();
        Scanner scanner = new Scanner(System.in);
        boolean whiteTurn = true;
        while(true) {
            String move = scanner.next();
            Piece.PieceType pieceType = determinePiece(move);
            boolean capture = determineCapture(move);
            int toRow  = determineMoveToRow(move);
            int toColumn = determineMoveToColumn(move);
            if(pieceType == null || toRow == -1 || toColumn == -1) continue;
            Set<Piece> set = squaresToPieces.get(new BoardLocation(toRow, toColumn));
            if(set.isEmpty()) {
                System.err.printf("Piece cannot move to [%d, %d]\n", toRow, toColumn);
                continue;
            }
            Piece.Color color = whiteTurn ? Piece.Color.WHITE : Piece.Color.BLACK;
            Piece piece = identifyMovePiece(set, pieceType, color, move);
            if(piece == null) continue;
            if(!piece.validMove(board, piece.boardLocation.row, piece.boardLocation.column, toRow, toColumn, capture, true)) continue;
            BoardLocation moveTo = new BoardLocation(toRow, toColumn);
            boolean successful = capture ? capture(piece, moveTo) : move(piece, moveTo);
            if(!successful) continue;
            determineChecks();
            updateMaps();
            printBoard();
            whiteTurn = !whiteTurn;
        }

    }

    protected static int determineMoveToColumn(String move) {
        int column = move.charAt(move.length()-2) - 96;
        if(column < 1 || column > 8) {
            System.err.println((char)column + "is not a valid column");
            return -1;
        }
        return column;
    }

    protected static int determineMoveToRow(String move) {
        int row = move.charAt(move.length()-1) - 48;
        if(row < 1 || row > 8) {
            System.err.println((char)row + "is not a valid row");
            return -1;
        }
        return row;
    }

    protected static String toChessLingo(int row, int column) {
        char[] chars = new char[2];
        chars[0] = (char)(column + 96);
        chars[1] = (char)(row + 48);
        return new String(chars);
    }

    private static boolean determineCapture(String move) {
        for(int i = 1; i < move.length(); i++) {
            if(move.charAt(i) == 'x') {
                return true;
            }
        }
        return false;
    }

    private static Piece.PieceType determinePiece(String move) {
        switch (move.charAt(0)) {
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
                return Piece.PieceType.PAWN;
            case 'B':
                return Piece.PieceType.BISHOP;
            case 'N':
                return Piece.PieceType.KNIGHT;
            case 'R':
                return Piece.PieceType.ROOK;
            case 'Q':
                return Piece.PieceType.QUEEN;
            case 'K':
                return Piece.PieceType.KING;
            default:
                System.err.println(move.charAt(0) + " is not a valid piece");
                return null;
        }
    }

    private static char specifyWhich(String move) {
        if(move.indexOf('x') == -1) {
            if(move.length() <= 3) {
                return 0;
            }
        }
        else {
            if(move.length() <= 4) {
                return 0;
            }
        }
        Set<Character> set = new HashSet<>(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', '1', '2', '3', '4', '5', '6', '7', '8'));
        return set.contains(move.charAt(1)) ? move.charAt(1) : 0;
    }

    private static void determineChecks() {
        for(Piece piece : piecesToSquares.keySet()) {
            BoardLocation kingLocation = (piece.color == Piece.Color.WHITE) ? blackKingLocation : whiteKingLocation;
            if(piece.validMove(board, piece.boardLocation.row, piece.boardLocation.column, kingLocation.row, kingLocation.column, true, false)) {
                if(piece.color == Piece.Color.WHITE) {
                    blackInCheck = true;
                }
                else {
                    whiteInCheck = true;
                }
            }
        }
    }

    private static void initializeBoard() {
        board = new Piece[9][9];
        board[1][1] = new Rook(Piece.Color.WHITE, new BoardLocation(1,1));
        board[1][2] = new Knight(Piece.Color.WHITE, new BoardLocation(1,2));
        board[1][3] = new Bishop(Piece.Color.WHITE, new BoardLocation(1,3));
        board[1][4] = new Queen(Piece.Color.WHITE, new BoardLocation(1,4));
        board[1][5] = new King(Piece.Color.WHITE, new BoardLocation(1,5));
        whiteKingLocation = board[1][5].boardLocation;
        board[1][6] = new Bishop(Piece.Color.WHITE, new BoardLocation(1,6));
        board[1][7] = new Knight(Piece.Color.WHITE, new BoardLocation(1,7));
        board[1][8] = new Rook(Piece.Color.WHITE, new BoardLocation(1,8));
        for(int i = 1; i <= 8; i++) {
            board[2][i] = new Pawn(Piece.Color.WHITE, new BoardLocation(2,i));
        }
        board[8][1] = new Rook(Piece.Color.BLACK, new BoardLocation(8,1));
        board[8][2] = new Knight(Piece.Color.BLACK, new BoardLocation(8,2));
        board[8][3] = new Bishop(Piece.Color.BLACK, new BoardLocation(8,3));
        board[8][4] = new Queen(Piece.Color.BLACK, new BoardLocation(8,4));
        board[8][5] = new King(Piece.Color.BLACK, new BoardLocation(8,5));
        blackKingLocation = board[8][5].boardLocation;
        board[8][6] = new Bishop(Piece.Color.BLACK, new BoardLocation(8,6));
        board[8][7] = new Knight(Piece.Color.BLACK, new BoardLocation(8,7));
        board[8][8] = new Rook(Piece.Color.BLACK, new BoardLocation(8,8));
        for(int i = 1; i <= 8; i++) {
            board[7][i] = new Pawn(Piece.Color.BLACK, new BoardLocation(7,i));
        }
        blackInCheck = false;
        whiteInCheck = false;
    }

    private static void initPTS() {
        piecesToSquares = new HashMap<>();
        int i = 2;
        for(int j = 1; j <= 8; j++) {
            Set<BoardLocation> set = new HashSet<>();
            set.add(new BoardLocation(i+1, j));
            set.add(new BoardLocation(i+2, j));
            PlayChess.piecesToSquares.put(board[i][j], set);
        }
        i = 7;
        for(int j = 1; j <= 8; j++) {
            Set<BoardLocation> set = new HashSet<>();
            set.add(new BoardLocation(i-1, j));
            set.add(new BoardLocation(i-2, j));
            PlayChess.piecesToSquares.put(board[i][j], set);
        }
        Set<BoardLocation> set = new HashSet<>();
        Set<BoardLocation> set1 = new HashSet<>();
        Set<BoardLocation> set2 = new HashSet<>();
        Set<BoardLocation> set3 = new HashSet<>();
        set.add(new BoardLocation(getCoordinateColumn("a3"), getCoordinateRow("a3")));
        set.add(new BoardLocation(getCoordinateColumn("c3"), getCoordinateRow("c3")));
        PlayChess.piecesToSquares.put(board[1][2], set);
        set1.add(new BoardLocation(getCoordinateColumn("f3"), getCoordinateRow("f3")));
        set1.add(new BoardLocation(getCoordinateColumn("h3"), getCoordinateRow("h3")));
        PlayChess.piecesToSquares.put(board[1][7], set1);
        set2.add(new BoardLocation(getCoordinateColumn("a6"), getCoordinateRow("a6")));
        set2.add(new BoardLocation(getCoordinateColumn("c6"), getCoordinateRow("c6")));
        PlayChess.piecesToSquares.put(board[8][2], set2);
        set3.add(new BoardLocation(getCoordinateColumn("f6"), getCoordinateRow("f6")));
        set3.add(new BoardLocation(getCoordinateColumn("h6"), getCoordinateRow("h6")));
        PlayChess.piecesToSquares.put(board[8][7], set3);
        for(i = 1; i <= 2; i++) {
            for(int j = 1; j <= 8; j++) {
                Piece piece = board[i][j];
                if(!piecesToSquares.containsKey(piece)) {
                    piecesToSquares.put(piece, new HashSet<>());
                }
            }
        }
        for(i = 7; i <= 8; i++) {
            for(int j = 1; j <= 8; j++) {
                Piece piece = board[i][j];
                if(!piecesToSquares.containsKey(piece)) {
                    piecesToSquares.put(piece, new HashSet<>());
                }
            }
        }
    }

    private static void initSTP() {
        squaresToPieces = new HashMap<>();
        for(Piece piece : piecesToSquares.keySet()) {
            for(BoardLocation square : piecesToSquares.get(piece)) {
                if(!squaresToPieces.containsKey(square)) {
                    Set<Piece> set = new HashSet<>();
                    set.add(piece);
                    squaresToPieces.put(square, set);
                }
                else {
                    Set<Piece> set = squaresToPieces.get(square);
                    set.add(piece);
                    squaresToPieces.put(square, set);
                }
            }
        }
        for(int i = 1; i <= 8; i++) {
            for(int j = 1; j <= 8; j++) {
                BoardLocation boardLocation = new BoardLocation(i, j);
                if(!squaresToPieces.containsKey(boardLocation)) {
                    squaresToPieces.put(boardLocation, new HashSet<>());
                }
            }
        }
    }

    private static int getCoordinateRow(String chessLingo) {
        return chessLingo.charAt(0) - 96;
    }

    private static int getCoordinateColumn(String chessLingo) {
        return chessLingo.charAt(1) - 48;
    }

    private static Piece identifyMovePiece(Set<Piece> set, Piece.PieceType type, Piece.Color color, String move) {
        int nOfType = 0;
        Piece returnPiece = null;
        for(Piece piece : set) {
            if(piece.pieceType.equals(type) && piece.color.equals(color)) {
                nOfType++;
                returnPiece = piece;
            }
        }
        if(nOfType == 0) {
            System.err.printf("There is no %s that can be moved to the desired square\n", type.toString());
        }
        else if(nOfType >= 2) {
            char which =specifyWhich(move);
            if(which != 0) { // The user properly specified which piece to be moved
                nOfType = 0;
                for(Piece piece : set) {
                    if(piece.boardLocation.chessLingo.indexOf(which) != -1) {
                        nOfType++;
                        returnPiece = piece;
                    }
                }
                if(nOfType == 0) {
                    System.err.println("Incorrectly specified piece. Please correctly identify which piece you want to move.");
                    return null;
                }
                if(nOfType == 1) {
                    return returnPiece;
                }
                nOfType = 0;
                for(Piece piece : set) {
                    if(move.substring(1,2).equals(piece.boardLocation.chessLingo)) {
                        nOfType++;
                        returnPiece = piece;
                    }
                }
                if(nOfType == 1) {
                    return returnPiece;
                }
                System.err.println("There are more than 2 pieces that can move to the given location. Please specify which.");
            }
            System.err.printf("There is more than one %s that can be moved to the desired square\nPlease specify which in your move syntax\n", type.toString());
        }
        return nOfType == 1 ? returnPiece : null;
    }

    private static boolean move(Piece piece, BoardLocation destination) {
        board[piece.boardLocation.row][piece.boardLocation.column] = null;
        board[destination.row][destination.column] = piece;
        piece.boardLocation = destination;
        return true;
    }

    private static boolean capture(Piece piece, BoardLocation destination) {
        Piece temp = board[destination.row][destination.column];
        move(piece, destination);
        piecesToSquares.remove(temp);
        for(BoardLocation boardLocation : squaresToPieces.keySet()) {
            Set<Piece> set = squaresToPieces.get(boardLocation);
            if(set.remove(temp)) squaresToPieces.put(boardLocation, set);
        }
        return true;
    }

    private static void updateMaps() {
        // go through every piece and go through all of it's potential valid squares and if it's valid add it to maps
        for(Piece piece : piecesToSquares.keySet()) {
            Set<BoardLocation> set = piecesToSquares.get(piece);
            set.clear();
            int currentRow = piece.boardLocation.row;
            int currentColumn = piece.boardLocation.column;
            for(int i = 1; i <= 8; i++) {
                for(int j = 1; j <= 8; j++) {
                    if(piece.validMove(board, currentRow, currentColumn, i, j, true, false) || piece.validMove(board, currentRow, currentColumn, i, j, false, false)) {
                        set.add(new BoardLocation(i, j));
                    }
                }
            }
            piecesToSquares.put(piece, set);
            if(piece.pieceType == Piece.PieceType.KING) {
                if(piece.color == Piece.Color.WHITE) {
                    whiteKingLocation = piece.boardLocation;
                }
                else {
                    blackKingLocation = piece.boardLocation;
                }
            }
        }
        for(BoardLocation boardLocation : squaresToPieces.keySet()) {
            Set<Piece> set0 = squaresToPieces.get(boardLocation);
            set0.clear();
            squaresToPieces.put(boardLocation, set0);
        }
        for(Piece piece : piecesToSquares.keySet()) {
            for(BoardLocation square : piecesToSquares.get(piece)) {
                Set<Piece> set = squaresToPieces.get(square);
                if(set == null) set = new HashSet<>();
                set.add(piece);
                squaresToPieces.put(square, set);
            }
        }
    }

    private static void printBoard() {
        for(int i = 8; i >= 1; i--) {
            System.out.print(i + " |");
            for(int j = 1; j <= 8; j++) {
                if(board[i][j] == null) {
                    System.out.print("  |");
                    continue;
                }
                System.out.printf("%2s|", board[i][j].toString());
            }
            System.out.print("\n");
        }
        System.out.println("   a  b  c  d  e  f  g  h");
    }

    private static class Command {
        
    }

}