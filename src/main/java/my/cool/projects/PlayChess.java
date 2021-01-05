package my.cool.projects;

import java.util.*;
import java.util.function.Function;

public class PlayChess {
    private static HashMap<Piece, Set<BoardLocation>> piecesToSquares;
    private static HashMap<BoardLocation, Set<Piece>> squaresToPieces;
    private static Piece[][] board;
    private static BoardLocation whiteKingLocation;
    private static BoardLocation blackKingLocation;
    protected static boolean whiteInCheck;
    protected static boolean blackInCheck;
    private static boolean whiteTurn;
    private static Stack<State> undoStack;
    private static Stack<State> redoStack;
    private static boolean castlingMove;
    private static boolean whiteCanCastle;
    private static boolean blackCanCastle;

    public static void main(String[] args) {
        initializeBoard();
        initPTS();
        initSTP();
        initStacks();
        whiteTurn = true;
        printBoard();
        Scanner scanner = new Scanner(System.in);
        while(true) {
            String move = scanner.next();
            if(move.equals("undo")) {
                undo();
                continue;
            }
            if(move.equals("redo")) {
                redo();
                continue;
            }
            if(!validInput(move)) continue;
            Piece.PieceType pieceType = determinePiece(move);
            boolean capture = determineCapture(move);
            int toRow  = determineMoveToRow(move);
            Piece.Color color = whiteTurn ? Piece.Color.WHITE : Piece.Color.BLACK;
            if(castlingMove) {
                castling(toRow, color);
            }
            else {
                int toColumn = determineMoveToColumn(move);
                if (pieceType == null || toRow == -1 || toColumn == -1) continue;
                Set<Piece> set = squaresToPieces.get(new BoardLocation(toRow, toColumn));
                if (set.isEmpty()) {
                    System.err.printf("Piece cannot move to [%d, %d]\n", toRow, toColumn);
                    continue;
                }
                Piece piece = identifyMovePiece(set, pieceType, color, move);
                if (piece == null) continue;
                if (!validMove(piece, toRow, toColumn, capture, true)) continue;
                BoardLocation moveTo = new BoardLocation(toRow, toColumn);
                Function<Piece, Boolean> successful = capture ? capture(piece, moveTo) : move(piece, moveTo);
                //if(!successful) continue;
            }
            redoStack.clear();
            determineChecks();
            updateMaps();
            printBoard();
            int result = determineCheckMateOrDraw(color);
            if(result == 1) {
                if(checkmate(scanner)) {
                    break;
                }
            }
            else if(result == 0) {
                if(drawByNoAvailableMoves(scanner)) {
                    break;
                }
            }
            whiteTurn = !whiteTurn;
            castlingMove = false;
            State state = new State(piecesToSquares, squaresToPieces, board, whiteKingLocation, blackKingLocation, whiteInCheck, blackInCheck, whiteTurn);
            undoStack.push(state);
            //TODO: upgrade pawns, en passant, draw by repetition, draw by insufficient material
        }
    }

    private static boolean validMove(Piece piece, int moveToRow, int moveToColumn, boolean capture, boolean printErrors) {
        BoardLocation savedLocation = piece.boardLocation;
        if(!piece.validMove(board, piece.boardLocation.row, piece.boardLocation.column, moveToRow, moveToColumn, capture, printErrors)) return false;
        if(piece.color == Piece.Color.WHITE && whiteInCheck || piece.color == Piece.Color.BLACK && blackInCheck) {
            BoardLocation moveTo = new BoardLocation(moveToRow, moveToColumn);
            Function<Piece, Boolean> undo;
            undo = capture ? capture(piece, moveTo) : move(piece, moveTo);
            determineChecks();
            if(piece.color == Piece.Color.WHITE && whiteInCheck || piece.color == Piece.Color.BLACK && blackInCheck) {
                if(printErrors) System.err.println("Illegal move: King is in check");
                piece.boardLocation = savedLocation;
                undo.apply(piece);
                return false;
            }
            piece.boardLocation = savedLocation;
            undo.apply(piece);
            return true;
        }
        return true;
    }

    private static void undo() {
        if(undoStack.size() < 2) {
            System.err.println("Cannot undo any more");
            return;
        }
        redoStack.push(undoStack.pop());
        State state = undoStack.peek();
        returnToState(state);
        printBoard();
    }

    private static void redo() {
        if(redoStack.size() == 0) {
            System.err.println("Cannot redo any more");
            return;
        }
        State state = redoStack.pop();
        returnToState(state);
        undoStack.push(state);
        printBoard();
    }

    private static void returnToState(State state) {
        piecesToSquares = state.piecesToSquares;
        initSTP();
        fillUpBoardFromPTS();
        whiteKingLocation = state.whiteKingLocation;
        blackKingLocation = state.blackKingLocation;
        whiteInCheck = state.whiteInCheck;
        blackInCheck = state.blackInCheck;
        whiteTurn = state.whiteTurn;
    }

    private static void fillUpBoardFromPTS() {
        for(int i = 0; i <= 8; i++) {
            for(int j = 0; j <= 8; j++) {
                board[i][j] = null;
            }
        }
        for(Piece piece : piecesToSquares.keySet()) {
            board[piece.boardLocation.row][piece.boardLocation.column] = piece;
        }
    }

    private static boolean castling(int side, Piece.Color color) {
        if((color == Piece.Color.WHITE && (whiteInCheck || !whiteCanCastle)) || (color == Piece.Color.BLACK && (blackInCheck || !blackCanCastle))) {
            return false;
        }
        if(side != 9 && side != 10) {
            throw new IllegalArgumentException("Something went wrong");
        }
        BoardLocation king = color == Piece.Color.WHITE ? whiteKingLocation : blackKingLocation;
        BoardLocation destination = calculateCastlingDestination(side, color);
        int direction = destination.column > king.column ? 1 : -1;
        for(int currentColumn = direction + king.column; direction <= destination.column; currentColumn += direction) {
            if(board[king.row][currentColumn] != null) {
                return false;
            }
            Set<Piece> pieces = squaresToPieces.get(new BoardLocation(king.row, currentColumn));
            for(Piece piece : pieces) {
                if(piece.color.equals(color)) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    private static BoardLocation calculateCastlingDestination(int side, Piece.Color color) {
        BoardLocation destination;
        if(side == 9) {
            if(color == Piece.Color.WHITE) {
                destination = new BoardLocation("g1");
            }
            else {
                destination = new BoardLocation("g8");
            }
        }
        else {
            if(color == Piece.Color.WHITE) {
                destination = new BoardLocation("c1");
            }
            else {
                destination = new BoardLocation("c8");
            }
        }
        return destination;
    }

    private static boolean checkmate(Scanner scanner) {
        String winner = (whiteTurn) ? "White" : "Black";
        System.out.println("Checkmate! " + winner + " wins!");
        System.out.println("Enter \"undo move\" or \"end game\"");
        String input = scanner.nextLine().trim();
        while(!(input.equals("undo move") || input.equals("end game"))) {
            System.out.println("Enter \"undo move\" or \"end game\"");
            input = scanner.nextLine().trim();
        }
        if(input.equals("undo move")) {
            undo();
            return false;
        }
        return true;
    }

    private static boolean drawByNoAvailableMoves(Scanner scanner) {
        System.out.println("Game drawn by no available moves");
        System.out.println("Enter \"undo move\" or \"end game\"");
        String input = scanner.nextLine().trim();
        while(!(input.equals("undo move") || input.equals("end game"))) {
            System.out.println("Enter \"undo move\" or \"end game\"");
            input = scanner.nextLine().trim();
        }
        if(input.equals("undo move")) {
            undo();
            return false;
        }
        return true;
    }

    private static boolean validInput(String move) {
        if(move.length() < 2 || move.length() > 6) {
            invalidMovePrintln();
            return false;
        }
        return true;
    }

    private static void invalidMovePrintln() {
        System.err.println("Invalid move");
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
        if(move.equals("O-O")) {
            castlingMove = true;
            return 9;
        }
        if(move.equals("O-O-O")) {
            castlingMove = true;
            return 10;
        }
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
            case 'O':
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
        //for(Piece piece : piecesToSquares.keySet()) {
        whiteInCheck = false;
        blackInCheck = false;
        for(int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                Piece piece = board[i][j];
                if(piece == null) continue;
                BoardLocation kingLocation = (piece.color == Piece.Color.WHITE) ? blackKingLocation : whiteKingLocation;
                if (piece.validMove(board, i, j, kingLocation.row, kingLocation.column, true, false)) {
                    if (piece.color == Piece.Color.WHITE) {
                        blackInCheck = true;
                    } else {
                        whiteInCheck = true;
                    }
                }
            }
        }
        //}
    }

    private static int determineCheckMateOrDraw(Piece.Color color) {
        Set<Piece> set = new HashSet<>(piecesToSquares.keySet());
        for(Piece piece : set) {
            if(piece.color == color) {
                continue;
            }
            for(int i = 1; i <= 8; i++) {
                for(int j = 1; j <= 8; j++) {
                    if(validMove(piece, i, j, true, false) || validMove(piece, i, j, false, false)) {
                        return -1;
                    }
                }
            }
        }
        if((color == Piece.Color.WHITE && !blackInCheck) || (color == Piece.Color.BLACK && !whiteInCheck)) {
            return 0;
        }
        return 1;
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
        castlingMove = false;
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

    private static void initStacks() {
        undoStack = new Stack<>();
        redoStack = new Stack<>();
        undoStack.push(new State(piecesToSquares, squaresToPieces, board, whiteKingLocation, blackKingLocation, whiteInCheck, blackInCheck, true));
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

    private static Function<Piece, Boolean> move(Piece piece, BoardLocation destination) {
        BoardLocation savedLocation = new BoardLocation(piece.boardLocation.chessLingo);
        boolean wic = whiteInCheck;
        boolean bic = blackInCheck;
        BoardLocation wkl = new BoardLocation(whiteKingLocation.chessLingo);
        BoardLocation bkl = new BoardLocation(blackKingLocation.chessLingo);
        board[piece.boardLocation.row][piece.boardLocation.column] = null;
        board[destination.row][destination.column] = piece;
        piece.boardLocation = destination;
        if(piece.pieceType.equals(Piece.PieceType.KING)) {
            if(piece.color.equals(Piece.Color.WHITE)) {
                whiteKingLocation = destination;
            }
            else {
                blackKingLocation = destination;
            }
        }
        return piece1 -> {
            piece.boardLocation = savedLocation;
            board[savedLocation.row][savedLocation.column] = piece;
            board[destination.row][destination.column] = null;
            whiteInCheck = wic;
            blackInCheck = bic;
            whiteKingLocation = wkl;
            blackKingLocation = bkl;
            return true;
        };
    }

    private static Function<Piece, Boolean> capture(Piece piece, BoardLocation destination) {
        BoardLocation savedLocation = new BoardLocation(piece.boardLocation.chessLingo);
        Piece temp = board[destination.row][destination.column];
        boolean wic = whiteInCheck;
        boolean bic = blackInCheck;
        BoardLocation wkl = new BoardLocation(whiteKingLocation.chessLingo);
        BoardLocation bkl = new BoardLocation(blackKingLocation.chessLingo);
        Set<BoardLocation> saveTempSTP = new HashSet<>();
        Set<BoardLocation> tempPTS = piecesToSquares.get(temp);
        board[piece.boardLocation.row][piece.boardLocation.column] = null;
        board[destination.row][destination.column] = piece;
        piece.boardLocation = destination;
        if(piece.pieceType.equals(Piece.PieceType.KING)) {
            if(piece.color.equals(Piece.Color.WHITE)) {
                whiteKingLocation = destination;
            }
            else {
                blackKingLocation = destination;
            }
        }
        piecesToSquares.remove(temp);
        for(BoardLocation boardLocation : squaresToPieces.keySet()) {
            Set<Piece> set = squaresToPieces.get(boardLocation);
            if(set.contains(temp)) {
                saveTempSTP.add(boardLocation);
                set.remove(temp);
                squaresToPieces.put(boardLocation, set);
            }
        }
        return piece1 -> {
            piece.boardLocation = savedLocation;
            board[savedLocation.row][savedLocation.column] = piece;
            board[destination.row][destination.column] = temp;
            piecesToSquares.put(temp, tempPTS);
            for(BoardLocation boardLocation : saveTempSTP) {
                Set<Piece> pieces = squaresToPieces.get(boardLocation);
                pieces.add(temp);
                squaresToPieces.put(boardLocation, pieces);
            }
            whiteInCheck = wic;
            blackInCheck = bic;
            whiteKingLocation = wkl;
            blackKingLocation = bkl;
            return true;
        };
    }

    private static void replaceOnAllMaps(Piece piece, Piece current) {
        piecesToSquares.remove(piece);
        Set<BoardLocation> set = new HashSet<>();
        int currentRow = current.boardLocation.row;
        int currentColumn = current.boardLocation.column;
        for(int i = 1; i <= 8; i++) {
            for(int j = 1; j <= 8; j++) {
                if(validMove(current, i, j, true, false) || validMove(current, i, j, false, false)) {
                    set.add(new BoardLocation(i, j));
                }
            }
        }
        piecesToSquares.put(current, set);
        if(current.pieceType == Piece.PieceType.KING) {
            if(current.color == Piece.Color.WHITE) {
                whiteKingLocation = current.boardLocation;
            }
            else {
                blackKingLocation = current.boardLocation;
            }
        }
        for(BoardLocation boardLocation : squaresToPieces.keySet()) {
            Set<Piece> set1 = squaresToPieces.get(boardLocation);
            if(set1.remove(piece)) squaresToPieces.put(boardLocation, set1);
        }
        for(BoardLocation boardLocation : set) {
            Set<Piece> set2 = squaresToPieces.get(boardLocation);
            set2.add(current);
        }
    }

    private static void updateMaps() {
        // go through every piece and go through all of it's potential valid squares and if it's valid add it to maps
        //HashMap<Piece, Set<BoardLocation>> tempMap = new HashMap<>(piecesToSquares);
        HashSet<Piece> tempSet = new HashSet<>(piecesToSquares.keySet());
        for(Piece piece : tempSet) {
            Set<BoardLocation> set = piecesToSquares.get(piece);
            if(set == null) {
                System.out.println(piece.toString());
            }
            set.clear();
            int currentRow = piece.boardLocation.row;
            int currentColumn = piece.boardLocation.column;
            for(int i = 1; i <= 8; i++) {
                for(int j = 1; j <= 8; j++) {
                    //if(validMove(piece, i, j, true, false) || validMove(piece, i, j, false, false)) {
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
                System.out.printf("%2s|", board[i][j].shorthand());
            }
            System.out.print("\n");
        }
        System.out.println("   a  b  c  d  e  f  g  h");
    }

}