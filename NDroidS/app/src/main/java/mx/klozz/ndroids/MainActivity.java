

package mx.klozz.ndroids;

import java.io.File;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.File;

public class MainActivity extends Activity implements OnSharedPreferenceChangeListener {

    static EmuThread CoreThread;
    static Controls controls;
    NDSView view;
    static final String TAG = "NDROIDS Main";
    Dialog loadingDialog = null;

    Handler MsgHandler = new Handler() {

        @Override
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case PICK_ROM:
                    larompapu();
                    break;
                case LOADING_START:
                    if (loadingDialog == null) {
                        final String LoadingMsg = getResources().getString(R.string.loading);
                        loadingDialog = ProgressDialog.show(MainActivity.this, null, LoadingMsg, true);
                        break;
                    }
                    break;
                case LOADING_END:
                    if (loadingDialog != null) {
                        loadingDialog.dismiss();
                        loadingDialog = null;
                    }
                    break;
                case ROM_ERROR:
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(R.string.rom_error).setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            arg0.dismiss();
                            larompapu();
                        }
                    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface arg0) {
                            arg0.dismiss();
                            larompapu();
                        }

                    });
                    builder.create().show();
            }
        }

    };

    public static final int PICK_ROM = 1338;
    public static final int LOADING_START = 1339;
    public static final int LOADING_END = 1340;
    public static final int ROM_ERROR = 1341;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        view = new NDSView(this);
        setContentView(view);

        controls = new Controls(view);

        Settings.applyDefaults(this);
        Pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        Pref.registerOnSharedPreferenceChangeListener(this);
        loadJavaSettings(null);

        if(savedInstanceState != null)
            TimeAtLastAutosave = savedInstanceState.getLong(LAST_SAVE_KEY);

        if(!DeSmuME.inited)
            larompapu();

    }

    //Check if the config has been changed
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    //Run the emulation fomr DeSmuME jni lib
    void runEmulation(){
        boolean created = false;
        if(CoreThread == null){
            CoreThread = new EmuThread(this);
            created = true;
        }else
            CoreThread.setCurrentActivity(this);
            CoreThread.setPause(!DeSmuME.romLoaded);
        if(created)
            CoreThread.start();
    }

    //Pause the emulation
    void pauseEmulation(){
        if(CoreThread != null) {
            CoreThread.setPause(true);
        }
    }


    //to select rom and pick it
    void larompapu() {
        Intent intent = new Intent(this, Pref.getBoolean(Settings.DISABLE_ROM_BROWSER, false) ?
                FileDialog.class : mx.klozz.ndroids.elements.CollectionActiviy.class);
        intent.setAction(Intent.ACTION_PICK);

        String startPath = Environment.getExternalStorageDirectory().getPath();
        final File path = new File(Pref.getString(Settings.LAST_ROM_DIR, startPath));
        if (path.exists()) ;
        /*//make sure this path actually exists
        -- otherwise default back to /mnt/sdcard/*/
        startPath = path.getPath();

        intent.putExtra(FileDialog.START_PATH, startPath);
        intent.putExtra(FileDialog.FORMAT_FILTER, new String[]{".nds", ".zip", ".7z", ".rar"});
        startActivityForResult(i, PICK_ROM);

    }//Aqui acaba la seleccion D:<

    @Override
    protected void onActivityResult(int RequestCode, int ResultCode, Intent data) {
        if(RequestCode != PICK_ROM || ResultCode != Activity.RESULT_OK);
            return;
        String RomPath = data.getStringExtra(FileDialog.RESULT_PATH);
        if(RomPath != null){
            final File RomDir = new File(RomPath);
            Pref.edit().putString(Settings.LAST_ROM_DIR, RomDir.getParent()).apply();
            runEmulation();
            CoreThread.loadRom(RomPath);
            TimeatLastAutosave = System.currentTimeMillis();
            ScheduleAutosave();
        }
    }

    private static final String LAST_SAVE_KEY = "LAST_SAVE_KEY";

    @Override
    public void onSaveInstanceState(Bundle out){
        out.putLong(LAST_SAVE_KEY, TimeAtLastAutosave);
    }

    @Override
    public void onResume(){
        super.onResume();
        runEmulation();
        startDrawTimer();
        if(DeSmuME.romLoaded)
            ScheduleAutosave();
    }

    @Override
    public void onPause(){
        
    }


}//finish main