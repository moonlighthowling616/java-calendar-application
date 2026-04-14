// JavaCalendarApplication.java is used for the defence.
package lepackage;

// Import necessary libraries for GUI, events, file I/O, and data structures
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
 * Acts as the primary window and holds the shared data structure.
 */
public class CalendarGUI extends JFrame {
    // UI Components for the main window
    private JLabel monthLabel;
    private DefaultTableModel calendarModel;
    private JTable calendarTable;

    // State variables to track the currently viewed month and year
    private int currentMonth;
    private int currentYear;
    
    // Variables to store the actual, real-world current date
    private int realDay;
    private int realMonth;
    private int realYear;

    // Shared data structure for appointments. Key: Date String, Value: List of Appointments.
    // Accessed by the other classes via getters.
    private Map<String, List<Appointment>> appointments = new HashMap<>();
    
    // File name for persistent data storage
    private final String FILE_NAME = "calendarsaveddata.txt";

    /**
     * Constructor: Initializes the main frame, loads data, and builds the UI.
     */
    public CalendarGUI() {
        // Set up the main application window
        setTitle("Calendar");
        setSize(670, 512);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Custom logic handles saving on exit
        setLayout(new BorderLayout()); // Top, Center, Bottom layout

        // Initialize background data and load saved appointments
        initializeRealDate();
        loadAppointments();

        // Build the three main sections of the UI
        initializeTopNavigation();
        initializeCalendarTable();
        initializeBottomPanel();
        
        // Add listener to save data when the window is closed
        initializeWindowListener();

        // Populate the initial calendar view
        updateCalendarDisplay();
    }

    /**
     * Captures today's exact date to highlight it and set the initial view.
     */
    private void initializeRealDate() {
        GregorianCalendar calendar = new GregorianCalendar();
        realDay = calendar.get(Calendar.DAY_OF_MONTH);
        realMonth = calendar.get(Calendar.MONTH);
        realYear = calendar.get(Calendar.YEAR);
        
        // Default the viewing calendar to the current real month
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
        
        // Setup the label that displays "Month Year"
        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        monthLabel.setPreferredSize(new Dimension(200, 30));

        // Attach actions to change the month
        previousButton.addActionListener(event -> changeMonth(-1));
        nextButton.addActionListener(event -> changeMonth(1));

        topPanel.add(previousButton);
        topPanel.add(monthLabel);
        topPanel.add(nextButton);
        
        // Add to the top of the main layout
        add(topPanel, BorderLayout.NORTH);
    }

    /**
     * Sets up the JTable that acts as the visual calendar grid.
     */
    private void initializeCalendarTable() {
        String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        
        // Create a table model that prevents direct typing in cells
        calendarModel = new DefaultTableModel(null, daysOfWeek) {
            public boolean isCellEditable(int row, int column) { 
                return false; 
            }
        };
        
        calendarTable = new JTable(calendarModel);
        calendarTable.setRowHeight(60); // Make cells large enough to look like calendar days
        
        // Apply custom renderer to colorize specific cells (e.g., today, appointments)
        calendarTable.setDefaultRenderer(Object.class, new CalendarRenderer());
        
        // Listen for mouse clicks to add appointments to specific days
        calendarTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                int row = calendarTable.rowAtPoint(event.getPoint());
                int column = calendarTable.columnAtPoint(event.getPoint());
                Object cellValue = calendarModel.getValueAt(row, column);
                
                // If the user clicked on a valid day number, prompt to add an appointment
                if (cellValue != null) {
                    handleDateSelection((Integer) cellValue);
                }
            }
        });

        add(new JScrollPane(calendarTable), BorderLayout.CENTER);
    }

    /**
     * Creates the bottom panel containing the "View Appointments" button.
     */
    private void initializeBottomPanel() {
        JPanel bottomPanel = new JPanel();
        JButton manageButton = new JButton("View Appointments");
        manageButton.setPreferredSize(new Dimension(250, 40));
        
        // Opens the secondary view via the separated ViewAppointment class
        manageButton.addActionListener(event -> new ViewAppointment(this).setVisible(true));
        
        bottomPanel.add(manageButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Intercepts the window closing event to save data before terminating.
     */
    private void initializeWindowListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                saveAppointments(); // Save memory to text file
                dispose();          // Close window resources
                System.exit(0);     // End process
            }
        });
    }

    /**
     * Triggered when a day is clicked. Initiates the AddAppointment logic.
     */
    private void handleDateSelection(int day) {
        int monthIndex = currentMonth + 1; // Calendar months are 0-indexed
        
        // Format date string to mm-dd-yyyy
        String dateKey = String.format("%02d-%02d-%04d", monthIndex, day, currentYear);
        
        int userChoice = JOptionPane.showConfirmDialog(
            this, 
            "Do you want to create an appointment for " + dateKey + "?", 
            "New Appointment", 
            JOptionPane.YES_NO_OPTION, 
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (userChoice == JOptionPane.YES_OPTION) {
            // Delegates the input form logic to the separated AddAppointment class
            AddAppointment addDialog = new AddAppointment(this, dateKey, null);
            addDialog.showDialog();
        }
    }

    /**
     * Saves the appointment map to a text file using a pipe-delimited format.
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
     * Loads appointment data from the text file into the map on startup.
     */
    private void loadAppointments() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return; // Skip if no previous saves exist

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                
                // Ensure the line has enough parts before parsing
                if (parts.length >= 7) {
                    // Backwards compatibility for older save versions
                    String category = parts.length >= 8 ? parts[6] : parts[5];
                    String status = parts.length >= 8 ? parts[7] : parts[6];
                    
                    Appointment appointment = new Appointment(parts[1], parts[2], parts[3], parts[4], category);
                    appointment.status = status;
                    
                    // Add to map, creating the list if it doesn't exist for this date
                    appointments.computeIfAbsent(parts[0], key -> new ArrayList<>()).add(appointment);
                }
            }
        } catch (Exception exception) { 
            exception.printStackTrace(); 
        }
    }

    /**
     * Adjusts the currently viewed month and handles year rollovers.
     */
    private void changeMonth(int offset) {
        currentMonth += offset;
        
        if (currentMonth < 0) { 
            currentMonth = 11; // Wrap back to December
            currentYear--;     // Decrement year
        } else if (currentMonth > 11) { 
            currentMonth = 0;  // Wrap forward to January
            currentYear++;     // Increment year
        }
        
        updateCalendarDisplay(); // Refresh grid
    }

    /**
     * Calculates the days of the selected month and populates the JTable grid.
     * Made public so external classes can trigger visual updates.
     */
    public void updateCalendarDisplay() {
        String[] monthNames = {
            "January", "February", "March", "April", "May", "June", 
            "July", "August", "September", "October", "November", "December"
        };
        
        // Update top label
        monthLabel.setText(monthNames[currentMonth] + " " + currentYear);
        
        // Reset table rows
        calendarModel.setRowCount(0);
        calendarModel.setRowCount(6); // Max 6 rows needed for a month
        
        // Determine the starting day of the week for the 1st of the month
        GregorianCalendar calendar = new GregorianCalendar(currentYear, currentMonth, 1);
        int startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; 
        int totalDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        int row = 0;
        int column = startDayOfWeek;
        
        // Fill the grid sequentially
        for (int dayOfMonth = 1; dayOfMonth <= totalDaysInMonth; dayOfMonth++) {
            calendarModel.setValueAt(dayOfMonth, row, column);
            column++;
            
            // Move to the next row when reaching Sunday
            if (column > 6) { 
                column = 0; 
                row++; 
            }
        }
    }

    /**
     * Public getter allowing other classes to read/modify the appointment map.
     */
    public Map<String, List<Appointment>> getAppointments() {
        return appointments;
    }

    /**
     * Custom TableCellRenderer to handle background colors of the calendar cells.
     */
    class CalendarRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (value != null) {
                int day = (Integer) value;
                int monthIndex = currentMonth + 1;
                String dateKey = String.format("%02d-%02d-%04d", monthIndex, day, currentYear);
                
                boolean isToday = (currentYear == realYear && currentMonth == realMonth && day == realDay);
                boolean hasAppointments = appointments.containsKey(dateKey);
                
                // Color Logic
                if (isToday) {
                    setBackground(new Color(255, 255, 180)); // Yellow for current real day
                    component.setForeground(Color.RED);      
                } else if (hasAppointments) {
                    // Check if all appointments on this day are done
                    boolean allTasksDone = appointments.get(dateKey).stream().allMatch(appointment -> appointment.status.equalsIgnoreCase("DONE"));
                    
                    // Green if all done, Blue if pending tasks exist
                    setBackground(allTasksDone ? new Color(180, 255, 180) : new Color(180, 220, 255));
                    component.setForeground(Color.BLACK);
                } else {
                    setBackground(Color.WHITE); // Default empty day
                    component.setForeground(Color.BLACK);
                }
            } else { 
                setBackground(Color.LIGHT_GRAY); // Blank cells outside the month
            }
            
            setHorizontalAlignment(CENTER);
            return component;
        }
    }

    /**
     * Data object representing a single appointment.
     * Public static so external classes can instantiate and utilize it.
     */
    public static class Appointment {
        public String title;
        public String startTime;
        public String endTime;
        public String location;
        public String category;
        public String status;

        public Appointment(String title, String startTime, String endTime, String location, String category) {
            this.title = title;
            this.startTime = startTime;
            this.endTime = endTime;
            this.location = location;
            this.category = category;
            this.status = "Pending"; // Always default to pending upon creation
        }
    }

    /**
     * Application entry point.
     */
    public static void main(String[] args) {
        // Ensure thread safety for Swing components
        SwingUtilities.invokeLater(() -> new CalendarGUI().setVisible(true));
    }
}