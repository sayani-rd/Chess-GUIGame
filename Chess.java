import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

// Main game window: side panel, menus and dialogs
// (OOP: Inheritance from JFrame, Composition of Game and BoardPanel, Encapsulation with private members)
public class Chess extends JFrame {
    private final Game game = new Game();                                   // game logic
    private final BoardPanel boardPanel = new BoardPanel(this, game);       // board display
    private final JLabel statusLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel capWhite = new JLabel(" "), capBlack = new JLabel(" ");
    private final JTextArea moveArea = new JTextArea();
    private final JButton undoBtn = new JButton("Undo");

    // Constructor: builds the window
    public Chess() {
        super("Chess");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(boardPanel, BorderLayout.CENTER);
        add(buildSidePanel(), BorderLayout.EAST);
        setJMenuBar(buildMenu());
        pack();
        setMinimumSize(new Dimension(640, 480));
        setLocationRelativeTo(null);
        refresh(false);
    }

    // UI construction :-

    // Builds the right-hand panel (status, captured pieces, move list, buttons)
    private JPanel buildSidePanel() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        side.setPreferredSize(new Dimension(250, 640));

        statusLabel.setOpaque(true);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        Font capFont = new Font(BoardPanel.PIECE_FONT, Font.PLAIN, 20);
        capWhite.setFont(capFont);
        capBlack.setFont(capFont);

        moveArea.setEditable(false);
        moveArea.setFocusable(false);
        moveArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        moveArea.setMargin(new Insets(6, 8, 6, 8));
        JScrollPane scroll = new JScrollPane(moveArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Moves"));

        // Buttons with lambda expressions as event handlers
        JPanel btns = new JPanel(new GridLayout(0, 2, 6, 6));
        JButton newBtn = new JButton("New Game");
        JButton flipBtn = new JButton("Flip Board");
        JButton rulesBtn = new JButton("Rules");
        newBtn.addActionListener(e -> newGame());
        undoBtn.addActionListener(e -> undo());
        flipBtn.addActionListener(e -> boardPanel.flip());
        rulesBtn.addActionListener(e -> showRules());
        for (JButton b : new JButton[]{newBtn, undoBtn, flipBtn, rulesBtn}) {
            b.setFocusPainted(false);
            b.setFocusable(false);
            btns.add(b);
        }
        btns.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));

        JLabel tip = new JLabel("<html><center><i>Click a piece, then a highlighted square.<br>Or use the arrow keys and Enter.</i></center></html>",
                SwingConstants.CENTER);
        tip.setAlignmentX(Component.CENTER_ALIGNMENT);

        side.add(statusLabel);
        side.add(Box.createVerticalStrut(10));
        side.add(labelled("Captured by White:", capWhite));
        side.add(labelled("Captured by Black:", capBlack));
        side.add(Box.createVerticalStrut(6));
        side.add(scroll);
        side.add(Box.createVerticalStrut(10));
        side.add(btns);
        side.add(Box.createVerticalStrut(8));
        side.add(tip);
        return side;
    }

    // Puts a title above a label
    private JPanel labelled(String title, JLabel content) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel(title), BorderLayout.NORTH);
        p.add(content, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        p.setAlignmentX(Component.CENTER_ALIGNMENT);
        return p;
    }

    // Builds the menu bar (Game and Help menus)
    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();
        int mod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        JMenu gameMenu = new JMenu("Game");
        gameMenu.add(item("New Game", KeyStroke.getKeyStroke(KeyEvent.VK_N, mod), e -> newGame()));
        gameMenu.add(item("Undo Move", KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod), e -> undo()));
        gameMenu.add(item("Flip Board", KeyStroke.getKeyStroke(KeyEvent.VK_F, mod), e -> boardPanel.flip()));
        gameMenu.addSeparator();
        gameMenu.add(item("Exit", null, e -> System.exit(0)));

        JMenu help = new JMenu("Help");
        help.add(item("Rules of Chess", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), e -> showRules()));
        help.add(item("About", null, e -> JOptionPane.showMessageDialog(this,
                "Java Swing Chess\nTwo players, full rules.", "About", JOptionPane.INFORMATION_MESSAGE)));
        bar.add(gameMenu);
        bar.add(help);
        return bar;
    }

    // Creates one menu item with an optional shortcut key
    private JMenuItem item(String text, KeyStroke key, ActionListener a) {
        JMenuItem i = new JMenuItem(text);
        if (key != null) {
            i.setAccelerator(key);
        }
        i.addActionListener(a);
        return i;
    }

    // Actions :-

    // Starts a new game (asks first if a game is in progress)
    private void newGame() {
        if (!game.notation.isEmpty() && !game.isOver()) {
            int a = JOptionPane.showConfirmDialog(this, "Abandon the current game and start a new one?",
                    "New Game", JOptionPane.YES_NO_OPTION);
            if (a != JOptionPane.YES_OPTION) {
                return;
            }
        }
        game.reset();
        boardPanel.clearSelection();
        refresh(false);
    }

    // Takes back the last move
    private void undo() {
        game.undo();
        boardPanel.clearSelection();
        refresh(false);
    }

    // Asks which piece a pawn should become
    char askPromotion(Side side) {
        String[] names = {"Queen", "Rook", "Bishop", "Knight"};
        char[] codes = {'Q', 'R', 'B', 'N'};
        int choice = JOptionPane.showOptionDialog(this, "Promote your pawn to:", "Pawn promotion",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
        return choice < 0 ? 'Q' : codes[choice];
    }

    // Syncs every widget with the game state
    void refresh(boolean announce) {
        statusLabel.setText("<html><center>" + game.statusText() + "</center></html>");
        boolean whiteTurn = game.turn == Side.WHITE;

        // Status colours: gold = game over, red = check, otherwise the colour of the side to move
        if (game.isOver()) {
            statusLabel.setBackground(new Color(255, 214, 102));
            statusLabel.setForeground(Color.BLACK);
        } else if (game.check) {
            statusLabel.setBackground(new Color(200, 50, 50));
            statusLabel.setForeground(Color.WHITE);
        } else {
            statusLabel.setBackground(whiteTurn ? new Color(245, 245, 240) : new Color(50, 48, 46));
            statusLabel.setForeground(whiteTurn ? Color.BLACK : Color.WHITE);
        }
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY), BorderFactory.createEmptyBorder(10, 8, 10, 8)));

        // Captured pieces and material advantage
        int bal = game.materialBalance();
        capWhite.setText(captured(game.capturedByWhite) + (bal > 0 ? "  +" + bal : ""));
        capBlack.setText(captured(game.capturedByBlack) + (bal < 0 ? "  +" + (-bal) : ""));

        // Move list, two moves (White and Black) per line
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < game.notation.size(); i += 2) {
            sb.append(String.format("%2d. %-8s", i / 2 + 1, game.notation.get(i)));
            if (i + 1 < game.notation.size()) {
                sb.append(game.notation.get(i + 1));
            }
            sb.append('\n');
        }
        moveArea.setText(sb.toString());
        moveArea.setCaretPosition(moveArea.getDocument().getLength());

        undoBtn.setEnabled(game.canUndo());
        boardPanel.repaint();

        // Show the result in a dialog when the game has just ended
        if (announce && game.isOver()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, game.statusText(),
                    "Game over", JOptionPane.INFORMATION_MESSAGE));
        }
    }

    // Builds the captured-pieces text, most valuable piece first
    private String captured(List<Piece> list) {
        List<Piece> sorted = new ArrayList<>(list);
        sorted.sort((a, b) -> b.value() - a.value());
        StringBuilder sb = new StringBuilder();
        for (Piece p : sorted) {
            sb.append(p.glyph(p.side == Side.BLACK));
        }
        return sb.length() == 0 ? " " : sb.toString();
    }

    // Rules dialog :-

    // Shows the rules of chess and the controls of this program
    private void showRules() {
        String html =
            "<html><body style='font-family:sans-serif; font-size:13px; margin:8px'>" +

            "<h2>The goal</h2>" +
            "<p>Trap the other player's King so it cannot escape. This is called <b>checkmate</b>. " +
            "White always moves first, then you take turns.</p>" +

            "<h2>How each piece moves</h2><ul>" +
            "<li><b>King</b> - 1 square in any direction.</li>" +
            "<li><b>Queen</b> - any distance in a straight line or diagonal.</li>" +
            "<li><b>Rook</b> - any distance up, down, left or right.</li>" +
            "<li><b>Bishop</b> - any distance diagonally.</li>" +
            "<li><b>Knight</b> - jumps in an L shape (2 squares one way, 1 to the side). It can jump over pieces.</li>" +
            "<li><b>Pawn</b> - 1 step forward (2 on its first move). It captures 1 step diagonally.</li></ul>" +
            "<p>To capture, move onto an enemy piece. Pieces cannot jump over others (only the Knight can).</p>" +

            "<h2>Check</h2>" +
            "<p>When your King is attacked, you are in <b>check</b>. You must fix it right away by moving the King, " +
            "blocking the attack, or capturing the attacker. You can never make a move that leaves your King in danger.</p>" +

            "<h2>Special moves</h2><ul>" +
            "<li><b>Castling</b> - move your King 2 squares toward a Rook and the Rook hops over it. " +
            "Only works if neither has moved yet, nothing is in between, and the King is not in check " +
            "or moving through danger.</li>" +
            "<li><b>En passant</b> - if an enemy Pawn jumps 2 squares and lands next to your Pawn, " +
            "you can capture it as if it moved only 1 square. You must do it right away.</li>" +
            "<li><b>Promotion</b> - a Pawn that reaches the far end of the board becomes a Queen, Rook, " +
            "Bishop or Knight (you choose).</li></ul>" +

            "<h2>How the game ends</h2><ul>" +
            "<li><b>Checkmate</b> - the King is in check and cannot escape. That player loses.</li>" +
            "<li><b>Draw</b> - nobody wins. This happens when a player has no legal move but is not in check " +
            "(stalemate), when neither side has enough pieces to win, when the same position appears 3 times, " +
            "or after 50 moves with no capture or Pawn move.</li></ul>" +

            "<h2>How to play here</h2><ul>" +
            "<li><b>Mouse:</b> click a piece, then click a highlighted square. " +
            "Dots show where it can go; rings show captures.</li>" +
            "<li><b>Keyboard:</b> use the <b>arrow keys</b> to move the blue square, " +
            "<b>Enter</b> or <b>Space</b> to pick a piece and then to move it, and <b>Esc</b> to cancel.</li>" +
            "<li>The last move is yellow. A King in check glows red.</li>" +
            "<li><b>Undo</b> takes back a move. <b>Flip Board</b> turns the board around.</li></ul>" +
            "</body></html>";

        JEditorPane pane = new JEditorPane("text/html", html);
        pane.setEditable(false);
        pane.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(pane);
        sp.setPreferredSize(new Dimension(560, 520));
        JOptionPane.showMessageDialog(this, sp, "Rules of Chess", JOptionPane.PLAIN_MESSAGE);
    }
}
