package to.pkgdo.list;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

public class MainApp {

    public static void main(String[] args) {
        // Basic Swing Setup
        JFrame frame = new JFrame("Smart To-Do List");
        frame.setSize(600, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);

        // Input Fields
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

        // Table to show tasks
        JTable taskTable = new JTable();
        JScrollPane tableScroll = new JScrollPane(taskTable);
        tableScroll.setBounds(20, 100, 530, 300);
        frame.add(tableScroll);

        Connection conn = DatabaseConnection.getConnection();

        // Load all tasks
        Runnable loadTasks = () -> {
            try {
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tasks");
                ResultSet rs = stmt.executeQuery();
                taskTable.setModel(buildTableModel(rs));
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Error loading tasks: " + ex.getMessage());
            }
        };

        // Add task to DB
        addButton.addActionListener(e -> {
            String title = titleField.getText().trim();
            String cat = categoryBox.getSelectedItem().toString();
            String date = dateField.getText().trim();
            String time = timeField.getText().trim();

            if (!title.isEmpty() && !date.isEmpty() && !time.isEmpty()) {
                try {
                    PreparedStatement stmt = conn.prepareStatement("INSERT INTO tasks(title, category, deadline_date, deadline_time) VALUES (?, ?, ?, ?)");
                    stmt.setString(1, title);
                    stmt.setString(2, cat);
                    stmt.setString(3, date);
                    stmt.setString(4, time);
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

        // Mark selected task as done
        markDoneButton.addActionListener(e -> {
            int row = taskTable.getSelectedRow();
            if (row != -1) {
                int id = Integer.parseInt(taskTable.getValueAt(row, 0).toString());
                try {
                    PreparedStatement stmt = conn.prepareStatement("UPDATE tasks SET is_done = true WHERE id = ?");
                    stmt.setInt(1, id);
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(frame, " Task marked done.");
                    loadTasks.run();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(frame, "Update error: " + ex.getMessage());
                }
            }
        });

        // Filter pending tasks
        showPendingButton.addActionListener(e -> {
            try {
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tasks WHERE is_done = false");
                ResultSet rs = stmt.executeQuery();
                taskTable.setModel(buildTableModel(rs));
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
            }
        });

        // Initial load
        loadTasks.run();
        frame.setVisible(true);
    }

    // Convert SQL result into table model
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
