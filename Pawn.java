import java.util.ArrayList;
import java.util.List;

// Pawn piece (OOP: Inheritance from Piece, Method Overriding of moves(), Encapsulation using private helpers)
class Pawn extends Piece {

    Pawn(Side s) {
        super(s);
    }

    String letter() {
        return "";
    }

    String name() {
        return "Pawn";
    }

    int kind() {
        return 5;
    }

    int value() {
        return 1;
    }

    Piece copy() {
        return mark(new Pawn(side));
    }

    // Direction of travel: White moves up the array (-1), Black moves down (+1)
    private int dir() {
        return side == Side.WHITE ? -1 : 1;
    }

    // Pawn attacks only the two forward diagonal squares
    List<int[]> attacks(Board b, int r, int c) {
        List<int[]> out = new ArrayList<>();
        for (int dc = -1; dc <= 1; dc += 2) {
            if (Board.in(r + dir(), c + dc)) {
                out.add(new int[]{r + dir(), c + dc});
            }
        }
        return out;
    }

    // Overrides the parent method: forward steps, captures and en passant
    @Override
    List<Move> moves(Board b, int r, int c) {
        List<Move> out = new ArrayList<>();
        int nr = r + dir();

        // One step forward, and two steps from the starting row
        if (Board.in(nr, c) && b.at(nr, c) == null) {
            add(out, r, c, nr, c, false);
            int start = side == Side.WHITE ? 6 : 1;
            if (r == start && b.at(nr + dir(), c) == null) {
                add(out, r, c, nr + dir(), c, false);
            }
        }

        // Diagonal captures and en passant
        for (int[] t : attacks(b, r, c)) {
            Piece o = b.at(t[0], t[1]);
            if (o != null && o.side != side) {
                add(out, r, c, t[0], t[1], false);
            } else if (o == null && t[0] == b.epRow && t[1] == b.epCol) {
                add(out, r, c, t[0], t[1], true);
            }
        }
        return out;
    }

    // Adds a move; creates four moves (Q, R, B, N) if the pawn reaches the last row
    private void add(List<Move> out, int fr, int fc, int tr, int tc, boolean ep) {
        if (tr == 0 || tr == 7) {
            for (char p : new char[]{'Q', 'R', 'B', 'N'}) {
                Move m = new Move(fr, fc, tr, tc);
                m.promo = p;
                out.add(m);
            }
        } else {
            Move m = new Move(fr, fc, tr, tc);
            m.enPassant = ep;
            out.add(m);
        }
    }
}
