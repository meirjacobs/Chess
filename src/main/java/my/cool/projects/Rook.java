package my.cool.projects;

public class Rook extends Piece {

    public Rook(Color color, BoardLocation boardLocation) {
        super(color, boardLocation);
        pieceType = PieceType.ROOK;
    }

    @Override
    public boolean validMove(Piece[][] board, int currentRow, int currentColumn, int moveToRow, int moveToColumn, boolean capture, boolean printErrors) {
        validateInput(board, currentRow, currentColumn, moveToRow, moveToColumn);
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
        if(moveToColumn != currentColumn && moveToRow != currentRow) {
            if(printErrors) System.err.println("Rooks must move up-down or left-right");
            return false;
        }
        if(currentColumn == moveToColumn && currentRow == moveToRow) {
            if(printErrors) System.err.println("You have not moved the Rook");
            return false;
        }
        int direction;
        if(moveToRow - currentRow != 0) {
            direction = moveToRow - currentRow;
            for(; currentRow != moveToRow; currentRow += direction) {
                if(board[currentRow][currentColumn] != null) {
                    if(printErrors) System.err.println("Cannot move Rook because there is a piece in the way of the destination");
                    return false;
                }
            }
        }
        else {
            direction = moveToColumn - currentColumn;
            for(; currentColumn != moveToColumn; currentColumn += direction) {
                if(board[currentRow][currentColumn] != null) {
                    if(printErrors) System.err.println("Cannot move Rook because there is a piece in the way of the destination");
                    return false;
                }
            }
        }
        return true;
    }
}