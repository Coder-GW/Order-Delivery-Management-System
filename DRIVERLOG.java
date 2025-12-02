import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/*
 Simple login window:
 - enter driver ID and password
 - on success: open main GUI (GUI.java) and close this window
 - on failure: show error
*/
public class DRIVERLOG extends JFrame {
	// UI
	private JTextField idField;
	private JPasswordField passwordField;
	private JButton loginButton;
	private JLabel statusLabel;
	// Show/hide toggle for password visibility
	private char defaultEcho;
	private JToggleButton toggleShow;
	private JPanel passPanel;

	public DRIVERLOG() {
		super("Delivery System - Login");
		setupUI();
	}

	private void setupUI() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(400, 200);
		setLocationRelativeTo(null);
		setLayout(new BorderLayout(8,8));

		JPanel form = new JPanel(new GridLayout(0,2,6,6));
		form.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));

		form.add(new JLabel("Driver ID:"));
		idField = new JTextField();
		form.add(idField);

		form.add(new JLabel("Password:"));
		passwordField = new JPasswordField();
		// Show/hide toggle for password visibility
		defaultEcho = passwordField.getEchoChar();
		toggleShow = new JToggleButton("Show");
		passPanel = new JPanel(new BorderLayout(4,0));
		passPanel.add(passwordField, BorderLayout.CENTER);
		passPanel.add(toggleShow, BorderLayout.EAST);
		form.add(passPanel);
		toggleShow.addActionListener(ae -> {
			if (toggleShow.isSelected()) {
				passwordField.setEchoChar((char)0);
				toggleShow.setText("Hide");
			} else {
				passwordField.setEchoChar(defaultEcho);
				toggleShow.setText("Show");
			}
		});

		add(form, BorderLayout.CENTER);

		JPanel bottom = new JPanel(new BorderLayout(6,6));
		// Back button to return to selection screen
		JButton backButton = new JButton("Back");
		loginButton = new JButton("Login");
		statusLabel = new JLabel(" ");

		bottom.add(backButton, BorderLayout.WEST);
		bottom.add(statusLabel, BorderLayout.CENTER);
		bottom.add(loginButton, BorderLayout.EAST);
		bottom.setBorder(BorderFactory.createEmptyBorder(0,12,12,12));

		add(bottom, BorderLayout.SOUTH);

		// Back action -> show base selection
		backButton.addActionListener(e -> {
			SwingUtilities.invokeLater(() -> {
				try { new base_page(); } catch (Throwable t) { /* ignore */ }
			});
			dispose();
		});

		// login action
		loginButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				attemptLogin();
			}
		});

		// Enter key on password triggers login
		passwordField.addActionListener(e -> attemptLogin());

		setVisible(true);
	}

	private void attemptLogin() {
		final String driverId = idField.getText().trim();
		final String pwd = new String(passwordField.getPassword());

		if (driverId.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Driver ID is required.", "Validation", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (pwd.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Password is required.", "Validation", JOptionPane.WARNING_MESSAGE);
			return;
		}

		// UI changes on EDT
		loginButton.setEnabled(false);
		statusLabel.setText("Verifying...");

		// run verify off the EDT
		new Thread(() -> {
			boolean ok = DeliveryDriver.verifyCredentials(driverId, pwd);
			SwingUtilities.invokeLater(() -> {
				loginButton.setEnabled(true);
				if (ok) {
					// success -> open main GUI and close login
					statusLabel.setText("Login successful.");
					// open main GUI (ensure on EDT)
					SwingUtilities.invokeLater(() -> {
						try {
							new DRIVERMANAGEMENT(driverId); // main application window
						} catch (Exception ex) {
							// if GUI constructor throws, show error
							JOptionPane.showMessageDialog(null, "Failed to open main UI: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
							ex.printStackTrace();
						}
						dispose();
					});
				} else {
					statusLabel.setText("Login failed.");
					JOptionPane.showMessageDialog(DRIVERLOG.this, "Invalid driver ID or password.", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
				}
			});
		}, "login-net").start();
	}

	// simple launcher
	public static void main(String[] args) {
    	SwingUtilities.invokeLater(DRIVERLOG::new);
	}
}