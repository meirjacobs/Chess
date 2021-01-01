package my.cool.projects;

import java.util.HashMap;
import java.util.Set;

public class State {
    protected HashMap<Piece, Set<BoardLocation>> piecesToSquares;
    protected HashMap<BoardLocation, Set<Piece>> squaresToPieces;
    protected Piece[][] board;
    protected BoardLocation whiteKingLocation;
    protected BoardLocation blackKingLocation;
    protected boolean whiteInCheck;
    protected boolean blackInCheck;
    protected boolean whiteTurn;

    State(HashMap<Piece, Set<BoardLocation>> piecesToSquares, HashMap<BoardLocation, Set<Piece>> squaresToPieces, Piece[][] board, BoardLocation whiteKingLocation, BoardLocation blackKingLocation, boolean whiteInCheck, boolean blackInCheck, boolean whiteTurn) {
        this.piecesToSquares = piecesToSquares;
        this.squaresToPieces = squaresToPieces;
        this.board = board;
        this.whiteKingLocation = whiteKingLocation;
        this.blackKingLocation = blackKingLocation;
        this.whiteInCheck = whiteInCheck;
        this.blackInCheck = blackInCheck;
        this.whiteTurn = whiteTurn;
    }

}
