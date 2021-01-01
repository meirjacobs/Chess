package my.cool.projects;

public class King extends Piece {

    public King(Color color, BoardLocation boardLocation) {
        super(color, boardLocation);
        pieceType = PieceType.KING;
    }

    @Override
    public boolean validMove(Piece[][] board, int currentRow, int currentColumn, int moveToRow, int moveToColumn, boolean capture, boolean printErrors) {
        validateInput(board, currentRow, currentColumn, moveToRow, moveToColumn);
        if(board[currentRow][currentColumn] == null) {
            if(printErrors) System.err.println("No piece at current location");
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
        if(Math.max(Math.abs(moveToRow - currentRow), Math.abs(moveToColumn - currentColumn)) != 1) {
            if(printErrors) System.err.println("King must move one square");
            return false;
        }
        return true;
    }
}