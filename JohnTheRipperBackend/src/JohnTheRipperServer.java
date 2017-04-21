import java.io.*;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Represent a server that initiates instances of the John The Ripper tool locally.
 */
public class JohnTheRipperServer extends ServerSocket {

    private static final int port = 5076;
    private static final int backlog = 5;

    private boolean running = false;

    @FunctionalInterface
    interface RequestHandler {
        void doAction(String[] args, Socket client);
    }

    private HashMap<String, RequestHandler> dispatchTable;

    public JohnTheRipperServer() throws IOException {
        super(port, backlog);

        dispatchTable = new HashMap<>();

        dispatchTable.put("runJohn", this::launchJohnTheRipper);
    }

    void run() {
        running = true;

        while (running) {
            Socket client;

            try {
                client = this.accept();
            } catch (IOException err) {
                continue;
            }

            try {
                handleRequest(client);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void launchJohnTheRipper(String[] args, Socket client) {
        String shadowFilepart1 = args[0];
        String shadowFilepart2 = args[1];
        String shadowFilepart3 = args[2];
        String shadowFilepart4 = args[3];

        String command1 = "john " + shadowFilepart1 + " --session=session1";
        String command2 = "john " + shadowFilepart2 + " --session=session2";
        String command3 = "john " + shadowFilepart3 + " --session=session3";
        String command4 = "john " + shadowFilepart4 + " --session=session4";

        Thread johnProcess1 = new Thread(() -> executeCommand(command1, "session1", client));
        Thread johnProcess2 = new Thread(() -> executeCommand(command2, "session2", client));
        Thread johnProcess3 = new Thread(() -> executeCommand(command3, "session3", client));
        Thread johnProcess4 = new Thread(() -> executeCommand(command4, "session4", client));

        johnProcess1.start();
        johnProcess2.start();
        johnProcess3.start();
        johnProcess4.start();
    }

    private void executeCommand(String command, String sessionName, Socket client) {
        Process p;

        try {
            p = Runtime.getRuntime().exec(command);

            Field f = p.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            long pid = f.getLong(p);
            f.setAccessible(false);

            BufferedReader reader;
            PrintWriter clientWriter = new PrintWriter(client.getOutputStream());
            String line;
            Process sighup_process;
            Process status_process;

            System.out.println(p.isAlive());

            while (p.isAlive()) {
                sighup_process = Runtime.getRuntime().exec("kill -1 " + pid);
                sighup_process.waitFor();

                status_process = Runtime.getRuntime().exec("john --status=" + sessionName);
                status_process.waitFor();

                reader = new BufferedReader(new InputStreamReader(status_process.getErrorStream()));

                if (reader.ready()) {
                    line = reader.readLine();
                } else {
                    continue;
                }

                String message = sessionName + ": " + line + "\n";
                System.out.println(message);

                clientWriter.write(message);
                clientWriter.flush();

                Thread.sleep(1000);
            }

            reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            while (reader.ready()) {
                clientWriter.write(reader.readLine() + "\n");
                clientWriter.flush();
            }

            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            while (reader.ready()) {
                clientWriter.write(reader.readLine() + "\n");
                clientWriter.flush();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Session " + sessionName + " finished");
    }

    private void handleRequest(Socket client) throws IOException {
        BufferedReader clientReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        String request = clientReader.readLine();
        String[] args = request.split(" ");
        String command = args[0];
        args = Arrays.copyOfRange(args, 1, args.length);

        dispatchTable.get(command).doAction(args, client);
    }
}

