package lepackage;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Secondary GUI class that displays a list of all saved appointments.
 * Allows the user to edit, delete, or mark appointments as done.
 */
public class ViewAppointment extends JFrame {
    private CalendarGUI mainGUI; // Reference to main window to access data and refresh visuals
    private JTable listTable;
    private DefaultTableModel listModel;

    /**
     * Constructor: Initializes the management window.
     * @param mainGUI The parent CalendarGUI instance
     */
    public ViewAppointment(CalendarGUI mainGUI) {
        this.mainGUI = mainGUI;

        setTitle("View Appointments");
        setSize(750, 400);
        setLayout(new BorderLayout());
        setLocationRelativeTo(mainGUI); // Center over the main window

        initializeTable();
        initializeButtons();
    }

    /**
     * Sets up the list table showing all appointments.
     */
    private void initializeTable() {
        String[] columns = {"Date", "Title", "Category", "Location", "Time", "Status"};
        
        // Create uneditable model for the list
        listModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) { 
                return false; 
            }
        };
        
        listTable = new JTable(listModel);

        // Fetch data from CalendarGUI and populate rows
        populateManageTable();
        add(new JScrollPane(listTable), BorderLayout.CENTER);
    }

    /**
     * Sets up the bottom action buttons.
     */
    private void initializeButtons() {
        JPanel buttonPanel = new JPanel();
        JButton editButton = new JButton("Edit");
        JButton markDoneButton = new JButton("Mark as Done");
        JButton deleteButton = new JButton("Delete");

        // Attach logic to buttons
        editButton.addActionListener(event -> editAppointment());
        markDoneButton.addActionListener(event -> markAppointmentDone());
        deleteButton.addActionListener(event -> deleteAppointment());

        buttonPanel.add(editButton);
        buttonPanel.add(markDoneButton);
        buttonPanel.add(deleteButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Reads from the main GUI's appointment map and adds rows to the JTable.
     */
    private void populateManageTable() {
        listModel.setRowCount(0); // Clear current rows before populating
        
        // Loop through all dates
        for (String dateKey : mainGUI.getAppointments().keySet()) {
            // Loop through all appointments on each date
            for (CalendarGUI.Appointment appointment : mainGUI.getAppointments().get(dateKey)) {
                String timeRange = appointment.startTime + " - " + appointment.endTime;
                
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
     * Changes the status of the selected appointment to "Done".
     */
    private void markAppointmentDone() {
        int selectedRow = listTable.getSelectedRow();
        if (selectedRow != -1) {
            // Get identifiers from the selected row
            String dateKey = (String) listModel.getValueAt(selectedRow, 0);
            String title = (String) listModel.getValueAt(selectedRow, 1);
            
            // Find the object in the main map and update it
            for (CalendarGUI.Appointment appointment : mainGUI.getAppointments().get(dateKey)) {
                if (appointment.title.equals(title)) {
                    appointment.status = "Done";
                }
            }
            
            // Update the local list UI
            listModel.setValueAt("Done", selectedRow, 5);
            
            // Tell the main calendar to update (changes cell color from blue to green)
            mainGUI.updateCalendarDisplay(); 
        } else {
            JOptionPane.showMessageDialog(this, "Please select an appointment to mark as done.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Removes the selected appointment from memory and the UI.
     */
    private void deleteAppointment() {
        int selectedRow = listTable.getSelectedRow();
        if (selectedRow != -1) {
            String dateKey = (String) listModel.getValueAt(selectedRow, 0);
            String title = (String) listModel.getValueAt(selectedRow, 1);
            
            // Remove from main data structure based on title match
            mainGUI.getAppointments().get(dateKey).removeIf(appointment -> appointment.title.equals(title));
            
            // Clean up empty date keys to prevent memory leaks/bugs
            if (mainGUI.getAppointments().get(dateKey).isEmpty()) {
                mainGUI.getAppointments().remove(dateKey);
            }
            
            // Update UIs
            listModel.removeRow(selectedRow);
            mainGUI.updateCalendarDisplay();
        } else {
            JOptionPane.showMessageDialog(this, "Please select an appointment to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Locates the selected appointment and hands it off to AddAppointment for editing.
     */
    private void editAppointment() {
        int selectedRow = listTable.getSelectedRow();
        if (selectedRow != -1) {
            String dateKey = (String) listModel.getValueAt(selectedRow, 0);
            String title = (String) listModel.getValueAt(selectedRow, 1);
            
            // Find the exact object to edit
            CalendarGUI.Appointment existingAppt = null;
            for (CalendarGUI.Appointment appt : mainGUI.getAppointments().get(dateKey)) {
                if (appt.title.equals(title)) {
                    existingAppt = appt;
                    break;
                }
            }
            
            if (existingAppt != null) {
                // Open the separated AddAppointment controller in "Edit" mode
                AddAppointment editDialog = new AddAppointment(mainGUI, dateKey, existingAppt);
                boolean updated = editDialog.showDialog();
                
                // If the user saved changes, refresh the table
                if (updated) {
                    populateManageTable(); 
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select an appointment to edit.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }
}