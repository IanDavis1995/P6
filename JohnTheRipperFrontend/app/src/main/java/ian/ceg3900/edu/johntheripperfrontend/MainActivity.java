package ian.ceg3900.edu.johntheripperfrontend;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;

import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.InetSocketAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String hostname = "ec2-35-166-139-99.us-west-2.compute.amazonaws.com";
    private final int port = 5076;

    private final String networkCommand = "runJohn shadow-1 shadow-2 shadow-3 shadow-4\n";
    private final String TAG = "JohnTheRipper";

    private Button runJohnButton;

    private TextView sessionOneTextView;
    private TextView sessionTwoTextView;
    private TextView sessionThreeTextView;
    private TextView sessionFourTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        runJohnButton = (Button) findViewById(R.id.run_john_button);
        runJohnButton.setOnClickListener(this);

        sessionOneTextView = (TextView) findViewById(R.id.session_1_output);
        sessionTwoTextView = (TextView) findViewById(R.id.session_2_output);
        sessionThreeTextView = (TextView) findViewById(R.id.session_3_output);
        sessionFourTextView = (TextView) findViewById(R.id.session_4_output);

        sessionOneTextView.setMovementMethod(new ScrollingMovementMethod());
        sessionTwoTextView.setMovementMethod(new ScrollingMovementMethod());
        sessionThreeTextView.setMovementMethod(new ScrollingMovementMethod());
        sessionFourTextView.setMovementMethod(new ScrollingMovementMethod());
    }

    private Handler mSessionHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            String message = (String) inputMessage.obj;
            String[] args = message.split(": ");

            Log.d(TAG, "Received message from backend: " + message);

            if (args.length < 2) {
                Log.d(TAG, "Unknown message received from socket: " + message);
                return;
            } else if (message.toLowerCase().contains("no such file")) {
                Log.d(TAG, "Could not check status this clock tick, checking again later");
                return;
            }

            Log.d(TAG, message);

            String sessionName = args[0];
            String progress = args[1];

            String[] progressTokens = progress.split(" ");

            String successfulGuesses = progressTokens[0].substring(0, progressTokens[0].indexOf("g"));
            String sessionDuration = progressTokens[1];
            String passesCompleted = progressTokens[2];
            String passwordsPerSecond = progressTokens[4];

            String progressMessage =
                    "Successfully Guessed: " + successfulGuesses + "\n" +
                    "Time elapsed: " + sessionDuration + "\n" +
                    "Passes Completed: " + passesCompleted + "\n" +
                    "Password Per Second: " + passwordsPerSecond + "\n";

            // 0g 0:00:00:13 3/3 0g/s 153223p/s 1977Kc/s 1977KC/s

            switch (sessionName) {
                case "session1":
                    sessionOneTextView.setText(progressMessage);
                    Log.d(TAG, "Got progress update for session1");
                    break;
                case "session2":
                    sessionTwoTextView.setText(progressMessage);
                    Log.d(TAG, "Got progress update for session2");
                    break;
                case "session3":
                    sessionThreeTextView.setText(progressMessage);
                    Log.d(TAG, "Got progress update for session3");
                    break;
                case "session4":
                    sessionFourTextView.setText(progressMessage);
                    Log.d(TAG, "Got progress update for session4");
                    break;
                default:
                    break;
            }
        }
    };

    private void runJohn() throws Exception {
        Socket serverConnection = new Socket();
        serverConnection.connect(new InetSocketAddress(hostname, port));

        PrintWriter writer = new PrintWriter(serverConnection.getOutputStream());
        writer.write(networkCommand);
        writer.flush();

        BufferedReader reader = new BufferedReader(new InputStreamReader(serverConnection.getInputStream()));

        while (serverConnection.isConnected()) {
            if (reader.ready()) {
                Message newMessage = new Message();
                newMessage.obj = reader.readLine() + "\n";
                mSessionHandler.sendMessage(newMessage);
            } else {
                Thread.sleep(100);
            }
        }
    }

    @Override
    public void onClick(View v) {
        Button clickedButton = (Button) v;

        if (clickedButton == runJohnButton) {
            Thread socketThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        runJohn();
                    } catch (Exception e) {
                        Log.d(TAG, "Unknown exception occurred connecting to server! " + e.getMessage());
                    }
                }
            });

            socketThread.start();
        }
    }
}
