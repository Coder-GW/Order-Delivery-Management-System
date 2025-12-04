import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class base_page {

    public base_page() {
        JFrame frame = new JFrame("Login Selection");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(450, 250);
        frame.setLocationRelativeTo(null);

        // Main panel with gradient background
        JPanel main = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(45, 52, 54),
                        0, getHeight(), new Color(99, 110, 114));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        main.setLayout(new BorderLayout(10, 20));
        main.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Title label
        JLabel question = new JLabel("Who are you trying to log in as?", SwingConstants.CENTER);
        question.setFont(new Font("Segoe UI", Font.BOLD, 20));
        question.setForeground(Color.WHITE);
        main.add(question, BorderLayout.NORTH);

        // Button panel
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
        buttons.setOpaque(false); // transparent to show gradient background

        JButton adminBtn = createStyledButton("Admin", new Color(52, 152, 219));
        JButton staffBtn = createStyledButton("Staff", new Color(46, 204, 113));

        adminBtn.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                try { new ADMINLOG(); } catch (Throwable t) {}
            });
            frame.dispose();
        });

        staffBtn.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                try { new STAFFLOG(); } catch (Throwable t) {}
            });
            frame.dispose();
        });

        buttons.add(adminBtn);
        buttons.add(staffBtn);

        main.add(buttons, BorderLayout.CENTER);
        frame.setContentPane(main);
        frame.setVisible(true);
    }

    // Helper to create modern styled buttons
    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(color.darker());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });
        return btn;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(base_page::new);
    }
}
