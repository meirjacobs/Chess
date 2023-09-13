package my.cool.projects;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.function.Function;

public class PlayChess {
    private static HashMap<Piece, Set<BoardLocation>> piecesToSquares;
    private static HashMap<BoardLocation, Set<Piece>> squaresToPieces;
    private static BoardMap boardMap;
    public static Board board;
    private static BoardLocation whiteKingLocation;
    private static BoardLocation blackKingLocation;
    protected static boolean whiteInCheck;
    protected static boolean blackInCheck;
    private static boolean whiteTurn;
    private static Stack<State> undoStack;
    private static Stack<State> redoStack;
    private static boolean castlingMove;
    private static Piece.PieceType upgradePawnTo;
    private static Piece[][] lastBoardInBoardMap;
    private static Scanner scanner;
    private static int score;
    private static Piece.Color computerPlayAs;
    private static int computerDepth;

    public static void main(String[] args) throws FileNotFoundException {
        initializeBoard();
        initPTS();
        initSTP();
        initStacks();
        printBoard(true);
        if(args.length == 0) {
            scanner = new Scanner(System.in);
        }
        else {
            File file = new File(args[0]);
            scanner = new Scanner(file);
        }
        boolean multiplayer = gameType();
        help();
        if(multiplayer) multiplayerGame();
        else {
            computerOptions();
            computerGame();
        }
    }

    private static boolean gameType() {
        while(true) {
            System.out.println("Write \"Multiplayer\" for multiplayer game\n" +
                                "Write \"Computer\" to play against computer");
            String selection = scanner.next();
            if(selection.equalsIgnoreCase("multiplayer")) return true;
            else if(selection.equalsIgnoreCase("computer")) return false;
        }
    }

    private static void computerOptions() {
        while(true) {
            System.out.println("Would you like to play as white or black?");
            String selection = scanner.next();
            if(selection.equalsIgnoreCase("white")) {
                computerPlayAs = Piece.Color.BLACK;
                break;
            }
            else if(selection.equalsIgnoreCase("black")) {
                computerPlayAs = Piece.Color.WHITE;
                break;
            }
        }
        System.out.println("Set computer depth");
        try {
            computerDepth = Integer.parseInt(scanner.next());
        } catch (Exception e) {
            computerDepth = 4;
        }
    }

    private static void multiplayerGame() {
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
            BoardLocation originalLocation = null;
            if(castlingMove) {
                if(!castling(toRow, color)) {
                    System.err.println("You cannot castle in this position");
                    castlingMove = false;
                    continue;
                }
            }
            else {
                int toColumn = determineMoveToColumn(move, false);
                if (pieceType == null || toRow == -1 || toColumn == -1) continue;
                if(determineEnPassant(move, pieceType, toRow, toColumn, capture)) {
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
                    originalLocation = piece.boardLocation;
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
            printBoard(!whiteTurn);
            int checkMateOrDraw = determineCheckMateOrDraw(color);
            boolean insufficientMaterial = determineDrawByInsufficientMaterial();
            if(pieceType == Piece.PieceType.KING || castlingMove) {
                if(whiteTurn) {
                    board.whiteCanCastleK = false;
                    board.whiteCanCastleQ = false;
                }
                else {
                    board.blackCanCastleK = false;
                    board.blackCanCastleQ = false;
                }
            }
            else if(pieceType == Piece.PieceType.ROOK && originalLocation != null) {
                if(whiteTurn) {
                    if(originalLocation.chessLingo.equals("a1")) {
                        board.whiteQRookMoved = true;
                        board.whiteCanCastleQ = false;
                    }
                    else if(originalLocation.chessLingo.equals("h1")) {
                        board.whiteKRookMoved = true;
                        board.whiteCanCastleK = false;
                    }
                }
                else {
                    if(originalLocation.chessLingo.equals("a8")) {
                        board.blackQRookMoved = true;
                        board.blackCanCastleQ = false;
                    }
                    else if(originalLocation.chessLingo.equals("h8")) {
                        board.blackKRookMoved = true;
                        board.blackCanCastleK = false;
                    }
                }
            }
            whiteTurn = !whiteTurn;
            castlingMove = false;
            move = editMoveToAccommodateCheck(move, checkMateOrDraw);
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

    private static void computerGame() {
        while(true) {
            upgradePawnTo = null;
            String move;
            if((whiteTurn && computerPlayAs == Piece.Color.BLACK) || (!whiteTurn && computerPlayAs == Piece.Color.WHITE)) {
                System.out.println("\nYour Turn: ");
                if (!scanner.hasNext()) break;
                move = scanner.next();
                if (move.equals("undo")) {
                    undo();
                    continue;
                }
                if (move.equals("redo")) {
                    redo();
                    continue;
                }
                if (move.equals("help")) {
                    help();
                    continue;
                }
                if (move.equals("log")) {
                    gameLog();
                    continue;
                }
            }
            else {
                move = computerTurn();
                System.out.println(move);
            }
            if (!validInput(move)) continue;
            Piece.PieceType pieceType = determinePiece(move);
            boolean capture = determineCapture(move);
            int toRow = determineMoveToRow(move);
            Piece.Color color = whiteTurn ? Piece.Color.WHITE : Piece.Color.BLACK;
            BoardLocation originalLocation = null;
            if (castlingMove) {
                if (!castling(toRow, color)) {
                    System.err.println("You cannot castle in this position");
                    castlingMove = false;
                    continue;
                }
            } else {
                int toColumn = determineMoveToColumn(move, false);
                if (pieceType == null || toRow == -1 || toColumn == -1) continue;
                if (determineEnPassant(move, pieceType, toRow, toColumn, capture)) {
                    if (!enPassant(move, toRow, toColumn)) {
                        continue;
                    }
                } else {
                    Set<Piece> set = squaresToPieces.get(new BoardLocation(toRow, toColumn));
                    if (set.isEmpty()) {
                        System.err.printf("Piece cannot move to [%d, %d]\n", toRow, toColumn);
                        continue;
                    }
                    Piece piece = identifyMovePiece(set, pieceType, color, move);
                    if (piece == null) continue;
                    if (!validMove(piece, toRow, toColumn, capture, true)) continue;
                    originalLocation = piece.boardLocation;
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
            printBoard(!whiteTurn);
            int checkMateOrDraw = determineCheckMateOrDraw(color);
            boolean insufficientMaterial = determineDrawByInsufficientMaterial();
            if(pieceType == Piece.PieceType.KING || castlingMove) {
                if(whiteTurn) {
                    board.whiteCanCastleK = false;
                    board.whiteCanCastleQ = false;
                }
                else {
                    board.blackCanCastleK = false;
                    board.blackCanCastleQ = false;
                }
            }
            else if(pieceType == Piece.PieceType.ROOK && originalLocation != null) {
                if(whiteTurn) {
                    if(originalLocation.chessLingo.equals("a1")) {
                        board.whiteQRookMoved = true;
                        board.whiteCanCastleQ = false;
                    }
                    else if(originalLocation.chessLingo.equals("h1")) {
                        board.whiteKRookMoved = true;
                        board.whiteCanCastleK = false;
                    }
                }
                else {
                    if(originalLocation.chessLingo.equals("a8")) {
                        board.blackQRookMoved = true;
                        board.blackCanCastleQ = false;
                    }
                    else if(originalLocation.chessLingo.equals("h8")) {
                        board.blackKRookMoved = true;
                        board.blackCanCastleK = false;
                    }
                }
            }
            whiteTurn = !whiteTurn;
            castlingMove = false;
            move = editMoveToAccommodateCheck(move, checkMateOrDraw);
            pushState(move);
            if (determineDrawByRepetition()) {
                if (drawByRepetition(scanner)) {
                    break;
                }
            }
            if (checkMateOrDraw == 1) {
                if (checkmate(scanner)) {
                    break;
                }
            } else if (checkMateOrDraw == 0) {
                if (drawByNoAvailableMoves(scanner)) {
                    break;
                }
            } else if (insufficientMaterial) {
                if (drawByInsufficientMaterial(scanner)) {
                    break;
                }
            }
        }
    }

    private static boolean validMove(Piece piece, int moveToRow, int moveToColumn, boolean capture, boolean printErrors) {
        BoardLocation savedLocation = piece.boardLocation;
        if(!piece.validMove(board.board, piece.boardLocation.row, piece.boardLocation.column, moveToRow, moveToColumn, capture, printErrors)) return false;
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
        printBoard(whiteTurn);
    }

    private static void redo() {
        if(redoStack.size() == 0) {
            System.err.println("Cannot redo any more");
            return;
        }
        State state = redoStack.pop();
        returnToState(state);
        undoStack.push(state);
        printBoard(whiteTurn);
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
                board.board[i][j] = null;
            }
        }
        for(Piece piece : piecesToSquares.keySet()) {
            board.board[piece.boardLocation.row][piece.boardLocation.column] = piece;
        }
    }

    private static boolean castling(int side, Piece.Color color) {
        if((color == Piece.Color.WHITE && (whiteInCheck || (side == 9 && !board.whiteCanCastleK) || (side == 10 && !board.whiteCanCastleQ))) ||
                (color == Piece.Color.BLACK && (blackInCheck || (side == 9 && !board.blackCanCastleK) || (side == 10 && !board.blackCanCastleQ)))) {
            return false;
        }
        if(side != 9 && side != 10) {// Take this part out after you know it works
            throw new IllegalArgumentException("Something went wrong");
        }
        BoardLocation kingLocation = color == Piece.Color.WHITE ? whiteKingLocation : blackKingLocation;
        BoardLocation destination = calculateCastlingDestination(side, color);
        Piece castlingRook = determineCastlingRook(destination);
        if(castlingRook == null) return false;
        BoardLocation rookDestination = determineCastlingRookDestination(castlingRook);
        int direction = destination.column > kingLocation.column ? 1 : -1;
        int count = 0;
        int currentColumn;
        for(currentColumn = direction + kingLocation.column; count < 2; currentColumn += direction) {
            if(board.board[kingLocation.row][currentColumn] != null) {
                return false;
            }
            Set<Piece> pieces = squaresToPieces.get(new BoardLocation(kingLocation.row, currentColumn));
            for(Piece piece : pieces) {
                if(!piece.color.equals(color)) {
                    return false;
                }
            }
            count++;
        }
        //currentColumn += direction;
        if(currentColumn != castlingRook.boardLocation.column && board.board[kingLocation.row][currentColumn] != null) {
            return false;
        }
        Piece king = board.board[kingLocation.row][kingLocation.column];
        board.board[destination.row][destination.column] = king;
        king.boardLocation = destination;
        board.board[kingLocation.row][kingLocation.column] = null;
        board.board[castlingRook.boardLocation.row][castlingRook.boardLocation.column] = null;
        board.board[rookDestination.row][rookDestination.column] = castlingRook;
        castlingRook.boardLocation = rookDestination;
        updateKingLocation(king);
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
                return board.board[1][8];
            }
            if(destination.column == 3) {
                return board.board[1][1];
            }
        }
        if(destination.row == 8) {
            if(destination.column == 7) {
                return board.board[8][8];
            }
            if(destination.column == 3) {
                return board.board[8][1];
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

    private static boolean determineEnPassant(String move, Piece.PieceType pieceType, int moveToRow, int moveToColumn, boolean capture) {
        if(!pieceType.equals(Piece.PieceType.PAWN) || !capture || !((whiteTurn && moveToRow == 6) || (!whiteTurn && moveToRow == 3)) || board.board[moveToRow][moveToColumn] != null) return false;
        String anticipatedLastMove = whiteTurn ? (Utils.columnToLetter(moveToColumn) + "5") : (Utils.columnToLetter(moveToColumn) + "4");
        return undoStack.peek().lastMove.equals(anticipatedLastMove);
    }

    private static boolean enPassant(String move, int moveToRow, int moveToColumn) {
        int rowOfPieceBeingCaptured = (whiteTurn) ? 5 : 4;
        int currentColumn = Utils.letterToColumn(move.charAt(0));
        if(currentColumn < 1 || currentColumn > 8) {
            System.err.println("Column " + currentColumn + " is out of range");
            return false;
        }
        Piece pawn = board.board[rowOfPieceBeingCaptured][currentColumn];
        Piece capturedPiece = board.board[rowOfPieceBeingCaptured][moveToColumn];

        // Undo data structures
        BoardLocation savedLocation = new BoardLocation(pawn.boardLocation.chessLingo);
        boolean wic = whiteInCheck;
        boolean bic = blackInCheck;
        Set<BoardLocation> saveTempSTP = new HashSet<>();
        Set<BoardLocation> tempPTS = piecesToSquares.get(capturedPiece);
        //Piece oldPiece = pawn;

        board.board[rowOfPieceBeingCaptured][currentColumn] = null;
        board.board[moveToRow][moveToColumn] = pawn;
        pawn.boardLocation = new BoardLocation(moveToRow, moveToColumn);
        board.board[rowOfPieceBeingCaptured][moveToColumn] = null;
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
            board.board[savedLocation.row][savedLocation.column] = oldPiece;
            board.board[rowOfPieceBeingCaptured][moveToColumn] = capturedPiece;
            board.board[moveToRow][moveToColumn] = null;
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
        System.out.println("Checkmate! " + winner + " wins!\n" +
                "Write \"undo\" to undo, \"end game\" to end the game, or \"log\" to see the game log.");
        String input = scanner.nextLine().trim();
        while(!(input.equals("undo") || input.equals("end game") || input.equals("log"))) {
            System.out.println("Enter \"undo\" or \"end game\" or \"log\"");
            input = scanner.nextLine().trim();
        }
        if(input.equals("undo")) {
            undo();
            return false;
        }
        if(input.equals("log")) {
            gameLog();
            System.out.println("Write \"undo\" to undo, or \"end game\" to end the game.");
            input = scanner.nextLine().trim();
            while(!(input.equals("undo") || input.equals("end game"))) {
                System.out.println("Enter \"undo\" or \"end game\"");
                input = scanner.nextLine().trim();
            }
            if(input.equals("undo")) {
                undo();
                return false;
            }
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
                Piece piece = board.board[i][j];
                if(piece == null) continue;
                BoardLocation kingLocation = (piece.color == Piece.Color.WHITE) ? blackKingLocation : whiteKingLocation;
                if (piece.validMove(board.board, i, j, kingLocation.row, kingLocation.column, true, false)) {
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

    public static void initializeBoard() {
        Piece[][] chessBoard = new Piece[9][9];
        chessBoard[1][1] = new Rook(Piece.Color.WHITE, new BoardLocation(1,1));
        chessBoard[1][2] = new Knight(Piece.Color.WHITE, new BoardLocation(1,2));
        chessBoard[1][3] = new Bishop(Piece.Color.WHITE, new BoardLocation(1,3));
        chessBoard[1][4] = new Queen(Piece.Color.WHITE, new BoardLocation(1,4));
        chessBoard[1][5] = new King(Piece.Color.WHITE, new BoardLocation(1,5));
        whiteKingLocation = chessBoard[1][5].boardLocation;
        chessBoard[1][6] = new Bishop(Piece.Color.WHITE, new BoardLocation(1,6));
        chessBoard[1][7] = new Knight(Piece.Color.WHITE, new BoardLocation(1,7));
        chessBoard[1][8] = new Rook(Piece.Color.WHITE, new BoardLocation(1,8));
        for(int i = 1; i <= 8; i++) {
            chessBoard[2][i] = new Pawn(Piece.Color.WHITE, new BoardLocation(2,i));
        }
        chessBoard[8][1] = new Rook(Piece.Color.BLACK, new BoardLocation(8,1));
        chessBoard[8][2] = new Knight(Piece.Color.BLACK, new BoardLocation(8,2));
        chessBoard[8][3] = new Bishop(Piece.Color.BLACK, new BoardLocation(8,3));
        chessBoard[8][4] = new Queen(Piece.Color.BLACK, new BoardLocation(8,4));
        chessBoard[8][5] = new King(Piece.Color.BLACK, new BoardLocation(8,5));
        blackKingLocation = chessBoard[8][5].boardLocation;
        chessBoard[8][6] = new Bishop(Piece.Color.BLACK, new BoardLocation(8,6));
        chessBoard[8][7] = new Knight(Piece.Color.BLACK, new BoardLocation(8,7));
        chessBoard[8][8] = new Rook(Piece.Color.BLACK, new BoardLocation(8,8));
        for(int i = 1; i <= 8; i++) {
            chessBoard[7][i] = new Pawn(Piece.Color.BLACK, new BoardLocation(7,i));
        }
        blackInCheck = false;
        whiteInCheck = false;
        castlingMove = false;
        upgradePawnTo = null;
        whiteTurn = true;
        board = new Board(chessBoard, true, true, true, false, false,
                true, true, false, false, null);
        boardMap = new BoardMap();
        score = 0;
    }

    private static void initPTS() {
        piecesToSquares = new HashMap<>();
        int i = 2;
        for(int j = 1; j <= 8; j++) {
            Set<BoardLocation> set = new HashSet<>();
            set.add(new BoardLocation(i+1, j));
            set.add(new BoardLocation(i+2, j));
            PlayChess.piecesToSquares.put(board.board[i][j], set);
        }
        i = 7;
        for(int j = 1; j <= 8; j++) {
            Set<BoardLocation> set = new HashSet<>();
            set.add(new BoardLocation(i-1, j));
            set.add(new BoardLocation(i-2, j));
            PlayChess.piecesToSquares.put(board.board[i][j], set);
        }
        Set<BoardLocation> set = new HashSet<>();
        Set<BoardLocation> set1 = new HashSet<>();
        Set<BoardLocation> set2 = new HashSet<>();
        Set<BoardLocation> set3 = new HashSet<>();
        set.add(new BoardLocation(getCoordinateColumn("a3"), getCoordinateRow("a3")));
        set.add(new BoardLocation(getCoordinateColumn("c3"), getCoordinateRow("c3")));
        PlayChess.piecesToSquares.put(board.board[1][2], set);
        set1.add(new BoardLocation(getCoordinateColumn("f3"), getCoordinateRow("f3")));
        set1.add(new BoardLocation(getCoordinateColumn("h3"), getCoordinateRow("h3")));
        PlayChess.piecesToSquares.put(board.board[1][7], set1);
        set2.add(new BoardLocation(getCoordinateColumn("a6"), getCoordinateRow("a6")));
        set2.add(new BoardLocation(getCoordinateColumn("c6"), getCoordinateRow("c6")));
        PlayChess.piecesToSquares.put(board.board[8][2], set2);
        set3.add(new BoardLocation(getCoordinateColumn("f6"), getCoordinateRow("f6")));
        set3.add(new BoardLocation(getCoordinateColumn("h6"), getCoordinateRow("h6")));
        PlayChess.piecesToSquares.put(board.board[8][7], set3);
        for(i = 1; i <= 2; i++) {
            for(int j = 1; j <= 8; j++) {
                Piece piece = board.board[i][j];
                if(!piecesToSquares.containsKey(piece)) {
                    piecesToSquares.put(piece, new HashSet<>());
                }
            }
        }
        for(i = 7; i <= 8; i++) {
            for(int j = 1; j <= 8; j++) {
                Piece piece = board.board[i][j];
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
        pushState(null);
    }

    private static void pushState(String move) {
        State state;
        Piece[][] matchingBoard = boardMap.getKeyDeepEquals(board.board);
        if(matchingBoard == null) {
            state = new State(piecesToSquares, squaresToPieces, board.board, whiteKingLocation, blackKingLocation, whiteInCheck, blackInCheck, whiteTurn, move);
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
        int scoreChange = 0;
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
                if(whiteTurn) scoreChange = scoreChange + upgrade.value - oldPiece.value;
                else scoreChange = scoreChange - upgrade.value + oldPiece.value;
                score += scoreChange;
            }
        }
        board.board[oldPiece.boardLocation.row][oldPiece.boardLocation.column] = null;
        board.board[destination.row][destination.column] = piece;
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
        int finalScoreChange = scoreChange;
        return piece1 -> {
            oldPiece.boardLocation = savedLocation;
            board.board[savedLocation.row][savedLocation.column] = oldPiece;
            board.board[destination.row][destination.column] = null;
            if(finalTempPTS != null) {
                piecesToSquares.put(oldPiece, finalTempPTS);
                for(BoardLocation boardLocation : saveTempSTP) {
                    Set<Piece> pieces = squaresToPieces.get(boardLocation);
                    pieces.add(oldPiece);
                    squaresToPieces.put(boardLocation, pieces);
                }
                score -= finalScoreChange;
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
        Piece temp = board.board[destination.row][destination.column];
        boolean wic = whiteInCheck;
        boolean bic = blackInCheck;
        BoardLocation wkl = new BoardLocation(whiteKingLocation.chessLingo);
        BoardLocation bkl = new BoardLocation(blackKingLocation.chessLingo);
        Set<BoardLocation> saveTempSTP = new HashSet<>();
        Set<BoardLocation> tempPTS = piecesToSquares.get(temp);
        Set<BoardLocation> savePawnsSTP = new HashSet<>();
        Set<BoardLocation> pawnsPTS = null;
        Piece oldPiece = piece;
        int scoreChange = 0;
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
//                        squaresToPieces.put(boardLocation, set);
                    }
                }
                piece = upgrade;
                piecesToSquares.put(piece, new HashSet<>());
                if(whiteTurn) scoreChange = scoreChange + upgrade.value - oldPiece.value;
                else scoreChange = scoreChange - upgrade.value + oldPiece.value;
            }
        }
        board.board[oldPiece.boardLocation.row][oldPiece.boardLocation.column] = null;
        board.board[destination.row][destination.column] = piece;
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
        if(whiteTurn) {
            scoreChange += temp.value;
        }
        else {
            scoreChange -= temp.value;
        }
        score += scoreChange;
        Piece finalPiece = piece;
        int finalScoreChange = scoreChange;
        return piece1 -> {
            oldPiece.boardLocation = savedLocation;
            board.board[savedLocation.row][savedLocation.column] = oldPiece;
            board.board[destination.row][destination.column] = temp;
            piecesToSquares.put(temp, tempPTS);
            for(BoardLocation boardLocation : saveTempSTP) {
                Set<Piece> pieces = squaresToPieces.get(boardLocation);
                pieces.add(temp);
//                squaresToPieces.put(boardLocation, pieces);
            }
            if(finalPawnsPTS != null) {
                piecesToSquares.put(oldPiece, finalPawnsPTS);
                for(BoardLocation boardLocation : savePawnsSTP) {
                    Set<Piece> pieces = squaresToPieces.get(boardLocation);
                    pieces.add(oldPiece);
                    squaresToPieces.put(boardLocation, pieces);
                }
                piecesToSquares.remove(finalPiece);
            }
            whiteInCheck = wic;
            blackInCheck = bic;
            whiteKingLocation = wkl;
            blackKingLocation = bkl;
            score -= finalScoreChange;
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
        // go through every piece and go through all of its potential valid squares and if it's valid add it to maps
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
                    if(piece.validMove(board.board, currentRow, currentColumn, i, j, true, false) || piece.validMove(board.board, currentRow, currentColumn, i, j, false, false)) {
                        set.add(new BoardLocation(i, j));
                    }
                }
            }
//            piecesToSquares.put(piece, set);
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
//            squaresToPieces.put(boardLocation, set0);
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

    private static void printBoard(boolean whitePerspective) {
        if(whitePerspective) {
            for (int i = 8; i >= 1; i--) {
                System.out.print(i + " |");
                for (int j = 1; j <= 8; j++) {
                    if (board.board[i][j] == null) {
                        //String s = "\u2009";
                        System.out.print("  |");
                        continue;
                    }
                    System.out.printf("\u200A%s\u200A|", board.board[i][j].shorthand());
                }
                System.out.print("\n");
            }
            System.out.println("\u2009" + "\u2009"  + "\u2009"  + "\u2009" + "\u2009" + "  a  b  c  d  e  f  g  h");
        }
        else {
            for (int i = 1; i <= 8; i++) {
                System.out.print(i + " |");
                for (int j = 8; j >= 1; j--) {
                    if (board.board[i][j] == null) {
                        //String s = "\u2009";
                        System.out.print("  |");
                        continue;
                    }
                    System.out.printf("\u200A%s\u200A|", board.board[i][j].shorthand());
                }
                System.out.print("\n");
            }
            System.out.println("\u2009" + "\u2009"  + "\u2009"  + "\u2009" + "\u2009" + "  h  g  f  e  d  c  b  a");
        }
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

    private static String editMoveToAccommodateCheck(String move, int checkmateCode) {
        if(checkmateCode == 1) return move + "++";
        if(whiteInCheck || blackInCheck) return move + "+";
        return move;
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

    public static String computerTurn() {
        double bestScore = whiteTurn ? Integer.MIN_VALUE: Integer.MAX_VALUE;
        Move bestMove = null;

        List<Move> legalMoves = generateLegalMoves(board, computerPlayAs);
        for(Move move : legalMoves) {
            BoardLocation originalLocation = move.piece.boardLocation;
            BeforeMoveState prev = computerMakeMove(board, move.piece, move.destination);
            double score = minimax(board, computerDepth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, !whiteTurn);
            computerUndoMove(board, move.piece, originalLocation, prev);
            if(whiteTurn) {
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }
            else {
                if(score < bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }
        }

        return constructMoveString(bestMove.piece, bestMove.destination);
    }

    private static BeforeMoveState computerMakeMove(Board chessBoard, Piece piece, BoardLocation destination) {
        Piece prev = chessBoard.board[destination.row][destination.column];
        chessBoard.board[destination.row][destination.column] = piece;
        chessBoard.board[piece.boardLocation.row][piece.boardLocation.column] = null;
        piece.boardLocation = destination;
        BeforeMoveState prevState = new BeforeMoveState(prev, chessBoard.whiteCanCastleK, chessBoard.whiteCanCastleQ, chessBoard.blackCanCastleK, chessBoard.blackCanCastleQ);
        if(piece.pieceType == Piece.PieceType.KING) {
            if(piece.color == Piece.Color.WHITE) {
                chessBoard.whiteCanCastleK = false;
                chessBoard.whiteCanCastleQ = false;
            }
            else {
                chessBoard.blackCanCastleK = false;
                chessBoard.blackCanCastleQ = false;
            }
        }
        else if(piece.pieceType == Piece.PieceType.ROOK) {
            if(piece.color == Piece.Color.WHITE) {
                if(piece.boardLocation.chessLingo.equals("a1")) {
                    chessBoard.whiteCanCastleQ = false;
                }
                else if(piece.boardLocation.chessLingo.equals("h1")) {
                    chessBoard.whiteCanCastleK = false;
                }
            }
            else {
                if(piece.boardLocation.chessLingo.equals("a8")) {
                    chessBoard.blackCanCastleQ = false;
                }
                else if(piece.boardLocation.chessLingo.equals("h8")) {
                    chessBoard.blackCanCastleK = false;
                }
            }
        }
        return prevState;
    }

    private static class BeforeMoveState {
        Piece capturedPiece;
        boolean prevWhiteCanCastleK;
        boolean prevWhiteCanCastleQ;
        boolean prevBlackCanCastleK;
        boolean prevBlackCanCastleQ;
        public BeforeMoveState(Piece capturedPiece, boolean prevWhiteCanCastleK, boolean prevWhiteCanCastleQ, boolean prevBlackCanCastleK, boolean prevBlackCanCastleQ) {
            this.capturedPiece = capturedPiece;
            this.prevBlackCanCastleK = prevBlackCanCastleK;
            this.prevBlackCanCastleQ = prevBlackCanCastleQ;
            this.prevWhiteCanCastleK = prevWhiteCanCastleK;
            this.prevWhiteCanCastleQ = prevWhiteCanCastleQ;
        }
    }

    private static void computerUndoMove(Board chessBoard, Piece piece, BoardLocation originalLocation, BeforeMoveState prev) {
        chessBoard.board[originalLocation.row][originalLocation.column] = piece;
        chessBoard.board[piece.boardLocation.row][piece.boardLocation.column] = prev.capturedPiece;
        piece.boardLocation = originalLocation;
        chessBoard.whiteCanCastleK = prev.prevWhiteCanCastleK;
        chessBoard.whiteCanCastleQ = prev.prevWhiteCanCastleQ;
        chessBoard.blackCanCastleK = prev.prevBlackCanCastleK;
        chessBoard.blackCanCastleQ = prev.prevBlackCanCastleQ;
    }

    private static double minimax(Board board, int depth, double alpha, double beta, boolean whiteMove) {
        if (depth == 0/* || gameIsOver(board)*/) {
            return evaluateBoard(board);
        }

        if (whiteMove) {
            double maxScore = Integer.MIN_VALUE;
            List<Move> legalMoves = generateLegalMoves(board, Piece.Color.WHITE);

            for (Move move : legalMoves) {
                BoardLocation originalLocation = move.piece.boardLocation;
                BeforeMoveState prev = computerMakeMove(board, move.piece, move.destination);
                double score = minimax(board, depth - 1, alpha, beta, false);
                computerUndoMove(board, move.piece, originalLocation, prev);
                maxScore = Math.max(maxScore, score);
                alpha = Math.max(alpha, score);
                if (beta <= alpha) {
                    break; // Beta cutoff
                }
            }

            return maxScore;
        } else {
            double minScore = Integer.MAX_VALUE;
            List<Move> legalMoves = generateLegalMoves(board, Piece.Color.BLACK);

            for (Move move : legalMoves) {
                BoardLocation originalLocation = move.piece.boardLocation;
                BeforeMoveState prev = computerMakeMove(board, move.piece, move.destination);
                double score = minimax(board, depth - 1, alpha, beta, true);
                computerUndoMove(board, move.piece, originalLocation, prev);
                minScore = Math.min(minScore, score);
                beta = Math.min(beta, score);
                if (beta <= alpha) {
                    break; // Alpha cutoff
                }
            }

            return minScore;
        }
    }

    private static double evaluateBoard(Board board) {
        // Measure: score
        return measureBoardScore(board);
    }

    private static String constructMoveString(Piece piece, BoardLocation square) {
        Piece.PieceType type = piece.pieceType;
        Set<Piece> possiblePieces = new HashSet<>();
        for(Piece p : squaresToPieces.get(square)) {
            if(p.pieceType == type && p.color == piece.color) possiblePieces.add(p);
        }
        boolean capture = board.board[square.row][square.column] != null;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Utils.pieceToAbbreviation(type));
        if(possiblePieces.size() > 1) {
            if(multipleSameColumn(possiblePieces)) {
                stringBuilder.append(Utils.columnToLetter(piece.boardLocation.column));
            }
            if(multipleSameRow(possiblePieces)) {
                stringBuilder.append(piece.boardLocation.row);
            }
        }
        else if(type == Piece.PieceType.PAWN) {
            stringBuilder.append(Utils.columnToLetter(piece.boardLocation.column));
        }
        if(capture) {
            stringBuilder.append('x');
        }
        if(type != Piece.PieceType.PAWN || capture) {
            stringBuilder.append(square.chessLingo);
        }
        else {
            stringBuilder.append(square.row);
        }
        return stringBuilder.toString();
    }

    private static boolean multipleSameRow(Set<Piece> pieces) {
        Iterator<Piece> iterator = pieces.iterator();
        int row = iterator.next().boardLocation.row;
        while(iterator.hasNext()) {
            if(iterator.next().boardLocation.row == row) return true;
        }
        return false;
    }

    private static boolean multipleSameColumn(Set<Piece> pieces) {
        Iterator<Piece> iterator = pieces.iterator();
        int column = iterator.next().boardLocation.column;
        while(iterator.hasNext()) {
            if(iterator.next().boardLocation.column == column) return true;
        }
        return false;
    }

    private static List<Move> generateLegalMoves(Board chessBoard, Piece.Color color) {
        BoardLocation kingLocation = null;
        List<Move> validMoves = new ArrayList<>();
        for(int i = 1; i <= 8; i++) {
            for(int j = 1; j <= 8; j++) {
                if(chessBoard.board[i][j] == null) continue;
                Piece piece = chessBoard.board[i][j];
                if(piece.pieceType == Piece.PieceType.KING && piece.color == color) kingLocation = new BoardLocation(i, j);
                for(int x = 1; x <= 8; x++) {
                    for (int y = 1; y <= 8; y++) {
                        if (chessBoard.board[x][y] == null) {
                            if (piece.validMove(chessBoard.board, i, j, x, y, false, false)) {
                                validMoves.add(new Move(piece, new BoardLocation(x, y), false));
                            }
                        } else {
                            if (piece.validMove(chessBoard.board, i, j, x, y, true, false)) {
                                validMoves.add(new Move(piece, new BoardLocation(x, y), true));
                            }
                        }
                    }
                }
            }
        }
        
        // Castling
        if(chessBoard.whiteTurn) {
            if(chessBoard.whiteCanCastleK) {
                boolean canCastle = true;
                for(Move move : validMoves) {
                    if(move.piece.color == Piece.Color.BLACK && (move.destination.chessLingo.equals("e1") || 
                            move.destination.chessLingo.equals("f1") || move.destination.chessLingo.equals("g1"))) {
                        canCastle = false;
                        break;
                    }
                }
                if(canCastle) {
                    validMoves.add(new Move(chessBoard.board[1][5], new BoardLocation("g1"), false, Move.SpecialMove.CASTLE_KINGSIDE));
                }
            }
            if(chessBoard.whiteCanCastleQ) {
                boolean canCastle = true;
                for(Move move : validMoves) {
                    if(move.piece.color == Piece.Color.BLACK && (move.destination.chessLingo.equals("e1") ||
                            move.destination.chessLingo.equals("d1") || move.destination.chessLingo.equals("c1"))) {
                        canCastle = false;
                        break;
                    }
                }
                if(canCastle) {
                    validMoves.add(new Move(chessBoard.board[1][5], new BoardLocation("c1"), false, Move.SpecialMove.CASTLE_QUEENSIDE));
                }
            }
        }
        else {
            if(chessBoard.blackCanCastleK) {
                boolean canCastle = true;
                for(Move move : validMoves) {
                    if(move.piece.color == Piece.Color.WHITE && (move.destination.chessLingo.equals("e8") ||
                            move.destination.chessLingo.equals("f8") || move.destination.chessLingo.equals("g8"))) {
                        canCastle = false;
                        break;
                    }
                }
                if(canCastle) {
                    validMoves.add(new Move(chessBoard.board[1][5], new BoardLocation("g8"), false, Move.SpecialMove.CASTLE_KINGSIDE));
                }
            }
            if(chessBoard.blackCanCastleQ) {
                boolean canCastle = true;
                for(Move move : validMoves) {
                    if(move.piece.color == Piece.Color.WHITE && (move.destination.chessLingo.equals("e8") ||
                            move.destination.chessLingo.equals("d8") || move.destination.chessLingo.equals("c8"))) {
                        canCastle = false;
                        break;
                    }
                }
                if(canCastle) {
                    validMoves.add(new Move(chessBoard.board[1][5], new BoardLocation("c8"), false, Move.SpecialMove.CASTLE_QUEENSIDE));
                }
            }
        }

        // Remove oppo color moves
        validMoves.removeIf(move -> move.piece.color != color);

        // En Passant
        int pieceRow = chessBoard.whiteTurn ? 5 : 4;
        int ogLocRow = chessBoard.whiteTurn ? 7 : 2;
        int destRow = chessBoard.whiteTurn ? 6 : 3;
        if (chessBoard.lastMove != null && chessBoard.lastMove.piece.pieceType == Piece.PieceType.PAWN && chessBoard.lastMove.destination.row == pieceRow
                && chessBoard.lastMove.originalLocation.row == ogLocRow) {
            if(chessBoard.lastMove.destination.column + 1 <= 8
                    && chessBoard.board[pieceRow][chessBoard.lastMove.destination.column + 1] != null
                    && chessBoard.board[pieceRow][chessBoard.lastMove.destination.column + 1].color == Piece.Color.WHITE
                    && chessBoard.board[pieceRow][chessBoard.lastMove.destination.column + 1].pieceType == Piece.PieceType.PAWN) {
                validMoves.add(new Move(chessBoard.board[pieceRow][chessBoard.lastMove.destination.column + 1],
                        new BoardLocation(destRow, chessBoard.lastMove.destination.column), true, Move.SpecialMove.EN_PASSANT));
            }
            if(chessBoard.lastMove.destination.column - 1 >= 0
                    && chessBoard.board[pieceRow][chessBoard.lastMove.destination.column - 1] != null
                    && chessBoard.board[pieceRow][chessBoard.lastMove.destination.column - 1].color == Piece.Color.WHITE
                    && chessBoard.board[pieceRow][chessBoard.lastMove.destination.column - 1].pieceType == Piece.PieceType.PAWN) {
                validMoves.add(new Move(chessBoard.board[pieceRow][chessBoard.lastMove.destination.column - 1],
                        new BoardLocation(destRow, chessBoard.lastMove.destination.column), true, Move.SpecialMove.EN_PASSANT));
            }
        }

        // Promotion
        destRow = chessBoard.whiteTurn ? 8 : 1;
        ListIterator<Move> iterator = validMoves.listIterator();
        while(iterator.hasNext()) {
            Move move = iterator.next();
            if(move.piece.pieceType == Piece.PieceType.PAWN && move.piece.boardLocation.row == destRow) {
                move.special = Move.SpecialMove.PROMOTION;
                move.promotionType = Piece.PieceType.KNIGHT;
                iterator.add(new Move(move.piece, move.destination, move.capture, Move.SpecialMove.PROMOTION, Piece.PieceType.BISHOP));
                iterator.add(new Move(move.piece, move.destination, move.capture, Move.SpecialMove.PROMOTION, Piece.PieceType.ROOK));
                iterator.add(new Move(move.piece, move.destination, move.capture, Move.SpecialMove.PROMOTION, Piece.PieceType.QUEEN));
            }
        }

        // Make sure no moves put you in check
        iterator = validMoves.listIterator();
        while(iterator.hasNext()) {
            Move move = iterator.next();
            if(putMeInCheck(chessBoard, move, kingLocation)) {
                iterator.remove();
            }
        }

        return validMoves;
    }

    private static boolean putMeInCheck(Board chessBoard, Move move, BoardLocation kingLocation) {
        BeforeMoveState capturedPiece = computerMakeMove(chessBoard, move.piece, move.destination);
        boolean putsInCheck = false;
        outerLoop:
        for(int i = 1; i <= 8; i++) {
            for(int j = 1; j <= 8; j++) {
                if(chessBoard.board[i][j] == null || chessBoard.board[i][j].color == move.piece.color) continue;
                Piece piece = chessBoard.board[i][j];
                if(kingLocation == null || piece.validMove(chessBoard.board, i, j, kingLocation.row, kingLocation.column, true, false)) {
                    putsInCheck = true;
                    break outerLoop;
                }
            }
        }
        computerUndoMove(chessBoard, move.piece, move.originalLocation, capturedPiece);
        return putsInCheck;
    }

    public static class Move {
        Piece piece;
        BoardLocation originalLocation;
        BoardLocation destination;
        boolean capture;
        SpecialMove special;
        Piece.PieceType promotionType;
        public Move(Piece piece, BoardLocation destination, boolean capture) {
            this.piece = piece;
            this.originalLocation = piece.boardLocation;
            this.destination = destination;
            this.capture = capture;
        }
        
        public Move(Piece piece, BoardLocation destination, boolean capture, SpecialMove special) {
            this(piece, destination, capture);
            this.special = special;
        }

        public Move(Piece piece, BoardLocation destination, boolean capture, SpecialMove special, Piece.PieceType promotionType) {
            this(piece, destination, capture, special);
            this.promotionType = promotionType;
        }

        public enum SpecialMove {EN_PASSANT, CASTLE_KINGSIDE, CASTLE_QUEENSIDE, PROMOTION}
    }

    public static int measureBoardScore(Board chessBoard) {
        int score = 0;
        for(int i = 1; i <= 8; i++) {
            for(int j = 1; j <= 8; j++) {
                if(chessBoard.board[i][j] != null) {
                    if(chessBoard.board[i][j].color == Piece.Color.WHITE) {
                        score += chessBoard.board[i][j].value;
                    }
                    else {
                        score -= chessBoard.board[i][j].value;
                    }
                }
            }
        }
        return score;
    }

}