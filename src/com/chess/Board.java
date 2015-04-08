package com.chess;

import com.chess.pieces.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Greg Pastorek on 2/6/2015.
 */
public class Board {

    /* board dimensions */
    int board_x;
    int board_y;

    /* 2d array holding the board spaces */
    Piece spaces[][];

    /* pointers to each player's pieces */
    ArrayList<Piece> player1Pieces = new ArrayList<Piece>();
    ArrayList<Piece> player2Pieces = new ArrayList<Piece>();

    /* points to the king pieces */
    Piece player1King;
    Piece player2King;

    public Board(int dim_x, int dim_y){
        board_x = dim_x;
        board_y = dim_y;
        spaces = new Piece[dim_x][dim_y];
    }

    /* board copy constructor, we call this when saving the previous board for the undo function */
    public Board(Board other) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        board_x = other.board_x;
        board_y = other.board_y;
        spaces = new Piece[board_x][board_y];

        /* copy the spaces and each piece there */
        for(int x = 0; x < other.getMaxX(); x++){
            for(int y = 0; y < other.getMaxY(); y++){

                if(other.spaces[x][y] != null) {

                    Class<?> PieceType = other.spaces[x][y].getClass();
                    Constructor<?> cons = PieceType.getConstructor(PieceType, this.getClass());
                    Piece copiedPiece = (Piece)cons.newInstance(other.spaces[x][y], this);
                    spaces[x][y] = copiedPiece;

                    /* update pointer containers for the pieces */
                    if(copiedPiece.getPlayer() == 1){
                        if(copiedPiece.getClass() == KingPiece.class)
                            player1King = copiedPiece;

                        player1Pieces.add(copiedPiece);
                    } else {
                        if(copiedPiece.getClass() == KingPiece.class)
                            player2King = copiedPiece;

                        player2Pieces.add(copiedPiece);
                    }

                }
            }
        }

    }

    /* Initializes all the board pieces to their starting positions. */
    /* Used for starting a game and also resets a current game. */
    public void resetBoard(boolean selectedClassicChess){
        player1King = new KingPiece(this, 1);
        player2King = new KingPiece(this, 2);
        if(selectedClassicChess) {
            addPiecesForPlayer_ClassicMode(1);
            addPiecesForPlayer_ClassicMode(2);
        } else {
            addPiecesForPlayer_ChuckMode(1);
            addPiecesForPlayer_ChuckMode(2);
        }
    }

    /* Initialize all the board pieces for given player using classic configuration. */
    /* Helper function for resetBoard.                                               */
    private void addPiecesForPlayer_ClassicMode(int player){

        /* select first/second row indices based on player number */
        int first_row = (player == 1) ? 0 : getMaxY()-1;
        int second_row = (player == 1) ? 1 : getMaxY()-2;

        /* add first row pieces to board for player*/
        addPiece(new RookPiece(this, player), 0, first_row);
        addPiece(new KnightPiece(this, player), 1, first_row);
        addPiece(new BishopPiece(this, player), 2, first_row);
        addPiece(getPlayerKing(player), 3, first_row);
        addPiece(new QueenPiece(this, player), 4, first_row);
        addPiece(new BishopPiece(this, player), 5, first_row);
        addPiece(new KnightPiece(this, player), 6, first_row);
        addPiece(new RookPiece(this, player), 7, first_row);

        /* add pawns for player */
        for(int loc_x = 0; loc_x < getMaxX(); loc_x++){
            addPiece(new PawnPiece(this, player), loc_x, second_row);
        }

    }


    /* Initialize all the board pieces for given player using Chuck Norris configuration. */
    /* Helper function for resetBoard.                                                   */
    private void addPiecesForPlayer_ChuckMode(int player){

        /* select first/second row indices based on player number */
        int first_row = (player == 1) ? 0 : getMaxY()-1;
        int second_row = (player == 1) ? 1 : getMaxY()-2;

        /* add first row pieces to board for player*/
        addPiece(new RookPiece(this, player), 0, first_row);
        addPiece(new ChuckNorrisPiece(this, player), 1, first_row);
        addPiece(new BishopPiece(this, player), 2, first_row);
        addPiece(getPlayerKing(player), 3, first_row);
        addPiece(new KillerQueenPiece(this, player), 4, first_row);
        addPiece(new BishopPiece(this, player), 5, first_row);
        addPiece(new ChuckNorrisPiece(this, player), 6, first_row);
        addPiece(new RookPiece(this, player), 7, first_row);

        /* add pawns for player */
        for(int loc_x = 0; loc_x < getMaxX(); loc_x++){
            addPiece(new PawnPiece(this, player), loc_x, second_row);
        }

    }

    /* get board width */
    public int getMaxX(){
        return board_x;
    }

    /* get board height */
    public int getMaxY(){
        return board_y;
    }

    /* Choose a piece at the given start location and move it to given end location.          */
    /* Checks validity of move: returns 0 on success, 1 and invalid move, 2 on causes check   */
    public int movePiece(int src_x, int src_y, int dest_x, int dest_y) throws Exception {

        Piece piece = getPiece(src_x, src_y);

        /* check if we found a piece at the location */
        if(piece == null){
            return 1;
        }

        /* check if

        /* check if the piece can move to the destination */
        if(!piece.isValidMove(src_x, src_y, dest_x, dest_y)) {
            return 1;
        }

        /*check if this move causes a check on self */
        if(causesCheck(piece, dest_x, dest_y)){
            return 2;
        }

        /* check if there is a piece to capture in the space, if so set flag */
        Piece capturedPiece = spaces[dest_x][dest_y];
        if(capturedPiece != null){
            capturedPiece.setCaptured(true);
        }

        /* change piece location on board */
        spaces[dest_x][dest_y] = piece;
        spaces[src_x][src_y] = null;
        piece.setLocation(dest_x, dest_y);

        /* set hasMoved flag since this piece has moved at least once */
        piece.setHasMoved(true);

        return 0;
    }

    /* Gets piece at given location.  Returns null if out of bounds or no piece is there. */
    public Piece getPiece(int loc_x, int loc_y){

        /* check that start space chosen is in the bounds of the board */
        if(loc_x < 0 || loc_y < 0 || loc_x >= board_x || loc_y >= board_y){
            return null;
        }

        return spaces[loc_x][loc_y];
    }

    /* Used for adding a single piece on board.  Places a piece at the given coordinates.   */
    /* Returns boolean indicating success.  Fails if out of bounds or a piece already is there.                           */
    public boolean addPiece(Piece piece, int loc_x, int loc_y){

        /* check that space chosen is in the bounds of the board */
        if(loc_x < 0 || loc_y < 0 || loc_x >= board_x || loc_y >= board_y){
            return false;
        }

        /* check if a piece is already in this space */
        if(getPiece(loc_x, loc_y) != null){
            return false;
        }

        /* place the piece on the board */
        spaces[loc_x][loc_y] = piece;

        piece.setLocation(loc_x, loc_y);

        /* store pointer to this piece */
        if(piece.getPlayer() == 1){
            player1Pieces.add(piece);
        } else {
            player2Pieces.add(piece);
        }

        return true;
    }

    /* Check for a checkmate on the given player's king */
    public boolean isCheckmated(int player) throws Exception {
        return isInCheck(player) && !playerCanMove(player);
    }

    /* brute force check if the piece can move somewhere valid, avoiding check */
    private boolean isMoveable(Piece piece) throws Exception {

        int loc_x = piece.getLocX();
        int loc_y = piece.getLocY();

        /* loop over all spaces on board, check if there is a valid move there */
        for(int test_x = 0; test_x < getMaxX(); test_x++) {

            for (int test_y = 0; test_y < getMaxY(); test_y++) {

                /* a valid move will satisfy "isValidMove" and will not cause a check */
                if (piece.isValidMove(loc_x, loc_y, test_x, test_y) &&
                        !causesCheck(piece, test_x, test_y)) {
                    return true;
                }

            }
        }

        return false;

    }

    /* test if moving piece to destination causes a check on self or maintains a check */
    public boolean causesCheck(Piece piece, int dest_x, int dest_y) throws Exception {

        /* copy the board, we will manipulate it and need a copy */
        Board copyBoard = new Board(this);

        return causesCheck(piece, dest_x, dest_y, copyBoard);
    }



    /* test if moving piece to destination causes a check on self or maintains a check */
    public boolean causesCheck(Piece piece, int dest_x, int dest_y, Board copyBoard) throws Exception {

        if(piece == null){
            return false;
        }

        /* get the dummy version of 'piece' */
        int src_x = piece.getLocX();
        int src_y = piece.getLocY();
        Piece copyPiece = copyBoard.getPiece(src_x, src_y);

        /* temporarily open up the space we are moving from */
        copyBoard.spaces[src_x][src_y] = null;

        /* temporarily emplace piece in new spot if empty, so checker doesn't find an open space  */
        if(copyBoard.spaces[dest_x][dest_y] == null){
            copyBoard.spaces[dest_x][dest_y] = copyPiece;
            copyPiece.setLocation(dest_x, dest_y);
        }

        /* get appropriate king piece and its coordinates */
        int king_x, king_y;
        Piece kingPiece = copyBoard.getPlayerKing(piece.getPlayer());
        if(copyPiece.getClass() == KingPiece.class){
            king_x = dest_x;
            king_y = dest_y;
        } else {
            king_x = kingPiece.getLocX();
            king_y = kingPiece.getLocY();
        }

        /* temporarily remove king so we can check if a piece can move there */
        copyBoard.spaces[king_x][king_y] = null;

        /* get opponent pieces */
        ArrayList<Piece> opponentPieces = (copyPiece.getPlayer() == 1) ? copyBoard.player2Pieces : copyBoard.player1Pieces;

        /* capture copyPiece so that further recursons of 'causesCheck' ignore it */
        copyPiece.setCaptured(true);

        /* check if each opponent piece could attack king */
        for(Piece oppPiece : opponentPieces){

            /* this piece would have been captured, skip check */
            if(oppPiece.getLocX() == dest_x && oppPiece.getLocY() == dest_y){
                break;
            }

            /* piece is captured, skip */
            if(oppPiece.isCaptured()){
                break;
            }

            /* a valid move will satisfy "isValidMove" and will not cause a check */
            if (oppPiece.isValidMove(oppPiece.getLocX(), oppPiece.getLocY(), king_x, king_y)
                    && !causesCheck(oppPiece, king_x, king_y, copyBoard)){
                return true;
            }

        }

        return false;
    }


    /* revert spaces we swapped while testing for a check.                                                           */
    /* Puts piece and king back in its stored location, and nulls the destination space if 'piece' was placed there. */
    private void revertPositions(Piece piece, Piece kingPiece, int dest_x, int dest_y){
        spaces[piece.getLocX()][piece.getLocY()] = piece;
        if(spaces[dest_x][dest_y] == piece){
            spaces[dest_x][dest_y] = null;
        }
        spaces[kingPiece.getLocX()][kingPiece.getLocY()] = kingPiece;
    }

    /* get corresponding player's king piece */
    public Piece getPlayerKing(int player){
        if(player == 1){
            return player1King;
        }
        else if (player == 2) {
            return player2King;
        }
        else {
            return null;
        }
    }

    /* check if corresponding player is currently in check                 */
    /* checks if keeping the king piece in its current position causesCheck */
    public boolean isInCheck(int player) throws Exception {
        Piece kingPiece = getPlayerKing(player);
        int king_x = kingPiece.getLocX();
        int king_y = kingPiece.getLocY();
        boolean ret = causesCheck(kingPiece, king_x, king_y);
        return ret;
    }

    public boolean isStalemate() throws Exception {
        boolean player1isStalemated = (!isInCheck(1) && !playerCanMove(1));
        boolean player2isStalemated = (!isInCheck(2) && !playerCanMove(2));
        return player1isStalemated || player2isStalemated;
    }

    /* check if any of player's pieces are moveable */
    private boolean playerCanMove(int player) throws Exception {

        ArrayList<Piece> pieceList = (player == 1) ? player1Pieces : player2Pieces;

        for(Piece piece : pieceList){
           if(!piece.isCaptured() && isMoveable(piece)){
               return true;
           }
        }

        return false;
    }

}
