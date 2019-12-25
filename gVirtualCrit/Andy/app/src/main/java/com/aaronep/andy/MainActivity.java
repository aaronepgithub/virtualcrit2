package com.aaronep.andy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ValueEventListener;

import org.qap.ctimelineview.TimelineRow;
import org.qap.ctimelineview.TimelineViewAdapter;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

public class MainActivity extends AppCompatActivity  implements TextToSpeech.OnInitListener {

    private TextToSpeech engine;
    private TextView mTextMessage;
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final UUID CSC_SERVICE_UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
    private static final UUID CSC_CHARACTERISTIC_UUID = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb");
    private final static UUID HR_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    private static final UUID HR_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");
    private static final UUID BTLE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final int NOT_SET = Integer.MIN_VALUE;
    private int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final long SCAN_PERIOD = 5000;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;

    // setup UI handler
    private final static int UPDATE_VALUE = 0;
    private final static int UPDATE_HR = 3;
    private final static int UPDATE_SPEED = 4;
    private final static int UPDATE_CADENCE = 5;

    @SuppressLint("HandlerLeak")
    private final Handler uiHandler = new Handler() {
        public void handleMessage(Message msg) {
            final int what = msg.what;
            final String value = (String) msg.obj;
            switch(what) {
                case UPDATE_VALUE: updateValue(value); break;
                case UPDATE_HR:
                    updateValueHR(value);
                    break;
                case UPDATE_CADENCE:
                    updateValueCADENCE(value);
                    break;
                case UPDATE_SPEED:
                    updateValueSPEED(value);
                    break;
            }
        }
    };

    private void updateValue(final String value){

        final TextView tvAt = findViewById(R.id.rtText1);
        final TextView tvAT = findViewById(R.id.tvFooter1);
        final TextView tvATGeo = findViewById(R.id.tvFooter1Geo);
        tvAt.setText(value);
        tvAT.setText(value);
        tvATGeo.setText(value);

    }

    private void updateValueHR(final String value) {
        final TextView t2 = findViewById(R.id.tvTop);
        final TextView t2Geo = findViewById(R.id.tvTopGeo);
        TextView t1 = findViewById(R.id.textView1);
        t1.setText(value);
        t2.setText(value);
        t2Geo.setText(value);
    }

    private void updateValueCADENCE(final String value) {
        final TextView tBot = findViewById(R.id.tvBottom);
        final TextView t3 = findViewById(R.id.textView3);
        tBot.setText(value);
        t3.setText(value);
    }

    private void updateValueSPEED(final String value) {

        final TextView tMid = findViewById(R.id.tvMiddle);
        final TextView t2 = findViewById(R.id.textView2);
        tMid.setText(value);
        t2.setText(value);
    }

    private int dashboardON = 0;
    private int timelineON = 0;


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            ScrollView sv = findViewById(R.id.svSettings);
            LinearLayout ll = findViewById(R.id.llView);
//            LinearLayout llMile = findViewById(R.id.llViewGeo);
            LinearLayout llGeo = findViewById(R.id.llViewGeo);
            LinearLayout svtl = findViewById(R.id.svTimeline);
            LinearLayout svleader = findViewById(R.id.svLeaderboards);

            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    ll.setVisibility(View.GONE);
                    llGeo.setVisibility(View.GONE);
                    svtl.setVisibility(View.GONE);
                    svleader.setVisibility(View.GONE);
                    sv.setVisibility(View.VISIBLE);
                    return true;
                case R.id.navigation_dashboard:
                    int x = dashboardON;
                    mTextMessage.setText("");
//                    if (x == 0) {
                    if (activityValue != "RUN") {
                        ll.setVisibility(View.VISIBLE);
                        llGeo.setVisibility(View.GONE);
                        sv.setVisibility(View.GONE);
                        svleader.setVisibility(View.GONE);
                        svtl.setVisibility(View.GONE);
                        dashboardON = 1;
                    }
                    if (activityValue == "RUN") {
                        ll.setVisibility(View.GONE);
                        llGeo.setVisibility(View.VISIBLE);
                        sv.setVisibility(View.GONE);
                        svleader.setVisibility(View.GONE);
                        svtl.setVisibility(View.GONE);
                        dashboardON = 0;
                    }

                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText("");
                    if (timelineON == 0) {
                        ll.setVisibility(View.GONE);
                        llGeo.setVisibility(View.GONE);
                        sv.setVisibility(View.GONE);
                        svleader.setVisibility(View.GONE);
                        svtl.setVisibility(View.VISIBLE);
                        timelineON = 1;
                    } else {
                        ll.setVisibility(View.GONE);
                        llGeo.setVisibility(View.GONE);
                        sv.setVisibility(View.GONE);
                        svleader.setVisibility(View.VISIBLE);
                        svtl.setVisibility(View.GONE);
                        timelineON = 0;
                    }

                    return true;
            }
            return false;
        }
    };

    private Calendar startTime;
    private Tim tim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tim = new Tim("TIM");
        //Log.i(TAG, "onCreate: tim.name:  " + tim.name);


        engine = new TextToSpeech(this, this);


        mTextMessage = findViewById(R.id.message);
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        startTime = Calendar.getInstance(Locale.ENGLISH);
        tim.startTime = startTime;

        setRandomUsernameOnStart();

        mPrinter("Starttime: " + ""+startTime.get(Calendar.HOUR_OF_DAY)+":"+startTime.get(Calendar.MINUTE)+":"+startTime.get(Calendar.SECOND));

        String currTimeStmp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        createTimeline("LET'S GET STARTED", "" + currTimeStmp);

        onPowerOn();


        int yearInt = Calendar.getInstance(Locale.ENGLISH).get(Calendar.YEAR);
        int monthInt = Calendar.getInstance(Locale.ENGLISH).get(Calendar.MONTH);
        int dayInt = Calendar.getInstance(Locale.ENGLISH).get(Calendar.DAY_OF_MONTH);
        tim.currentDate = String.format("%02d%02d%02d", yearInt, monthInt + 1, dayInt);
        //Log.i(TAG, "onCreate: currentDate  " + tim.currentDate);


        //TODO:  TO LAUNCH WITH EMULATOR, DISABLE...

        //START BT SETUP
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<>();
            ScanFilter scanFilter = new ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid.fromString("0000180D-0000-1000-8000-00805f9b34fb"))
                    .build();
            filters.add(scanFilter);
            ScanFilter scanFilter2 = new ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid.fromString("00001816-0000-1000-8000-00805f9b34fb"))
                    .build();
            filters.add(scanFilter2);
        }
        //END BT SETUP
        //COMMENT TO HERE FOR EMULATOR

        //TODO:  ... END DISABLE FOR EMULATOR




        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

//        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle("This app needs location access");
//            builder.setMessage("Please grant location access so this app can detect peripherals.");
//            builder.setPositiveButton(android.R.string.ok, null);
//            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
//                @Override
//                public void onDismiss(DialogInterface dialog) {
//                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
//                }
//            });
//            builder.show();
//        }

        valuesRounds.add("My Rounds Completed (Speeds)");
        valuesRoundsHeartrates.add("My Rounds Completed (Heartrates)");
        valuesRoundsScores.add("My Rounds Completed (Scores)");
        valuesMiles.add("Miles Completed (Speeds)");  //BT
        valuesMilesGeo.add("Miles Completed (Speeds (G))");  //GEO
        valuesRoundLeaders.add("Round Leaders (Speeds)");
        valuesTotalsLeaders.add("Total Leaders (Speeds)");
        valuesRoundLeadersScores.add("Round Leaders (Scores)");
        valuesTotalsLeadersScores.add("Total Leaders (Scores)");

    }
    //END ON_CREATE

    private Timer timer = new Timer();
    private Integer timerSecondsCounter = 0;
    private Integer currentMileBT = 1;
    private Integer currentMileGEO = 1;
    private Integer currentMile = 1;
    private Integer secondsAtEndOfMileGeo = 0;
    private Integer secondsAtEndOfMileBT = 0;
    private double bestMileMPH = 0;
    private double lastMileMPH = 0;
    private double currentMileSpeedBT = 0;
    private double currentMileSpeedGEO = 0;
    private double endMileSpeedBT = 0;
    private double endMileSpeedGEO = 0;

    private Integer secondsPerRound = 1800;
    private Integer currentRound = 1;

    private double bestRoundMPH = 0;
    private double currentRoundSpeedBT = 0;
    private double currentRoundSpeedGEO = 0;

    private Boolean newRoundFlagGEO = false;
    private Boolean newRoundFlagBT = false;


    //STRINGS USED FOR LISTS
    private ArrayList<String> valuesRounds = new ArrayList<>(); //SPEEDS
    private ArrayList<String> valuesRoundsHeartrates = new ArrayList<>(); //HEARTRATES
    private ArrayList<String> valuesRoundsScores = new ArrayList<>(); //SCORES

    private ArrayList<String> valuesMiles = new ArrayList<>();  //BT
    private ArrayList<String> valuesMilesGeo = new ArrayList<>();  //GEO

    private ArrayList<String> valuesRoundLeaders = new ArrayList<>();
    private ArrayList<String> valuesTotalsLeaders = new ArrayList<>();

    private ArrayList<String> valuesRoundLeadersScores = new ArrayList<>();
    private ArrayList<String> valuesTotalsLeadersScores = new ArrayList<>();

    //ARRLISTS OF DOUBLES USED FOR INDEX VALUES
    private ArrayList<Double> doubleValuesRounds = new ArrayList<>(); //SPEEDS
    private ArrayList<Double> doubleValuesRoundsHeartrates = new ArrayList<>(); //HEARTRATES
    private ArrayList<Double> doubleValuesRoundsScores = new ArrayList<>(); //SCORES
    private ArrayList<Double> doubleValuesMiles = new ArrayList<>(); //MILES

    public void speakText(TimerTask v, String st) {

//        String textContents = "Hi Kazumi, Let's get Started";
        if (!audioValue) {return;}

        engine.speak(st, TextToSpeech.QUEUE_FLUSH, null, null);


    }

    @Override
    public void onInit(int i) {


        if (i == TextToSpeech.SUCCESS) {
            //Setting speech Language
            engine.setLanguage(Locale.ENGLISH);
            engine.setPitch(1);
        }
    }


    private int fetchRoundData = 0;
    private int fetchRoundDataScores = 0;
    private int fetchTotalsData = 0;
    private int fetchTotalsDataScores = 0;


    private Boolean usingBT = false;
    private String speakLeaderNames = "";


    private void onPowerOn() {
        Button b0 = findViewById(R.id.button0);
        String on1 = "ON";
        b0.setText(on1);



        timer.scheduleAtFixedRate(new TimerTask() {
              @Override
              public void run() {


                  timerSecondsCounter += 1;
                  tim.setTotalTimeInSeconds(timerSecondsCounter);

                  Message msg = Message.obtain();
                  msg.obj = tim.getTotalTimeString();
                    msg.what = 0;
                    msg.setTarget(uiHandler);
                    msg.sendToTarget();

                  if (timerSecondsCounter == reconnectFlag) {
                      //Log.i(TAG, "run: RECONNECT FLAG, TRY TO RECONNECT");
                      reconnectFlag = 0;
                      if (connectedGatt != null) {
                          //Log.i(TAG, "Clear connectedGatt, name:  " + connectedGatt.getDevice().getName());
                          connectedGatt.disconnect();
                          connectedGatt.close();
                      }

                      connectedGatt = tryToConnectDevice.connectGatt(getApplicationContext(), false, bluetoothGattCallback);
                  }

                  if (timerSecondsCounter > 31) {
                      if (timerSecondsCounter % 25 == 0) {veloTester1();}
                      if (timerSecondsCounter % 35 == 0) {veloTester2();}
                  }


                  //START END OF ROUND LOGIC


                  //FOR IN ROUND DISPLAY
                  double calcCurrentRoundSpd = currentRoundSpeedBT;
                  if (currentRoundSpeedGEO > calcCurrentRoundSpd) {
                      calcCurrentRoundSpd = currentRoundSpeedGEO;
                  }
                  final double currentRoundSpeed = calcCurrentRoundSpd;


                  //END OF ROUND
                  double bestRoundSpeed = 0;
                  if (timerSecondsCounter % secondsPerRound == 0 && timerSecondsCounter > 50) {

                      usingBT = false;  //ANY NEW READING WILL CHANGE IT TO TRUE
                      //Log.i(TAG, "NEW ROUND: " + timerSecondsCounter);

                      tim.setRoundSpeed(currentRoundSpeed);
                      valuesRounds.add(String.format("%d.  %s", currentRound, String.format(Locale.US, "%.2f MPH", currentRoundSpeed)));

                      valuesRoundsHeartrates.add(String.format("%d.  %s", currentRound, String.format(Locale.US, "%.1f BPM", tim.getRoundHR())));
                      valuesRoundsScores.add(String.format("%d.  %s", currentRound, String.format(Locale.US, "%.1f %% MAX", tim.getRoundScore())));

                      doubleValuesRounds.add(tim.getRoundSpeed());
                      doubleValuesRoundsHeartrates.add(tim.getRoundHR());
                      doubleValuesRoundsScores.add(tim.getRoundScore());

                      Collections.sort(doubleValuesRounds, Collections.reverseOrder());
                      Collections.sort(doubleValuesRoundsHeartrates, Collections.reverseOrder());
                      Collections.sort(doubleValuesRoundsScores, Collections.reverseOrder());

                      currentRound += 1;
                      newRoundFlagBT = true;
                      newRoundFlagGEO = true;


                      //DETERMINE BEST AND LAST
                      if (currentRoundSpeed > bestRoundMPH) {
                          bestRoundMPH = currentRoundSpeed;
                      }
                      bestRoundSpeed = bestRoundMPH;

                      String roundPaceString = calcPace(currentRoundSpeed);
                      String roundIndexString = "";
                      String toSpeak1 = "Round complete, your speed was " + String.format(Locale.US, "%.1f Miles Per Hour.", currentRoundSpeed);
                      String toSpeak1b = "A Pace of " + roundPaceString + ".  ";
                      String toSpeak2 = ".  Your best is " + String.format(Locale.US, "%.1f", bestRoundSpeed) + " .  ";
                      String toSpeak1c = "";
                      //GET INDEX VALS SPEED
                      int position = -1;
                      position = doubleValuesRounds.indexOf(currentRoundSpeed);
                      if (position == -1) {
                          //Log.i(TAG, "Object not found in List");
                      } else {
                          position += 1;
                          //Log.i(TAG, "SPEED RANK IS NUMBER " + position + " OUT OF " + doubleValuesRounds.size());
                          roundIndexString = "SPEED RANK IS NUMBER " + position + " OUT OF " + doubleValuesRounds.size() + ".";
                          toSpeak1c = ", Ranking " + position + " out of " + doubleValuesRounds.size();
                      }
                      String toSpeak3 = ",  " + roundIndexString;

                      String toSpeak1d = "";
                      //GET INDEX VALS SCORE
                      int positionS = -1;
                      positionS = doubleValuesRoundsHeartrates.indexOf(tim.getRoundHR());
                      if (positionS == -1) {
                          //Log.i(TAG, "HR Object not found in List");
                      } else {
                          positionS += 1;
                          //Log.i(TAG, "SCORE RANK IS NUMBER " + positionS + " OUT OF " + doubleValuesRoundsHeartrates.size());
                          toSpeak1d = ", Your score ranking was number  " + positionS + " . ";
                      }





                      //CREATE TIMELINE POST
                      createTimeline("ROUND COMPLETE\n" + String.format(Locale.US, "%.1f MPH", currentRoundSpeed)  + " \n" + String.format(Locale.US, "%.1f %% MAX", tim.getRoundScore())  + " \n" + "  YOUR BEST: " + String.format(Locale.US, "%.1f MPH", bestRoundSpeed) + "\n" + roundIndexString, "");

                      speakText(this, toSpeak1 + toSpeak1b + toSpeak1c + toSpeak1d);

                      final double finalBestRoundSpeed = bestRoundSpeed;
                      runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                          TextView tr = findViewById(R.id.rtStatus);
                          tr.setText(String.format(Locale.US,"ROUND COMPLETED: %d", currentRound - 1));

                          TextView t7b = findViewById(R.id.rtText7b);
                          t7b.setText(String.format(Locale.US,"%.1f MPH", currentRoundSpeed));

                          TextView t7c = findViewById(R.id.rtText7c);
                          t7c.setText(String.format(Locale.US,"%.1f MPH", finalBestRoundSpeed));
                          }
                      });

                      //WRITE TO FB
                      writeToFB();

                  }
                  //END END_OF_ROUND PROCESSING

                  if (timerSecondsCounter == fetchRoundData) {
                      //Log.i(TAG, "run: readFromFB - ROUNDS");
                      readFromFB();
                  }

                  if (timerSecondsCounter == fetchRoundDataScores) {
                      //Log.i(TAG, "run: readFromFB - ROUNDS/Scores");
//                      speakText(this, "The speed leader is " + stLeaderName);
                      readFromFB_RoundScores();
                  }

                  if (timerSecondsCounter == fetchTotalsData) {
                      //Log.i(TAG, "run: readFromFBII - TOTALS");
                      readFromFBII();
                  }


                  if (timerSecondsCounter == fetchTotalsDataScores) {
                      //Log.i(TAG, "run: readFromFBIII - TOTALS SCORES");
                      readFromFBIII();

                      String tempSpeak = "The speed leader is " + stLeaderName + ", The leading score is from " + stLeaderNameScore;
                      if (Objects.equals(tempSpeak, speakLeaderNames)) {return;}
                      speakText(this, "The speed leader is " + stLeaderName + ", The leading score is from " + stLeaderNameScore);
                      speakLeaderNames = tempSpeak;
                  }


//
                  //START MILE LOGIC
                  //CURRENT MILE
                  double currentMileSpeed = currentMileSpeedBT;
                  if (currentMileSpeedGEO > currentMileSpeedBT) {
                      currentMileSpeed = currentMileSpeedGEO;
                  }
//
//
//                 //LAST MILE
                  lastMileMPH = endMileSpeedBT;
                  if (endMileSpeedGEO > endMileSpeedBT) {
                      lastMileMPH = endMileSpeedGEO;
                  }
//
//                  //BEST MILE
                  if (endMileSpeedBT > bestMileMPH) {
                      bestMileMPH = endMileSpeedBT;
                  }
                  if (endMileSpeedGEO > bestMileMPH) {
                      bestMileMPH = endMileSpeedGEO;
                  }
                  //END...IN-MILE LOGIC


                  //END OF MILE CALC
                  final double finalLastMileSpeed = lastMileMPH;
                  final double finalCurrentMileSpeed = currentMileSpeed;
                  final double finalBestMileMPH = bestMileMPH;

                  //DETERMINE END OF MILE
//                  double comboMileSpeed = 0;
                  if (currentMileBT > currentMile || currentMileGEO > currentMile) {
                      currentMile += 1;
                      double comboMileSpeed = 0;
                      //NOW WE HAVE A SINGLE END OF MILE
                      if (currentMileBT == currentMile) {
                          //WE KNOW IT WAS THE BT MILE
                          comboMileSpeed = endMileSpeedBT;
                      } else {
                          //WE KNOW IT WAS THE GEO MILE
                          comboMileSpeed = endMileSpeedGEO;
                      }

                      //Log.i(TAG, "COMBO MILE MPH  " + comboMileSpeed);
                      doubleValuesMiles.add(comboMileSpeed);

                      String mileRankingString = "";
                      String toSpeakMile4 = "";
                      int positionM = -1;
                      Collections.sort(doubleValuesMiles, Collections.reverseOrder());
                      positionM = doubleValuesMiles.indexOf(finalLastMileSpeed);
                      if (positionM == -1) {
                          Log.e(TAG, "Object not found in List");
                      } else {
                          positionM += 1;
                          //Log.i(TAG, "LAST MILE RANKED " + positionM + " OUT OF " + doubleValuesMiles.size());
                          mileRankingString = "LAST MILE RANKED " + positionM + " OUT OF " + doubleValuesMiles.size();
                          toSpeakMile4 = ", Ranking " + positionM + " out of " + doubleValuesMiles.size();
                      }

                      String stBestMileMph = String.format(Locale.US, "%.1f Miles Per Hour.,", finalBestMileMPH);
                      String stLastMileMph = String.format(Locale.US, "%.1f Miles Per Hour.,", finalLastMileSpeed);
                      String toSpeakMile1 = "Speed for the last mile was " + stLastMileMph;
                      String toSpeakMile2 = "Your fastest is, " + stBestMileMph;
                      String toSpeakMile3 = ",  A Pace of " + calcPace(finalBestMileMPH);
                      //speakText(this, toSpeakMile1 + toSpeakMile2 + mileRankingString);

                      speakText(this, toSpeakMile1 + toSpeakMile3 + toSpeakMile4);


                      //readMileInfo = false;
                      createTimeline("MILE COMPLETE\n" + String.format(Locale.US, "%.1f MPH\n  ", finalLastMileSpeed) + "  BEST MILE: " + String.format(Locale.US, "%.1f MPH", finalBestMileMPH) + "\n" + mileRankingString, "");

                  }



//                  if (finalLastMileSpeed != lastMileTester) {
//                      //Log.i(TAG, "END OF MILE, REGARDLESS OF GEO OR BT");
//                      //Log.i(TAG, "Your last miles's speed was " + String.format(Locale.US, "%.1f Miles Per Hour.", finalLastMileSpeed));
//                      //Log.i(TAG, "Your best is " + String.format(Locale.US, "%.1f", finalBestMileMPH));

//                      doubleValuesMiles.add(finalLastMileSpeed);

//                      String mileRankingString = "";
//                      int positionM = -1;
//                      positionM = doubleValuesMiles.indexOf(finalLastMileSpeed);
//                      if (positionM == -1) {
//                          Log.e(TAG, "Object not found in List");
//                      } else {
//                          positionM += 1;
//                          //Log.i(TAG, "LAST MILE RANKED " + positionM + " OUT OF " + doubleValuesMiles.size());
//                          mileRankingString = "LAST MILE RANKED " + positionM + " OUT OF " + doubleValuesMiles.size();
//                      }



                      //lastMileTester = finalLastMileSpeed;
//                      String toSpeakMile1 = "Your last miles's speed was " + String.format(Locale.US, "%.1f Miles Per Hour.", finalLastMileSpeed);
//                      String toSpeakMile2 = ".  Your best is " + String.format(Locale.US, "%.1f", finalBestMileMPH);
//                      speakText(this, toSpeakMile1 + toSpeakMile2 + mileRankingString);
//                      readMileInfo = false;
//                      createTimeline("MILE COMPLETE\n" + String.format(Locale.US, "%.1f MPH\n  ", finalLastMileSpeed) + "  BEST MILE: " + String.format(Locale.US, "%.1f MPH", finalBestMileMPH) + "\n" + mileRankingString, "");

//                  }

//                  if (readMileInfo) {
//                      String toSpeakMile1 = "Your last miles's speed was " + String.format(Locale.US, "%.1f Miles Per Hour.", finalLastMileSpeed);
//                      String toSpeakMile2 = ".  Your best is " + String.format(Locale.US, "%.1f", finalBestMileMPH);
//                      speakText(this, toSpeakMile1 + toSpeakMile2);
//                      readMileInfo = false;
//                      //createTimeline("MILE COMPLETE\n" + String.format(Locale.US, "%.1f MPH\n  ", finalLastMileSpeed) + "  BEST MILE: " + String.format(Locale.US, "%.1f MPH", finalBestMileMPH), "");
//                  }



//                  runOnUiThread(new Runnable() {
//                      @Override
//                      public void run() {
//                      TextView t = findViewById(R.id.rtText6a);
//                          t.setText(String.format("%.1f MPH", finalCurrentMileSpeed));
//
//                          TextView t1 = findViewById(R.id.rtText6b);
//                          t1.setText(String.format("%.1f MPH", finalLastMileSpeed));
//
//                          TextView t2 = findViewById(R.id.rtText6c);
//                          t2.setText(String.format("%.1f MPH", bestMileMPH));
//
//                          //MILE PACE
//                          TextView tt = findViewById(R.id.rtText6aa);
//                          tt.setText(calcPace(finalCurrentMileSpeed));
//
//                          TextView tt1 = findViewById(R.id.rtText6bb);
//                          tt1.setText(calcPace(finalLastMileSpeed));
//
//                          TextView tt2 = findViewById(R.id.rtText6cc);
//                          tt2.setText(calcPace(bestMileMPH));
//
//
//                          TextView t37 = findViewById(R.id.rtText7a);
//                          t37.setText(String.format("%.1f MPH", currentRoundSpeed));
//
//
//                      }
//                      });



                  //final double finalBestRoundSpeed = bestRoundSpeed;
                  runOnUiThread(new Runnable() {
                      @Override
                      public void run() {


//                          TextView tr = findViewById(R.id.rtStatus);
//                          tr.setText(String.format("ROUND COMPLETED: %d", currentRound - 1));

//                          TextView t7b = findViewById(R.id.rtText7b);
//                          t7b.setText(String.format("%.1f MPH", currentRoundSpeed));
//
//                          TextView t7c = findViewById(R.id.rtText7c);
//                          t7c.setText(String.format("%.1f MPH", finalBestRoundSpeed));

                          TextView t = findViewById(R.id.rtText6a);
                          t.setText(String.format(Locale.US,"%.1f MPH", finalCurrentMileSpeed));
                          TextView t1 = findViewById(R.id.rtText6b);
                          t1.setText(String.format(Locale.US,"%.1f MPH", finalLastMileSpeed));

                          TextView tMile1 = findViewById(R.id.tvTopMile);
                          tMile1.setText(String.format(Locale.US,"%.1f NOW", finalCurrentMileSpeed));
                          TextView tMile2 = findViewById(R.id.tvMiddleMile);
                          tMile2.setText(String.format(Locale.US,"%.1f BEST", bestMileMPH));
                          TextView tMile3 = findViewById(R.id.tvBottomMile);
                          tMile3.setText(String.format(Locale.US,"%.1f LAST", finalLastMileSpeed));


                          TextView t2 = findViewById(R.id.rtText6c);
                          t2.setText(String.format(Locale.US,"%.1f MPH", bestMileMPH));

                          //MILE PACE
                          TextView tt = findViewById(R.id.rtText6aa);
                          tt.setText(calcPace(finalCurrentMileSpeed));

                          TextView tt1 = findViewById(R.id.rtText6bb);
                          tt1.setText(calcPace(finalLastMileSpeed));

                          TextView tt2 = findViewById(R.id.rtText6cc);
                          tt2.setText(calcPace(bestMileMPH));


                          TextView t37 = findViewById(R.id.rtText7a);
                          t37.setText(String.format(Locale.US,"%.1f MPH", currentRoundSpeed));
                      }
                  });

                  //UPDATE DATA
                  //updateView();




                                      }
                                  },
                1000, 1000);
        //END TIMER
    }


    private Boolean readMileInfo = false;

    private int currentlyWriting = 0;

    private void writeToFB() {

        if (currentlyWriting == 1) {
            return;
        }

        currentlyWriting = 1;

        Log.i(TAG, "WRITE TO FB");
        //        FirebaseDatabase database = FirebaseDatabase.getInstance();
        //        DatabaseReference myRef = database.getReference("message");
        //        myRef.setValue("Hello, World!");

        //WRITE END OF ROUND DATA
        String roundURL = "rounds/" + tim.currentDate;
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference(roundURL);
        // Creating new user node, which returns the unique key value
        // new user node would be /users/$userid/
        String userId = mDatabase.push().getKey();
        // creating user object
        Round round = new Round(tim.getName(), tim.getRoundSpeed(), tim.getRoundHR(), tim.getRoundScore());
        //Log.i(TAG, "writeToFB/ROUND SPEED:  " + tim.getRoundSpeed());
        //Log.i(TAG, "writeToFB/ROUND SCORE:  " + tim.getRoundScore());
        // pushing user to 'users' node using the userId
        mDatabase.child(userId).setValue(round);

        //RUNNER FB WRITE
        if (activityValue == "RUN") {
            String roundURLrun = "rounds/run/" + tim.currentDate;
            DatabaseReference mDatabaseRun = FirebaseDatabase.getInstance().getReference(roundURLrun);
            mDatabaseRun.child(userId).setValue(round);
        }
        //END RUNNER FB WRITE


        //WRITE UPDATE TOTAL DATA
        String totalsURL = "totals/"+ tim.currentDate +"/" + tim.getName();
        DatabaseReference mDatabaseTotals = FirebaseDatabase.getInstance().getReference(totalsURL);
        //Log.i(TAG, "writeToFB/TOTAL:  " + tim.getTotalAvgSpeed());
        DecimalFormat df = new DecimalFormat("#.###");
        //Double.valueOf(df.format(totalAverageScore));
//        Total total = new Total(tim.getName(), totalAverageScore, tim.getTotalAvgSpeed());
        Total total = new Total(tim.getName(), Double.valueOf(df.format(totalAverageScore)), tim.getTotalAvgSpeed());
        mDatabaseTotals.setValue(total);

        //RUNNER FB WRITE TOTALS
        if (activityValue == "RUN") {
            String totalsURLrun = "totals/run/"+ tim.currentDate +"/" + tim.getName();
            DatabaseReference mDatabaseTotalsRun = FirebaseDatabase.getInstance().getReference(totalsURLrun);
            mDatabaseTotalsRun.setValue(total);
        }
        //END RUNNER FB WRITE TOTALS


        //SCHEDULE READS
        fetchRoundData = timerSecondsCounter + 30;
        fetchTotalsData = timerSecondsCounter + 90;
        fetchTotalsDataScores = timerSecondsCounter + 75;
        fetchRoundDataScores = timerSecondsCounter + 60;

        //AT END OF ROUND, CLEAR HR
        arrHeartrates.clear();

    }



    //START READ FROM FB III
    //VALUES TOTALS LEADERS SCORES
    private String stTotalsLeadersScores = "";
    private void readFromFBIII() {
        //Log.i(TAG, "READ FROM FBIII - TOTALS, SCORES");

        //READ TOTALS SCORES
        String totalsURL = "totals/" + tim.currentDate;
        if (activityValue == "RUN") {
            totalsURL = "totals/run/" + tim.currentDate;
        }
        DatabaseReference mDatabaseTotals = FirebaseDatabase.getInstance().getReference(totalsURL);
        mDatabaseTotals.limitToLast(15).orderByChild("a_scoreHRTotal").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ////Log.i(TAG, "onDataChange - TOTALS/SCORES");
                ////Log.i(TAG, "onDataChange: " + dataSnapshot.toString());
                ArrayList<String> names= new ArrayList<>();
                valuesTotalsLeadersScores.clear();
                stTotalsLeadersScores = "";

                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    String name = ds.child("fb_timName").getValue(String.class);
                    if (name == tim.name) {
                        name = "ME";
                    }
                    Double score = ds.child("a_scoreHRTotal").getValue(Double.class);
                    //names.add(String.format("%s.  %s", name, String.format(Locale.US, "%.2f %% MAX", score)));
                    names.add(String.format("%s.  %s", String.format(Locale.US, "%.2f %% MAX", score) , name));


                    ////Log.i("FB", name);
                    ////Log.i("FB", String.valueOf(speed));
                    //valuesTotalsLeaders.add(String.format("%s.  %s", name, String.format(Locale.US, "%.2f MPH", speed)));
                    ////Log.i(TAG, String.format("%s.  %s", name, String.format(Locale.US, "%.2f PERCENT MAX", score)));
                }  //COMPLETED - READING EACH SNAP
//                valuesTotalsLeaders.add("Total Leaders (Speeds)");
                for(String name : names) {  //NOW READING EACH IN ARRAYLIST
                    ////Log.i(TAG, "onDataChange: (name) " + name);
                    valuesTotalsLeadersScores.add(name);
                    stTotalsLeadersScores = name + "\n" + stTotalsLeadersScores;
                }
                valuesTotalsLeadersScores.add("Total Leaders (Scores)");
                Collections.reverse(valuesTotalsLeadersScores);

                stTotalsLeadersScores = "Totals Leaders (Scores)" + "\n" + stTotalsLeadersScores;
                //createTimeline(stTotalsLeadersScores, "");
                //Log.i(TAG, "stTotalsLeadersScores: " + stTotalsLeadersScores);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Failed to read value
                //Log.i(TAG, "Failed to read value - totals, scores.", databaseError.toException());
            }
        });
        //END READ TOTALS SCORES


    }

    //END VALUES TOTALS LEADERS SCORES
    private String stTotalsLeaders = "";
    private void readFromFBII() {
        //Log.i(TAG, "READ FROM FBII - TOTALS");
        valuesTotalsLeaders.clear();
        //READ TOTALS
        String totalsURL = "totals/" + tim.currentDate;
        if (activityValue == "RUN") {
            totalsURL = "totals/run/" + tim.currentDate;
        }
        DatabaseReference mDatabaseTotals = FirebaseDatabase.getInstance().getReference(totalsURL);
        mDatabaseTotals.limitToLast(15).orderByChild("a_speedTotal").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Log.i(TAG, "onDataChange - TOTALS");
                ////Log.i(TAG, "onDataChange: " + dataSnapshot.toString());
                ArrayList<String> names= new ArrayList<>();
                valuesTotalsLeaders.clear();
                stTotalsLeaders = "";

                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    String name = ds.child("fb_timName").getValue(String.class);
                    if (name == tim.getName()) {
                        name = "-ME-";
                    }
                    Double speed = ds.child("a_speedTotal").getValue(Double.class);
//                    names.add(String.format("%s.  %s", name, String.format(Locale.US, "%.2f MPH", speed)));
                    names.add(String.format("%s.  %s", String.format(Locale.US, "%.2f MPH", speed), name));
                    ////Log.i("FB", name);
                    ////Log.i("FB", String.valueOf(speed));
                    //valuesTotalsLeaders.add(String.format("%s.  %s", name, String.format(Locale.US, "%.2f MPH", speed)));
                    ////Log.i(TAG, String.format("%s.  %s", name, String.format(Locale.US, "%.2f MPH", speed)));
                }  //COMPLETED - READING EACH SNAP
//                valuesTotalsLeaders.add("Total Leaders (Speeds)");
                for(String name : names) {  //NOW READING EACH IN ARRAYLIST
                    ////Log.i(TAG, "onDataChange: (name) " + name);
                    valuesTotalsLeaders.add(name);
                    stTotalsLeaders = name + "\n" + stTotalsLeaders;
                }
                valuesTotalsLeaders.add("Total Leaders (Speeds)");
                Collections.reverse(valuesTotalsLeaders);

                stTotalsLeaders = "Totals Leaders (Speeds)" + "\n" + stTotalsLeaders;
                //createTimeline(stTotalsLeaders, "");
                //Log.i(TAG, "stTotalsLeaders: " + stTotalsLeaders);

                //readFromFBIII();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Failed to read value
                //Log.i(TAG, "Failed to read value - totals.", databaseError.toException());
            }
        });
        //END READ TOTALS


    }

    String stLeaderName = "";
    private String stRoundLeaders = "";
    private void readFromFB() {

        currentlyWriting = 0;
        //Log.i(TAG, "READ FROM FB/ROUNDS/SPEED");
        String roundsURL = "rounds";
        if (activityValue == "RUN") {
            roundsURL = "rounds/run";
        }
        DatabaseReference mDatabaseRounds = FirebaseDatabase.getInstance().getReference(roundsURL);
        mDatabaseRounds.child(tim.currentDate).limitToLast(15).orderByChild("fb_SPD").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ////Log.i(TAG, "onDataChange - ROUNDS");
                ////Log.i(TAG, "onDataChange: " + dataSnapshot.toString());
                ArrayList<String> names= new ArrayList<>();
                valuesRoundLeaders.clear();
                stRoundLeaders = "";

                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    String name = ds.child("fb_timName").getValue(String.class);
                    Double speed = ds.child("fb_SPD").getValue(Double.class);
//                    names.add(String.format("%s.  %s", name, String.format(Locale.US, "%.2f MPH", speed)));
                    names.add(String.format("%s.  %s", String.format(Locale.US, "%.2f MPH", speed), name));
                    stLeaderName = name;
//                    //Log.i("FB", name);
//                    //Log.i("FB", String.valueOf(speed));
                    //valuesRoundLeaders.add(String.format("%s.  %s", name, String.format(Locale.US, "%.2f MPH", speed)));
                    ////Log.i(TAG, String.format("%s.  %s", name, String.format(Locale.US, "%.2f MPH", speed)));
                }  //COMPLETED - READING EACH SNAP
//                valuesRoundLeaders.add("Round Leaders (Speeds)");
                for(String name : names) {  //NOW READING EACH IN ARRAYLIST
                    ////Log.i(TAG, "onDataChange: (name) " + name);
                    valuesRoundLeaders.add(name);
                    stRoundLeaders = name + "\n" + stRoundLeaders;
                }
                valuesRoundLeaders.add("Round Leaders (Speeds)");
                Collections.reverse(valuesRoundLeaders);
                stRoundLeaders = "Round Leaders (Speeds)" + "\n" + stRoundLeaders;
                createTimeline(stRoundLeaders, "");
                //Log.i(TAG, "stRoundLeaders: " + stRoundLeaders);


            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Failed to read value
                //Log.i(TAG, "Failed to read value - rounds.", databaseError.toException());
            }
        });
        //END READ ROUNDS
    }


    //READ ROUND LEADERS - SCORES
    private String stRoundLeadersScores = "";
    String stLeaderNameScore = "";
    private void readFromFB_RoundScores() {
        //Log.i(TAG, "READ FROM FB/ROUNDS/SCORES");
        String roundsURL = "rounds";
        if (activityValue == "RUN") {
            roundsURL = "rounds/run";
        }
        DatabaseReference mDatabaseRounds = FirebaseDatabase.getInstance().getReference(roundsURL);
        mDatabaseRounds.child(tim.currentDate).limitToLast(15).orderByChild("fb_RND").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ////Log.i(TAG, "onDataChange - ROUND SCORES");
                ArrayList<String> names= new ArrayList<>();
                valuesRoundLeadersScores.clear();
                stRoundLeadersScores = "";
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    String name = ds.child("fb_timName").getValue(String.class);
                    Double score = ds.child("fb_RND").getValue(Double.class);
//                    names.add(String.format("%s.  %s", name, String.format(Locale.US, "%.1f %% MAX", score)));
                    names.add(String.format("%s.  %s", String.format(Locale.US, "%.1f %% MAX", score), name));
//                    //Log.i("FB", name);
//                    //Log.i("FB", String.valueOf(speed));
                    stLeaderNameScore = name;
                    //valuesRoundLeaders.add(String.format("%s.  %s", name, String.format(Locale.US, "%.2f MPH", speed)));
                    ////Log.i(TAG, String.format("%s.  %s", name, String.format(Locale.US, "%.1f PERCENT MAX", score)));
                }  //COMPLETED - READING EACH SNAP
//                valuesRoundLeaders.add("Round Leaders (Speeds)");
                for(String name : names) {  //NOW READING EACH IN ARRAYLIST
                    ////Log.i(TAG, "onDataChange: (name) " + name);
                    valuesRoundLeadersScores.add(name);
                    stRoundLeadersScores = name + "\n" + stRoundLeadersScores;
                }
                valuesRoundLeadersScores.add("Round Leaders (Scores)");
                Collections.reverse(valuesRoundLeadersScores);
                stRoundLeadersScores = "Round Leaders (Scores)" + "\n" + stRoundLeadersScores;
                createTimeline(stRoundLeadersScores, "");
                //Log.i(TAG, "stRoundLeaderScores:  " + stRoundLeadersScores);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Failed to read value
                //Log.i(TAG, "Failed to read value - rounds/scores.", databaseError.toException());
            }
        });
        //END READ ROUNDS
    }


    //END READ ROUND LEADERS - SCORES



    //private Boolean toggleClick0 = true;
    public void onClick_0(View view) {

        Button b0 = findViewById(R.id.button0);
//        String on1 = "OFF";
//        b0.setText(on1);

//        readFromFB();

//        if (toggleClick0) {
//            b0.setText("READ");
//            writeToFB();
//            toggleClick0 = false;
//        } else {
//            b0.setText("WRITE");
//            readFromFB();
//            toggleClick0 = true;
//        }




        //READ FROM FB
//        Handler mHandler = new Handler();
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                readFromFB();
//            }
//        }, 10000);

    }


//    private void updateView() {
//
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                //Log.i(TAG, "run: UPDATE DASHBOARD VIEW");
//                TextView fHeader1 = findViewById(R.id.rtText1a);
//                TextView fHeader2 = findViewById(R.id.rtText8a);
//                TextView tHeader1 = findViewById(R.id.tvHeader1);
//                TextView tHeader2 = findViewById(R.id.tvHeader2);
//
//                //tHeader.setText(String.format("%s    %s", fHeader1.getText().toString(), fHeader2.getText().toString()));
//
//                TextView fTop1 = findViewById(R.id.textView1);
//                TextView tTop1 = findViewById(R.id.tvTop);
//                tTop1.setText(String.format("%s", fTop1.getText().toString()));
//
//                TextView fMid = findViewById(R.id.textView2);
//                TextView tMid = findViewById(R.id.tvMiddle);
//                tMid.setText(String.format("%s", fMid.getText().toString().substring(0, fMid.getText().toString().length() - 4)));
//
//                TextView fBot = findViewById(R.id.textView3);
//                TextView tBot = findViewById(R.id.tvBottom);
//                tBot.setText(String.format("%s", fBot.getText().toString()));
//
//                TextView fFooter1 = findViewById(R.id.rtText1);
//                TextView fFooter2 = findViewById(R.id.rtText4a);
//                TextView tFooter = findViewById(R.id.tvFooter);
//                tFooter.setText(String.format("%s    %s", fFooter1.getText().toString(), fFooter2.getText().toString()));
//            }
//        });
//    }

    private String oldHR = "START", oldSPD = "START", oldCAD = "START";
    private void veloTester1() {
        //TEST FOR 0, SPD/CAD
        //SET TEXTVIEW TO "0", VELO
        ////Log.i("TIMER", "TEST FOR 0 VAL SPD/CAD");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView t2 = findViewById(R.id.textView2);
                String s2 = t2.getText().toString();
                if (Objects.equals(s2, oldSPD)) {
                    resetSPD0();
                }
                oldSPD = s2;

                TextView t3 = findViewById(R.id.textView3);
                String s3 = t3.getText().toString();
                if (Objects.equals(s3, oldCAD)) {
                    resetCAD0();
                }
                oldCAD = s3;
            }
        });



    }
    private void veloTester2() {
        //TEST FOR 0, HR
        //SET TEXTVIEW TO "0", VELO
        ////Log.i("TIMER", "TEST FOR 0 VAL HR");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView t1 = findViewById(R.id.textView1);
                String s1 = t1.getText().toString();
                if (Objects.equals(s1, oldHR)) {
                    resetHR0();
                }
                oldHR = s1;
            }
        });

    }

    private void resetSPD0() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ////Log.i("SPD", "RESET SPD");
                TextView t1 = findViewById(R.id.textView2);
                String s1x = "0.0 MPH";
                t1.setText(s1x);
            }
        });
    }
    private void resetHR0() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ////Log.i("HR", "RESET HR");
                TextView t1 = findViewById(R.id.textView1);
                String s1x = "0 HR";
                t1.setText(s1x);
            }
        });
    }
    private void resetCAD0() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ////Log.i("CAD", "RESET CAD");
                TextView t1 = findViewById(R.id.textView3);
                String s1x = "0 RPM";
                t1.setText(s1x);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

//    private void tryToConnectAgain() {
//        connectedGatt.disconnect();
//        connectedGatt.close();
//        for (String deviceAddress : devicesConnectedAddresses) {
//            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
//            if (device == null) {
//                //Log.i(TAG, "Device not found.  Unable to connect.");
//                return;
//            }
//            connectToDevice(device);
//            //Log.i(TAG, "Trying to create a new connection.");
//        }
//    }

    public int reconnectFlag;
    public BluetoothDevice tryToConnectDevice;



//2ND CB START


    //private BluetoothManager bluetooth;
//    private BluetoothGattCallback bluetoothGattCallback2 = new BluetoothGattCallback() {
//        @Override
//        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int state) {
//
//
//            super.onConnectionStateChange(gatt, status, state);
//
//            switch (status) {
//                case BluetoothGatt.GATT_SUCCESS: {
//                    //Log.i(TAG, "onConnectionStateChange: GATT_SUCCESS");
//                    break;
//                }
//                case BluetoothGatt.GATT_FAILURE: {
//                    //Log.i(TAG, "onConnectionStateChange: GATT_FAILURE");
//                    break;
//                }
//                default:
//                    //Log.i(TAG, "onConnectionStateChange: NOT SUCCESS OR FAILURE");
//
//            }
//
//            switch (state) {
//                case BluetoothProfile.STATE_CONNECTED: {
//                    //Log.i(TAG, "onConnectionStateChange: STATE_CONNECTED");
//                    //setConnectedGatt(gatt);
//                    gatt.discoverServices();
//                    break;
//                }
//                case BluetoothProfile.STATE_DISCONNECTED: {
//                    //Log.i(TAG, "onConnectionStateChange: STATE_DISCONNECTED");
//                    //setConnectedGatt(null);
//
////                    runOnUiThread(new Runnable() {
////                        @Override
////                        public void run() {
////                            TextView tr = findViewById(R.id.rtStatus);
////                            tr.setText(String.format("BT DISCONNECT:  %s", gatt.getDevice().getName()));
////                            //Log.i(TAG, "BT DISCONNECT:  " + gatt.getDevice().getName());
////                        }
////                    });
//
//
//                    tryToConnectDevice = gatt.getDevice();
//                    reconnectFlag = timerSecondsCounter + 20;
//                    break;
//                }
//                case BluetoothProfile.STATE_CONNECTING: {
//                    //Log.i(TAG, "onConnectionStateChange: STATE_CONNECTING");
//                    break;
//                }
//                case BluetoothProfile.STATE_DISCONNECTING: {
//                    //Log.i(TAG, "onConnectionStateChange: STATE_DISCONNECTING");
//                    break;
//                }
//                default:
//                    //Log.i("gattCallback", "STATE_OTHER");
//
//            }
//        }  //END CONNECTION STATE CHANGE
//
////        @Override
////        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
////            super.onReadRemoteRssi(gatt, rssi, status);
////            //Log.i(TAG, "onReadRemoteRssi: " + rssi);
////        }
//
//        private Boolean tryVelo = false;
//
//        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            super.onServicesDiscovered(gatt, status);
//            Log.d(TAG, "onServicesDiscovered status:" + BluetoothUtil.statusToString(status));
//
//            Boolean hasHR = false;
//            Boolean hasCSC = false;
//
//            List<BluetoothGattService> services = gatt.getServices();
//            //Log.i("TEST", "DETERMINE IF BOTH SERVICES EXIST");
//            for (BluetoothGattService service : services) {
//
//
//                if (service.getUuid().equals(HR_SERVICE_UUID)) {
//                    hasHR = true;
//                    BluetoothGattCharacteristic valueCharacteristic = gatt.getService(HR_SERVICE_UUID).getCharacteristic(HR_CHARACTERISTIC_UUID);
//                    boolean notificationSet = gatt.setCharacteristicNotification(valueCharacteristic, true);
//                    //Log.i(TAG, "registered for HR updates " + (notificationSet ? "successfully" : "unsuccessfully"));
//                    BluetoothGattDescriptor descriptor = valueCharacteristic.getDescriptor(BTLE_NOTIFICATION_DESCRIPTOR_UUID);
//                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                    boolean writeDescriptorSuccess = gatt.writeDescriptor(descriptor);
//                    //Log.i(TAG, "wrote Descriptor for HR updates " + (writeDescriptorSuccess ? "successfully" : "unsuccessfully"));
//                }
//                if (service.getUuid().equals(CSC_SERVICE_UUID)) {
//                    hasCSC = true;
//                    if (hasHR) {
//                        //need to wait and then try to notify
//                        //Log.i(TAG, "onServicesDiscovered: IS A VELO");
//                        gatt0 = gatt;
//                        if (!tryVelo) {
//                            tryVelo = true;
//                        }
//
//                    }
//
//                    BluetoothGattCharacteristic valueCharacteristic = gatt.getService(CSC_SERVICE_UUID).getCharacteristic(CSC_CHARACTERISTIC_UUID);
//                    boolean notificationSet = gatt.setCharacteristicNotification(valueCharacteristic, true);
//                    //Log.i(TAG, "registered for CSC updates " + (notificationSet ? "successfully" : "unsuccessfully"));
//                    BluetoothGattDescriptor descriptor = valueCharacteristic.getDescriptor(BTLE_NOTIFICATION_DESCRIPTOR_UUID);
//                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                    boolean writeDescriptorSuccess = gatt.writeDescriptor(descriptor);
//                    //Log.i(TAG, "wrote Descriptor for CSC updates " + (writeDescriptorSuccess ? "successfully" : "unsuccessfully"));
//                }
//
//            }
//
//        }
//
//
//
//        @Override
//        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//            super.onCharacteristicChanged(gatt, characteristic);
//            byte[] value = characteristic.getValue();
//
//
//            if (HR_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
//                //Log.i(TAG, "onCharacteristicChanged: HR, START");
//                gatt1 = gatt;
//                //int flag = characteristic.getProperties();
//                final int flag = characteristic.getValue()[0]; // 1 byte
//                int format;
//                if ((flag & 0x01) != 0) {
//                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
//                } else {
//                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
//                }
//                final int hrValue = characteristic.getIntValue(format, 1);
//
////                runOnUiThread(new Runnable() {
////                    @Override
////                    public void run() {
////                        //Log.i(TAG, "run: UPDATE HR DATA");
////                        TextView t1 = findViewById(R.id.textView1);
////                        TextView tTop1 = findViewById(R.id.tvTop);
////                        t1.setText(String.format("%d BPM", hrValue));
////                        tTop1.setText(String.format("%d BPM", hrValue));
////                    }
////                });
//
//                Message msg = Message.obtain();
//                String hrString = String.format("%d BPM", hrValue);
//                msg.obj = hrString;
//                msg.what = 3;
//                msg.setTarget(uiHandler);
//                msg.sendToTarget();
//
//
//
//
//
//
//                if (tryVelo) {
//                    tryVelo = false;
//                    ////Log.i(TAG, "onCharacteristicChanged: TRYING VELO...SET NOTIFY");
//                    BluetoothGattCharacteristic valueCharacteristic = gatt0.getService(CSC_SERVICE_UUID).getCharacteristic(CSC_CHARACTERISTIC_UUID);
//                    boolean notificationSet = gatt0.setCharacteristicNotification(valueCharacteristic, true);
//                    //Log.d(TAG, "registered for VELO CSC updates " + (notificationSet ? "successfully" : "unsuccessfully"));
//                    BluetoothGattDescriptor descriptor = valueCharacteristic.getDescriptor(BTLE_NOTIFICATION_DESCRIPTOR_UUID);
//                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                    boolean writeDescriptorSuccess = gatt0.writeDescriptor(descriptor);
//                    //Log.d(TAG, "wrote Descriptor for VELO CSC updates " + (writeDescriptorSuccess ? "successfully" : "unsuccessfully"));
//                }
//                //Log.i(TAG, "onCharacteristicChanged: HR, END");
//                return;
//            }  //END HR
//
//
//            final byte WHEEL_REVOLUTIONS_DATA_PRESENT = 0x01; // 1 bit
//            final byte CRANK_REVOLUTION_DATA_PRESENT = 0x02; // 1 bit
//
//            if (CSC_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
//                //Log.i(TAG, "onCharacteristicChanged: CSC");
//
//                final int flags = characteristic.getValue()[0]; // 1 byte
//                final boolean wheelRevPresent = (flags & WHEEL_REVOLUTIONS_DATA_PRESENT) > 0;
//                final boolean crankRevPreset = (flags & CRANK_REVOLUTION_DATA_PRESENT) > 0;
//
//                if (wheelRevPresent) {
//                    gatt2 = gatt;
//                    final int cumulativeWheelRevolutions = (value[1] & 0xff) | ((value[2] & 0xff) << 8);
//                    final int lastWheelEventReadValue = (value[5] & 0xff) | ((value[6] & 0xff) << 8);
//
//
//                    ////Log.i("WHEEL_EVENT", "onCharacteristicChanged, revs, time:  " + cumulativeWheelRevolutions + ", " + lastWheelEventReadValue);
////                    runOnUiThread(new Runnable() {
////                        @SuppressLint("DefaultLocale")
////                        public void run() {
////                            TextView t = findViewById(R.id.textView1);
////                            t.setText(String.valueOf(cumulativeWheelRevolutions));
////                        }
////                    });
//
//
//                    //Log.i("WHEEL_EVENT", "onCharacteristicChanged, revs, time:  " + cumulativeWheelRevolutions + ",  " + lastWheelEventReadValue);
//
//                    //Log.i(TAG, "onCharacteristicChanged: CALLING onWheelMeasurementReceived");
//                    onWheelMeasurementReceived(cumulativeWheelRevolutions, lastWheelEventReadValue);
////
////                    Message msg = Message.obtain();
////                    msg.obj = cumulativeWheelRevolutions + " W";
////                    msg.what = 4;
////                    msg.setTarget(uiHandler);
////                    msg.sendToTarget();
//
//
//                    if (crankRevPreset) {
//                        gatt3 = gatt;
//                        final int cumulativeCrankRevolutions = (value[7] & 0xff) | ((value[8] & 0xff) << 8);
//                        final int lastCrankEventReadValue = (value[9] & 0xff) | ((value[10] & 0xff) << 8);
//                        //Log.i("CRANK_EVENT", "onCharacteristicChanged, revs, time:  " + cumulativeCrankRevolutions + ", " + lastCrankEventReadValue);
//                        onCrankMeasurementReceived(cumulativeCrankRevolutions, lastCrankEventReadValue);
////                        Message msg2 = Message.obtain();
////                        msg2.obj = cumulativeCrankRevolutions + " C";
////                        msg2.what = 5;
////                        msg2.setTarget(uiHandler);
////                        msg2.sendToTarget();
//
//                    }
//                } else {
//                    if (crankRevPreset) {
//                        gatt3 = gatt;
//                        final int cumulativeCrankRevolutions = (value[1] & 0xff) | ((value[2] & 0xff) << 8);
//                        final int lastCrankEventReadValue = (value[3] & 0xff) | ((value[4] & 0xff) << 8);
//                        //Log.i("CRANK_EVENT", "onCharacteristicChanged, revs, time:  " + cumulativeCrankRevolutions + ", " + lastCrankEventReadValue);
//                        onCrankMeasurementReceived(cumulativeCrankRevolutions, lastCrankEventReadValue);
//                    }
//                }
//
//            }  //END CSC CALC
//
//        }  //END ON CHAR CHANGED
//
//
//    }; //END BLUETOOTHGATTCALLBACK



//2ND GATT CALLBACK END




    //private BluetoothManager bluetooth;
    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int state) {


            super.onConnectionStateChange(gatt, status, state);

            switch (status) {
                case BluetoothGatt.GATT_SUCCESS: {
                    //Log.i(TAG, "onConnectionStateChange: GATT_SUCCESS");
                    break;
                }
                case BluetoothGatt.GATT_FAILURE: {
                    //Log.i(TAG, "onConnectionStateChange: GATT_FAILURE");
                    break;
                }
                default:
                    //Log.i(TAG, "onConnectionStateChange: NOT SUCCESS OR FAILURE");

            }

            switch (state) {
                case BluetoothProfile.STATE_CONNECTED: {
                    //Log.i(TAG, "onConnectionStateChange: STATE_CONNECTED");
                    //setConnectedGatt(gatt);
                    gatt.discoverServices();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView tr = findViewById(R.id.rtStatus);
                            tr.setText(String.format("BT CONNECTED:  %s", gatt.getDevice().getName()));
                            //Log.i(TAG, "BT DISCONNECT:  " + gatt.getDevice().getName());
                        }
                    });


                    break;
                }
                case BluetoothProfile.STATE_DISCONNECTED: {
                    //Log.i(TAG, "onConnectionStateChange: STATE_DISCONNECTED");
                    //Log.i(TAG, "Waiting 30 sec, then attempt reconnect");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView tr = findViewById(R.id.rtStatus);
                            tr.setText(String.format("BT DISCONNECT:  %s", gatt.getDevice().getName()));
                            //Log.i(TAG, "BT DISCONNECT:  " + gatt.getDevice().getName());
                        }
                    });

                    tryToConnectDevice = gatt.getDevice();
                    reconnectFlag = timerSecondsCounter + 30;
                    break;
                }
                case BluetoothProfile.STATE_CONNECTING: {
                    //Log.i(TAG, "onConnectionStateChange: STATE_CONNECTING");
                    break;
                }
                case BluetoothProfile.STATE_DISCONNECTING: {
                    //Log.i(TAG, "onConnectionStateChange: STATE_DISCONNECTING");
                    break;
                }
                default:
                    //Log.i("gattCallback", "STATE_OTHER");

            }
        }  //END CONNECTION STATE CHANGE

//        @Override
//        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
//            super.onReadRemoteRssi(gatt, rssi, status);
//            //Log.i(TAG, "onReadRemoteRssi: " + rssi);
//        }

        private Boolean tryVelo = false;

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered status:" + BluetoothUtil.statusToString(status));

            Boolean hasHR = false;
            Boolean hasCSC = false;

            List<BluetoothGattService> services = gatt.getServices();
            //Log.i("TEST", "DETERMINE IF BOTH SERVICES EXIST");
            for (BluetoothGattService service : services) {


                if (service.getUuid().equals(HR_SERVICE_UUID)) {
                    hasHR = true;
                    BluetoothGattCharacteristic valueCharacteristic = gatt.getService(HR_SERVICE_UUID).getCharacteristic(HR_CHARACTERISTIC_UUID);
                    boolean notificationSet = gatt.setCharacteristicNotification(valueCharacteristic, true);
                    //Log.i(TAG, "registered for HR updates " + (notificationSet ? "successfully" : "unsuccessfully"));
                    BluetoothGattDescriptor descriptor = valueCharacteristic.getDescriptor(BTLE_NOTIFICATION_DESCRIPTOR_UUID);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    boolean writeDescriptorSuccess = gatt.writeDescriptor(descriptor);
                    //Log.i(TAG, "wrote Descriptor for HR updates " + (writeDescriptorSuccess ? "successfully" : "unsuccessfully"));
                }
                if (service.getUuid().equals(CSC_SERVICE_UUID)) {
                    hasCSC = true;
                    if (hasHR) {
                        //need to wait and then try to notify
                        //Log.i(TAG, "onServicesDiscovered: IS A VELO");
                        gatt0 = gatt;
                        if (!tryVelo) {
                            tryVelo = true;
                        }

                    }

                    BluetoothGattCharacteristic valueCharacteristic = gatt.getService(CSC_SERVICE_UUID).getCharacteristic(CSC_CHARACTERISTIC_UUID);
                    boolean notificationSet = gatt.setCharacteristicNotification(valueCharacteristic, true);
                    //Log.i(TAG, "registered for CSC updates " + (notificationSet ? "successfully" : "unsuccessfully"));
                    BluetoothGattDescriptor descriptor = valueCharacteristic.getDescriptor(BTLE_NOTIFICATION_DESCRIPTOR_UUID);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    boolean writeDescriptorSuccess = gatt.writeDescriptor(descriptor);
                    //Log.i(TAG, "wrote Descriptor for CSC updates " + (writeDescriptorSuccess ? "successfully" : "unsuccessfully"));
                }

            }

        }



        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] value = characteristic.getValue();


            if (HR_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                ////Log.i(TAG, "onCharacteristicChanged: HR, START");
                gatt1 = gatt;
                //int flag = characteristic.getProperties();
                final int flag = characteristic.getValue()[0]; // 1 byte
                int format;
                if ((flag & 0x01) != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                } else {
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
                }
                final int hrValue = characteristic.getIntValue(format, 1);

                if (hrValue < 20 || hrValue > 230) {return;}

                arrHeartrates.add((double) hrValue);
                sumOfTotalHeartRates += hrValue;
                indexOfTotalHeartrates += 1;
                computeRoundHR();

//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        //Log.i(TAG, "run: UPDATE HR DATA");
//                        TextView t1 = findViewById(R.id.textView1);
//                        TextView tTop1 = findViewById(R.id.tvTop);
//                        t1.setText(String.format("%d BPM", hrValue));
//                        tTop1.setText(String.format("%d BPM", hrValue));
//                    }
//                });

                Message msg = Message.obtain();
                String hrString = String.format("%d HR", hrValue);
                msg.obj = hrString;
                msg.what = 3;
                msg.setTarget(uiHandler);
                msg.sendToTarget();






                if (tryVelo) {
                    tryVelo = false;
                    ////Log.i(TAG, "onCharacteristicChanged: TRYING VELO...SET NOTIFY");
                    BluetoothGattCharacteristic valueCharacteristic = gatt0.getService(CSC_SERVICE_UUID).getCharacteristic(CSC_CHARACTERISTIC_UUID);
                    boolean notificationSet = gatt0.setCharacteristicNotification(valueCharacteristic, true);
                    //Log.d(TAG, "registered for VELO CSC updates " + (notificationSet ? "successfully" : "unsuccessfully"));
                    BluetoothGattDescriptor descriptor = valueCharacteristic.getDescriptor(BTLE_NOTIFICATION_DESCRIPTOR_UUID);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    boolean writeDescriptorSuccess = gatt0.writeDescriptor(descriptor);
                    //Log.d(TAG, "wrote Descriptor for VELO CSC updates " + (writeDescriptorSuccess ? "successfully" : "unsuccessfully"));
                }
                ////Log.i(TAG, "onCharacteristicChanged: HR, END");
                return;
            }  //END HR


            final byte WHEEL_REVOLUTIONS_DATA_PRESENT = 0x01; // 1 bit
            final byte CRANK_REVOLUTION_DATA_PRESENT = 0x02; // 1 bit

            if (CSC_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                ////Log.i(TAG, "onCharacteristicChanged: CSC");

                final int flags = characteristic.getValue()[0]; // 1 byte
                final boolean wheelRevPresent = (flags & WHEEL_REVOLUTIONS_DATA_PRESENT) > 0;
                final boolean crankRevPresent = (flags & CRANK_REVOLUTION_DATA_PRESENT) > 0;

                if (wheelRevPresent) {
                    gatt2 = gatt;
                    final int cumulativeWheelRevolutions = (value[1] & 0xff) | ((value[2] & 0xff) << 8);
                    final int lastWheelEventReadValue = (value[5] & 0xff) | ((value[6] & 0xff) << 8);


                    ////Log.i("WHEEL_EVENT", "onCharacteristicChanged, revs, time:  " + cumulativeWheelRevolutions + ", " + lastWheelEventReadValue);
//                    runOnUiThread(new Runnable() {
//                        @SuppressLint("DefaultLocale")
//                        public void run() {
//                            TextView t = findViewById(R.id.textView1);
//                            t.setText(String.valueOf(cumulativeWheelRevolutions));
//                        }
//                    });


                    ////Log.i("WHEEL_EVENT", "onCharacteristicChanged, revs, time:  " + cumulativeWheelRevolutions + ",  " + lastWheelEventReadValue);
                    ////Log.i(TAG, "onCharacteristicChanged: CALLING onWheelMeasurementReceived");
                    onWheelMeasurementReceived(cumulativeWheelRevolutions, lastWheelEventReadValue);

//                    Message msg = Message.obtain();
//                    msg.obj = cumulativeWheelRevolutions + " W";
//                    msg.what = 4;
//                    msg.setTarget(uiHandler);
//                    msg.sendToTarget();


                    if (crankRevPresent) {
                        gatt3 = gatt;
                        final int cumulativeCrankRevolutions = (value[7] & 0xff) | ((value[8] & 0xff) << 8);
                        final int lastCrankEventReadValue = (value[9] & 0xff) | ((value[10] & 0xff) << 8);
                        ////Log.i("CRANK_EVENT", "onCharacteristicChanged, revs, time:  " + cumulativeCrankRevolutions + ", " + lastCrankEventReadValue);
                        onCrankMeasurementReceived(cumulativeCrankRevolutions, lastCrankEventReadValue);
//                        Message msg2 = Message.obtain();
//                        msg2.obj = cumulativeCrankRevolutions + " C";
//                        msg2.what = 5;
//                        msg2.setTarget(uiHandler);
//                        msg2.sendToTarget();

                    }
                } else {
                    if (crankRevPresent) {
                        gatt3 = gatt;
                        final int cumulativeCrankRevolutions = (value[1] & 0xff) | ((value[2] & 0xff) << 8);
                        final int lastCrankEventReadValue = (value[3] & 0xff) | ((value[4] & 0xff) << 8);
                        ////Log.i("CRANK_EVENT", "onCharacteristicChanged, revs, time:  " + cumulativeCrankRevolutions + ", " + lastCrankEventReadValue);
                        onCrankMeasurementReceived(cumulativeCrankRevolutions, lastCrankEventReadValue);
                    }
                }

            }  //END CSC CALC

        }  //END ON CHAR CHANGED


    }; //END BLUETOOTHGATTCALLBACK



    private int sumOfTotalHeartRates = 1;
    private int indexOfTotalHeartrates = 1;
    private double totalAverageHeartrate = 0;
    private double totalAverageScore = 0;
    private ArrayList<Double> arrHeartrates = new ArrayList<>();
    private void computeRoundHR() {
        ////Log.i(TAG, "roundHR: compute roundHR");
        if (reconnectFlag == 0) {
            reconnectFlag = 1;
            return;
        }
        double sum = 0;

        for (double hr : arrHeartrates) {
            sum += hr;
        }
        tim.setRoundHR(arrHeartrates.isEmpty()? 0: 1.0*sum/arrHeartrates.size());
        totalAverageHeartrate = (double) sumOfTotalHeartRates / (double) indexOfTotalHeartrates;
        totalAverageScore  = (totalAverageHeartrate / maxHRdouble) * 100;
    }


    private int mFirstWheelRevolutions = -1;
    private int mLastWheelRevolutions = -1;
    private int mLastWheelEventTime = -1;
    private int mFirstCrankRevolutions = -1;
    private int mLastCrankRevolutions = -1;
    private int mLastCrankEventTime = -1;

    private double totalWheelRevolutions = 0;
    private double totalTimeInSeconds = 0;
    private double distanceAtStartOfPreviousRound = 0;
    private double secondsAtStartOfPreviousRound = 0;


    //CSC ADVANCED CALC
    private void onWheelMeasurementReceived(final int wheelRevolutionValue, final int wheelRevolutionTimeValue) {
        ////Log.i(TAG, "onWheelMeasurementReceived:  START");

        usingBT = true;
        final int localTimerSecCounter = timerSecondsCounter;
        
        if (mFirstWheelRevolutions < 0) {
            mFirstWheelRevolutions = wheelRevolutionValue;
            mLastWheelRevolutions = wheelRevolutionValue;
            mLastWheelEventTime = wheelRevolutionTimeValue;
            return;
        }

        if (mLastWheelEventTime == wheelRevolutionTimeValue) {
            return;
        }


        final int timeDiff = do16BitDiff(wheelRevolutionTimeValue, mLastWheelEventTime);
        final int wheelDiff = do16BitDiff(wheelRevolutionValue, mLastWheelRevolutions);

        if (wheelDiff == 0 || wheelDiff > 35) {
            mLastWheelRevolutions = wheelRevolutionValue;
            mLastWheelEventTime = wheelRevolutionTimeValue;
            return;
        }

        if (timeDiff < 1000) {
            //LET'S NOT PROCESS SO MANY, IGNORE EVERY OTHER ONE?
            //TODO:  MAY LOWER OR RAISE...
            return;
        }

        if (timeDiff > 30000) {
            mLastWheelRevolutions = wheelRevolutionValue;
            mLastWheelEventTime = wheelRevolutionTimeValue;
            return;
        }


        totalWheelRevolutions += (double) wheelDiff;
        totalTimeInSeconds += (double) timeDiff / 1024.0;

        tim.addWheelDiff(wheelDiff);
        tim.addTimeDiff(timeDiff);

        mLastWheelRevolutions = wheelRevolutionValue;
        mLastWheelEventTime = wheelRevolutionTimeValue;

        //final double localTotalTimeInSeconds = totalTimeInSeconds;
//        //Log.i(TAG, "onWheelMeasurementReceived: totalTimeInSeconds: " + totalTimeInSeconds);
//        //Log.i(TAG, "onWheelMeasurementReceived: totalWheelRevolutions: " + totalWheelRevolutions);

        final double wheelTimeInSeconds = timeDiff / 1024.0;
        final double wheelCircumference = wheelSizeMM;
        final double wheelCircumferenceCM = wheelCircumference / 10;
        final double wheelRPM = (double) wheelDiff / (wheelTimeInSeconds / 60.0);
        final double cmPerMi = 0.00001 * 0.621371;
        final double minsPerHour = 60.0;
        final double speed = wheelRPM * wheelCircumferenceCM * cmPerMi * minsPerHour;  //MPH CURRENT
        final double totalDistance = totalWheelRevolutions * wheelCircumferenceCM * cmPerMi;

//        //Log.i(TAG, "onWheelMeasurementReceived: speed: " + speed);
//        //Log.i(TAG, "onWheelMeasurementReceived: totalDistance: " + totalDistance);
//        //Log.i(TAG, "onWheelMeasurementReceived: calc totaltimeBT and avgSpeedBT");


        final long millis = (long) totalTimeInSeconds * 1000;
        final String hms = getTimeStringFromMilli(millis);

        final double btAvgSpeed = totalDistance / (totalTimeInSeconds / 60.0 / 60.0);
        tim.setBtAvgSpeed(btAvgSpeed);
//        //Log.i(TAG, "onWheelMeasurementReceived: btAvgSpeed:  " + btAvgSpeed);
//        //Log.i(TAG, "onWheelMeasurementReceived: btElapsedTime: " + hms);

//        tim.btTotalDistance = totalDistance;
//        tim.btMovingTime = (long) wheelTimeInSeconds;
//        tim.btAvgSpeed = btAvgSpeed;

        tim.setBtSpeed(speed);
        tim.setBtPace(calcPace(speed));
        tim.setBtTotalDistance(totalDistance);
        tim.setBtAvgSpeed(btAvgSpeed);
        tim.setBtAvgPace(calcPace(speed));


        //final String btAvgPce = calcPace(speed);

        updateUI(tim.getBtTotalDistance(), hms, tim.getBtAvgSpeed(), tim.getBtAvgPace());


        Message msg = Message.obtain();
        msg.obj = String.format(Locale.US,"%.1f MPH", tim.getBtSpeed());
        msg.what = 4;
        msg.setTarget(uiHandler);
        msg.sendToTarget();



        //END OF MILE CALC
        if (localTimerSecCounter - secondsAtEndOfMileBT > 10) {
            ////Log.i(TAG, "onWheelMeasurementReceived: end of mile calc");
            if (totalDistance > currentMileBT) {
                //Log.i(TAG, "onWheelMeasurementReceived: END OF MILE BT");
                  endMileSpeedBT = 1 / (((double) localTimerSecCounter - (double) secondsAtEndOfMileBT) / 60.0 / 60.0);
                  valuesMiles.add(String.format("%d.  %s", currentMileBT, String.format(Locale.US, "%.2f MPH (BT)", endMileSpeedBT)));
                  currentMileBT += 1;
                  secondsAtEndOfMileBT = localTimerSecCounter;
                //current round starts at 1

              }
          }

        //CURRENT MILE CALC
        final double secAtEndOfMile = secondsAtEndOfMileBT;
        final int curMile = currentMileBT;
        double curMileResult = 0.0;

          if (localTimerSecCounter - secondsAtEndOfMileBT > 10) {
              ////Log.i(TAG, "onWheelMeasurementReceived: current mile calc");
              if (curMile == 1 && totalDistance > (curMile - 1)) {
                  curMileResult = totalDistance / (((double) localTimerSecCounter - secAtEndOfMile) / 60.0 / 60.0);
              }

              if (curMile > 1 && totalDistance > (curMile - 1)) {
                  curMileResult = ( totalDistance - ((double) curMile - 1.0) ) / (( (double) localTimerSecCounter - secAtEndOfMile) / 60.0 / 60.0);
              }
          }
        currentMileSpeedBT = curMileResult;



        //final String hms_act = getTimeStringFromMilli((long) localTimerSecCounter * 1000);


        //ROUND CALC
        //USES TIMERCOUNTER, NOT BT.TOTALTIME IN SECONDS
        double distanceDuringCurrentRound = totalDistance - distanceAtStartOfPreviousRound;
        double elapsedSecondsInCurrentRound = (double) localTimerSecCounter - secondsAtStartOfPreviousRound;
        double currentRoundSpeedMPH = 0;
        if (elapsedSecondsInCurrentRound > 5) {
            ////Log.i(TAG, "onWheelMeasurementReceived: calc roundspeedBT");
            currentRoundSpeedMPH = distanceDuringCurrentRound / (elapsedSecondsInCurrentRound / 60.0 / 60.0);
        }
        currentRoundSpeedBT = currentRoundSpeedMPH;

        if (newRoundFlagBT && totalDistance > distanceAtStartOfPreviousRound) {
            ////Log.i(TAG, "BT NEW ROUND");
            distanceAtStartOfPreviousRound = totalDistance;
            secondsAtStartOfPreviousRound = localTimerSecCounter;
            newRoundFlagBT = false;
        }

//
//



        ////Log.i(TAG, "onWheelMeasurementReceived:  END");

    }



    private void updateUI(final double dist, final String btTime, final double avgSpd, final String rtPace) {
        final TextView foot2 = findViewById(R.id.tvFooter2);
        final TextView head1 = findViewById(R.id.tvHeader1);
        final TextView head2 = findViewById(R.id.tvHeader2);

        final TextView bTime = findViewById(R.id.rtText1a);
        final TextView bPace = findViewById(R.id.rtText3a);
        final TextView bAvgSp = findViewById(R.id.rtText8a);
        final TextView bAvgPc = findViewById(R.id.rtText9a);
        final TextView bDist = findViewById(R.id.rtText4a);

        runOnUiThread(new Runnable() {
            public void run() {

                bTime.setText(btTime);
                head1.setText(btTime);

                bAvgSp.setText(String.format(Locale.US,"%.1f MPH", avgSpd));
                bAvgPc.setText(calcPace(avgSpd));

                foot2.setText(String.format(Locale.US,"%.2f MI", dist));
                head2.setText(String.format(Locale.US,"%.1f MPH", avgSpd));
                bDist.setText(String.format(Locale.US,"%.2f MI", dist));
                bPace.setText(rtPace);

            }
        });
    }





    private void onCrankMeasurementReceived(final int crankRevolutionValue, final int crankRevolutionTimeValue) {
        if (mFirstCrankRevolutions < 0) {
            mFirstCrankRevolutions = crankRevolutionValue;
            mLastCrankRevolutions = crankRevolutionValue;
            mLastCrankEventTime = crankRevolutionTimeValue;
            return;
        }

        if (mLastCrankEventTime == crankRevolutionTimeValue) {
            return;
        }


        final int timeDiff = do16BitDiff(crankRevolutionTimeValue, mLastCrankEventTime);
        final int crankDiff = do16BitDiff(crankRevolutionValue, mLastCrankRevolutions);

        if (crankDiff == 0) {
            mLastCrankRevolutions = crankRevolutionValue;
            mLastCrankEventTime = crankRevolutionTimeValue;
            return;
        }

        if (timeDiff < 2000) {
            return;
        }

        if (timeDiff > 30000) {
            mLastCrankRevolutions = crankRevolutionValue;
            mLastCrankEventTime = crankRevolutionTimeValue;
            return;
        }


        ////Log.i("CAD", "onWheelMeasurementReceived: crankDiff, timeDiff: " + crankDiff + ", " + timeDiff);
        final double cadence = (double) crankDiff / ((((double) timeDiff) / 1024.0) / 60);
        if (cadence == 0) {
            return;
        }
        if (cadence > 150) {
            return;
        }
        ////Log.i("CAD", "CADENCE: " + cadence);

        Message msg = Message.obtain();
        msg.obj = String.format(Locale.US,"%.0f RPM", cadence);
        msg.what = 5;
        msg.setTarget(uiHandler);
        msg.sendToTarget();

    }





    //END CSC ADVANCED CALC

    private int do16BitDiff(int a, int b) {
        if (a >= b)
            return a - b;
        else
            return (a + 65536) - b;
    }

    public String getTimeStringFromMilli(long totalMilliseconds) {
        final String hms = String.format(Locale.US,"%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(totalMilliseconds),
                TimeUnit.MILLISECONDS.toMinutes(totalMilliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(totalMilliseconds)),
                TimeUnit.MILLISECONDS.toSeconds(totalMilliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalMilliseconds)));
        return hms;
    }


    private BluetoothGatt connectedGatt;

    private BluetoothGatt gatt0;
    private BluetoothGatt gatt1;
    private BluetoothGatt gatt2;
    private BluetoothGatt gatt3;
    //private BluetoothGatt gatt4;

    private int cycle = 1;

    //DISCONNECT ALL
    public void onClick_4(View view) {
        //Log.i(TAG, "onClick_4: clicked, disconnect all");


        //TODO, CYCLE THROUGH gattsConnected arrList, disconnect all nonNull
        for (BluetoothGatt btGatt : bluetoothGatts) {
            if (btGatt != null) {
                //Log.i(TAG, "bluetoothGatts to disconnect, Name: " + btGatt.getDevice().getName());
                btGatt.disconnect();
                btGatt.close();
            }

        }


        if (connectedGatt != null) {
            //Log.i(TAG, "onClick_4: connectedGatt name:  " + connectedGatt.getDevice().getName());
            connectedGatt.disconnect();
            connectedGatt.close();
        }


        int locCycle = cycle;
        if (locCycle == 1) {
            if (gatt1 != null) {
                //Log.i(TAG, "onClick_4: disconnect gatt1,  name:  " + gatt1.getDevice().getName());
                gatt1.disconnect();
                gatt1.close();
                gatt1 = null;
            }
            cycle = 2;
        }
        if (locCycle == 2) {
            if (gatt2 != null) {
                //Log.i(TAG, "onClick_4: disconnect gatt2 name:  " + gatt2.getDevice().getName());
                gatt2.disconnect();
                gatt2.close();
                gatt2 = null;
            }
            cycle = 3;
        }
        if (locCycle == 3) {
            if (gatt3 != null) {
                //Log.i(TAG, "onClick_4: disconnect gatt3 name:  " + gatt3.getDevice().getName());
                gatt3.disconnect();
                gatt3.close();
                gatt3 = null;
            }
            cycle = 1;
        }

        Button btn100 = findViewById(R.id.button100);
        Button btn101 = findViewById(R.id.button101);
        Button btn102 = findViewById(R.id.button102);
        Button btn103 = findViewById(R.id.button103);
        Button btn104 = findViewById(R.id.button104);

        btn100.setVisibility(View.GONE);
        btn101.setVisibility(View.GONE);
        btn102.setVisibility(View.GONE);
        btn103.setVisibility(View.GONE);
        btn104.setVisibility(View.GONE);


    }



    //start on location received

    private ArrayList<Double> arrLats = new ArrayList<>();
    private ArrayList<Double> arrLons = new ArrayList<>();
    private Double oldLat = 0.0;
    private Double oldLon = 0.0;
    private Double geoDistance = 0.0;
    private Double geoAvgSpeed = 0.0;
    private float[] results = new float[2];
    private long oldTime = 0;
    private long totalTimeGeo = 0;  //GPS MOVING TIME IN MILLI

    Double distanceAtStartOfPreviousRoundGeo = 0.0;
    Integer secondsAtStartOfPreviousRoundGeo = 0;


    @SuppressLint("DefaultLocale")
    public void onLocationReceived(Location location) {
        //mPrinter("ON LOCATION RECEIVED:  " + location.getProvider() + "," + location.getLatitude() + "," + location.getLongitude());
        arrLats.add(location.getLatitude());
        arrLons.add(location.getLongitude());
        //mPrinter("ARRLATS.SIZE: " + arrLats.size());

        if (arrLats.size() < 5) {
            oldLat = location.getLatitude();
            oldLon = location.getLongitude();
            oldTime = location.getTime();
        } else {
            Location.distanceBetween(oldLat, oldLon, location.getLatitude(), location.getLongitude(), results);

            if (results.length > 0) {

                //mPrinter("RESULTS[0]  " + results[0] * 0.000621371 +  "  MILES"); //AS MILES
                if (results[0] == 0) {
                    //mPrinter("NOTHING AT RESULTS[0] - RETURN");
                    return;
                }
                if (results[0] * 0.000621371 <= 0) {
                    //mPrinter("NO DISTANCE TRAVELED - RETURN");
                    return;
                }

                //OPT 1.  QUICKREAD GEO SPEED
                final double geoSpeedQuick = (double) location.getSpeed() * 2.23694;  //meters/sec to mi/hr
                //mPrinter("GEO SPEED Q: " + geoSpeedQuick);

                //OPT 2.  GEO SPEED, LONG VERSION
                Double gd = results[0] * 0.000621371;
                long gt = (location.getTime() - oldTime);  //MILLI
                Double geoSpeed = gd / ((double) gt / 1000 / 60 / 60);
                //mPrinter("GEO SPEED: " + geoSpeed);
                //END GEO SPEED CALC
                geoDistance += results[0] * 0.000621371;


                //END OF MILE CALC
                if (timerSecondsCounter - secondsAtEndOfMileGeo > 10) {
                    if (geoDistance > currentMileGEO) {
                        //Log.i(TAG, "onLocationReceived: END OF CURRENT MILE GEO");
                        endMileSpeedGEO = 1 / (((double) timerSecondsCounter - (double) secondsAtEndOfMileGeo) / 60.0 / 60.0);
                        valuesMilesGeo.add(String.format("%d.  %s", currentMileGEO, String.format(Locale.US, "%.2f MPH (GEO)", endMileSpeedGEO)));
                        currentMileGEO += 1;
                        secondsAtEndOfMileGeo = timerSecondsCounter;
                        readMileInfo = true;
                    }
                }

                //CURRENT MILE CALC GEO
                //CURRENT MILE CALC
                final double curDist = geoDistance;
                final double timerSecCtr = timerSecondsCounter;
                final double secAtEndOfMile = secondsAtEndOfMileGeo;
                final int curMile = currentMileGEO;
                double curMileResult = 0.0;

                if (timerSecondsCounter - secondsAtEndOfMileBT > 10) {
                    //if (totalDistance > (currentMileBT - 1)) {
                    //currentMileSpeedBT = 1 / (((double) timerSecondsCounter - (double) secondsAtEndOfMileBT) / 60.0 / 60.0);
                    //CALC BASED ON ELAPSED DISTANCE WITHIN CURRENT MILE
                    if (curMile == 1 && curDist > (curMile - 1)) {
//                  double currentMileSpeedBT = ( totalDistance - ((double) currentMileBT - 1.0) ) / (((double) timerSecondsCounter - (double) secondsAtEndOfMileBT) / 60.0 / 60.0);
//                  //Log.i(TAG, "currentMileSpeedBT: " + currentMileSpeedBT);
                        curMileResult = curDist / ((timerSecCtr - secAtEndOfMile) / 60.0 / 60.0);
                        ////Log.i(TAG, "curMileResult: " + curMileResult);
                    }

                    if (curMile > 1 && curDist > (curMile - 1)) {
                        curMileResult = ( curDist - ((double) curMile - 1.0) ) / (( timerSecCtr - secAtEndOfMile) / 60.0 / 60.0);
                        ////Log.i(TAG, "curMileResult2: " + curMileResult);
                    }
                    //}
                }
                currentMileSpeedGEO = curMileResult;


                //END MILE CALC
//                mPrinter("OLDTIME " + oldTime);
//                mPrinter("NEWTIME " + location.getTime());
//                mPrinter("totalTimeGeo " + totalTimeGeo);
                totalTimeGeo += (location.getTime() - oldTime);  //MILLI
//                mPrinter("GEODISTANCE: " + geoDistance);
//                mPrinter("TOTALTIMEGEO: " + totalTimeGeo);

                double ttg = totalTimeGeo;  //IN MILLI
                geoAvgSpeed = geoDistance / (ttg / 1000.0 / 60.0 / 60.0);
                tim.setGeoAvgSpeed(geoAvgSpeed);

                long millis = totalTimeGeo;
                @SuppressLint("DefaultLocale") final String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                        TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                        TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));


                //mPrinter("ELAPSED TIME (GEO): " + hms);

                //START ROUND CALC
                double distanceDuringCurrentRoundGeo = geoDistance - distanceAtStartOfPreviousRoundGeo;
                double elapsedSecondsInCurrentRoundGeo = (double) timerSecondsCounter - (double) secondsAtStartOfPreviousRoundGeo;

                double currentRoundSpeedMPHGeo = 0;
                if (elapsedSecondsInCurrentRoundGeo > 5) {
                    currentRoundSpeedMPHGeo = distanceDuringCurrentRoundGeo / (elapsedSecondsInCurrentRoundGeo / 60.0 / 60.0);
                }
                currentRoundSpeedGEO = currentRoundSpeedMPHGeo;
                ////Log.i(TAG, "CURRENT ROUND SPEED GEO: " + currentRoundSpeedMPHGeo);
                if (newRoundFlagGEO && geoDistance > distanceAtStartOfPreviousRoundGeo) {
                    //Log.i(TAG, "GEO NEW ROUND");

                    distanceAtStartOfPreviousRoundGeo = geoDistance;
                    secondsAtStartOfPreviousRoundGeo = timerSecondsCounter;
                    newRoundFlagGEO = false;
                }
                //END ROUND CALC


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView rtText2b = findViewById(R.id.rtText2b);
                        rtText2b.setText(String.format("%.1f MPH (G)", geoSpeedQuick));

                        TextView rtText3b = findViewById(R.id.rtText3b);
                        rtText3b.setText(String.format("%s (G)", calcPace(geoSpeedQuick)));

                        TextView rtText8b = findViewById(R.id.rtText8b);
                        rtText8b.setText(String.format("%.1f AVG (G)", geoAvgSpeed));

                        TextView rtText9b = findViewById(R.id.rtText9b);
                        rtText9b.setText(String.format("%s (G)", calcPace(geoAvgSpeed)));

                        TextView t = findViewById(R.id.rtText1b);
                        t.setText(String.format("%s  (G)", hms));

                        TextView rtText4b = findViewById(R.id.rtText4b);
                        rtText4b.setText(String.format("%.2f MI (G)", geoDistance));


                        final TextView tMid = findViewById(R.id.tvMiddleGeo);
                        tMid.setText(String.format("%s", calcPace(geoSpeedQuick)));
                        final TextView tBot = findViewById(R.id.tvBottomGeo);
                        tBot.setText(String.format("%s (a)", calcPace(geoAvgSpeed)));
                        final TextView foot2 = findViewById(R.id.tvFooter2Geo);
                        foot2.setText(String.format("%.2f MI (G)", geoDistance));
                        final TextView head2 = findViewById(R.id.tvHeader2Geo);
                        head2.setText(String.format("%.1f AVG (G)", geoAvgSpeed));
                        final TextView head1 = findViewById(R.id.tvHeader1Geo);
                        head1.setText(String.format("%s  (G)", hms));

                        if (usingBT == false) {
                            final TextView tMidXXX = findViewById(R.id.tvMiddle);
                            tMidXXX.setText(String.format("%.1f MPH", geoSpeedQuick));
                            final TextView tBotXXX = findViewById(R.id.tvBottom);
                            tBotXXX.setText(String.format("%s", calcPace(geoSpeedQuick)));
                            final TextView foot2XXX = findViewById(R.id.tvFooter2);
                            foot2XXX.setText(String.format("%.2f MI (G)", geoDistance));
                            final TextView head2XXX = findViewById(R.id.tvHeader2);
                            head2XXX.setText(String.format("%.1f AVG (G)", geoAvgSpeed));
                            final TextView head1XXX = findViewById(R.id.tvHeader1);
                            head1XXX.setText(String.format("%s  (G)", hms));

                        }

                    }
                });


            }

            oldLat = location.getLatitude();
            oldLon = location.getLongitude();
            oldTime = location.getTime();
        }


    }

    private void mPrinter(String s) {
        //Log.i(TAG, ":" + s);
    }

    //end on location


private String calcPace(double mph) {


        double a = (60.0 / mph);
        if (a == 0 || a > 50) {
            return "00:00";
        }

        double m = a * 60.0 * 1000.0;
        long mill = (long) m;
//    final String minutesPerMile = String.format(Locale.US,"%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(mill),
//            TimeUnit.MILLISECONDS.toMinutes(mill) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(mill)),
//            TimeUnit.MILLISECONDS.toSeconds(mill) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(mill)));

    final String minutesPerMile = String.format(Locale.US,"%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(mill) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(mill)),
            TimeUnit.MILLISECONDS.toSeconds(mill) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(mill)));

        return minutesPerMile;

}


    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private Handler mServiceHandler;
    private Location mLocation;


    //GPS BUTTON...
    private Boolean isGpsActive = false;
    public void onClick_2(View view) {
        //Log.i(TAG, "onClick_2: clicked");
        Button b2 = findViewById(R.id.button2);

        if (isGpsActive) {
            b2.setText("OFF");
            try {
                mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            } catch (Exception e){
                //Log.i(TAG, "onClick_2 Error,  DIDN'T STOP LOCATION");
            }
        }


        isGpsActive = true;
        b2.setText("ON");

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(3000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }

            private void onNewLocation(Location lastLocation) {
                ////Log.i(TAG, "onNewLocation: " + lastLocation.getSpeed());
                onLocationReceived(lastLocation);
            }
        };

        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            //Utils.setRequestingLocationUpdates(this, false);
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }

        //when ready to stop...mFusedLocationClient.removeLocationUpdates(mLocationCallback);





}  //end onclick2





    //START SCAN BUTTON
    public void onClick_1(View view) {
        //Log.i("CLICK", "onClick_1: clicked");
        final Button b1 = findViewById(R.id.button1);
        b1.setText("...");
        Toast.makeText(this,"SCANNING...", Toast.LENGTH_LONG).show();
        //scanLeDevice(true);

            //Log.i("SCANLEDEVICE", "START SCANNING");
        mLEScanner.startScan(filters, settings, mScanCallback);
        isScanning = true;
        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mLEScanner.stopScan(mScanCallback);
                //Log.i("SCANLEDEVICE", "STOP SCANNING");
//                    sendToaster("SCAN COMPLETE");
                isScanning = false;
                sendToaster("SCAN COMPLETE");
                b1.setText("SCAN");
            }
        }, SCAN_PERIOD);

        if (!isScanning) {
            devicesDiscovered = new ArrayList<>();
            deviceIndexVal = 0;

            Button btn100 = findViewById(R.id.button100);
            Button btn101 = findViewById(R.id.button101);
            Button btn102 = findViewById(R.id.button102);
            Button btn103 = findViewById(R.id.button103);
            Button btn104 = findViewById(R.id.button104);

            btn100.setVisibility(View.GONE);
            btn101.setVisibility(View.GONE);
            btn102.setVisibility(View.GONE);
            btn103.setVisibility(View.GONE);
            btn104.setVisibility(View.GONE);

            mLEScanner.startScan(filters, settings, mScanCallback);
            isScanning = true;
            //Log.i("SCAN", "isScanning: ");
        }


    } //END BTN1 CLICK

    private void sendToaster(String toasterText) {
        Toast.makeText(this,toasterText, Toast.LENGTH_SHORT).show();
    }

    //SCANCALLBACK - START
    public ScanCallback mScanCallback;
    {
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {

                if (result.getDevice().getName() != null) {
//                    //Log.i("SCAN_CB", "onScanResult: " + result.getDevice().getName());
                    BluetoothDevice deviceDiscovered = result.getDevice();
                    String deviceName = result.getDevice().getName();
                    String deviceAddress = result.getDevice().getAddress();

                    if (!devicesDiscovered.contains(deviceDiscovered)) {
                        devicesDiscovered.add(deviceDiscovered);

                        sendToaster("FOUND:  " + deviceName);
                        //Log.i("deviceIndexVal", "deviceIndexVal  " + deviceIndexVal);
                        //Log.i("result", "NAME  " + result.getDevice().getName());
                        //Log.i("result", "ADDRESS  " + result.getDevice().getAddress());

                        Button btn100 = findViewById(R.id.button100);
                        Button btn101 = findViewById(R.id.button101);
                        Button btn102 = findViewById(R.id.button102);
                        Button btn103 = findViewById(R.id.button103);
                        Button btn104 = findViewById(R.id.button104);

                        if (deviceIndexVal == 0) {
                            btn100.setVisibility(View.VISIBLE);
                            btn100.setText(deviceName);
                            //Log.i("btn100", "deviceIndexVal:  " + deviceIndexVal + " - " + deviceName);
                        }
                        if (deviceIndexVal == 1) {
                            btn101.setVisibility(View.VISIBLE);
                            btn101.setText(deviceName);
                            //Log.i("btn101", "deviceIndexVal:  " + deviceIndexVal + " - " + deviceName);
                        }
                        if (deviceIndexVal == 2) {
                            btn102.setVisibility(View.VISIBLE);
                            btn102.setText(deviceName);
                            //Log.i("btn102", "deviceIndexVal:  " + deviceIndexVal + " - " + deviceName);
                        }
                        if (deviceIndexVal == 3) {
                            btn103.setVisibility(View.VISIBLE);
                            btn103.setText(deviceName);
                            //Log.i("btn103", "deviceIndexVal:  " + deviceIndexVal + " - " + deviceName);
                        }
                        if (deviceIndexVal == 4) {
                            btn104.setVisibility(View.VISIBLE);
                            btn104.setText(deviceName);
                            //Log.i("btn104", "deviceIndexVal:  " + deviceIndexVal + " - " + deviceName);
                        }
                        deviceIndexVal = deviceIndexVal + 1;
                    }  //DUPLICATE, DON'T ADD
                }  //NO NAME, DON'T, ADD


            }
            //END ON RESULT

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                for (ScanResult sr : results) {
                    //Log.i("ScanResult - Results", sr.toString());
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                //Log.i("Scan Failed", "Error Code: " + errorCode);
            }
        };
    }
//SCANCALLBACK - FINISH


    public void reconnectToDevice(BluetoothDevice mDevice) {
    //Log.i("ReconnectToDevice", "Device: " + mDevice.getName());
    //Log.i("ReconnectToDevice", "Address: " + mDevice.getAddress());

//disconnect/close old gatts first?
    connectedGatt = mDevice.connectGatt(this, true, bluetoothGattCallback);


}


    public void connectToDevice(BluetoothDevice mDevice) {
        //Log.i("connectToDevice", "Device: " + mDevice.getName());
        //Log.i("connectToDevice", "Addresss: " + mDevice.getAddress());


        connectedGatt = mDevice.connectGatt(this, false, bluetoothGattCallback);
        Toast.makeText(this,"Connecting to: " + mDevice.getName(), Toast.LENGTH_LONG).show();
        devicesConnected.add(mDevice);
        bluetoothGatts.add(connectedGatt);
        devicesConnectedAddresses.add(mDevice.getAddress());
    }


    private ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<>();
    private ArrayList<BluetoothDevice> devicesConnected = new ArrayList<>();
    private ArrayList<String> devicesConnectedAddresses = new ArrayList<>();
    private ArrayList<BluetoothGatt> bluetoothGatts = new ArrayList<>();
    private Integer deviceIndexVal = 0;
    private Boolean isScanning = false;



    private BluetoothDevice mDevice;

    public void mConnectToDevice(final int indexVal) {
        mDevice = devicesDiscovered.get(indexVal);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("CONNECT TO:  " + mDevice.getName())
                .setCancelable(false)
                .setPositiveButton("CONNECT", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //Log.i("BTN","CONNECT");

                        connectToDevice(mDevice);

//                        if (indexVal == 0) {
//                            connectToDeviceII(mDevice);
//                        } else {
//                            connectToDevice(mDevice);
//                        }


                        switch (indexVal) {
                            case 0: {
                                Button btn100 = findViewById(R.id.button100);
                                btn100.setTextColor(Color.RED);
                                dialog.cancel();
                                break;
                            }
                            case 1: {
                                Button btn101 = findViewById(R.id.button101);
                                btn101.setTextColor(Color.RED);
                                dialog.cancel();
                                break;
                            }
                            case 2: {
                                Button btn102 = findViewById(R.id.button102);
                                btn102.setTextColor(Color.RED);
                                dialog.cancel();
                                break;
                            }
                            case 3: {
                                Button btn103 = findViewById(R.id.button103);
                                btn103.setTextColor(Color.RED);
                                dialog.cancel();
                                break;
                            }
                            case 4: {
                                Button btn104 = findViewById(R.id.button104);
                                btn104.setTextColor(Color.RED);
                                dialog.cancel();
                                break;
                            }


                        }


                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //Log.i("BTN","CANCEL");
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }


    public void onClick_100(View view) {
        Button b1 = findViewById(R.id.button1);
        mConnectToDevice(0);
    }

    public void onClick_101(View view) {
        mConnectToDevice(1);
    }

    public void onClick_102(View view) {
        mConnectToDevice(2);
    }

    public void onClick_103(View view) {
        mConnectToDevice(3);
    }

    public void onClick_104(View view) {
        mConnectToDevice(4);
    }

    public void onClick_3(View view) {

            devicesDiscovered = new ArrayList<>();
            deviceIndexVal = 0;

            Button btn100 = findViewById(R.id.button100);
            Button btn101 = findViewById(R.id.button101);
            Button btn102 = findViewById(R.id.button102);
            Button btn103 = findViewById(R.id.button103);
            Button btn104 = findViewById(R.id.button104);

            btn100.setVisibility(View.GONE);
            btn101.setVisibility(View.GONE);
            btn102.setVisibility(View.GONE);
            btn103.setVisibility(View.GONE);
            btn104.setVisibility(View.GONE);

//mBluetoothAdapter.getBondedDevices().toString();

            mBluetoothAdapter.disable();
            //BA.disable();
            Toast.makeText(getApplicationContext(), "BT OFF" ,Toast.LENGTH_LONG).show();

        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.enable();
                Toast.makeText(getApplicationContext(), "BT ON", Toast.LENGTH_LONG).show();
            }
        }, SCAN_PERIOD);




//            mLEScanner.startScan(filters, settings, mScanCallback);
//            isScanning = true;
//            //Log.i("SCAN", "isScanning: ");

    }


    public void onClick_setSecondsPerRound(View view) {
        if (timerSecondsCounter > 245) {return;}
        Button b = findViewById(R.id.button54b);
        switch (secondsPerRound) {
            case 1800:
                secondsPerRound = 300;
                b.setText("300 Seconds");
                break;
            case 300:
                secondsPerRound = 1800;
                b.setText("30 Minutes");
                break;
        }
        //Log.i(TAG, "onClick_setSecondsPerRound: " + secondsPerRound);
    }

    public Boolean audioValue = true;
    public void onClick_setAudio(View view) {
        Button b = findViewById(R.id.button53b);
        if (audioValue) {
            audioValue = false;
            b.setText("OFF");
        } else {
            audioValue = true;
            b.setText("ON");
        }
    }

    public double wheelSizeMM = 2105.0;
    public void onClick_setTireSize(View view) {
        Button b = findViewById(R.id.button52b);
        String bVal = b.getText().toString();
        switch (bVal) {
            case "700X25": {
                wheelSizeMM = 2136.0;
                b.setText("700X28");
                break;
            }
            case "700X28": {
                wheelSizeMM = 2155.0;
                b.setText("700X32");
                break;
            }
            case "700X32": {
                wheelSizeMM = 2105.0;
                b.setText("700X25");
                break;
            }
        }

    }

    public String activityValue = "BIKE";
    public void onClick_setActivity(View view) {
        if (timerSecondsCounter > 245) {return;}
        Button b = findViewById(R.id.button51b);
        Button b2 = findViewById(R.id.button54b);
        switch (activityValue) {
            case "BIKE":
                b.setText("RUN");
                activityValue = "RUN";
                secondsPerRound = 300;
                b2.setText("300 Seconds");
                break;
//            case "RUN":
//                b.setText("ROW");
//                activityValue = "ROW";
//                //ADD SUPPORT FOR ROW LATER
//                secondsPerRound = 300;
//                b2.setText("300 Seconds");
//                break;
            case "RUN":
                activityValue = "BIKE";
                b.setText("BIKE");
                secondsPerRound = 1800;
                b2.setText("30 Minutes");
                break;
        }
    }


    private void setRandomUsernameOnStart() {
        userName = "TIM";
        Random r = new Random();
        int i1 = r.nextInt(9999 - 1001);
        userName = userName + i1;
        Button b = findViewById(R.id.button50b);
        b.setText(userName);
        tim.setName(userName);
        //Log.i(TAG, "setRandomUsernameOnStart: " + userName);
        try {
            getSupportActionBar().setTitle("VIRTUAL CRIT (" + userName + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String userName = "TIM";
    public void onClick_setName(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog;
        builder.setTitle("NAME");

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);


// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                userName = input.getText().toString().toUpperCase();
                Button b = findViewById(R.id.button50b);
                b.setText(userName);
                tim.setName(userName);

                try {
                    getSupportActionBar().setTitle("VIRTUAL CRIT (" + userName + ")");
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });
        builder.setNegativeButton("RANDOM", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                userName = "TIM";
                Random r = new Random();
                int i1 = r.nextInt(9999 - 1001);
                userName = userName + i1;
                Button b = findViewById(R.id.button50b);
                b.setText(userName);
                tim.setName(userName);

                try {
                    getSupportActionBar().setTitle("VIRTUAL CRIT (" + userName + ")");
                } catch (Exception e) {
                    e.printStackTrace();
                }


                dialog.cancel();
            }
        });

        builder.show();
    }

    private ListView listView;
//    private String[] values2;

    private Integer lvToggle = 0;
    public void tlButton1_Click(View view) {

        listView = findViewById(R.id.lv1);

        switch (lvToggle) {
            case 0: {
                //readFromFB();
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_1, android.R.id.text1, valuesRounds);
                listView.setAdapter(adapter);
                lvToggle = 1;
                break;
            }
            case 1: {
                //readFromFBII();
//                Collections.reverse(valuesRoundLeaders);
                ArrayList<String> temp = valuesRoundLeaders;
                if (temp.size() < 1) {return;}
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_1, android.R.id.text1, temp);
                listView.setAdapter(adapter);
                lvToggle = 2;
                break;
            }
            case 2: {
                ArrayList<String> temp2 = valuesRoundLeadersScores;
                if (temp2.size() < 1) {return;}
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_1, android.R.id.text1, temp2);
                listView.setAdapter(adapter);
                lvToggle = 3;
                break;
            }
            case 3: {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_1, android.R.id.text1, valuesRoundsScores);
                listView.setAdapter(adapter);
                lvToggle = 0;
                break;
            }
        }

//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
//                android.R.layout.simple_list_item_1, android.R.id.text1, valuesRounds);

        // Assign adapter to ListView
//        listView.setAdapter(adapter);

    }

    private int tlButton2_toggle = 0;
    public void tlButton2_Click(View view) {

        listView = findViewById(R.id.lv1);

        if (tlButton2_toggle == 0) {
            //Log.i(TAG, "tlButton2_Click: " + tlButton2_toggle);
            tlButton2_toggle = 1;
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, valuesMiles);

            listView.setAdapter(adapter);
        } else {
            //Log.i(TAG, "tlButton2_Click: " + tlButton2_toggle);
            tlButton2_toggle = 0;
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, valuesMilesGeo);

            listView.setAdapter(adapter);
        }
//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
//                android.R.layout.simple_list_item_1, android.R.id.text1, valuesMiles);
//
//        listView.setAdapter(adapter);


    }

    private int tlButton3_toggle = 0;
    public void tlButton3_Click(View view) {

        listView = findViewById(R.id.lv1);

        if (tlButton3_toggle == 0) {
            //Log.i(TAG, "tlButton3_Click: " + tlButton3_toggle);
            ArrayList<String> temp = valuesTotalsLeaders;
            if (temp.size() < 1) {return;}
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, temp);

            listView.setAdapter(adapter);
            tlButton3_toggle = 1;
        } else {

            //Log.i(TAG, "tlButton3_Click: " + tlButton3_toggle);
            ArrayList<String> temp = valuesTotalsLeadersScores;
            if (temp.size() < 1) {return;}
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, temp);

            listView.setAdapter(adapter);
            tlButton3_toggle = 0;

        }

//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
//                android.R.layout.simple_list_item_1, android.R.id.text1, valuesTotalsLeaders);
//
//        listView.setAdapter(adapter);

    }

    private ArrayList<TimelineRow> timelineRowsList = new ArrayList<>();

    private void createTimeline(String tlTitle, String tlDescription) {

        // Create new timeline row (Row Id)
        TimelineRow myRow = new TimelineRow(0);

// To set the row Date (optional)
        myRow.setDate(new Date());
// To set the row Title (optional)
        myRow.setTitle(tlTitle);
// To set the row Description (optional)
        myRow.setDescription(tlDescription);
// To set the row bitmap image (optional)
        myRow.setImage(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
// To set row Below Line Color (optional)
        myRow.setBellowLineColor(Color.argb(255, 0, 0, 0));
// To set row Below Line Size in dp (optional)
        myRow.setBellowLineSize(2);
// To set row Image Size in dp (optional)
        myRow.setImageSize(2);
// To set background color of the row image (optional)
        myRow.setBackgroundColor(Color.argb(255, 0, 0, 0));
// To set the Background Size of the row image in dp (optional)
        myRow.setBackgroundSize(10);
// To set row Date text color (optional)
        myRow.setDateColor(Color.argb(255, 0, 0, 0));
// To set row Title text color (optional)
        myRow.setTitleColor(Color.argb(255, 0, 0, 0));
// To set row Description text color (optional)
        myRow.setDescriptionColor(Color.argb(255, 0, 0, 0));

// Add the new row to the list
        timelineRowsList.add(myRow);

// Create the Timeline Adapter
        final ArrayAdapter<TimelineRow> myAdapter = new TimelineViewAdapter(this, 0, timelineRowsList,
                //if true, list will be sorted by date
                true);

// Get the ListView and Bind it with the Timeline Adapter

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ListView myListView = (ListView) findViewById(R.id.timeline_listView);
                myListView.setAdapter(myAdapter);
            }
        });




    }

    private int maxHR = 185;
    private double maxHRdouble = 185.0;
    public void onClick_setMaxHR(View view) {
        //Log.i(TAG, "onClick_setMaxHR: " + maxHR);
        Button b = findViewById(R.id.button153b);
        switch (maxHR) {
            case 185: {
                maxHR = 190;
                b.setText(String.valueOf(maxHR));
                maxHRdouble = (double) maxHR;
                tim.maxHR = maxHR;
                tim.maxHRdouble = maxHRdouble;
                break;
            }
            case 190: {
                maxHR = 195;
                b.setText(String.valueOf(maxHR));
                maxHRdouble = (double) maxHR;
                tim.maxHR = maxHR;
                tim.maxHRdouble = maxHRdouble;
                break;
            }
            case 195: {
                maxHR = 200;
                b.setText(String.valueOf(maxHR));
                maxHRdouble = (double) maxHR;
                tim.maxHR = maxHR;
                tim.maxHRdouble = maxHRdouble;
                break;
            }
            case 200: {
                maxHR = 185;
                b.setText(String.valueOf(maxHR));
                maxHRdouble = (double) maxHR;
                tim.maxHR = maxHR;
                tim.maxHRdouble = maxHRdouble;
                break;
            }
        }

        //Log.i(TAG, "onClick_setMaxHR: " + maxHR);
    }



    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        TextView ortTview = findViewById(R.id.tvBottom);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
            ortTview.setVisibility(View.GONE);

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            //Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
            ortTview.setVisibility(View.VISIBLE);
        }
    }

    public void onClick_sendToLegal(View view) {

        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.virtualcrit.com/tandc.html")));

    }
}  //END MAIN ACTIVITY
