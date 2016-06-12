

package mx.klozz.ndroids;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Handler;
import android.os.Message;

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

}//finish main