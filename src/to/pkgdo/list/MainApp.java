package to.pkgdo.list;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

public class MainApp {
    private static int currentUserId = -1; // Track logged-in user
    
    public static void main(String[] args) {
        showLoginScreen();
    }
    
    private static void showLoginScreen() {
        JFrame loginFrame = new JFrame("Login or Register");
        loginFrame.setSize(400, 300);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLayout(new GridLayout(5, 2, 10, 10));
        
        JLabel userLabel = new JLabel("Username:");
        JTextField userField = new JTextField();
        JLabel passLabel = new JLabel("Password:");
        JPasswordField passField = new JPasswordField();
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");
        
        loginFrame.add(userLabel);
        loginFrame.add(userField);
        loginFrame.add(passLabel);
        loginFrame.add(passField);
        loginFrame.add(loginButton);
        loginFrame.add(registerButton);
        
        Connection conn = DatabaseConnection.getConnection();
        
        loginButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();
            
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(loginFrame, "Please enter both username and password");
                return;
            }
            
            try {
                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id FROM users WHERE username = ? AND password = ?");
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    currentUserId = rs.getInt("id");
                    loginFrame.dispose();
                    showMainApplication();
                } else {
                    JOptionPane.showMessageDialog(loginFrame, "Invalid credentials");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(loginFrame, "Error: " + ex.getMessage());
            }
        });
        
        registerButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();
            
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(loginFrame, "Please enter both username and password");
                return;
            }
            
            try {
                // Check if username exists
                PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT id FROM users WHERE username = ?");
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                
                if (rs.next()) {
                    JOptionPane.showMessageDialog(loginFrame, "Username already exists");
                    return;
                }
                
                // Create new user
                PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO users (username, password) VALUES (?, ?)", 
                    Statement.RETURN_GENERATED_KEYS);
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                insertStmt.executeUpdate();
                
                // Get the new user's ID
                ResultSet keys = insertStmt.getGeneratedKeys();
                if (keys.next()) {
                    currentUserId = keys.getInt(1);
                    loginFrame.dispose();
                    showMainApplication();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(loginFrame, "Error: " + ex.getMessage());
            }
        });
        
        loginFrame.setVisible(true);
    }
    
    private static void showMainApplication() {
        JFrame frame = new JFrame("Smart To-Do List - User ID: " + currentUserId);
        frame.setSize(600, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);

        JTextField titleField = new JTextField("Task title");
        titleField.setBounds(20, 20, 200, 30);
        frame.add(titleField);

        String[] categories = {"Study", "Work", "Health", "Other"};
        JComboBox<String> categoryBox = new JComboBox<>(categories);
        categoryBox.setBounds(230, 20, 100, 30);
        frame.add(categoryBox);

        JTextField dateField = new JTextField("yyyy-mm-dd");
        dateField.setBounds(340, 20, 100, 30);
        frame.add(dateField);

        JTextField timeField = new JTextField("hh:mm");
        timeField.setBounds(450, 20, 100, 30);
        frame.add(timeField);

        JButton addButton = new JButton("Add Task");
        addButton.setBounds(20, 60, 130, 30);
        frame.add(addButton);

        JButton markDoneButton = new JButton("Mark Done");
        markDoneButton.setBounds(160, 60, 130, 30);
        frame.add(markDoneButton);

        JButton showPendingButton = new JButton("Show Pending");
        showPendingButton.setBounds(300, 60, 150, 30);
        frame.add(showPendingButton);
        
        JButton logoutButton = new JButton("Logout");
        logoutButton.setBounds(460, 60, 90, 30);
        frame.add(logoutButton);

        JTable taskTable = new JTable();
        JScrollPane tableScroll = new JScrollPane(taskTable);
        tableScroll.setBounds(20, 100, 530, 300);
        frame.add(tableScroll);

        Connection conn = DatabaseConnection.getConnection();

        Runnable loadTasks = () -> {
            try {
                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM tasks WHERE user_id = ?");
                stmt.setInt(1, currentUserId);
                ResultSet rs = stmt.executeQuery();
                taskTable.setModel(buildTableModel(rs));
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Error loading tasks: " + ex.getMessage());
            }
        };

        addButton.addActionListener(e -> {
            String title = titleField.getText().trim();
            String cat = categoryBox.getSelectedItem().toString();
            String date = dateField.getText().trim();
            String time = timeField.getText().trim();

            if (!title.isEmpty() && !date.isEmpty() && !time.isEmpty()) {
                try {
                    PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO tasks(title, category, deadline_date, deadline_time, user_id, is_done) " +
                        "VALUES (?, ?, ?, ?, ?, false)");
                    stmt.setString(1, title);
                    stmt.setString(2, cat);
                    stmt.setString(3, date);
                    stmt.setString(4, time);
                    stmt.setInt(5, currentUserId);
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(frame, "Task added!");
                    loadTasks.run();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Fill all fields");
            }
        });

        markDoneButton.addActionListener(e -> {
            int row = taskTable.getSelectedRow();
            if (row != -1) {
                int id = Integer.parseInt(taskTable.getValueAt(row, 0).toString());
                try {
                    PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE tasks SET is_done = true WHERE id = ? AND user_id = ?");
                    stmt.setInt(1, id);
                    stmt.setInt(2, currentUserId);
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(frame, "Task marked done.");
                    loadTasks.run();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(frame, "Update error: " + ex.getMessage());
                }
            }
        });

        showPendingButton.addActionListener(e -> {
            try {
                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM tasks WHERE is_done = false AND user_id = ?");
                stmt.setInt(1, currentUserId);
                ResultSet rs = stmt.executeQuery();
                taskTable.setModel(buildTableModel(rs));
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
            }
        });
        
        logoutButton.addActionListener(e -> {
            currentUserId = -1;
            frame.dispose();
            showLoginScreen();
        });

        loadTasks.run();
        frame.setVisible(true);
    }

    public static javax.swing.table.DefaultTableModel buildTableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Vector<String> colNames = new Vector<>();
        int cols = meta.getColumnCount();
        for (int i = 1; i <= cols; i++) {
            colNames.add(meta.getColumnName(i));
        }

        Vector<Vector<Object>> data = new Vector<>();
        while (rs.next()) {
            Vector<Object> row = new Vector<>();
            for (int i = 1; i <= cols; i++) {
                row.add(rs.getObject(i));
            }
            data.add(row);
        }

        return new javax.swing.table.DefaultTableModel(data, colNames);
    }
}