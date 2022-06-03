package edu.mnu.wearabledatalayer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivitySelf extends AppCompatActivity implements
        View.OnClickListener {

    Thread thread, beepThread;
    boolean isThread = true;
    boolean isbeepThread = true;
    boolean soundOn = false;
    ToneGenerator tone;
    //  private SlideHandler handler = new SlideHandler();
    private Beephandler beephandler = new Beephandler();
    ImageView imageView;
    Button btnLeft, btnRight;


    int[] interval = {1500,1500,1500,
            10000,10000,5000,90000,15000, 5000,20000,15000,15500,5000,
            5000,5000,5000,60000,10000};


    int indexofinterval=0;

    int [] ImageId = {R.drawable.s00,R.drawable.s01,R.drawable.s02, R.drawable.s03, R.drawable.s04, R.drawable.s05,R.drawable.s06,R.drawable.s07,R.drawable.s08,
            R.drawable.s09,R.drawable.s10,R.drawable.s11,R.drawable.s12,R.drawable.s13,R.drawable.s14,R.drawable.s15,R.drawable.s16,
            R.drawable.s17};



    String datapath = "/message_path";
    Button mybutton;
    TextView logger;
    protected Handler handler;
    String TAG = "Mobile MainActivity";
    int num = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mybutton = findViewById(R.id.sendbtn);
        mybutton.setOnClickListener(this);
        logger = findViewById(R.id.logger);

        //message handler for the send thread.
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Bundle stuff = msg.getData();
                logthis(stuff.getString("logthis"));
                return true;
            }
        });

        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        imageView = findViewById(R.id.imageView1);
        btnLeft = (Button) findViewById(R.id.buttonLeft);
        btnRight=(Button)findViewById(R.id.buttonRight);

        tone = new ToneGenerator(AudioManager.STREAM_MUSIC,ToneGenerator.MAX_VOLUME);

        btnLeft.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                indexofinterval-=1;
                Slihandler.sendEmptyMessage(indexofinterval);

                if(indexofinterval == ImageId.length) indexofinterval = 0;
            }
        });

        btnRight.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {

                indexofinterval+=1;
                Slihandler.sendEmptyMessage(indexofinterval);

                if(indexofinterval == ImageId.length) indexofinterval = 0;
            }
        });

        thread = new Thread(){
            @Override
            public void run() {
                while(isThread){
                    try {


                        Thread.sleep(interval[indexofinterval]);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.d("ylee","interrupt" );
                    }


                    //   handler.sendMessage(indexofinterval);
                    Log.d("ylee", indexofinterval+"+" +String.valueOf(interval[indexofinterval]));
                    indexofinterval++;
                    Slihandler.sendEmptyMessage(indexofinterval);


                }
            }
        };
        thread.start();

        beepThread = new Thread(){
            @Override
            public void run() {
                while(isbeepThread){
                    try {
                        sleep(545);


                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    beephandler.sendEmptyMessage(indexofinterval);

                }
            }
        };
        beepThread.start();
    }

    /*
     * simple method to add the log TextView.
     */
    public void logthis(String newinfo) {
        //   if (newinfo.compareTo("") != 0) {
        logger.append("\n" + newinfo);
        //    }
    }

    //setup a broadcast receiver to receive the messages from the wear device via the listenerService.
    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.v(TAG, "Main activity received message: " + message);
            // Display message in UI
            logthis(message);

        }
    }

    //button listener
    @Override
    public void onClick(View v) {
        String message = "CPR 훈련!" + num;
        //Requires a new thread to avoid blocking the UI
        new SendThread(datapath, message).start();
        num++;
    }

    //method to create up a bundle to send to a handler via the thread below.
    public void sendmessage(String logthis) {
        Bundle b = new Bundle();
        b.putString("logthis", logthis);
        Message msg = handler.obtainMessage();
        msg.setData(b);
        msg.arg1 = 1;
        msg.what = 1; //so the empty message is not used!
        handler.sendMessage(msg);

    }

    //This actually sends the message to the wearable device.
    class SendThread extends Thread {
        String path;
        String message;

        //constructor
        SendThread(String p, String msg) {
            path = p;
            message = msg;
        }

        //sends the message via the thread.  this will send to all wearables connected, but
        //since there is (should only?) be one, no problem.
        public void run() {

            //first get all the nodes, ie connected wearable devices.
            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                List<Node> nodes = Tasks.await(nodeListTask);

                //Now send the message to each device.
                for (Node node : nodes) {
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(MainActivitySelf.this).sendMessage(node.getId(), path, message.getBytes());

                    try {
                        // Block on a task and get the result synchronously (because this is on a background
                        // thread).
                        Integer result = Tasks.await(sendMessageTask);
                        sendmessage("SendThread: message send to " + node.getDisplayName());
                        Log.v(TAG, "SendThread: message send to " + node.getDisplayName());

                    } catch (ExecutionException exception) {
                        sendmessage("SendThread: message failed to" + node.getDisplayName());
                        Log.e(TAG, "Send Task failed: " + exception);

                    } catch (InterruptedException exception) {
                        Log.e(TAG, "Send Interrupt occurred: " + exception);
                    }

                }

            } catch (ExecutionException exception) {
                sendmessage("Node Task failed: " + exception);
                Log.e(TAG, "Node Task failed: " + exception);

            } catch (InterruptedException exception) {
                Log.e(TAG, "Node Interrupt occurred: " + exception);
            }

        }
    }

    private class Beephandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {

            if(soundOn) {
                tone.startTone(ToneGenerator.TONE_PROP_BEEP,300);

            }

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        isThread =  false;
        isbeepThread = false;
    }

    private void createNotification() {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");

        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("CPR Alarm");
        builder.setContentText("Emergency");

        builder.setColor(Color.RED);
        // 사용자가 탭을 클릭하면 자동 제거
        builder.setAutoCancel(true);


        // 알림 표시
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel("default", "기본 채널", NotificationManager.IMPORTANCE_HIGH));
        }

        // id값은
        // 정의해야하는 각 알림의 고유한 int값
        notificationManager.notify(1, builder.build());
    }

    private void removeNotification() {

        // Notification 제거
        NotificationManagerCompat.from(this).cancel(1);
    }
    private void vibration(){

        String message = "CPR 훈련!" + num;
        //Requires a new thread to avoid blocking the UI
        new SendThread(datapath, message).start();
        num++;

//        createNotification();
//        removeNotification();
//
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    private Handler Slihandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {

            Log.d("ylee", String.valueOf(msg.what));

            switch (msg.what){
                case 0: //3
                    imageView.setImageResource(R.drawable.s00);
                    soundOn = false;
                    break;
                case 1: //2
                    imageView.setImageResource(R.drawable.s01);
                    soundOn = false;
                    break;
                case 2: //1
                    imageView.setImageResource(R.drawable.s02);
                    soundOn = false;
                    break;
                case 3: //1번
                    vibration();
                    imageView.setImageResource(R.drawable.s03);
                    soundOn = false;
                    break;
                case 4: //2번
                    vibration();
                    imageView.setImageResource(R.drawable.s04);
                    soundOn = false;
                    break;
                case 5:
                    vibration();
                    imageView.setImageResource(R.drawable.s05);
                    soundOn = false;
                    break;
                case 6: //4번
                    vibration();
                    imageView.setImageResource(R.drawable.s06);
                    soundOn = true;
                    break;
                case 7: //5번
                    //createNotification();
                    //removeNotification();
                    vibration();
                    imageView.setImageResource(R.drawable.s07);
                    soundOn = false;
                    break;
                case 8: //6번
                    //createNotification();
                    //removeNotification();
                    vibration();
                    imageView.setImageResource(R.drawable.s08);
                    soundOn = false;
                    break;
                case 9: //7번
                    vibration();
                    imageView.setImageResource(R.drawable.s09);
                    soundOn = false;
                    break;
                case 10: //8번
                    vibration();
                    imageView.setImageResource(R.drawable.s10);
                    soundOn = false;
                    break;
                case 11: //9번
                    //createNotification();
                    //removeNotification();
                    vibration();
                    imageView.setImageResource(R.drawable.s11);
                    soundOn = false;
                    break;
                case 12: //10번
                    vibration();
                    imageView.setImageResource(R.drawable.s12);
                    soundOn = true;
                    break;
                case 13: //11
                    //createNotification();
                    //removeNotification();
                    vibration();
                    imageView.setImageResource(R.drawable.s13);
                    soundOn = false;
                    break;
                case 14: //12
                    vibration();
                    imageView.setImageResource(R.drawable.s14);
                    soundOn = false;
                    break;
                case 15: //13
                    //createNotification();
                    //removeNotification();
                    vibration();
                    imageView.setImageResource(R.drawable.s15);
                    soundOn = false;
                    break;
                case 16: //14
                    //createNotification();
                    //removeNotification();
                    vibration();
                    imageView.setImageResource(R.drawable.s16);
                    soundOn = false;
                    break;
                case 17:
                    //createNotification();
                    //removeNotification();
                    vibration();
                    imageView.setImageResource(R.drawable.s17);
                    soundOn = false;
                    break;

            }




        }
    };

}