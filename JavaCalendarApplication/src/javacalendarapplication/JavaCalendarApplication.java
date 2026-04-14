// JavaCalendarApplication.java is used for testing purposes and studying.
package javacalendarapplication;

// Import necessary libraries for GUI (Swing/AWT), File I/O, and Data Structures
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Main GUI class for the Calendar application.
 * Extends JFrame to act as the primary window.
 */
public class JavaCalendarApplication extends JFrame {
    // UI Components
    private JLabel monthLabel;                 // Displays the current month and year
    private DefaultTableModel calendarModel;   // Holds the data (days) for the calendar table
    private JTable calendarTable;              // The visual grid representing the calendar

    // Variables to track the currently displayed month and year
    private int currentMonth;
    private int currentYear;

    // Variables to track the actual, real-world current date
    private int realDay;
    private int realMonth;
    private int realYear;

    // Data structure to hold appointments. Key: Date String (mm-dd-yyyy), Value: List of Appointments
    private Map<String, List<Appointment>> appointments = new HashMap<>();
    
    // File where appointment data is saved persistently
    private final String FILE_NAME = "calendarsaveddata.txt";

    /**
     * Constructor: Sets up the main window and initializes components.
     */
    public JavaCalendarApplication() {
        // Set basic window properties
        setTitle("Calendar");
        setSize(670, 512);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Custom close logic handles saving
        setLayout(new BorderLayout()); // Use BorderLayout for standard Top/Center/Bottom layout

        // Initialize background data
        initializeRealDate();
        loadAppointments();

        // Build the user interface
        initializeTopNavigation();
        initializeCalendarTable();
        initializeBottomPanel();
        initializeWindowListener();

        // Populate the calendar grid for the current month
        updateCalendarDisplay();
    }

    /**
     * Captures today's exact date to highlight it on the calendar and set the initial view.
     */
    private void initializeRealDate() {
        GregorianCalendar calendar = new GregorianCalendar();
        realDay = calendar.get(Calendar.DAY_OF_MONTH);
        realMonth = calendar.get(Calendar.MONTH);
        realYear = calendar.get(Calendar.YEAR);
        
        // Default the viewing month to the real current month
        currentMonth = realMonth;
        currentYear = realYear;
    }

    /**
     * Creates the top panel containing the Previous/Next buttons and the Month/Year label.
     */
    private void initializeTopNavigation() {
        JPanel topPanel = new JPanel();
        JButton previousButton = new JButton("<");
        JButton nextButton = new JButton(">");
        
        // Setup the label that shows "Month Year"
        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        monthLabel.setPreferredSize(new Dimension(200, 30));

        // Add action listeners to change the month when buttons are clicked
        previousButton.addActionListener(event -> changeMonth(-1));
        nextButton.addActionListener(event -> changeMonth(1));

        // Add components to the panel, then add the panel to the top (NORTH) of the frame
        topPanel.add(previousButton);
        topPanel.add(monthLabel);
        topPanel.add(nextButton);
        add(topPanel, BorderLayout.NORTH);
    }

    /**
     * Sets up the JTable that acts as the visual calendar grid.
     */
    private void initializeCalendarTable() {
        String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        
        // Create a table model that prevents users from directly typing in the calendar cells
        calendarModel = new DefaultTableModel(null, daysOfWeek) {
            public boolean isCellEditable(int row, int column) { 
                return false; 
            }
        };
        
        // Initialize the table with the model
        calendarTable = new JTable(calendarModel);
        calendarTable.setRowHeight(60); // Make cells large enough to look like a calendar
        
        // Apply custom renderer to colorize cells (e.g., today's date, days with appointments)
        calendarTable.setDefaultRenderer(Object.class, new CalendarRenderer());
        
        // Listen for mouse clicks on the calendar cells to add appointments
        calendarTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                // Find out which cell was clicked
                int row = calendarTable.rowAtPoint(event.getPoint());
                int column = calendarTable.columnAtPoint(event.getPoint());
                Object cellValue = calendarModel.getValueAt(row, column);
                
                // If the cell contains a day number, trigger the appointment creation process
                if (cellValue != null) {
                    handleDateSelection((Integer) cellValue);
                }
            }
        });

        // Wrap the table in a ScrollPane and add it to the center
        add(new JScrollPane(calendarTable), BorderLayout.CENTER);
    }

    /**
     * Creates the bottom panel containing the "View Appointments" button.
     */
    private void initializeBottomPanel() {
        JPanel bottomPanel = new JPanel();
        JButton manageButton = new JButton("View Appointments");
        manageButton.setPreferredSize(new Dimension(250, 40));
        
        // Open the management window when clicked
        manageButton.addActionListener(event -> openManageWindow());
        
        bottomPanel.add(manageButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Intercepts the window closing event to save data before actually exiting.
     */
    private void initializeWindowListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                saveAppointments(); // Save to txt file
                dispose();          // Close window
                System.exit(0);     // Terminate program
            }
        });
    }

    /**
     * Opens a secondary window to view, edit, delete, or mark appointments as done.
     */
    private void openManageWindow() {
        JFrame manageFrame = new JFrame("View Appointments");
        manageFrame.setSize(750, 400);
        manageFrame.setLayout(new BorderLayout());

        // Define columns for the appointment list table
        String[] columns = {"Date", "Title", "Category", "Location", "Time", "Status"};
        DefaultTableModel listModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) { 
                return false; // Prevent direct editing in the table
            }
        };
        JTable listTable = new JTable(listModel);

        // Fill the table with data from our map
        populateManageTable(listModel);

        // Create action buttons for the management window
        JPanel buttonPanel = new JPanel();
        JButton editButton = new JButton("Edit");
        JButton markDoneButton = new JButton("Mark as Done");
        JButton deleteButton = new JButton("Delete");

        // Attach logic to the buttons
        editButton.addActionListener(event -> editAppointment(listTable, listModel));
        markDoneButton.addActionListener(event -> markAppointmentDone(listTable, listModel));
        deleteButton.addActionListener(event -> deleteAppointment(listTable, listModel));

        buttonPanel.add(editButton);
        buttonPanel.add(markDoneButton);
        buttonPanel.add(deleteButton);
        
        // Add components to the frame and display it
        manageFrame.add(new JScrollPane(listTable), BorderLayout.CENTER);
        manageFrame.add(buttonPanel, BorderLayout.SOUTH);
        manageFrame.setLocationRelativeTo(this); // Center relative to main window
        manageFrame.setVisible(true);
    }

    /**
     * Fills the management table with all saved appointments.
     */
    private void populateManageTable(DefaultTableModel listModel) {
        // Iterate through all dates in the map
        for (String dateKey : appointments.keySet()) {
            // Iterate through all appointments on that specific date
            for (Appointment appointment : appointments.get(dateKey)) {
                String timeRange = appointment.startTime + " - " + appointment.endTime;
                // Add the appointment data as a new row
                listModel.addRow(new Object[]{
                    dateKey, 
                    appointment.title, 
                    appointment.category,
                    appointment.location, 
                    timeRange, 
                    appointment.status
                });
            }
        }
    }

    /**
     * Marks a selected appointment in the management window as "Done".
     */
    private void markAppointmentDone(JTable listTable, DefaultTableModel listModel) {
        int selectedRow = listTable.getSelectedRow();
        if (selectedRow != -1) {
            // Extract identifying info from the selected row
            String dateKey = (String) listModel.getValueAt(selectedRow, 0);
            String title = (String) listModel.getValueAt(selectedRow, 1);
            
            // Find the exact appointment object and update its status
            for (Appointment appointment : appointments.get(dateKey)) {
                if (appointment.title.equals(title)) {
                    appointment.status = "Done";
                }
            }
            
            // Update the UI table to reflect the change
            listModel.setValueAt("Done", selectedRow, 5);
            calendarTable.repaint(); // Repaint main calendar in case cell colors need updating
        } else {
            JOptionPane.showMessageDialog(this, "Please select an appointment to mark as done.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Deletes a selected appointment from memory and the list UI.
     */
    private void deleteAppointment(JTable listTable, DefaultTableModel listModel) {
        int selectedRow = listTable.getSelectedRow();
        if (selectedRow != -1) {
            String dateKey = (String) listModel.getValueAt(selectedRow, 0);
            String title = (String) listModel.getValueAt(selectedRow, 1);
            
            // Remove the appointment from the list in the map based on title
            appointments.get(dateKey).removeIf(appointment -> appointment.title.equals(title));
            
            // If the date has no more appointments, remove the date key entirely
            if (appointments.get(dateKey).isEmpty()) {
                appointments.remove(dateKey);
            }
            
            // Remove from the UI list and update the main calendar display
            listModel.removeRow(selectedRow);
            updateCalendarDisplay();
        } else {
            JOptionPane.showMessageDialog(this, "Please select an appointment to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Opens the appointment dialog in "Edit" mode for the selected appointment.
     */
    private void editAppointment(JTable listTable, DefaultTableModel listModel) {
        int selectedRow = listTable.getSelectedRow();
        if (selectedRow != -1) {
            String dateKey = (String) listModel.getValueAt(selectedRow, 0);
            String title = (String) listModel.getValueAt(selectedRow, 1);
            
            // Search for the specific Appointment object to edit
            Appointment existingAppt = null;
            for (Appointment appt : appointments.get(dateKey)) {
                if (appt.title.equals(title)) {
                    existingAppt = appt;
                    break;
                }
            }
            
            // If found, open the dialog pre-filled with its data
            if (existingAppt != null) {
                boolean updated = showAppointmentDialog(dateKey, existingAppt);
                // If changes were successfully made, refresh the management table
                if (updated) {
                    listModel.setRowCount(0); // Clear current table
                    populateManageTable(listModel); // Refresh with updated data
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select an appointment to edit.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Triggered when a user clicks a day on the main calendar. Prompts to add a new appointment.
     */
    private void handleDateSelection(int day) {
        int monthIndex = currentMonth + 1; // Months are 0-indexed in Java Calendar
        
        // Format date string to mm-dd-yyyy (e.g., 04-15-2024)
        String dateKey = String.format("%02d-%02d-%04d", monthIndex, day, currentYear);
        
        // Ask for confirmation before opening the input form
        int userChoice = JOptionPane.showConfirmDialog(
            this, 
            "Do you want to create an appointment for " + dateKey + "?", 
            "New Appointment", 
            JOptionPane.YES_NO_OPTION, 
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (userChoice == JOptionPane.YES_OPTION) {
            showAppointmentDialog(dateKey, null); // Pass null because it's a NEW appointment
        }
    }

    /**
     * Opens a dialog for creating OR editing an appointment. 
     * @return true if an appointment was successfully saved/updated, false if cancelled/failed.
     */
    private boolean showAppointmentDialog(String dateKey, Appointment existingAppt) {
        // Initialize UI input fields. If editing (existingAppt != null), pre-fill with existing data
        JTextField titleField = new JTextField(existingAppt != null ? existingAppt.title : "");
        JTextField locationField = new JTextField(existingAppt != null ? existingAppt.location : "");
        JComboBox<String> categoryBox = new JComboBox<>(new String[]{"School", "Work", "Personal"});
        if (existingAppt != null) {
            categoryBox.setSelectedItem(existingAppt.category);
        }
        
        // Time selection dropdown arrays
        String[] hours = {"1","2","3","4","5","6","7","8","9","10","11","12"};
        String[] ampmOptions = {"AM", "PM"};
        
        // Dropdowns for Start Time
        JComboBox<String> startHourBox = new JComboBox<>(hours); 
        JComboBox<String> startAmPmBox = new JComboBox<>(ampmOptions);
        
        // Dropdowns for End Time
        JComboBox<String> endHourBox = new JComboBox<>(hours); 
        JComboBox<String> endAmPmBox = new JComboBox<>(ampmOptions);

        // Pre-fill time dropdowns if editing
        if (existingAppt != null) {
            String[] startParts = existingAppt.startTime.split(" ");
            startHourBox.setSelectedItem(startParts[0]);
            startAmPmBox.setSelectedItem(startParts[1]);
            
            String[] endParts = existingAppt.endTime.split(" ");
            endHourBox.setSelectedItem(endParts[0]);
            endAmPmBox.setSelectedItem(endParts[1]);
        }

        // Build the layout panel for the dialog
        JPanel inputPanel = buildAppointmentInputPanel(
            titleField, categoryBox, locationField, 
            startHourBox, startAmPmBox, endHourBox, endAmPmBox
        );

        // Show the dialog box
        String dialogTitle = existingAppt == null ? "New Appointment: " + dateKey : "Edit Appointment: " + dateKey;
        int result = JOptionPane.showConfirmDialog(this, inputPanel, dialogTitle, JOptionPane.OK_CANCEL_OPTION);
        
        // If user clicks "OK", process the data
        if (result == JOptionPane.OK_OPTION) {
            // Validation: Title cannot be blank
            if (titleField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "The event title cannot be blank.", "Warning", JOptionPane.WARNING_MESSAGE);
                return false;
            }

            // Construct full time strings
            String fullStartTime = startHourBox.getSelectedItem() + " " + startAmPmBox.getSelectedItem();
            String fullEndTime = endHourBox.getSelectedItem() + " " + endAmPmBox.getSelectedItem();
            
            // Convert times to total minutes from midnight for logical comparisons
            int startInMinutes = convertToMinutes((String) startHourBox.getSelectedItem(), (String) startAmPmBox.getSelectedItem());
            int endInMinutes = convertToMinutes((String) endHourBox.getSelectedItem(), (String) endAmPmBox.getSelectedItem());

            // Validation: End time must strictly be after start time
            if (endInMinutes <= startInMinutes) {
                JOptionPane.showMessageDialog(this, "End time must be after start time!", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            // Validation: Check for overlapping appointments
            if (hasTimeConflict(dateKey, startInMinutes, endInMinutes, existingAppt)) {
                return false; 
            }

            // If we are editing, remove the old version before saving the new one
            if (existingAppt != null) {
                appointments.get(dateKey).remove(existingAppt);
            }

            // Create the new Appointment object
            Appointment newAppointment = new Appointment(
                titleField.getText().trim(), fullStartTime, fullEndTime, 
                locationField.getText().trim(), (String) categoryBox.getSelectedItem()
            );
            
            // Preserve the original status if editing (so we don't accidentally reset "Done" to "Pending")
            if (existingAppt != null) {
                newAppointment.status = existingAppt.status;
            }
            
            // Add to the main data structure (create the list if it doesn't exist yet for this date)
            appointments.computeIfAbsent(dateKey, key -> new ArrayList<>()).add(newAppointment);
            
            // Refresh main calendar (in case this is the first appointment and the cell needs to turn blue)
            updateCalendarDisplay();
            return true;
        }
        return false;
    }

    /**
     * Helper method to layout the input fields for the dialog box.
     */
    private JPanel buildAppointmentInputPanel(
        JTextField titleField, JComboBox<String> categoryBox, JTextField locationField,
        JComboBox<String> startHourBox, JComboBox<String> startAmPmBox,
        JComboBox<String> endHourBox, JComboBox<String> endAmPmBox) {
        
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5)); // 2 columns, unlimited rows
        
        // Group start time dropdowns horizontally
        JPanel startPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); 
        startPanel.add(startHourBox); 
        startPanel.add(startAmPmBox);
        
        // Group end time dropdowns horizontally
        JPanel endPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); 
        endPanel.add(endHourBox); 
        endPanel.add(endAmPmBox);

        // Add labels and fields to the main panel
        panel.add(new JLabel("Title:")); 
        panel.add(titleField);
        panel.add(new JLabel("Category:")); 
        panel.add(categoryBox);
        panel.add(new JLabel("Location:")); 
        panel.add(locationField);
        panel.add(new JLabel("Start Time:")); 
        panel.add(startPanel);
        panel.add(new JLabel("End Time:")); 
        panel.add(endPanel);
        
        return panel;
    }

    /**
     * Checks if a new appointment overlaps with any existing appointments on the same day.
     */
    private boolean hasTimeConflict(String dateKey, int newStartInMinutes, int newEndInMinutes, Appointment ignoreAppt) {
        // If there are no appointments on this date, there can't be a conflict
        if (!appointments.containsKey(dateKey)) {
            return false;
        }

        // Loop through all existing appointments on this date
        for (Appointment existingAppt : appointments.get(dateKey)) {
            // If we are editing, don't check for conflicts against the appointment itself
            if (existingAppt == ignoreAppt) continue;

            String[] startParts = existingAppt.startTime.split(" ");
            String[] endParts = existingAppt.endTime.split(" ");
            
            int existingStart = convertToMinutes(startParts[0], startParts[1]);
            int existingEnd = convertToMinutes(endParts[0], endParts[1]);
            
            // Conflict logic: Overlaps occur if the new start is before the existing ends, 
            // AND the new end is after the existing starts.
            if (newStartInMinutes < existingEnd && newEndInMinutes > existingStart) {
                String conflictMessage = "Conflict with '" + existingAppt.title + 
                                         "' (" + existingAppt.startTime + " - " + existingAppt.endTime + ")";
                JOptionPane.showMessageDialog(this, conflictMessage, "Error", JOptionPane.ERROR_MESSAGE);
                return true;
            }
        }
        return false;
    }

    /**
     * Converts a 12-hour format time string into total minutes since midnight (0 - 1439).
     * Used for mathematical time comparisons.
     */
    private int convertToMinutes(String hourString, String amPmIndicator) {
        int hour = Integer.parseInt(hourString);
        
        // Adjust for PM (e.g., 1 PM becomes 13)
        if (amPmIndicator.equals("PM") && hour != 12) {
            hour += 12;
        }
        // Adjust for midnight (12 AM becomes 0)
        if (amPmIndicator.equals("AM") && hour == 12) {
            hour = 0;
        }
        
        return hour * 60; // We only support whole hours in the UI, so multiply by 60
    }

    /**
     * Saves the appointment dictionary to a text file.
     * Format per line: Date|Title|Start|End|Location|Category|Status
     */
    private void saveAppointments() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (String dateKey : appointments.keySet()) {
                for (Appointment appointment : appointments.get(dateKey)) {
                    writer.println(
                        dateKey + "|" + 
                        appointment.title + "|" + 
                        appointment.startTime + "|" + 
                        appointment.endTime + "|" + 
                        appointment.location + "|" + 
                        appointment.category + "|" + 
                        appointment.status
                    );
                }
            }
        } catch (IOException exception) { 
            exception.printStackTrace(); 
        }
    }

    /**
     * Loads appointment data from the text file into the memory Map on startup.
     */
    private void loadAppointments() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return; // If file doesn't exist, just start with an empty map
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                
                // Safety check: ensure line has enough parts before parsing
                if (parts.length >= 7) {
                    // Backwards compatibility check for older save versions
                    String category = parts.length >= 8 ? parts[6] : parts[5];
                    String status = parts.length >= 8 ? parts[7] : parts[6];
                    
                    // Create the object and add it to the map
                    Appointment appointment = new Appointment(parts[1], parts[2], parts[3], parts[4], category);
                    appointment.status = status;
                    appointments.computeIfAbsent(parts[0], key -> new ArrayList<>()).add(appointment);
                }
            }
        } catch (Exception exception) { 
            exception.printStackTrace(); 
        }
    }

    /**
     * Changes the current viewed month. offset is -1 (previous) or 1 (next).
     */
    private void changeMonth(int offset) {
        currentMonth += offset;
        
        // Handle year roll-over logic backwards
        if (currentMonth < 0) { 
            currentMonth = 11; // Wrap back to December
            currentYear--;     // Decrement year
        } 
        // Handle year roll-over logic forwards
        else if (currentMonth > 11) { 
            currentMonth = 0;  // Wrap to January
            currentYear++;     // Increment year
        }
        
        // Refresh the UI
        updateCalendarDisplay();
    }

    /**
     * Calculates the days of the selected month and populates the JTable grid.
     */
    private void updateCalendarDisplay() {
        String[] monthNames = {
            "January", "February", "March", "April", "May", "June", 
            "July", "August", "September", "October", "November", "December"
        };
        
        // Update top label
        monthLabel.setText(monthNames[currentMonth] + " " + currentYear);
        
        // Reset the table grid (clear old numbers)
        calendarModel.setRowCount(0);
        calendarModel.setRowCount(6); // Max 6 rows needed for a month grid
        
        // Use GregorianCalendar to find out what day of the week the 1st falls on
        GregorianCalendar calendar = new GregorianCalendar(currentYear, currentMonth, 1);
        int startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun, 1=Mon, etc.
        int totalDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        int row = 0;
        int column = startDayOfWeek;
        
        // Fill in the numbers sequentially
        for (int dayOfMonth = 1; dayOfMonth <= totalDaysInMonth; dayOfMonth++) {
            calendarModel.setValueAt(dayOfMonth, row, column);
            column++;
            
            // Move to the next row when we hit the end of the week (Saturday -> Sunday)
            if (column > 6) { 
                column = 0; 
                row++; 
            }
        }
    }

    /**
     * Custom TableCellRenderer. Controls the background and font color of calendar cells.
     */
    class CalendarRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // Get the default Swing rendering behavior first
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            // If the cell has a date number in it...
            if (value != null) {
                int day = (Integer) value;
                int monthIndex = currentMonth + 1;
                // Generate the key string for this specific cell's date
                String dateKey = String.format("%02d-%02d-%04d", monthIndex, day, currentYear);
                
                boolean isToday = (currentYear == realYear && currentMonth == realMonth && day == realDay);
                boolean hasAppointments = appointments.containsKey(dateKey);
                
                // Color formatting logic:
                if (isToday) {
                    setBackground(new Color(255, 255, 180)); // Light Yellow for today
                    component.setForeground(Color.RED);      // Red text
                } else if (hasAppointments) {
                    // Check if EVERY appointment on this date is marked as "Done"
                    boolean allTasksDone = appointments.get(dateKey).stream().allMatch(appointment -> appointment.status.equalsIgnoreCase("DONE"));
                    
                    // Light Green if all done, Light Blue if tasks are pending
                    setBackground(allTasksDone ? new Color(180, 255, 180) : new Color(180, 220, 255));
                    component.setForeground(Color.BLACK);
                } else {
                    setBackground(Color.WHITE); // Default empty day
                    component.setForeground(Color.BLACK);
                }
            } else { 
                // Gray out cells that do not belong to the current month
                setBackground(Color.LIGHT_GRAY); 
            }
            
            // Center the date number in the cell
            setHorizontalAlignment(CENTER);
            return component;
        }
    }

    /**
     * Inner class acting as a simple data model to represent an Appointment.
     */
    static class Appointment {
        String title;
        String startTime;
        String endTime;
        String location;
        String category;
        String status;

        public Appointment(String title, String startTime, String endTime, String location, String category) {
            this.title = title;
            this.startTime = startTime;
            this.endTime = endTime;
            this.location = location;
            this.category = category;
            this.status = "Pending"; // New appointments always default to Pending
        }
    }

    /**
     * Main method. Entry point of the program.
     */
    public static void main(String[] args) {
        // SwingUtilities ensures GUI code executes on the Event Dispatch Thread (Thread safety)
        SwingUtilities.invokeLater(() -> new JavaCalendarApplication().setVisible(true));
    }
}
