import java.util.ArrayList;
import java.util.List;

// Abstract parent class of all chess pieces (OOP: Abstraction, Inheritance, Polymorphism)
abstract class Piece {
    // Direction tables shared by all pieces (static constants)
    static final int[][] ROOK_DIRS    = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    static final int[][] BISHOP_DIRS  = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    static final int[][] ALL_DIRS     = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    static final int[][] KNIGHT_JUMPS = {{2, 1}, {2, -1}, {-2, 1}, {-2, -1}, {1, 2}, {1, -2}, {-1, 2}, {-1, -2}};

    final Side side;   // colour of the piece
    boolean moved;     // needed for castling and pawn rules

    // Constructor: every piece must belong to a side
    Piece(Side side) {
        this.side = side;
    }

    // Abstract methods: each child class must give its own definition
    abstract String letter();   // SAN letter ("" for pawn)

    abstract String name();

    abstract int kind();        // 0=K 1=Q 2=R 3=B 4=N 5=P  (unicode offset)

    abstract int value();

    abstract Piece copy();

    // Squares this piece attacks (ignoring pins / check)
    abstract List<int[]> attacks(Board b, int r, int c);

    // Pseudo-legal moves (may leave own king in check; Board filters those)
    // Child classes can override this method (Polymorphism)
    List<Move> moves(Board b, int r, int c) {
        List<Move> out = new ArrayList<>();
        for (int[] t : attacks(b, r, c)) {
            Piece o = b.at(t[0], t[1]);
            if (o == null || o.side != side) {
                out.add(new Move(r, c, t[0], t[1]));
            }
        }
        return out;
    }

    // Unicode chess glyph. Solid set is used for drawing, hollow for captured list
    String glyph(boolean solid) {
        return new String(Character.toChars(0x2654 + (solid ? 6 : 0) + kind()));
    }

    // Copies the "moved" flag onto a new piece (used by copy())
    protected Piece mark(Piece p) {
        p.moved = moved;
        return p;
    }

    // Helper for long-range pieces: slides along directions until blocked
    protected List<int[]> slide(Board b, int r, int c, int[][] dirs) {
        List<int[]> out = new ArrayList<>();
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            while (Board.in(nr, nc)) {
                out.add(new int[]{nr, nc});
                if (b.at(nr, nc) != null) {
                    break;
                }
                nr += d[0];
                nc += d[1];
            }
        }
        return out;
    }

    // Helper for short-range pieces: one fixed jump per direction
    protected List<int[]> step(int r, int c, int[][] deltas) {
        List<int[]> out = new ArrayList<>();
        for (int[] d : deltas) {
            if (Board.in(r + d[0], c + d[1])) {
                out.add(new int[]{r + d[0], c + d[1]});
            }
        }
        return out;
    }

    // Creates the new piece chosen when a pawn is promoted
    static Piece promote(char c, Side s) {
        Piece p;
        switch (c) {
            case 'R':
                p = new Rook(s);
                break;
            case 'B':
                p = new Bishop(s);
                break;
            case 'N':
                p = new Knight(s);
                break;
            default:
                p = new Queen(s);
        }
        p.moved = true;
        return p;
    }
}
