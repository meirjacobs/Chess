package my.cool.projects;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.function.Function;

public class PlayChess {
    private static HashMap<Piece, Set<BoardLocation>> piecesToSquares;
    private static HashMap<BoardLocation, Set<Piece>> squaresToPieces;
    private static BoardMap boardMap;
    private static Piece[][] board;
    private static BoardLocation whiteKingLocation;
    private static BoardLocation blackKingLocation;
    protected static boolean whiteInCheck;
    protected static boolean blackInCheck;
    private static boolean whiteTurn;
    private static Stack<State> undoStack;
    private static Stack<State> redoStack;
    private static Stack<String> gameLog;
    private static boolean castlingMove;
    private static boolean whiteCanCastle;
    private static boolean blackCanCastle;
    private static Piece.PieceType upgradePawnTo;
    private static Piece[][] lastBoardInBoardMap;

    public static void main(String[] args) throws FileNotFoundException {
        initializeBoard();
        initPTS();
        initSTP();
        initStacks();
        printBoard();
        Scanner scanner;
        if(args.length == 0) {
            scanner = new Scanner(System.in);
        }
        else {
            File file = new File(args[0]);
            scanner = new Scanner(file);
        }
        help();
        while(true) {
            upgradePawnTo = null;
            if(whiteTurn) System.out.println("\nWhite Turn: ");
            else System.out.println("\nBlack Turn: ");
            if(!scanner.hasNext()) break;
            String move = scanner.next();
            if(move.equals("undo")) {
                undo();
                continue;
            }
            if(move.equals("redo")) {
                redo();
                continue;
            }
            if(move.equals("help")) {
                help();
                continue;
            }
            if(move.equals("log")) {
                gameLog();
                continue;
            }
            if(!validInput(move)) continue;
            Piece.PieceType pieceType = determinePiece(move);
            boolean capture = determineCapture(move);
            int toRow  = determineMoveToRow(move);
            Piece.Color color = whiteTurn ? Piece.Color.WHITE : Piece.Color.BLACK;
            if(castlingMove) {
                if(!castling(toRow, color)) {
                    System.err.println("Castling unsuccessful");
                    castlingMove = false;
                    continue;
                }
            }
            else {
                int toColumn = determineMoveToColumn(move, false);
                if (pieceType == null || toRow == -1 || toColumn == -1) continue;
                if(determineEnPassant(pieceType, toRow, toColumn)) {
                    if(!enPassant(move, toRow, toColumn)) {
                        continue;
                    }
                }
                else {
                    Set<Piece> set = squaresToPieces.get(new BoardLocation(toRow, toColumn));
                    if (set.isEmpty()) {
                        System.err.printf("Piece cannot move to [%d, %d]\n", toRow, toColumn);
                        continue;
                    }
                    Piece piece = identifyMovePiece(set, pieceType, color, move);
                    if (piece == null) continue;
                    if (!validMove(piece, toRow, toColumn, capture, true)) continue;
                    BoardLocation moveTo = new BoardLocation(toRow, toColumn);
                    if (capture) {
                        capture(piece, moveTo);
                    } else {
                        move(piece, moveTo);
                    }
                }
            }
            redoStack.clear();
            determineChecks();
            updateMaps();
            printBoard();
            int checkMateOrDraw = determineCheckMateOrDraw(color);
            boolean insufficientMaterial = determineDrawByInsufficientMaterial();
            whiteTurn = !whiteTurn;
            castlingMove = false;
            pushState(move);
            if(determineDrawByRepetition()) {
                if(drawByRepetition(scanner)) {
                    break;
                }
            }
            if(checkMateOrDraw == 1) {
                if(checkmate(scanner)) {
                    break;
                }
            }
            else if(checkMateOrDraw == 0) {
                if(drawByNoAvailableMoves(scanner)) {
                    break;
                }
            }
            else if(insufficientMaterial) {
                if(drawByInsufficientMaterial(scanner)) {
                    break;
                }
            }
        }
    }

    private static boolean validMove(Piece piece, int moveToRow, int moveToColumn, boolean capture, boolean printErrors) {
        BoardLocation savedLocation = piece.boardLocation;
        if(!piece.validMove(board, piece.boardLocation.row, piece.boardLocation.column, moveToRow, moveToColumn, capture, printErrors)) return false;
        if(upgradePawnTo == null && piece.pieceType == Piece.PieceType.PAWN && ((piece.color.equals(Piece.Color.WHITE) && moveToRow == 8) || piece.color.equals(Piece.Color.BLACK) && moveToRow == 1)) {
            if(printErrors) System.err.println("Must specify which piece you're upgrading the pawn to. Use notation e.g. \"g8=Q\" or \"gxh8=Q\"");
            return false;
        }
        //if(piece.color == Piece.Color.WHITE && whiteInCheck || piece.color == Piece.Color.BLACK && blackInCheck) {
        BoardLocation moveTo = new BoardLocation(moveToRow, moveToColumn);
        Function<Piece, Boolean> undo;
        undo = capture ? capture(piece, moveTo) : move(piece, moveTo);
        determineChecks();
        if(piece.color == Piece.Color.WHITE && whiteInCheck || piece.color == Piece.Color.BLACK && blackInCheck) {
            if(printErrors) {
                if(piece.pieceType.equals(Piece.PieceType.KING)) System.err.println("Illegal move: King cannot move there, it would be putting itself into check");
                else System.err.println("Illegal move: King is in check");
            }
            piece.boardLocation = savedLocation;
            undo.apply(piece);
            return false;
        }
        piece.boardLocation = savedLocation;
        undo.apply(piece);
        return true;
        //}
        /*if(piece.pieceType.equals(Piece.PieceType.KING)) {
            BoardLocation moveTo = new BoardLocation(moveToRow, moveToColumn);
            Function<Piece, Boolean> undo;
            undo = capture ? capture(piece, moveTo) : move(piece, moveTo);
            determineChecks();
            if(piece.color == Piece.Color.WHITE && whiteInCheck || piece.color == Piece.Color.BLACK && blackInCheck) {
                if(printErrors) System.err.println("Illegal move: King cannot move there, it would be putting itself into check");
                piece.boardLocation = savedLocation;
                undo.apply(piece);
                return false;
            }
            piece.boardLocation = savedLocation;
            undo.apply(piece);
            return true;
        }
        return true;*/
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
        copyPTS(state.piecesToSquares);
        initSTP();
        fillUpBoardFromPTS();
        whiteKingLocation = state.whiteKingLocation;
        blackKingLocation = state.blackKingLocation;
        whiteInCheck = state.whiteInCheck;
        blackInCheck = state.blackInCheck;
        whiteTurn = state.whiteTurn;
        Piece[][] deepEquals = boardMap.getKeyDeepEquals(state.board);
        boardMap.put(deepEquals, boardMap.get(deepEquals) - 1);
    }

    private static void copyPTS(HashMap<Piece, Set<BoardLocation>> statePTS) {
        piecesToSquares.clear();
        for(Piece piece : statePTS.keySet()) {
            Set<BoardLocation> boardLocations = statePTS.get(piece);
            Set<BoardLocation> newSet = new HashSet<>();
            for(BoardLocation boardLocation : boardLocations) {
                newSet.add(new BoardLocation(boardLocation.chessLingo));
            }
            piecesToSquares.put(copyPiece(piece), newSet);
        }
    }

    private static Piece copyPiece(Piece piece) {
        if(piece instanceof Bishop) {
            return new Bishop(piece.color, piece.boardLocation);
        }
        if(piece instanceof King) {
            return new King(piece.color, piece.boardLocation);
        }
        if(piece instanceof Knight) {
            return new Knight(piece.color, piece.boardLocation);
        }
        if(piece instanceof Pawn) {
            return new Pawn(piece.color, piece.boardLocation);
        }
        if(piece instanceof Queen) {
            return new Queen(piece.color, piece.boardLocation);
        }
        if(piece instanceof Rook) {
            return new Rook(piece.color, piece.boardLocation);
        }
        return null;
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
        if(side != 9 && side != 10) {// Take this part out after you know it works
            throw new IllegalArgumentException("Something went wrong");
        }
        BoardLocation king = color == Piece.Color.WHITE ? whiteKingLocation : blackKingLocation;
        BoardLocation destination = calculateCastlingDestination(side, color);
        Piece castlingRook = determineCastlingRook(destination);
        if(castlingRook == null) return false;
        BoardLocation rookDestination = determineCastlingRookDestination(castlingRook);
        int direction = destination.column > king.column ? 1 : -1;
        int count = 0;
        int currentColumn;
        for(currentColumn = direction + king.column; count < 2; currentColumn += direction) {
            if(board[king.row][currentColumn] != null) {
                return false;
            }
            Set<Piece> pieces = squaresToPieces.get(new BoardLocation(king.row, currentColumn));
            for(Piece piece : pieces) {
                if(!piece.color.equals(color)) {
                    return false;
                }
            }
            count++;
        }
        //currentColumn += direction;
        if(currentColumn != castlingRook.boardLocation.column && board[king.row][currentColumn] != null) {
            return false;
        }
        Piece theKing = board[king.row][king.column];
        board[destination.row][destination.column] = theKing;
        theKing.boardLocation = destination;
        board[king.row][king.column] = null;
        board[castlingRook.boardLocation.row][castlingRook.boardLocation.column] = null;
        board[rookDestination.row][rookDestination.column] = castlingRook;
        castlingRook.boardLocation = rookDestination;
        updateKingLocation(theKing);
        return true;
    }

    private static void updateKingLocation(Piece king) {
        if(king.color.equals(Piece.Color.WHITE)) {
            whiteKingLocation = king.boardLocation;
        }
        else {
            blackKingLocation = king.boardLocation;
        }
    }

    private static BoardLocation calculateCastlingDestination(int side, Piece.Color color) {
        return side == 9 ? color == Piece.Color.WHITE ? new BoardLocation("g1") : new BoardLocation("g8") : color == Piece.Color.WHITE ? new BoardLocation("c1") : new BoardLocation("c8");
    }

    private static Piece determineCastlingRook(BoardLocation destination) {
        if(destination.row == 1) {
            if(destination.column == 7) {
                return board[1][8];
            }
            if(destination.column == 3) {
                return board[1][1];
            }
        }
        if(destination.row == 8) {
            if(destination.column == 7) {
                return board[8][8];
            }
            if(destination.column == 3) {
                return board[8][1];
            }
        }
        return null;
    }

    private static BoardLocation determineCastlingRookDestination(Piece rook) {
        if(rook.boardLocation.row == 1) {
            if(rook.boardLocation.column == 1) {
                return new BoardLocation("d1");
            }
            else {
                return new BoardLocation("f1");
            }
        }
        else {
            if(rook.boardLocation.column == 1) {
                return new BoardLocation("d8");
            }
            else {
                return new BoardLocation("f8");
            }
        }
    }

    private static boolean determineEnPassant(Piece.PieceType pieceType, int moveToRow, int moveToColumn) {
        if(!pieceType.equals(Piece.PieceType.PAWN) || !((whiteTurn && moveToRow == 6) || (!whiteTurn && moveToRow == 3)) || board[moveToRow][moveToColumn] != null) return false;
        if(undoStack.size() < 2) return false;
        redoStack.push(undoStack.pop());
        Piece[][] previousBoard = undoStack.peek().board;
        undoStack.push(redoStack.pop());
        int rowOfOpposingPawnPreviousLocation;
        int rowOfOpposingPawnCurrentLocation;
        if(whiteTurn) {
            rowOfOpposingPawnPreviousLocation = 7;
            rowOfOpposingPawnCurrentLocation = 5;
        }
        else {
            rowOfOpposingPawnPreviousLocation = 2;
            rowOfOpposingPawnCurrentLocation = 4;
        }
        Piece prev = previousBoard[rowOfOpposingPawnPreviousLocation][moveToColumn];
        Piece curr = board[rowOfOpposingPawnCurrentLocation][moveToColumn];
        if(prev == null || curr == null) return false;
        if(!(prev.pieceType == Piece.PieceType.PAWN && curr.pieceType == Piece.PieceType.PAWN)) return false;
        if(whiteTurn) {
            return prev.color != Piece.Color.WHITE && curr.color != Piece.Color.WHITE;
        }
        else {
            return prev.color != Piece.Color.BLACK && curr.color != Piece.Color.BLACK;
        }
    }

    private static boolean enPassant(String move, int moveToRow, int moveToColumn) { // REMEMBER TO PUT IN SUPPORT FOR NOT BEING ABLE TO EN PASSANT A PINNED PIECE
        int rowOfPieceBeingCaptured = (whiteTurn) ? 5 : 4;
        int currentColumn;
        switch (move.charAt(0)) {
            case 'a':
                currentColumn = 1;
                break;
            case 'b':
                currentColumn = 2;
                break;
            case 'c':
                currentColumn = 3;
                break;
            case 'd':
                currentColumn = 4;
                break;
            case 'e':
                currentColumn = 5;
                break;
            case 'f':
                currentColumn = 6;
                break;
            case 'g':
                currentColumn = 7;
                break;
            case 'h':
                currentColumn = 8;
                break;
            default:
                System.err.println("Column '" + move.charAt(0) + "' is out of range");
                return false;
        }
        Piece pawn = board[rowOfPieceBeingCaptured][currentColumn];
        Piece capturedPiece = board[rowOfPieceBeingCaptured][moveToColumn];

        // Undo data structures
        BoardLocation savedLocation = new BoardLocation(pawn.boardLocation.chessLingo);
        boolean wic = whiteInCheck;
        boolean bic = blackInCheck;
        Set<BoardLocation> saveTempSTP = new HashSet<>();
        Set<BoardLocation> tempPTS = piecesToSquares.get(capturedPiece);
        //Piece oldPiece = pawn;

        board[rowOfPieceBeingCaptured][currentColumn] = null;
        board[moveToRow][moveToColumn] = pawn;
        pawn.boardLocation = new BoardLocation(moveToRow, moveToColumn);
        board[rowOfPieceBeingCaptured][moveToColumn] = null;
        piecesToSquares.remove(capturedPiece);
        for(BoardLocation boardLocation : squaresToPieces.keySet()) {
            Set<Piece> set = squaresToPieces.get(boardLocation);
            if(set.contains(capturedPiece)) {
                saveTempSTP.add(boardLocation);
                set.remove(capturedPiece);
                squaresToPieces.put(boardLocation, set);
            }
        }

        Function<Piece, Boolean> undo = oldPiece -> {
            oldPiece.boardLocation = savedLocation;
            board[savedLocation.row][savedLocation.column] = oldPiece;
            board[rowOfPieceBeingCaptured][moveToColumn] = capturedPiece;
            board[moveToRow][moveToColumn] = null;
            piecesToSquares.put(capturedPiece, tempPTS);
            for(BoardLocation boardLocation : saveTempSTP) {
                Set<Piece> pieces = squaresToPieces.get(boardLocation);
                pieces.add(capturedPiece);
                squaresToPieces.put(boardLocation, pieces);
            }
            whiteInCheck = wic;
            blackInCheck = bic;
            return true;
        };
        determineChecks();
        if((whiteTurn && whiteInCheck) || (!whiteTurn && blackInCheck)) {
            System.err.println("Cannot do en passant because it would put your king in check");
            undo.apply(pawn);
            return false;
        }
        return true;
    }

    private static boolean checkmate(Scanner scanner) {
        String winner = (whiteTurn) ? "White" : "Black";
        System.out.println("Checkmate! " + winner + " wins!");
        String input = scanner.nextLine().trim();
        while(!(input.equals("undo") || input.equals("end game"))) {
            System.out.println("Enter \"undo\" or \"end game\"");
            input = scanner.nextLine().trim();
        }
        if(input.equals("undo")) {
            undo();
            return false;
        }
        return true;
    }

    private static boolean drawByNoAvailableMoves(Scanner scanner) {
        System.out.println("Game drawn by no available moves");
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

    protected static int determineMoveToColumn(String move, boolean chessLingo) {
        if(upgradePawnTo != null && !chessLingo) {
            int column = move.charAt(move.length()-4) - 96;
            if(column < 1 || column > 8) {
                System.err.println((char)column + "is not a valid column");
                return -1;
            }
            return column;
        }
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
        for(int i = 0; i < move.length(); i++) {
            if(move.charAt(i) == '=') {
                int row = move.charAt(move.length()-3) - 48;
                upgradePawnTo = upgradePawnPieceType(move);
                if(upgradePawnTo == null) return -1;
                if(row < 1 || row > 8) {
                    System.err.println((char)row + "is not a valid row");
                    return -1;
                }
                return row;
            }
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

    private static char specifyWhich(String move, Piece.PieceType type) {
        if(type.equals(Piece.PieceType.PAWN)) return move.charAt(0);
        if(move.indexOf('x') == -1) { // if it's not a capture
            if(move.length() <= 3) {
                return 0; // indeterminate
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

    private static Piece.PieceType upgradePawnPieceType(String move) {
        switch (move.charAt(move.length() - 1)) {
            case 'Q':
                return Piece.PieceType.QUEEN;
            case 'N':
                return Piece.PieceType.KNIGHT;
            case 'B':
                return Piece.PieceType.BISHOP;
            case 'R':
                return Piece.PieceType.ROOK;
            default:
                System.err.println("Invalid type of piece to upgrade the pawn to");
                return null;
        }
    }

    private static void determineChecks() {
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

    private static boolean determineDrawByInsufficientMaterial() {
        Set<Piece> set = new HashSet<>(piecesToSquares.keySet());
        for(Piece piece : set) {
            switch (piece.pieceType) {
                case PAWN:
                case ROOK:
                case QUEEN:
                    return false;
                default:
            }
        }
        return true;
    }

    private static boolean drawByInsufficientMaterial(Scanner scanner) {
        System.out.println("Game drawn by insufficient material");
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

    private static boolean determineDrawByRepetition() {
        return boardMap.get(lastBoardInBoardMap) > 2;
    }

    private static boolean drawByRepetition(Scanner scanner) {
        System.out.println("Game drawn by repetition");
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
        whiteCanCastle = true;
        blackCanCastle = true;
        upgradePawnTo = null;
        whiteTurn = true;
        boardMap = new BoardMap();
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
        gameLog = new Stack<>();
        pushState(null);
    }

    private static void pushState(String move) {
        State state;
        Piece[][] matchingBoard = boardMap.getKeyDeepEquals(board);
        if(matchingBoard == null) {
            state = new State(piecesToSquares, squaresToPieces, board, whiteKingLocation, blackKingLocation, whiteInCheck, blackInCheck, whiteTurn, move);
            boardMap.put(state.board, 1);
            lastBoardInBoardMap = state.board;
        }
        else {
            state = new State(piecesToSquares, squaresToPieces, matchingBoard, whiteKingLocation, blackKingLocation, whiteInCheck, blackInCheck, whiteTurn, move);
            int got = boardMap.get(matchingBoard);
            boardMap.put(matchingBoard, got + 1);
            lastBoardInBoardMap = matchingBoard;
        }
        undoStack.push(state);
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
            char which = specifyWhich(move, type);
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
        HashSet<BoardLocation> saveTempSTP = new HashSet<>();
        Set<BoardLocation> tempPTS = null;
        BoardLocation wkl = new BoardLocation(whiteKingLocation.chessLingo);
        BoardLocation bkl = new BoardLocation(blackKingLocation.chessLingo);
        Piece oldPiece = piece;
        if(upgradePawnTo != null) {
            Piece upgrade = upgradePawn(piece, destination);
            if(upgrade != null) {
                tempPTS = piecesToSquares.get(oldPiece);
                piecesToSquares.remove(piece);
                for(BoardLocation boardLocation : squaresToPieces.keySet()) {
                    Set<Piece> set = squaresToPieces.get(boardLocation);
                    if(set.contains(piece)) {
                        saveTempSTP.add(boardLocation);
                        set.remove(piece);
                        squaresToPieces.put(boardLocation, set);
                    }
                }
                piece = upgrade;
                piecesToSquares.put(piece, new HashSet<>());
            }
        }
        board[oldPiece.boardLocation.row][oldPiece.boardLocation.column] = null;
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
        Set<BoardLocation> finalTempPTS = tempPTS;
        return piece1 -> {
            oldPiece.boardLocation = savedLocation;
            board[savedLocation.row][savedLocation.column] = oldPiece;
            board[destination.row][destination.column] = null;
            if(finalTempPTS != null) {
                piecesToSquares.put(oldPiece, finalTempPTS);
                for(BoardLocation boardLocation : saveTempSTP) {
                    Set<Piece> pieces = squaresToPieces.get(boardLocation);
                    pieces.add(oldPiece);
                    squaresToPieces.put(boardLocation, pieces);
                }
            }
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
        Set<BoardLocation> savePawnsSTP = new HashSet<>();
        Set<BoardLocation> pawnsPTS = null;
        Piece oldPiece = piece;
        if(upgradePawnTo != null) {
            Piece upgrade = upgradePawn(piece, destination);
            if(upgrade != null) {
                pawnsPTS = piecesToSquares.get(oldPiece);
                piecesToSquares.remove(piece);
                for(BoardLocation boardLocation : squaresToPieces.keySet()) {
                    Set<Piece> set = squaresToPieces.get(boardLocation);
                    if(set.contains(piece)) {
                        savePawnsSTP.add(boardLocation);
                        set.remove(piece);
                        squaresToPieces.put(boardLocation, set);
                    }
                }
                piece = upgrade;
                piecesToSquares.put(piece, new HashSet<>());
            }
        }
        board[oldPiece.boardLocation.row][oldPiece.boardLocation.column] = null;
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
        Set<BoardLocation> finalPawnsPTS = pawnsPTS;
        return piece1 -> {
            oldPiece.boardLocation = savedLocation;
            board[savedLocation.row][savedLocation.column] = oldPiece;
            board[destination.row][destination.column] = temp;
            piecesToSquares.put(temp, tempPTS);
            for(BoardLocation boardLocation : saveTempSTP) {
                Set<Piece> pieces = squaresToPieces.get(boardLocation);
                pieces.add(temp);
                squaresToPieces.put(boardLocation, pieces);
            }
            if(finalPawnsPTS != null) {
                piecesToSquares.put(oldPiece, finalPawnsPTS);
                for(BoardLocation boardLocation : savePawnsSTP) {
                    Set<Piece> pieces = squaresToPieces.get(boardLocation);
                    pieces.add(oldPiece);
                    squaresToPieces.put(boardLocation, pieces);
                }
            }
            whiteInCheck = wic;
            blackInCheck = bic;
            whiteKingLocation = wkl;
            blackKingLocation = bkl;
            return true;
        };
    }

    private static Piece upgradePawn(Piece pawn, BoardLocation destination) {
        if(!(pawn.pieceType.equals(Piece.PieceType.PAWN) && ((pawn.color.equals(Piece.Color.WHITE) && destination.row == 8) || (pawn.color.equals(Piece.Color.BLACK) && destination.row == 1)))) {
            return null;
        }
        switch (upgradePawnTo) {
            case QUEEN:
                return new Queen(pawn.color, destination);
            case KNIGHT:
                return new Knight(pawn.color, destination);
            case BISHOP:
                return new Bishop(pawn.color, destination);
            case ROOK:
                return new Rook(pawn.color, destination);
            default:
                return null;
        }
    }

    private static void updateMaps() {
        // go through every piece and go through all of it's potential valid squares and if it's valid add it to maps
        //HashMap<Piece, Set<BoardLocation>> tempMap = new HashMap<>(piecesToSquares);
        HashSet<Piece> tempSet = new HashSet<>(piecesToSquares.keySet());
        for(Piece piece : tempSet) {
            Set<BoardLocation> set = piecesToSquares.get(piece);
            if(set == null) {
                set = new HashSet<>();
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
                    //String s = "\u2009";
                    System.out.print("\u3000" + "|");
                    continue;
                }
                System.out.printf("%1s|", board[i][j].shorthand());
            }
            System.out.print("\n");
        }
        System.out.println("   a  b  c  d  e  f  g  h");
    }

    private static void help() {
        System.out.println("Write move in chess notation (e.g. \"e4\", \"Bc6\", \"Qxa8\", \"O-O\") and hit enter.\n" +
                "Do not use \"+\" for check or \"++\" for checkmate.\n" +
                "Write \"undo\" to undo move or \"redo\" to redo move.\n" +
                "Write \"log\" to print the game log." +
                "Write \"help\" to repeat these instructions at any time."
        );
    }

    private static void gameLog() {
        System.out.println("\nGAME LOG:");
        if(undoStack.size() <= 1) {
            System.out.println("No moves yet");
            return;
        }
        System.out.println("\n    | White | Black\n---------------------------");
        Iterator<State> iterator = undoStack.iterator();
        boolean white = true;
        int nTurn = 1;
        while(iterator.hasNext()) {
            String move = iterator.next().lastMove;
            if(move == null) continue;
            if(white) {
                System.out.printf("%3d |%6s | ", nTurn, move);
            }
            else {
                System.out.println(move);
                nTurn++;
            }
            white = !white;
        }
        System.out.println("");
    }

    private static class BoardMap extends HashMap<Piece[][], Integer> {

        public Piece[][] getKeyDeepEquals(Object key) {
            if(!(key instanceof Piece[][])) {
                return null;
            }
            Piece[][] board = (Piece[][]) key;
            Piece[][] match;
            for(Piece[][] boardInMap : keySet()) {
                match = (Arrays.deepEquals(boardInMap, board)) ? boardInMap : null;
                if(match != null) {
                    return match;
                }
            }
            return null;
        }
    }

}