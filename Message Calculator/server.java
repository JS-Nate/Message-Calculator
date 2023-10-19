import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.ServerNotActiveException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class server {
    private static List<Socket> clients = new ArrayList<>(); // List of Socket instances for each client connection
    private static int clientCount = 0; // Counts the number of connections
    private static Map<Integer, StringBuilder> clientHistories; // Stores the client histories
    private static int maxClients = 2; // Maximum number of concurrent clients



    public static void main(String[] args) {
        // makes sure the user enters a valid argument and quits if not
        if (args.length != 1) {
            System.out.println("Usage: java server <port_number>");
            System.exit(1);
        }
        
        // to fix an issue with comparing multiple clients
        maxClients ++;

        // makes sure the user enters a proper integer as port number anf quits if not
        String stringPort = args[0];
        try {
            int test = Integer.parseInt(stringPort);
            // System.out.println("Converted integer: " + test);
        } catch (NumberFormatException e) {
            System.out.println("Invalid Port or ID input. Not a valid integer.");
            System.exit(1);
        }

        // initializes the multithreaded server and socket connection
        int port = Integer.parseInt(stringPort);
        ExecutorService threadPool = Executors.newFixedThreadPool(maxClients);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Runnable clientHandler = new ClientHandler(clientSocket);
                threadPool.execute(clientHandler);
                clientHistories = new HashMap<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    static class ClientHandler implements Runnable {
        
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        
        @Override
        public void run() {

            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                // increments each time a client connects
                clientCount++;

                // prints the number of clients connected to the server
                System.out.println(clientCount + " client(s) connected");

                // ensures the current number of clients isnt exceeded
                if (clientCount < (maxClients)) {
                    // informs the client they are allowed to connect and their connection status
                    out.println("Connected to the server");
                    // gets the ID from the client
                    int clientID = Integer.parseInt(in.readLine());
                    System.out.println("Client ID " + clientID + " connected");
                    

                    String clientMessage;
                    while (true) {

                        // Asks the client what function they want to use
                        // And makes sure they enter a valid input
                        out.println("Please enter a function you want to use <A> or <B>:");
                        clientMessage = in.readLine();

                        // makes sure the client's always entering a valid input
                        while (!clientMessage.equalsIgnoreCase("A") && !clientMessage.equalsIgnoreCase("B") && !clientMessage.equalsIgnoreCase("exit")) {
                            out.println("Invalid input, which function would you like to use?");
                            clientMessage = in.readLine();
                        }

                        
                        String result="";
                        // First function: encrypting/decrypting the client's message
                        if("A".equalsIgnoreCase(clientMessage)){

                            // Asks the client what function they want to use
                            // And makes sure they enter a valid input
                            out.println("Enter <1> to encrypt or <2> to decrypt");
                            clientMessage = in.readLine();

                            // makes sure the client's always entering a valid input
                            while (!clientMessage.equalsIgnoreCase("1") && !clientMessage.equalsIgnoreCase("2") && !clientMessage.equalsIgnoreCase("exit")) {
                                out.println("Invalid input, try again");
                                clientMessage = in.readLine();
                            }

                            // Performs the message encryption function for the client
                            if("1".equalsIgnoreCase(clientMessage)){
                                out.println("Enter the name message you want to encrypt");
                                String Message = in.readLine();
                                out.println("Enter the number of digits to encrypt it by");
                                String Shift = in.readLine();
                                //if (!Message.equalsIgnoreCase("exit") && !Shift.equalsIgnoreCase("exit")) {
                                    // Begins encryption of the client's name message
                                    result = encryptMessage(Message, Shift);
                                    // prints ... to inform the client each time an extra message will be sent from the server
                                    out.println(result+"...");
                                //}
                            }

                            // Performs the message decryption function for the client
                            else if("2".equalsIgnoreCase(clientMessage)){
                                out.println("Enter the name message you want to decrypt");
                                String Message = in.readLine();
                                out.println("Enter the number of digits to decrypt it by");
                                String Shift = in.readLine();
                                    result = decryptMessage(Message, Shift);
                                    out.println(result+"...");                                
                            }                           
                        }


                        // performs the binary conversion for the user based on the binary number and their converison choice
                        else if("B".equalsIgnoreCase(clientMessage)){
                            out.println("Enter the binary message you want to convert");
                            String Message = in.readLine();
                            out.println("Would you like to convert it to octal, decimal or hex?");
                            String convertTo = in.readLine();
                            result = binaryConversion(Message, convertTo);
                            out.println(result+"...");
                        }

                        // exits if the user chooses to
                        else if (clientMessage.equalsIgnoreCase("exit")) {
                            break;
                        }
                        
                        // Adds the client's last operation to their history, invalid ones
                        if(!result.equalsIgnoreCase("")){
                            addHistory(clientID, result);
                        }
                        

                        // Asks the client if they want to view the history
                        // And makes sure they enter a valid input
                        out.println("Would you like to view your history?");
                        clientMessage = in.readLine();
                        while (!clientMessage.equalsIgnoreCase("yes") && !clientMessage.equalsIgnoreCase("no") && !clientMessage.equalsIgnoreCase("exit")) {
                            out.println("Invalid input, try again. yes or no");
                            clientMessage = in.readLine();
                        }


                        // asks the client if they want to view the history
                        if(clientMessage.contains("history") || clientMessage.contains("yes")){
                            String history = myHistory(clientID);
                            out.println("Client " + clientID + " performed the following: " + history+"...");
                        }

                        // asks the client if they want to continue or exit
                        out.println("Would to continue the app, or exit?");
                        clientMessage = in.readLine();
                        if (clientMessage.equalsIgnoreCase("exit")) {
                            break;
                        }
                    }
                }


                // if the max clients is reached, informs the last client
                else{
                    out.println("Error: Maximum number of clients reached");
                }
            } 
            
            // catches exceptions and closes the connection
            catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                    out.close();
                    clientSocket.close();
                    clients.remove(clientSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        
        
        // encryption function(novel feature 1)    
        public String encryptMessage(String m, String s){
            try {
                // ASCII library for encryption
                String l = "0123456789;:<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_'abcdefghijklmnopqrstuvwxyz";

                // converts each input into an int and char array for easy for loop navigation
                int y = Integer.parseInt(s);
                char[] letters = l.toCharArray();
                char[] message = m.toCharArray();
                String output="";


                for (int i = 0; i < m.length(); i++) {
                    // adds a space to the encrypted message if the old one had one
                    if (message[i] == ' ') {
                        output += ' ';
                    }
            
                    // on each character in the input, checks what letter it is in the ASCII alphabet
                    // and adds a new character to the encrypted message based on its ASCII position + the shifted position
                    for (int j = 0; j < 75; j++) {
                        if (message[i] == letters[j]) {
                            int shift = j + y;
                            if (shift >= 74) {
                                shift = shift % 75;
                            }
                            output += letters[shift];
                        }
                    }
                }


                // returns the encrypted message
                return "Encrypted " + m + " to " + output;

            // if the client entered an invalid shift amount
            } catch (NumberFormatException e) {
                return "No message. Invalid shift";
            }

            

        }

        // decryption function(novel feature (also) 1)
        public String decryptMessage(String m, String s){
            try {
                // ASCII library for decryption
                String l = "0123456789;:<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_'abcdefghijklmnopqrstuvwxyz";

                // converts each input into an int and char array for easy for loop navigation
                int y = Integer.parseInt(s);
                char[] letters = l.toCharArray();
                char[] message = m.toCharArray();
                String output="";


                for (int i = 0; i < m.length(); i++) {
                    // adds a space to the decrypted message if the old one had one
                    if (message[i] == ' ') {
                        output += ' ';
                    }
            
                    // on each character in the input, checks what letter it is in the ASCII alphabet
                    // and adds a new character to the decrypted message based on its ASCII position + the shifted position
                    for (int j = 0; j < 75; j++) {
                        if (message[i] == letters[j]) {
                            int shift = j - y;
                            while (shift < 0) {
                                shift += 75;
                            }
                            output += letters[shift];
                        }
                    }
                }

                // returns the decrypted message
                return "Decrypted " + m + " to " + output;
             // if the client entered an invalid shift amount
            } catch (NumberFormatException e) {
                return "No message, Invalid shift";
            }

        }

        // binary convertion function(novel feature 2)
        public String binaryConversion(String input, String convertTo){
            // converts the input into a char array for easy for loop navigation
            char[] binaryInput = input.toCharArray();
            String forOctal = input;
            // String forDecimal = input;
            for (int i = 0; i < input.length(); i++) {
                if (binaryInput[i] != '1' && binaryInput[i] != '0') {
                    return "No output. Invalid input";
                }
            }

            // Octal conversion
            if(convertTo.equalsIgnoreCase("octal")){
                String octal = "";
                // Ensures the binary input is a multiple of 3
                while (forOctal.length() % 3 != 0) {
                    forOctal = "0" + forOctal;
                }
                // counts every 3 digits
                for (int i = 0; i < forOctal.length(); i += 3) {
                    int total = 0;
                    String sub = forOctal.substring(i, i + 3);
                    // calculates the decimal value of this binary digit and add it to the total
                    // for each position (from right to left), calculate 2^(position) and add to total
                    for (int j = 0; j < sub.length(); j++) {
                        if (sub.charAt(j) == '1') {
                            total += Math.pow(2, 3 - j - 1);
                        }
                    }
                    octal += Integer.toString(total);
                }
                // returns the octal number as a full message
                return "Converted " + input + " binary to " + octal + " octal";

            }

            // decimal conversion
            else if (convertTo.equalsIgnoreCase("decimal")){
                int decimal = 0;
                // checks each 1's position and adds 2^(that 1's position) to the decimal sum output
                for (int i = 0; i < input.length(); i++) {
                    if (binaryInput[i] == '1') {
                        decimal = (int) (decimal + Math.pow(2, ((input.length() - 1) - i)));
                    }
                }
                // returns the decimal number as a full message               
                return "Converted " + input + " binary to " + Integer.toString(decimal)  + " decimal";

            }


            // hexadecimal conversion
            else if(convertTo.equalsIgnoreCase("hex") || convertTo.equalsIgnoreCase("hexadecimal")){
                String hexaDecimal="";
                // converts it to String builter to ease addition of digits
                StringBuilder b = new StringBuilder(input);
                // ensures the binary input is a multiple of 4
                while (b.length() % 4 != 0) {
                    b.insert(0, "0");
                }
                // converts the updated input back to string
                String forHexadecimal = b.toString();
                // Counts every 4 digits of the input and matches them to their respective hexadecimal value, which is added to the output
                char Hex[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
                String Bin[] = { "0000", "0001", "0010", "0011", "0100", "0101", "0110", "0111", "1000", "1001", "1010", "1011", "1100", "1101", "1110", "1111" };
                for (int i = 0; i < forHexadecimal.length(); i = i + 4) {
                    String sub = forHexadecimal.substring(i, i + 4);
                    for (int j = 0; j < 16; j++) {
                        if (sub.equals(Bin[j])) {
                            hexaDecimal += Hex[j];
                        }
                    }
                }
                // returns the hexadecimal number as a full message
                return "Converted " + input + " binary to " + hexaDecimal + " hexadecimal";
            }


            else{
                // returns this if the client entered an invalid choice for conversion
                return "No output. Invalid input";
            }

        }

        // to view the history of the client
        public String myHistory (int clientID){
            // gets the client's history if it exists
            StringBuilder history = clientHistories.get(clientID);
    
            // if history exists, return it as a string
            if (history != null) {
                return history.toString();
            } else {
                // if no history exists, return a message indicating that there is no history available
                return "No download history available for client with ID: " + clientID;
            }    
        }
       
        // adds operations to the client's history
        private void addHistory(int clientID, String result){
            // gets the client's history or create a new one if it doesn't exist
            StringBuilder history = clientHistories.get(clientID);
            if (history == null) {
                history = new StringBuilder();
                clientHistories.put(clientID, history);
            }
            // appends the result to the client's history
            history.append(result).append(", ");

            // adds the client's history to the txt file as well
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("History.txt", true))) {
                // appends the result along with client ID to a history file
                writer.write(result + ": Client ID " + clientID + "\n");
            } catch (IOException e) {
                // handles IO errors by printing the exception details
                e.printStackTrace();
            }
            
            

        }


    }       


}
