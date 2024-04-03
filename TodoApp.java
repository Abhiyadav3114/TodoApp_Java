package project;

import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.sql.*;

public class TodoApp extends JPanel {
    private DefaultListModel<TodoItem> todoListModel;
    private JList<TodoItem> todoList;
    private JTextField inputField;
    private JDateChooser dateChooser;
    private JSpinner timeSpinner;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/todo_app_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "12001880";

    public TodoApp() {
        todoListModel = new DefaultListModel<>();
        todoList = new JList<>(todoListModel);
        todoList.setCellRenderer(new TodoListRenderer());
        JScrollPane scrollPane = new JScrollPane(todoList);
        inputField = new JTextField();
        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");

        dateChooser = new JDateChooser();
        dateChooser.setDateFormatString("yyyy-MM-dd");

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.AM_PM, Calendar.AM);

        SpinnerDateModel spinnerModel = new SpinnerDateModel(calendar.getTime(), null, null, Calendar.HOUR);
        timeSpinner = new JSpinner(spinnerModel);

        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "hh:mm a");
        timeSpinner.setEditor(timeEditor);

        Dimension spinnerSize = timeSpinner.getPreferredSize();
        spinnerSize = new Dimension(80, spinnerSize.height);
        timeSpinner.setPreferredSize(spinnerSize);

        setLayout(null);

        int panelWidth = 1100;
        int panelHeight = 400;
        int componentWidth = 100;
        int componentHeight = 25;
        int x = (panelWidth - componentWidth) / 2;
        int y = (panelHeight - componentHeight) / 2;

        JLabel taskLabel = new JLabel("Task:");
        JLabel dateLabel = new JLabel("Date:");
        JLabel timeLabel = new JLabel("Time:");

        taskLabel.setBounds(x - 60, y + 5, 50, 20);
        inputField.setBounds(x, y, 270, 40);
        dateLabel.setBounds(x - 60, y + 60, 50, 20);
        dateChooser.setBounds(x, y + 60, 270, 35);
        timeLabel.setBounds(x - 60, y + 110, 50, 20);
        timeSpinner.setBounds(x, y + 110, 270, 35);
        addButton.setBounds(x + 320, y + 5, 90, 30);
        editButton.setBounds(x + 60, y + 380, 100, 40);
        deleteButton.setBounds(x + 230, y + 380, 100, 40);
        scrollPane.setBounds(x, y + 160, 410, 200);

        add(taskLabel);
        add(inputField);
        add(dateLabel);
        add(dateChooser);
        add(timeLabel);
        add(timeSpinner);
        add(addButton);
        add(scrollPane);
        add(editButton);
        add(deleteButton);

        addButton.addActionListener(e -> addTodo());
        editButton.addActionListener(e -> editTodo());
        deleteButton.addActionListener(e -> deleteTodo());

        loadTodosFromDatabase();
    }

    private void loadTodosFromDatabase() {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT * FROM todo_items";
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String text = resultSet.getString("text");
                Date datetime = resultSet.getTimestamp("datetime");
                TodoItem todoItem = new TodoItem(id, text, datetime);
                todoListModel.addElement(todoItem);
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addTodo() {
        String todoText = inputField.getText().trim();
        Date selectedDate = dateChooser.getDate();
        Date selectedTime = (Date) timeSpinner.getValue();
        if (!todoText.isEmpty() && selectedDate != null) {
            selectedDate.setHours(selectedTime.getHours());
            selectedDate.setMinutes(selectedTime.getMinutes());
            selectedDate.setSeconds(selectedTime.getSeconds());

            TodoItem todoItem = new TodoItem(todoText, selectedDate);
            todoListModel.addElement(todoItem);
            addTodoToDatabase(todoItem);
            inputField.setText("");
            dateChooser.setDate(null);
            timeSpinner.setValue(new Date());
        } else {
            JOptionPane.showMessageDialog(this, "Please enter a task and select a date.");
        }
    }

    private void editTodo() {
        int selectedIndex = todoList.getSelectedIndex();
        if (selectedIndex != -1) {
            TodoItem selectedTodo = todoListModel.getElementAt(selectedIndex);

            String newText = JOptionPane.showInputDialog(this, "Edit Task", selectedTodo.getText());
            if (newText != null && !newText.isEmpty()) {
                selectedTodo.setText(newText);

                // Create a new JDialog to update date and time
                JDialog dialog = new JDialog();
                dialog.setTitle("Edit Date and Time");
                dialog.setModal(true);

                JDateChooser updatedDateChooser = new JDateChooser();
                updatedDateChooser.setDateFormatString("yyyy-MM-dd");
                updatedDateChooser.setDate(selectedTodo.getDatetime());

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(selectedTodo.getDatetime());
                SpinnerDateModel spinnerModel = new SpinnerDateModel(calendar.getTime(), null, null, Calendar.HOUR);
                JSpinner updatedTimeSpinner = new JSpinner(spinnerModel);
                JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(updatedTimeSpinner, "hh:mm a");
                updatedTimeSpinner.setEditor(timeEditor);

                JButton okButton = new JButton("OK");
                okButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Date selectedDate = updatedDateChooser.getDate();
                        Date selectedTime = (Date) updatedTimeSpinner.getValue();
                        selectedDate.setHours(selectedTime.getHours());
                        selectedDate.setMinutes(selectedTime.getMinutes());
                        selectedDate.setSeconds(selectedTime.getSeconds());
                        selectedTodo.setDatetime(selectedDate);
                        updateTodoInDatabase(selectedTodo);
                        dialog.dispose();
                    }
                });

                JButton cancelButton = new JButton("Cancel");
                cancelButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        dialog.dispose();
                    }
                });

                JPanel buttonPanel = new JPanel();
                buttonPanel.add(okButton);
                buttonPanel.add(cancelButton);

                dialog.setLayout(new BorderLayout());
                dialog.add(updatedDateChooser, BorderLayout.NORTH);
                dialog.add(updatedTimeSpinner, BorderLayout.CENTER);
                dialog.add(buttonPanel, BorderLayout.SOUTH);

                dialog.pack();
                dialog.setLocationRelativeTo(this);
                dialog.setVisible(true);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a task to edit.");
        }
    }


    private void deleteTodo() {
        int selectedIndex = todoList.getSelectedIndex();
        if (selectedIndex != -1) {
            TodoItem selectedTodo = todoListModel.getElementAt(selectedIndex);
            todoListModel.remove(selectedIndex);
            deleteTodoFromDatabase(selectedTodo);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a task to delete.");
        }
    }

    private void addTodoToDatabase(TodoItem todoItem) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "INSERT INTO todo_items (text, datetime) VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, todoItem.getText());
            statement.setTimestamp(2, new Timestamp(todoItem.getDatetime().getTime()));
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateTodoInDatabase(TodoItem todoItem) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "UPDATE todo_items SET text = ?, datetime = ? WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, todoItem.getText());
            statement.setTimestamp(2, new Timestamp(todoItem.getDatetime().getTime()));
            statement.setInt(3, todoItem.getId());  // Set the id parameter
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void deleteTodoFromDatabase(TodoItem todoItem) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "DELETE FROM todo_items WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, todoItem.getId());
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static class TodoItem {
        private int id;
        private String text;
        private Date datetime;

        public TodoItem(int id, String text, Date datetime) {
            this.id = id;
            this.text = text;
            this.datetime = datetime;
        }

        public TodoItem(String text, Date datetime) {
            this.text = text;
            this.datetime = datetime;
        }

        public int getId() {
            return id;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Date getDatetime() {
            return datetime;
        }

        public void setDatetime(Date datetime) {
            this.datetime = datetime;
        }

        @Override
        public String toString() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd hh:mm a");
            return text + " - " + dateFormat.format(datetime);
        }
    }

    private class TodoListRenderer extends JPanel implements ListCellRenderer<TodoItem> {
        private TodoItem item;
        private JLabel label;

        public TodoListRenderer() {
            setLayout(new BorderLayout());
            label = new JLabel();
            label.setFont(label.getFont().deriveFont(Font.PLAIN, 16));
            add(label, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends TodoItem> list, TodoItem value, int index, boolean isSelected, boolean cellHasFocus) {
            item = value;
            label.setText(value.toString());
            if (isSelected || cellHasFocus) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Todo List");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(new TodoApp());
            frame.setSize(500, 400);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
