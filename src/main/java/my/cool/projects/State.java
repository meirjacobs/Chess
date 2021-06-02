package my.cool.projects;
import java.util.*;

public class State {
    protected HashMap<Piece, Set<BoardLocation>> piecesToSquares;
    protected HashMap<BoardLocation, Set<Piece>> squaresToPieces;
    protected Piece[][] board;
    protected BoardLocation whiteKingLocation;
    protected BoardLocation blackKingLocation;
    protected boolean whiteInCheck;
    protected boolean blackInCheck;
    protected boolean whiteTurn;
    protected String lastMove;

    State(HashMap<Piece, Set<BoardLocation>> piecesToSquares, HashMap<BoardLocation, Set<Piece>> squaresToPieces, Piece[][] board, BoardLocation whiteKingLocation, BoardLocation blackKingLocation, boolean whiteInCheck, boolean blackInCheck, boolean whiteTurn, String lastMove) {
        copyPTS(piecesToSquares);
        copySTP(squaresToPieces);
        copyBoard(board);
        this.whiteKingLocation = new BoardLocation(whiteKingLocation.chessLingo);
        this.blackKingLocation = new BoardLocation(blackKingLocation.chessLingo);
        this.whiteInCheck = whiteInCheck;
        this.blackInCheck = blackInCheck;
        this.whiteTurn = whiteTurn;
        this.lastMove = lastMove;
    }

    private void copyBoard(Piece[][] board) {
        this.board = new Piece[9][9];
        for(int i = 0; i <= 8; i++) {
            for(int j = 0; j <= 8; j++) {
                Piece piece = board[i][j];
                if(piece == null) {
                    this.board[i][j] = null;
                    continue;
                }
                switch (piece.pieceType) {
                    case PAWN:
                        this.board[i][j] = new Pawn(piece.color, new BoardLocation(piece.boardLocation.chessLingo));
                        break;
                    case ROOK:
                        this.board[i][j] = new Rook(piece.color, new BoardLocation(piece.boardLocation.chessLingo));
                        break;
                    case BISHOP:
                        this.board[i][j] = new Bishop(piece.color, new BoardLocation(piece.boardLocation.chessLingo));
                        break;
                    case KNIGHT:
                        this.board[i][j] = new Knight(piece.color, new BoardLocation(piece.boardLocation.chessLingo));
                        break;
                    case QUEEN:
                        this.board[i][j] = new Queen(piece.color, new BoardLocation(piece.boardLocation.chessLingo));
                        break;
                    case KING:
                        this.board[i][j] = new King(piece.color, new BoardLocation(piece.boardLocation.chessLingo));
                        break;
                    default:
                }
            }
        }
    }

    private void copyPTS(HashMap<Piece, Set<BoardLocation>> orig) {
        this.piecesToSquares = new HashMap<>();
        for(Piece piece : orig.keySet()) {
            Set<BoardLocation> set = copyBLSet(orig.get(piece));
            switch (piece.pieceType) {
                case KNIGHT:
                    this.piecesToSquares.put(new Knight(piece.color, piece.boardLocation), set);
                    break;
                case BISHOP:
                    this.piecesToSquares.put(new Bishop(piece.color, piece.boardLocation), set);
                    break;
                case QUEEN:
                    this.piecesToSquares.put(new Queen(piece.color, piece.boardLocation), set);
                    break;
                case KING:
                    this.piecesToSquares.put(new King(piece.color, piece.boardLocation), set);
                    break;
                case ROOK:
                    this.piecesToSquares.put(new Rook(piece.color, piece.boardLocation), set);
                    break;
                case PAWN:
                    this.piecesToSquares.put(new Pawn(piece.color, piece.boardLocation), set);
                    break;
                default:
            }
        }
    }

    private Set<BoardLocation> copyBLSet(Set<BoardLocation> orig) {
        Set<BoardLocation> set = new HashSet<>();
        for(BoardLocation boardLocation : orig) {
            set.add(new BoardLocation(boardLocation.chessLingo));
        }
        return set;
    }

    private void copySTP(HashMap<BoardLocation, Set<Piece>> orig) {
        this.squaresToPieces = new HashMap<>();
        for(BoardLocation boardLocation : orig.keySet()) {
            Set<Piece> set = copyPieceSet(orig.get(boardLocation));
            this.squaresToPieces.put(new BoardLocation(boardLocation.chessLingo), set);
        }
    }

    private Set<Piece> copyPieceSet(Set<Piece> orig) {
        Set<Piece> set = new HashSet<>();
        for(Piece piece : orig) {
            switch (piece.pieceType) {
                case KNIGHT:
                    set.add(new Knight(piece.color, piece.boardLocation));
                    break;
                case BISHOP:
                    set.add(new Bishop(piece.color, piece.boardLocation));
                    break;
                case QUEEN:
                    set.add(new Queen(piece.color, piece.boardLocation));
                    break;
                case KING:
                    set.add(new King(piece.color, piece.boardLocation));
                    break;
                case ROOK:
                    set.add(new Rook(piece.color, piece.boardLocation));
                    break;
                case PAWN:
                    set.add(new Pawn(piece.color, piece.boardLocation));
                    break;
                default:
            }
        }
        return set;
    }

}
