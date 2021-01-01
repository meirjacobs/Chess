package my.cool.projects;

public class Knight extends Piece {

    public Knight(Color color, BoardLocation boardLocation) {
        super(color, boardLocation);
        pieceType = PieceType.KNIGHT;
    }

    @Override
    public boolean validMove(Piece[][] board, int currentRow, int currentColumn, int moveToRow, int moveToColumn, boolean capture, boolean printErrors) {
        validateInput(board, currentRow, currentColumn, moveToRow, moveToColumn);
        int deltaX = moveToRow - currentRow;
        int deltaY = moveToColumn - currentColumn;
        if(!((Math.abs(deltaX) == 2 || Math.abs(deltaX) == 1) && (Math.abs(deltaY) == 2 || Math.abs(deltaY) == 1)) || Math.abs(deltaX) == Math.abs(deltaY)) {
            if(printErrors) System.err.println("Knights must move 2 in one direction and 1 in the other");
            return false;
        }
        if(!capture) {
            if(board[moveToRow][moveToColumn] != null) {
                if(printErrors) System.err.println("Move-to square is already occupied");
                return false;
            }
        }
        else {
            if(board[moveToRow][moveToColumn] != null && board[moveToRow][moveToColumn].color.equals(board[currentRow][currentColumn].color)) {
                if(printErrors) System.err.println("Cannot capture your own piece");
                return false;
            }
        }
        return true;
    }
}