package my.cool.projects;

public class Bishop extends Piece {

    public Bishop(Color color, BoardLocation boardLocation) {
        super(color, boardLocation);
        pieceType = PieceType.BISHOP;
    }

    @Override
    public boolean validMove(Piece[][] board, int currentRow, int currentColumn, int moveToRow, int moveToColumn, boolean capture, boolean printErrors) {
        if(!validateInput(board, currentRow, currentColumn, moveToRow, moveToColumn)) return false;
        if(currentColumn == moveToColumn) {
            if(printErrors) System.err.println("You have not moved the Bishop");
            return false;
        }
        if(Math.abs(moveToRow - currentRow) != Math.abs(moveToColumn - currentColumn)) {
            if(printErrors) System.err.println("Bishops must move diagonally");
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
        int rowDirection;
        int columnDirection;
        rowDirection = (moveToRow - currentRow > 0) ? 1 : -1;
        columnDirection = (moveToColumn - currentColumn > 0) ? 1 : -1;
        currentRow += rowDirection;
        currentColumn += columnDirection;
        for(; currentRow != moveToRow && currentColumn != moveToColumn;) {
            if(board[currentRow][currentColumn] != null) {
                if(printErrors) System.err.println("Cannot move Bishop because there is a piece in the way of the destination");
                return false;
            }
            currentRow += rowDirection;
            currentColumn += columnDirection;
        }
        return true;
    }
}