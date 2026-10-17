// Class that stores one move (OOP: Class and Objects, Encapsulation of move data)
class Move {
    final int fr, fc, tr, tc;  // from row/col and to row/col
    char promo = 0;            // 'Q','R','B','N' when a pawn promotes
    boolean castle, enPassant; // flags for special moves

    // Constructor: creates a plain move from one square to another
    Move(int fr, int fc, int tr, int tc) {
        this.fr = fr;
        this.fc = fc;
        this.tr = tr;
        this.tc = tc;
    }
}
