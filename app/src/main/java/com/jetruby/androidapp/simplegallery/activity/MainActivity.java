package com.jetruby.androidapp.simplegallery.activity;

import android.app.Dialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jetruby.androidapp.simplegallery.Item;
import com.jetruby.androidapp.simplegallery.R;
import com.jetruby.androidapp.simplegallery.util.Network.NetworkUtil;
import com.jetruby.androidapp.simplegallery.util.io.FilePicker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static ArrayList<Drawable> drawableArrayList;
    private static Drawable currentDrawable;
    private static ImageView fragmentIV;
    private static Context mContext;
    private MyThread myThread;
    private int pauseDuration;
    private int tempDuration;
    private String savedPath;
    private String tempPath;
    private final String PREFERENCE_PATH = "PREFERENCE_PATH";
    private final String PREFERENCE_DURATION = "PREFERENCE_DURATION";
    private final String PREFERENCE_INTERNET = "PREFERENCE_INTERNET";
    private SharedPreferences sPref;
    private Item[] fileList;
    private boolean mustCancel;
    private TextView currentPathTV;
    private boolean isInternetLoad;
    private boolean tempInternetLoad;
    private ProgressDialog pDialog;
    private static final int STATUS_GET_NEXT_IMAGE = 50;
    private static final int STATUS_IMAGE_DOWNLOADED = 100;
    private int internetLoadIndex = 0;
    private final String[] internetURLS = {
            "http://developingnewz.com/wp-content/uploads/2015/04/150420153633-progress-eagle-5-large-169.jpg",
            "http://www.sligotoday.ie/images/1319530080.jpg",
            "http://img.rl0.ru/pgc/432x288/553c9dd4-0c48-2d25-0c48-2d2acd809822.photo.0.jpg",
            "http://www.boeingimages.com/Docs/BOE/Media/TR6_WATERMARKED/3/c/1/c/BI231951.jpg",
            "http://maestro-clip.ru/uploads/thumbs/4/0/6/406874c078172e6d56accdd809e1b5a0.jpg"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mContext = this;

        checkSimpleGalleryCatalogExist();

        currentDrawable = null;
        drawableArrayList = new ArrayList<>();
        myThread = new MyThread();

        sPref = getSharedPreferences(mContext.getPackageName(), MODE_PRIVATE);
        savedPath = sPref.getString(PREFERENCE_PATH, Environment.getExternalStorageDirectory().toString());
        pauseDuration = sPref.getInt(PREFERENCE_DURATION, 5);
        isInternetLoad = sPref.getBoolean(PREFERENCE_INTERNET, false);

        if (isInternetLoad && NetworkUtil.getConnectivityStatus(mContext) != NetworkUtil.TYPE_NOT_CONNECTED) {
            mHandler.sendEmptyMessage(STATUS_GET_NEXT_IMAGE);
        } else {
            File path = new File(savedPath);
            if (path.exists()) {
                File rootPath = new File(Environment.getExternalStorageDirectory() + "");
                boolean firstLvl = false;

                if (rootPath == path) {
                    firstLvl = true;
                }

                Item[] fileList = FilePicker.getFileList(path, firstLvl);

                setPath(null, path, fileList);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            showSettingsDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        myThread.stopThread();
    }

    private void nextImage(int index) {
        if (index < 0) {
            currentDrawable = null;
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new SlideFragment())
                    .commit();

        } else {
            currentDrawable = drawableArrayList.get(index);
            getFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.animator.slide_in, R.animator.slide_out)
                    .replace(R.id.container, new SlideFragment())
                    .commit();
        }


    }

    public static class SlideFragment extends Fragment {

        public SlideFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            RelativeLayout fragmentRL = (RelativeLayout) rootView.findViewById(R.id.fragmentRL);
            fragmentIV = (ImageView) fragmentRL.findViewById(R.id.fragmentIV);
            TextView noImageTV = (TextView) fragmentRL.findViewById(R.id.noImageTV);

            if (currentDrawable == null) {
                fragmentIV.setVisibility(View.GONE);
                noImageTV.setVisibility(View.VISIBLE);
            } else {
                fragmentIV.setVisibility(View.VISIBLE);
                fragmentIV.setImageDrawable(currentDrawable);
                noImageTV.setVisibility(View.GONE);
            }

            return rootView;
        }
    }

    public void setPath(DialogInterface dialog, File path, Item[] fileList) {
        tempPath = path.getAbsolutePath();
        this.fileList = fileList;

        if (dialog != null) {
            dialog.dismiss();
            currentPathTV.setText(tempPath);
        } else {
            startSlideShow();
        }
    }

    class MyThread extends Thread {
        private volatile Thread runner;

        public synchronized void startThread() {
            if (runner == null) {
                runner = new Thread(this);
                runner.start();
            }
        }

        public synchronized void stopThread() {
            if (runner != null) {
                Thread moribund = runner;
                runner = null;
                moribund.interrupt();
            }
        }

        public void run() {
            try {
                if (!isInternetLoad) {

                    if (drawableArrayList.size() == 0) {
                        nextImage(-1);
                    } else {
                        for (int i = 0; i < drawableArrayList.size(); i++) {
                            nextImage(i);
                            Thread.sleep(pauseDuration * 1000);
                        }
                    }
                } else {
                    nextImage(internetLoadIndex);
                    Thread.sleep(pauseDuration * 1000);

                    if (internetLoadIndex < internetURLS.length - 1) {
                        internetLoadIndex++;
                        mHandler.sendEmptyMessage(STATUS_GET_NEXT_IMAGE);
                    } else {
                        internetLoadIndex = 0;
                    }
                }
            } catch (InterruptedException e) {
                //Log.e("Error: ", e.getMessage());
            }
        }
    }

    private void showSettingsDialog() {
        final Dialog myDialog = new Dialog(mContext);

        myDialog.setTitle(getResources().getString(R.string.dialog_settings));
        myDialog.setContentView(R.layout.dialog_settings);
        currentPathTV = (TextView) myDialog.findViewById(R.id.currentPathTV);

        currentPathTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FilePicker fp = new FilePicker(mContext);
                fp.showFilePickerDialog();
            }
        });

        tempDuration = pauseDuration;
        NumberPicker durationNP = (NumberPicker) myDialog.findViewById(R.id.durationNP);
        durationNP.setMinValue(1);
        durationNP.setMaxValue(60);
        durationNP.setValue(tempDuration);

        durationNP.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                tempDuration = newVal;
            }
        });

        mustCancel = false;
        Button cancelBtn = (Button) myDialog.findViewById(R.id.cancelBtn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mustCancel = true;
                myDialog.dismiss();
            }
        });

        final ToggleButton toggleButton = (ToggleButton) myDialog.findViewById(R.id.toggleButton);
        toggleButton.setChecked(isInternetLoad);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (NetworkUtil.getConnectivityStatus(mContext) == NetworkUtil.TYPE_NOT_CONNECTED) {
                    Toast.makeText(mContext, "No connection to internet!", Toast.LENGTH_SHORT).show();
                    toggleButton.setChecked(!isChecked);
                } else if (isChecked) {
                    currentPathTV.setText("From internet");
                    currentPathTV.setClickable(false);
                    tempInternetLoad = true;
                } else {
                    currentPathTV.setText("...");
                    currentPathTV.setClickable(true);
                    tempInternetLoad = false;
                }
            }
        });

        String text = "...";
        if (!savedPath.isEmpty() && !isInternetLoad) {
            text = savedPath;
        } else if (isInternetLoad) {
            text = "From internet";
        }
        currentPathTV.setText(text);

        myDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {

                if (!mustCancel) {
                    pauseDuration = tempDuration;
                    savedPath = tempPath;
                    isInternetLoad = tempInternetLoad;

                    SharedPreferences.Editor editor = sPref.edit();
                    editor.putString(PREFERENCE_PATH, savedPath);
                    editor.putInt(PREFERENCE_DURATION, pauseDuration);
                    editor.putBoolean(PREFERENCE_INTERNET, isInternetLoad);
                    editor.apply();

                    if (isInternetLoad) {
                        drawableArrayList.clear();
                        internetLoadIndex = 0;
                        mHandler.sendEmptyMessage(STATUS_GET_NEXT_IMAGE);
                    } else {
                        startSlideShow();
                    }


                }
            }
        });

        myDialog.show();
    }

    void startSlideShow() {
        if (!isInternetLoad) {
            drawableArrayList.clear();

            for (Item item : fileList) {
                if (item.isFile) {
                    String filePath = savedPath + "/" + item.file;
                    Drawable drawable = Drawable.createFromPath(filePath);
                    drawableArrayList.add(drawable);
                }
            }
        }

        myThread.stopThread();
        //nextImage(-1);
        myThread.startThread();
    }

    private void showProgressDialog() {
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Downloading file. Please wait...");
        pDialog.setIndeterminate(false);
        pDialog.setMax(100);
        pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pDialog.setCancelable(true);
        pDialog.show();
    }

    private class DownloadFileFromURL extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog();
        }

        @Override
        protected String doInBackground(String... f_url) {
            int count;
            String urlStr = f_url[0];
            String fileName = urlStr.substring(urlStr.lastIndexOf("/") + 1, urlStr.length());
            File path = new File(Environment.getExternalStorageDirectory().toString() + "/simpleGallery/" + fileName);
            try {
                URL url = new URL(urlStr);
                URLConnection connection = url.openConnection();
                connection.connect();

                int lengthOfFile = connection.getContentLength();
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                OutputStream output = new FileOutputStream(path.getAbsolutePath());

                byte data[] = new byte[1024];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress("" + (int) ((total * 100) / lengthOfFile));
                    output.write(data, 0, count);
                }

                output.flush();

                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return path.getAbsolutePath();
        }

        protected void onProgressUpdate(String... progress) {
            pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(String file_url) {
            pDialog.dismiss();
            Drawable drawable = Drawable.createFromPath(file_url);
            drawableArrayList.add(drawable);
            mHandler.sendEmptyMessage(STATUS_IMAGE_DOWNLOADED);
        }

    }

    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case STATUS_GET_NEXT_IMAGE:
                    String url = internetURLS[internetLoadIndex];
                    new DownloadFileFromURL().execute(url);
                    break;
                case STATUS_IMAGE_DOWNLOADED:
                    myThread.stopThread();
                    myThread.startThread();
                    break;
            }
        }
    };

    private void checkSimpleGalleryCatalogExist() {
        File path = new File(Environment.getExternalStorageDirectory() + "/simpleGallery");
        if (!path.exists()) {
            path.mkdir();
        }
    }
}
