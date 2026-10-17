import java.util.List;

// Knight piece (OOP: Inheritance from Piece, Polymorphism through its own attacks() rule)
class Knight extends Piece {

    Knight(Side s) {
        super(s);
    }

    String letter() {
        return "N";
    }

    String name() {
        return "Knight";
    }

    int kind() {
        return 4;
    }

    int value() {
        return 3;
    }

    Piece copy() {
        return mark(new Knight(side));
    }

    // Knight jumps in an L shape and can jump over other pieces
    List<int[]> attacks(Board b, int r, int c) {
        return step(r, c, KNIGHT_JUMPS);
    }
}
