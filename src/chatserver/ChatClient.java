package chatserver;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.awt.BorderLayout;

import javax.swing.*;



/**
 * A simple Swing-based client for the chat server.  Graphically
 * it is a frame with a text field for entering messages and a
 * textarea to see the whole dialog.
 *
 * The client follows the Chat Protocol which is as follows.
 * When the server sends "SUBMITNAME" the client replies with the
 * desired screen name.  The server will keep sending "SUBMITNAME"
 * requests as long as the client submits screen names that are
 * already in use.  When the server sends a line beginning
 * with "NAMEACCEPTED" the client is now allowed to start
 * sending the server arbitrary strings to be broadcast to all
 * chatters connected to the server.  When the server sends a
 * line beginning with "MESSAGE " then all characters following
 * this string should be displayed in its message area.
 */
public class ChatClient {

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(8, 40);
    // TODO: Add a list box
    DefaultListModel<String> active_users = new DefaultListModel<>();
    JList<String> activeUsersComponent = new JList<>(active_users);
    JCheckBox broadcastCheck = new JCheckBox("Broadcast");

    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Return in the
     * listener sends the textfield contents to the server.  Note
     * however that the textfield is initially NOT editable, and
     * only becomes editable AFTER the client receives the NAMEACCEPTED
     * message from the server.
     */
    public ChatClient() {

        // Layout GUI
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, BorderLayout.NORTH);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.getContentPane().add(activeUsersComponent, BorderLayout.EAST);
        frame.getContentPane().add(broadcastCheck, BorderLayout.AFTER_LAST_LINE);
        frame.pack();

        // TODO: You may have to edit this event handler to handle point to point messaging,
        // where one client can send a message to a specific client. You can add some header to 
        // the message to identify the recipient. You can get the receipient name from the listbox.
        textField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield by sending
             * the contents of the text field to the server.    Then clear
             * the text area in preparation for the next message.
             */
            public void actionPerformed(ActionEvent e) {
                List<String> receivers = activeUsersComponent.getSelectedValuesList();
                StringBuilder message = new StringBuilder();
                if (!receivers.isEmpty()) {
                    for (String receiver : receivers) {
                        message.append(receiver).append("|");
                    }
                    message.append(">>");
                    message.append(textField.getText());
                    out.println(message);
                }
                else if (receivers.isEmpty() || broadcastCheck.isSelected() ) {
                    message.append(textField.getText());
                    out.println(message);
                }
                
                textField.setText("");
            }
            
        });
        
        
    }

    /**
     * Prompt for and return the address of the server.
     */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
            frame,
            "Enter IP Address of the Server:",
            "Welcome to the Chatter",
            JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private String getName() {
        return JOptionPane.showInputDialog(
            frame,
            "Choose a screen name:",
            "Screen name selection",
            JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException {

        // Make connection and initialize streams
        String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, 9001);
        in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Process all messages from server, according to the protocol.
        
        // TODO: You may have to extend this protocol to achieve task 9 in the lab sheet
        while (true) {
            String line = in.readLine();
            if (line.startsWith("SUBMITNAME")) {
                out.println(getName());
            } else if (line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true);
            } else if (line.startsWith("MESSAGE")) {
                messageArea.append(line.substring(8) + "\n");
            } else if (line.startsWith("ADD_USER")) {
                // Catching the message to add a new user to the active users list
                active_users.addElement(line.substring(8));
            } else if (line.startsWith("USER_LEFT")) {
                // Catching the message to remove the said user from the activer users list
                String user_left = line.substring(10);
                for (int i = 0; i < active_users.size(); i++) {
                    String element = active_users.getElementAt(i);
                    if (element.equals(user_left)) {
                        active_users.removeElementAt(i);
                        break;
                    }

                }
            }
        }
    }

    /**
     * Runs the client as an application with a closeable frame.
     */
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setSize(450, 500);
        client.frame.setVisible(true);
        client.run();
    }
}