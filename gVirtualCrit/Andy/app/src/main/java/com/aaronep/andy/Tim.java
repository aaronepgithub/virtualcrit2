package com.aaronep.andy;

import android.util.Log;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;



public class Tim {

    private static final String TAG = "TIM";
    public Calendar startTime;
    public String currentDate;
    public String name;
    private long totalTimeInSeconds;
    private String actualTotalTimeString;

    public double maxHRdouble = 185.0;
    public int maxHR = 185;

    DecimalFormat df = new DecimalFormat("#.###");

    //FB ROUND OBJ
    private double roundSpeed = 0;

    public double getRoundSpeed() {
        //return roundSpeed;
        return Double.valueOf(df.format(roundSpeed));
    }

    public void setRoundSpeed(double roundSpeed) {
        this.roundSpeed = roundSpeed;
    }

    public double getRoundHR() {
        return Double.valueOf(df.format(roundHR));
    }

    public void setRoundHR(double roundHR) {
        setRoundScore((roundHR / maxHRdouble) * 100);
        this.roundHR = roundHR;
    }

    public double totalAvgSpeed = 0;
    public double getTotalAvgSpeed() {

        if (btAvgSpeed == 0 && geoAvgSpeed == 0) {
            return 0;
        }

        if (btAvgSpeed > geoAvgSpeed) {
            //return btAvgSpeed;
            return Double.valueOf(df.format(btAvgSpeed));
        } else {
            //return geoAvgSpeed;
            return Double.valueOf(df.format(geoAvgSpeed));
        }
    }

    public double getRoundScore() {
        //return roundScore;
        return Double.valueOf(df.format(roundScore));
    }

    public void setRoundScore(double roundScore) {
        this.roundScore = roundScore;
    }

    public double getRoundCadence() {
        //return roundCadence;
        return Double.valueOf(df.format(roundCadence));
    }

    public void setRoundCadence(double roundCadence) {
        this.roundCadence = roundCadence;
    }

    private double roundHR = 0;
    private double roundScore = 0;
    private double roundCadence = 0;


    //ACTUAL TIME
    public long getTotalTimeInSeconds() {
        return totalTimeInSeconds;
    }

    public String getTotalTimeString() {
        return actualTotalTimeString;
    }

    public void setTotalTimeInSeconds(long totalTimeInSeconds) {
        this.totalTimeInSeconds = totalTimeInSeconds;
        if (totalTimeInSeconds == 30) {
            Log.i(TAG, "setTotalTimeInSeconds: 30");
        }


        Calendar nowTime = Calendar.getInstance(Locale.ENGLISH);
        Long st = startTime.getTimeInMillis();
        Long nt = nowTime.getTimeInMillis();
        long millis_act = nt - st;
        String hms_act = String.format(Locale.US,"%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis_act),
                TimeUnit.MILLISECONDS.toMinutes(millis_act) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis_act)),
                TimeUnit.MILLISECONDS.toSeconds(millis_act) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis_act)));
        Log.i(TAG, hms_act);
        actualTotalTimeString = hms_act;

    }


    //BT
    double btTotalWheelRevolutions = 0;
    double btTotalTimeInSeconds = 0;
    double btTotalDistance = 0;
    double btAvgSpeed = 0;
    String btAvgPace;
    double btSpeed = 0;
    String btPace;

    //GEO
    double geoTotalDistance = 0;
    double geoAvgSpeed = 0;
    long geoMovingTime;




    public Tim(String name) {
        this.name = name;
    }
    public String getName() {
        //Log.i(TAG, "getName: name: " + name);
        return name;
    }
    public void setName(String name) {
        this.name = name;
        Log.i(TAG, "setName: name: " + name);
    }

    public void addWheelDiff(int w) {
        btTotalWheelRevolutions += (double) w;
        //Log.i(TAG, "addWheelDiff: " + w);
    }

    public void addTimeDiff(int t) {
        btTotalTimeInSeconds += (double) t;
    }




    //BT
    public double getBtTotalDistance() {
        return btTotalDistance;
    }

    public void setBtTotalDistance(double btTotalDistance) {
        this.btTotalDistance = btTotalDistance;
    }

    public void addToBtDistance(double btSegmentInMiles) {
        btTotalDistance += btSegmentInMiles;
    }

    public double getBtAvgSpeed() {
        //return btAvgSpeed;
        return Double.valueOf(df.format(btAvgSpeed));

    }

    public void setBtAvgSpeed(double btAvgSpeed) {
        this.btAvgSpeed = btAvgSpeed;
    }


    public String getBtAvgPace() {
        return btAvgPace;
    }

    public void setBtAvgPace(String btAvgPace) {
        this.btAvgPace = btAvgPace;
    }

    public double getBtSpeed() {
        //return btSpeed;
        return Double.valueOf(df.format(btSpeed));
    }

    public void setBtSpeed(double btSpeed) {
        this.btSpeed = btSpeed;
    }

    public String getBtPace() {
        return btPace;
    }

    public void setBtPace(String btPace) {
        this.btPace = btPace;
    }





    //GEO
    public double getGeoTotalDistance() {
        return geoTotalDistance;
    }

    public void setGeoTotalDistance(double geoTotalDistance) {
        this.geoTotalDistance = geoTotalDistance;
    }

    public double getGeoAvgSpeed() {
        //return geoAvgSpeed;
        return Double.valueOf(df.format(geoAvgSpeed));
    }

    public void setGeoAvgSpeed(double geoAvgSpeed) {
        this.geoAvgSpeed = geoAvgSpeed;
    }

    public long getGeoMovingTime() {
        return geoMovingTime;
    }

    public void setGeoMovingTime(long geoMovingTime) {
        this.geoMovingTime = geoMovingTime;
    }
}
