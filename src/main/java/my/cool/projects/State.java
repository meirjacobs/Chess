package my.cool.projects;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
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
        this.piecesToSquares = new HashMap<>(piecesToSquares);
        this.squaresToPieces = new HashMap<>(squaresToPieces);
        copy(board);
        this.whiteKingLocation = new BoardLocation(whiteKingLocation.chessLingo);
        this.blackKingLocation = new BoardLocation(blackKingLocation.chessLingo);
        this.whiteInCheck = whiteInCheck;
        this.blackInCheck = blackInCheck;
        this.whiteTurn = whiteTurn;
    }

    private void copy(Piece[][] board) {
        this.board = new Piece[9][9];
        for(int i = 0; i <= 8; i++) {
            for(int j = 0; j <= 8; j++) {
                this.board[i][j] = board[i][j];
            }
        }
    }

}
