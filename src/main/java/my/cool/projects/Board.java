package my.cool.projects;

public class Board {
    Piece[][] board;
    boolean whiteTurn;
    boolean whiteCanCastleK;
    boolean whiteCanCastleQ;
    boolean whiteQRookMoved;
    boolean whiteKRookMoved;
    boolean blackCanCastleK;
    boolean blackCanCastleQ;
    boolean blackQRookMoved;
    boolean blackKRookMoved;
    PlayChess.Move lastMove;
    
    public Board(Piece[][] board, boolean whiteTurn, boolean whiteCanCastleK, boolean whiteCanCastleQ,
                 boolean whiteQRookMoved, boolean whiteKRookMoved, boolean blackCanCastleK, boolean blackCanCastleQ,
                 boolean blackQRookMoved, boolean blackKRookMoved, PlayChess.Move lastMove) {
        this.board = board;
        this.whiteTurn = whiteTurn;
        this.whiteCanCastleK = whiteCanCastleK;
        this.whiteCanCastleQ = whiteCanCastleQ;
        this.whiteQRookMoved = whiteQRookMoved;
        this.whiteKRookMoved = whiteKRookMoved;
        this.blackCanCastleK = blackCanCastleK;
        this.blackCanCastleQ = blackCanCastleQ;
        this.blackQRookMoved = blackQRookMoved;
        this.blackKRookMoved = blackKRookMoved;
        this.lastMove = lastMove;
    }
}
