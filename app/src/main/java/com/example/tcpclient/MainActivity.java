package com.example.tcpclient;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    EditText ipAddressText;
    EditText portNumberText;
    EditText fileName;
    EditText messageText;

    Button submitIpAndPort;
    Button sendMessage;

    ClientThread clientThread;

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        clientThread = new ClientThread();
        new Thread(clientThread).start();

        ipAddressText = findViewById(R.id.ipAddress);
        ipAddressText.setText("35.203.25.23");
        portNumberText = findViewById(R.id.portNumber);
        portNumberText.setText("3389");
        fileName = findViewById(R.id.fileName);
        fileName.setText("29MB.txt");
        messageText = findViewById(R.id.message);

        submitIpAndPort = findViewById(R.id.submitIpAndPort);
        sendMessage = findViewById(R.id.sendMessage);

        submitIpAndPort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("TCPClient", "Setup ip and port");

                int port = Integer.parseInt(portNumberText.getText().toString());
                clientThread.setupIpAndPort(ipAddressText.getText().toString(), port);
            }
        });
        
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("TCPClient", "Send message to server");

                clientThread.sendMsg(messageText.getText().toString(), fileName.getText().toString());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] ==PackageManager.PERMISSION_GRANTED){
                    Log.e("value", "Permission Granted, Now you can use local drive .");
                } else{
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + requestCode);
        }
    }

    private class ClientThread implements Runnable {
        private volatile String msg = "Hello Server";
        private volatile String ipAddress = "127.0.0.1";
        private volatile int portNumber = 8080;
        private volatile String fileName = "";

        Socket socket = null;
        DataOutputStream dos = null;
        DataInputStream dis = null;
        FileOutputStream fos = null;
        FileInputStream fis = null;

        @SuppressLint("SuspiciousIndentation")
        @Override
        public void run() {
            TextFile textFile = new TextFile();
            File file = textFile.getFile(fileName);

            long fileLength = file.length();
            String separator = "<SEPARATOR>";
            String fileInfo = fileName+separator+fileLength;
            Log.d("TCPClientLog", "first message: " + fileInfo + " " +fileInfo.length());

            try {
                socket = new Socket(ipAddress, portNumber);
                dos = new DataOutputStream(socket.getOutputStream());
                fis = new FileInputStream(file);
                // send file information
                dos.write(fileInfo.getBytes(), 0, fileInfo.length());

                byte[] buffer = new byte[2*1024];
                int bytes = 0;

                while ((bytes = fis.read(buffer)) > 0) {
                    Log.d("TCPClientLog", "buffer");
//                    socket = new Socket(ipAddress, portNumber);
//                    dos = new DataOutputStream(socket.getOutputStream());
                    dos.write(buffer, 0, bytes);
//                    dos.flush();
                }

                Log.d("TCPClientLog", "outside while");

                dos.writeUTF("Exit");

                if (dos != null) {
                    dos.close();
                    dos.flush();
                }
                if (socket != null) {
                    socket.close();
                }
                if(fis != null) fis.close();
            } catch (IOException e) {
                Log.d("TCPClientLog", "Error for running socket " + e.getMessage());
            }
        }

        public void sendMsg(String msg, String fileName) {
            Log.d("TCPClientLog", "send msg");
            this.msg = msg;
            this.fileName = fileName;
            run();
        }

        public void setupIpAndPort(String ipAddress, int portNumber) {
            this.ipAddress = ipAddress;
            this.portNumber = portNumber;
        }
    }

    private class TextFile {
        static final int PERMISSION_REQUEST_CODE = 100;
        public File getFile(String fileName) {
            if (checkPermission()) {
                File sdcard = Environment.getExternalStorageDirectory();
                File dir = new File(sdcard.getAbsolutePath() + "/");
                if (dir.exists()) {
//                    Log.d("TCPClientLog", "directory" + Float.toString(dir));
                    File file = new File(dir, fileName);
//                    FileOutputStream os = null;
                    if (!file.exists()) {
                        Log.d("TCPClientLog", "File is not existed");
                        return null;
                    }
                    return file;
                } else {
                    Log.d("TCPClientLog", "Directory is not existed");
                }
            } else {
                Log.d("TCPClientLog", "Request for file permission");
                requestPermission(); // Code for permission
            }
            Log.d("TCPClientLog", "Permission related error");
            return null;
        }

        private boolean checkPermission() {
            int result = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
            if (result == PackageManager.PERMISSION_GRANTED){
                return true;
            } else{
                return false;
            }
        }

        private void requestPermission() {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(MainActivity.this, "Write External Storage permission allows us to read files. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        }
    }
}