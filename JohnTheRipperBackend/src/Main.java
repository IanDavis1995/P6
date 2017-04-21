import java.io.IOException;

/**
 * Entry-point into the application, start and run the john the ripper server.
 */
public class Main {
    public static void main(String[] args) {
        JohnTheRipperServer server;
        try {
            server = new JohnTheRipperServer();
        } catch (IOException err) {
            err.printStackTrace();
            System.exit(1);
            return;
        }

        server.run();

        System.exit(0);
    }
}
