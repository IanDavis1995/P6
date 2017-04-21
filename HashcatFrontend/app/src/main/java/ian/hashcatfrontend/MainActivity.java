package ian.hashcatfrontend;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.jcraft.jsch.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private JSch jsch = new JSch();
    private Session mSession;

    private final String username = "ubuntu";
    private final String host = "ec2-35-166-139-99.us-west-2.compute.amazonaws.com";

    private Button runSSHButton;

    private EditText wordsFileEdit;
    private EditText hashesFileEdit;

    private TextView resultsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        runSSHButton = (Button) findViewById(R.id.run_ssh_button);
        runSSHButton.setOnClickListener(this);

        wordsFileEdit = (EditText) findViewById(R.id.upload_path_words);
        hashesFileEdit = (EditText) findViewById(R.id.upload_path_hashes);

        resultsView = (TextView) findViewById(R.id.results_view);

        Thread connectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    createSession();
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        });

        connectionThread.start();
    }

    @Override
    public void onClick(View v) {
        Button clickedButton = (Button) v;

        if (clickedButton == runSSHButton) {
            Thread sessionThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String wordsFile = wordsFileEdit.getText().toString();
                    String hashesFile = hashesFileEdit.getText().toString();

                    try {
                        final String command = "./hashcat-3.5.0/hashcat64.bin -m 0 -a 0 --show "
                                + hashesFile + " " + wordsFile;
                        final String results = executeSSHCommand(command);
                        System.out.println(results);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                resultsView.setText(results);
                            }
                        });
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                }
            });
            sessionThread.start();
        }
    }

    private String executeSSHCommand(String command) throws Exception {
        ChannelExec channelExec = (ChannelExec) mSession.openChannel("exec");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        channelExec.setOutputStream(output);
        channelExec.setErrStream(error);

        channelExec.setCommand(command);
        channelExec.connect();

        while (!channelExec.isClosed()) {
            Thread.sleep(100);
        }

        channelExec.disconnect();

        return output.toString();
    }

    private void createSession() throws JSchException, IOException {
        InputStream privateKeyByteStream = getResources().openRawResource(R.raw.icdavis_ceg3900);
        byte[] privateKeyBytes = new byte[privateKeyByteStream.available()];

        privateKeyByteStream.read(privateKeyBytes);

        jsch.addIdentity("ceg3900_icdavis", privateKeyBytes, new byte[0], new byte[0]);

        mSession = jsch.getSession(username, host, 22);

        UserInfo ui = new UserInfo(){
            public void showMessage(String message){
                System.out.println(message);
            }

            @Override
            public String getPassphrase() {
                return null;
            }

            @Override
            public String getPassword() {
                return null;
            }

            @Override
            public boolean promptPassword(String message) {
                return false;
            }

            @Override
            public boolean promptPassphrase(String message) {
                return false;
            }

            public boolean promptYesNo(String message){
                return true;
            }

        };

        mSession.setUserInfo(ui);

        mSession.connect(30000);
    }
}
