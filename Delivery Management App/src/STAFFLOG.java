import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class STAFFLOG extends JFrame {

    private JTextField idField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;
    private JToggleButton toggleShow;
    private JPanel passPanel;

    private final char defaultEcho;

    public STAFFLOG() {
        super("Staff Login");
        defaultEcho = '\u2022';
        setupUI();
    }

    private void setupUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 250);
        setLocationRelativeTo(null);

        // Gradient background
        JPanel main = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(44, 62, 80),
                        0, getHeight(), new Color(52, 73, 94));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        main.setLayout(new BorderLayout(10, 15));
        main.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Title label
        JLabel title = new JLabel("STAFF LOGIN", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        main.add(title, BorderLayout.NORTH);

        // Form panel
        JPanel form = new JPanel(new GridLayout(0, 1, 11, 11));
        form.setOpaque(false);

        idField = new JTextField();
        idField.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        idField.setBorder(BorderFactory.createTitledBorder("Staff ID"));
        form.add(idField);

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        passwordField.setBorder(BorderFactory.createTitledBorder("Password"));

        toggleShow = new JToggleButton("Show");
        toggleShow.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        toggleShow.setFocusPainted(false);

        passPanel = new JPanel(new BorderLayout(5, 0));
        passPanel.setOpaque(false);
        passPanel.add(passwordField, BorderLayout.CENTER);
        passPanel.add(toggleShow, BorderLayout.EAST);
        form.add(passPanel);

        toggleShow.addActionListener(ae -> {
            if (toggleShow.isSelected()) {
                passwordField.setEchoChar((char) 0);
                toggleShow.setText("Hide");
            } else {
                passwordField.setEchoChar(defaultEcho);
                toggleShow.setText("Show");
            }
        });

        main.add(form, BorderLayout.CENTER);

        // Bottom panel with buttons
        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setOpaque(false);

        JButton backButton = createStyledButton("Back", new Color(231, 76, 60));
        loginButton = createStyledButton("Login", new Color(46, 204, 113));
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Color.WHITE);

        bottom.add(backButton, BorderLayout.WEST);
        bottom.add(statusLabel, BorderLayout.CENTER);
        bottom.add(loginButton, BorderLayout.EAST);

        main.add(bottom, BorderLayout.SOUTH);
        add(main);

        // Button actions
        backButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                try { new base_page(); } catch (Throwable t) {}
            });
            dispose();
        });

        loginButton.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin());

        setVisible(true);
    }

    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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

    private void attemptLogin() {
        final String staffId = idField.getText().trim();
        final String pwd = new String(passwordField.getPassword());

        if (staffId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Staff ID is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (pwd.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        loginButton.setEnabled(false);
        statusLabel.setText("Verifying...");

        new Thread(() -> {
            boolean ok = DeliveryDriver.verifyInhouseCredentials(staffId, pwd);
            SwingUtilities.invokeLater(() -> {
                loginButton.setEnabled(true);
                if (ok) {
                    statusLabel.setText("Login successful.");
                    SwingUtilities.invokeLater(() -> {
                        try {
                            new INHOUSEMANAGEMENT();
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(null, "Failed to open main UI: " + ex.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                            ex.printStackTrace();
                        }
                        dispose();
                    });
                } else {
                    statusLabel.setText("Login failed.");
                    JOptionPane.showMessageDialog(this, "Invalid staff ID or password.",
                            "Authentication Failed", JOptionPane.ERROR_MESSAGE);
                }
            });
        }, "staff-login-thread").start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(STAFFLOG::new);
    }
}
