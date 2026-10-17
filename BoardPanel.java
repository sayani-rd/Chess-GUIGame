import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Panel that draws the board and handles mouse / keyboard input
// (OOP: Inheritance from JPanel, Method Overriding of paintComponent(), Anonymous inner classes for events)
class BoardPanel extends JPanel {
    static final String PIECE_FONT = pickFont();
    private static final Color LIGHT = new Color(240, 217, 181), DARK = new Color(181, 136, 99);
    private static final Color BG = new Color(49, 46, 43);

    private final Chess owner;   // the main window (used to ask promotion and refresh)
    private final Game game;     // the game this panel displays
    private boolean flipped;     // true when the board is shown from Black's side
    private int selR = -1, selC = -1;                  // selected square
    private List<Move> selMoves = new ArrayList<>();   // legal moves of the selected piece
    private int curR = 6, curC = 4;          // keyboard cursor (board coordinates)
    private boolean showCursor;

    // Constructor: sets size and registers mouse and keyboard handlers
    BoardPanel(Chess owner, Game game) {
        this.owner = owner;
        this.game = game;
        setPreferredSize(new Dimension(640, 640));
        setMinimumSize(new Dimension(360, 360));

        // Anonymous classes that override only the needed event methods
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                click(e.getX(), e.getY());
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hover(e.getX(), e.getY());
            }
        });

        // Keyboard controls
        bindKey(KeyEvent.VK_UP,    () -> moveCursor(-1, 0));
        bindKey(KeyEvent.VK_DOWN,  () -> moveCursor(1, 0));
        bindKey(KeyEvent.VK_LEFT,  () -> moveCursor(0, -1));
        bindKey(KeyEvent.VK_RIGHT, () -> moveCursor(0, 1));
        bindKey(KeyEvent.VK_ENTER, this::pressCursor);
        bindKey(KeyEvent.VK_SPACE, this::pressCursor);
        bindKey(KeyEvent.VK_ESCAPE, () -> {
            showCursor = true;
            clearSelection();
        });
    }

    // Links a key to an action
    private void bindKey(int keyCode, Runnable action) {
        String name = "key" + keyCode;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0), name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    // Moves the keyboard cursor in screen directions (up = towards the top of the screen)
    private void moveCursor(int dRow, int dCol) {
        int sign = flipped ? -1 : 1;
        curR = Math.max(0, Math.min(7, curR + sign * dRow));
        curC = Math.max(0, Math.min(7, curC + sign * dCol));
        showCursor = true;
        repaint();
    }

    // Enter / Space pressed: act on the square under the cursor
    private void pressCursor() {
        showCursor = true;
        activate(curR, curC);
    }

    // Chooses a font that can show chess symbols
    private static String pickFont() {
        Set<String> have = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : new String[]{"Segoe UI Symbol", "Apple Symbols", "DejaVu Sans", "FreeSerif",
                                     "Noto Sans Symbols2", "Arial Unicode MS"}) {
            if (have.contains(f)) {
                return f;
            }
        }
        return Font.SANS_SERIF;
    }

    // Turns the board around
    void flip() {
        flipped = !flipped;
        repaint();
    }

    // Removes the current selection
    void clearSelection() {
        selR = selC = -1;
        selMoves = new ArrayList<>();
        repaint();
    }

    // Calculates square size and board position from the panel size
    private int[] geom() {
        int margin = 22;
        int sq = Math.max(10, (Math.min(getWidth(), getHeight()) - 2 * margin) / 8);
        return new int[]{sq, (getWidth() - 8 * sq) / 2, (getHeight() - 8 * sq) / 2};
    }

    // Converts pixel to board square, or null if outside the board
    private int[] squareAt(int x, int y) {
        int[] gm = geom();
        if (x < gm[1] || y < gm[2]) {
            return null;
        }
        int dc = (x - gm[1]) / gm[0], dr = (y - gm[2]) / gm[0];
        if (dc > 7 || dr > 7) {
            return null;
        }
        return new int[]{flipped ? 7 - dr : dr, flipped ? 7 - dc : dc};
    }

    // Shows a hand cursor over pieces that can be selected or squares that can be moved to
    private void hover(int x, int y) {
        int[] s = squareAt(x, y);
        boolean hand = false;
        if (s != null && !game.isOver()) {
            Piece p = game.board.at(s[0], s[1]);
            hand = (p != null && p.side == game.turn);
            for (Move m : selMoves) {
                if (m.tr == s[0] && m.tc == s[1]) {
                    hand = true;
                }
            }
        }
        setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    // Mouse click handler
    private void click(int x, int y) {
        showCursor = false;
        if (game.isOver()) {
            return;
        }
        int[] s = squareAt(x, y);
        if (s == null) {
            clearSelection();
            return;
        }
        activate(s[0], s[1]);
    }

    // Select a piece or play a move on square (r,c) - shared by mouse and keyboard
    private void activate(int r, int c) {
        if (game.isOver()) {
            return;
        }
        curR = r;
        curC = c;

        // If the square is a valid target of the selected piece, play the move
        List<Move> options = new ArrayList<>();
        for (Move m : selMoves) {
            if (m.tr == r && m.tc == c) {
                options.add(m);
            }
        }
        if (!options.isEmpty()) {
            Move chosen = options.get(0);
            if (chosen.promo != 0) {
                char pick = owner.askPromotion(game.turn);
                for (Move m : options) {
                    if (m.promo == pick) {
                        chosen = m;
                    }
                }
            }
            game.play(chosen);
            clearSelection();
            owner.refresh(true);
            return;
        }

        // Otherwise try to select the player's own piece
        Piece p = game.board.at(r, c);
        if (p != null && p.side == game.turn) {
            selR = r;
            selC = c;
            selMoves = game.board.legalMoves(r, c);
        } else {
            selR = selC = -1;
            selMoves = new ArrayList<>();
        }
        repaint();
    }

    // Overrides JPanel's drawing method to paint the board (Method Overriding)
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(BG);
        g.fillRect(0, 0, getWidth(), getHeight());

        int[] gm = geom();
        int sq = gm[0], ox = gm[1], oy = gm[2];
        int[] king = game.check ? game.board.findKing(game.turn) : null;
        Move last = game.last;

        // Draw every square with its highlights and piece
        for (int dr = 0; dr < 8; dr++) {
            for (int dc = 0; dc < 8; dc++) {
                int r = flipped ? 7 - dr : dr, c = flipped ? 7 - dc : dc;
                int x = ox + dc * sq, y = oy + dr * sq;
                g.setColor((r + c) % 2 == 0 ? LIGHT : DARK);
                g.fillRect(x, y, sq, sq);

                // Yellow highlight for the last move
                if (last != null && ((r == last.fr && c == last.fc) || (r == last.tr && c == last.tc))) {
                    g.setColor(new Color(255, 235, 59, 100));
                    g.fillRect(x, y, sq, sq);
                }

                // Green highlight for the selected piece
                if (r == selR && c == selC) {
                    g.setColor(new Color(46, 204, 113, 130));
                    g.fillRect(x, y, sq, sq);
                }

                // Red glow around a king in check
                if (king != null && r == king[0] && c == king[1]) {
                    g.setPaint(new RadialGradientPaint(x + sq / 2f, y + sq / 2f, sq * 0.7f,
                            new float[]{0f, 1f},
                            new Color[]{new Color(255, 40, 40, 220), new Color(255, 40, 40, 0)}));
                    g.fillRect(x, y, sq, sq);
                }

                Piece p = game.board.at(r, c);
                if (p != null) {
                    drawPiece(g, p, x, y, sq);
                }

                // Blue keyboard cursor
                if (showCursor && r == curR && c == curC) {
                    g.setColor(new Color(30, 120, 255));
                    g.setStroke(new BasicStroke(Math.max(3f, sq / 14f)));
                    g.drawRect(x + 2, y + 2, sq - 4, sq - 4);
                }

                // Dots for possible moves, rings for possible captures
                for (Move m : selMoves) {
                    if (m.tr != r || m.tc != c) {
                        continue;
                    }
                    if (p != null || m.enPassant) {
                        g.setColor(new Color(0, 0, 0, 90));
                        g.setStroke(new BasicStroke(Math.max(3f, sq / 12f)));
                        int in = sq / 10;
                        g.drawOval(x + in, y + in, sq - 2 * in, sq - 2 * in);
                    } else {
                        g.setColor(new Color(0, 0, 0, 70));
                        int d = sq / 3;
                        g.fillOval(x + (sq - d) / 2, y + (sq - d) / 2, d, d);
                    }
                    break;
                }
            }
        }

        // Draw file letters and rank numbers around the board
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, sq / 6)));
        g.setColor(new Color(200, 200, 200));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < 8; i++) {
            String file = String.valueOf((char) (flipped ? 'h' - i : 'a' + i));
            String rank = String.valueOf(flipped ? i + 1 : 8 - i);
            g.drawString(file, ox + i * sq + (sq - fm.stringWidth(file)) / 2, oy + 8 * sq + fm.getAscent() + 3);
            g.drawString(rank, ox - fm.stringWidth(rank) - 6, oy + i * sq + (sq + fm.getAscent()) / 2 - 2);
        }
        g.dispose();
    }

    // Draws one piece symbol with an outline so it is visible on any square
    private void drawPiece(Graphics2D g, Piece p, int x, int y, int sq) {
        g.setFont(new Font(PIECE_FONT, Font.PLAIN, (int) (sq * 0.78)));
        FontMetrics fm = g.getFontMetrics();
        String s = p.glyph(true);
        int tx = x + (sq - fm.stringWidth(s)) / 2;
        int ty = y + (sq - fm.getHeight()) / 2 + fm.getAscent();
        boolean white = p.side == Side.WHITE;
        Color fill = white ? new Color(252, 252, 250) : new Color(35, 33, 31);
        Color edge = white ? new Color(30, 28, 26) : new Color(225, 225, 220);
        int o = Math.max(1, sq / 45);

        // Outline first, then the filled symbol on top
        g.setColor(edge);
        for (int dx = -o; dx <= o; dx += o) {
            for (int dy = -o; dy <= o; dy += o) {
                if (dx != 0 || dy != 0) {
                    g.drawString(s, tx + dx, ty + dy);
                }
            }
        }
        g.setColor(fill);
        g.drawString(s, tx, ty);
    }
}
