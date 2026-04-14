package lepackage;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Controller class to handle the logic and UI dialogs for adding or editing an appointment.
 * Centralizes data validation and conflict checking.
 */
public class AddAppointment {
    private CalendarGUI mainGUI;     // Needed to access the global map and trigger UI updates
    private String dateKey;          // The specific date being modified
    private CalendarGUI.Appointment existingAppt; // Holds data if we are editing, null if creating new

    /**
     * Constructor
     * @param mainGUI Reference to main program
     * @param dateKey The string ID (mm-dd-yyyy) of the date
     * @param existingAppt If null, acts as "Create". If provided, acts as "Edit".
     */
    public AddAppointment(CalendarGUI mainGUI, String dateKey, CalendarGUI.Appointment existingAppt) {
        this.mainGUI = mainGUI;
        this.dateKey = dateKey;
        this.existingAppt = existingAppt;
    }

    /**
     * Builds and shows the input dialog. Processes the user's input.
     * @return true if an appointment was successfully saved/updated, false if canceled or failed validation.
     */
    public boolean showDialog() {
        // Initialize UI input fields. Pre-fill with existingAppt data if in Edit mode.
        JTextField titleField = new JTextField(existingAppt != null ? existingAppt.title : "");
        JTextField locationField = new JTextField(existingAppt != null ? existingAppt.location : "");
        JComboBox<String> categoryBox = new JComboBox<>(new String[]{"School", "Work", "Personal"});
        if (existingAppt != null) {
            categoryBox.setSelectedItem(existingAppt.category);
        }
        
        // Time selection dropdown arrays
        String[] hours = {"1","2","3","4","5","6","7","8","9","10","11","12"};
        String[] ampmOptions = {"AM", "PM"};
        
        // Start Time components
        JComboBox<String> startHourBox = new JComboBox<>(hours); 
        JComboBox<String> startAmPmBox = new JComboBox<>(ampmOptions);
        
        // End Time components
        JComboBox<String> endHourBox = new JComboBox<>(hours); 
        JComboBox<String> endAmPmBox = new JComboBox<>(ampmOptions);

        // Pre-fill time dropdowns if in Edit mode
        if (existingAppt != null) {
            String[] startParts = existingAppt.startTime.split(" ");
            startHourBox.setSelectedItem(startParts[0]);
            startAmPmBox.setSelectedItem(startParts[1]);
            
            String[] endParts = existingAppt.endTime.split(" ");
            endHourBox.setSelectedItem(endParts[0]);
            endAmPmBox.setSelectedItem(endParts[1]);
        }

        // Layout the fields into a panel
        JPanel inputPanel = buildAppointmentInputPanel(
            titleField, categoryBox, locationField, 
            startHourBox, startAmPmBox, endHourBox, endAmPmBox
        );

        // Dynamically set title based on mode
        String dialogTitle = existingAppt == null ? "New Appointment: " + dateKey : "Edit Appointment: " + dateKey;
        
        // Show dialog and wait for user response
        int result = JOptionPane.showConfirmDialog(mainGUI, inputPanel, dialogTitle, JOptionPane.OK_CANCEL_OPTION);
        
        // Process data if user clicked OK
        if (result == JOptionPane.OK_OPTION) {
            
            // Validation 1: Title cannot be blank
            if (titleField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(mainGUI, "The event title cannot be blank.", "Warning", JOptionPane.WARNING_MESSAGE);
                return false;
            }

            // Construct full time strings for storage
            String fullStartTime = startHourBox.getSelectedItem() + " " + startAmPmBox.getSelectedItem();
            String fullEndTime = endHourBox.getSelectedItem() + " " + endAmPmBox.getSelectedItem();
            
            // Convert times to integers (minutes since midnight) for mathematical comparisons
            int startInMinutes = convertToMinutes((String) startHourBox.getSelectedItem(), (String) startAmPmBox.getSelectedItem());
            int endInMinutes = convertToMinutes((String) endHourBox.getSelectedItem(), (String) endAmPmBox.getSelectedItem());

            // Validation 2: Logic check (cannot end before starting)
            if (endInMinutes <= startInMinutes) {
                JOptionPane.showMessageDialog(mainGUI, "End time must be after start time!", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            // Validation 3: Prevent overlapping appointments
            if (hasTimeConflict(dateKey, startInMinutes, endInMinutes, existingAppt)) {
                return false; 
            }

            // If we are editing, remove the old version before creating the new one
            if (existingAppt != null) {
                mainGUI.getAppointments().get(dateKey).remove(existingAppt);
            }

            // Create the new data object
            CalendarGUI.Appointment newAppointment = new CalendarGUI.Appointment(
                titleField.getText().trim(), fullStartTime, fullEndTime, 
                locationField.getText().trim(), (String) categoryBox.getSelectedItem()
            );
            
            // Preserve the original status if editing (so we don't accidentally reset "Done" to "Pending")
            if (existingAppt != null) {
                newAppointment.status = existingAppt.status;
            }
            
            // Insert into the main data structure
            mainGUI.getAppointments().computeIfAbsent(dateKey, key -> new ArrayList<>()).add(newAppointment);
            
            // Tell the main GUI to refresh the calendar grid colors
            mainGUI.updateCalendarDisplay(); 
            return true;
        }
        return false;
    }

    /**
     * Helper method to organize the layout of the input fields.
     */
    private JPanel buildAppointmentInputPanel(
        JTextField titleField, JComboBox<String> categoryBox, JTextField locationField,
        JComboBox<String> startHourBox, JComboBox<String> startAmPmBox,
        JComboBox<String> endHourBox, JComboBox<String> endAmPmBox) {
        
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5)); // 2 columns
        
        // Group start time elements
        JPanel startPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); 
        startPanel.add(startHourBox); 
        startPanel.add(startAmPmBox);
        
        // Group end time elements
        JPanel endPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); 
        endPanel.add(endHourBox); 
        endPanel.add(endAmPmBox);

        // Add pairs of Label -> Field
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
     * Checks if the newly entered time range overlaps with any existing appointments on the same day.
     */
    private boolean hasTimeConflict(String dateKey, int newStartInMinutes, int newEndInMinutes, CalendarGUI.Appointment ignoreAppt) {
        // No conflict possible if there are no other appointments today
        if (!mainGUI.getAppointments().containsKey(dateKey)) {
            return false;
        }

        for (CalendarGUI.Appointment existingAppt : mainGUI.getAppointments().get(dateKey)) {
            // Skip checking the appointment against itself when editing
            if (existingAppt == ignoreAppt) continue;

            String[] startParts = existingAppt.startTime.split(" ");
            String[] endParts = existingAppt.endTime.split(" ");
            
            int existingStart = convertToMinutes(startParts[0], startParts[1]);
            int existingEnd = convertToMinutes(endParts[0], endParts[1]);
            
            // Overlap logic: New starts before old ends, AND new ends after old starts
            if (newStartInMinutes < existingEnd && newEndInMinutes > existingStart) {
                String conflictMessage = "Conflict with '" + existingAppt.title + 
                                         "' (" + existingAppt.startTime + " - " + existingAppt.endTime + ")";
                JOptionPane.showMessageDialog(mainGUI, conflictMessage, "Error", JOptionPane.ERROR_MESSAGE);
                return true;
            }
        }
        return false;
    }

    /**
     * Converts a 12-hour format time string into total minutes since midnight (0 - 1439).
     * Necessary to accurately perform mathematical comparisons on time ranges.
     */
    private int convertToMinutes(String hourString, String amPmIndicator) {
        int hour = Integer.parseInt(hourString);
        
        // 1 PM to 11 PM becomes 13 to 23
        if (amPmIndicator.equals("PM") && hour != 12) {
            hour += 12;
        }
        // 12 AM becomes 0
        if (amPmIndicator.equals("AM") && hour == 12) {
            hour = 0;
        }
        
        return hour * 60; // Multiply by 60 to get minutes
    }
}