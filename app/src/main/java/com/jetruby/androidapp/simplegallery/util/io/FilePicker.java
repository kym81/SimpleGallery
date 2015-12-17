package com.jetruby.androidapp.simplegallery.util.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.jetruby.androidapp.simplegallery.Item;
import com.jetruby.androidapp.simplegallery.activity.MainActivity;
import com.jetruby.androidapp.simplegallery.R;

public class FilePicker {

    Context mContext;
    ArrayList<String> str = new ArrayList<String>();
    private Boolean firstLvl = true;
    private static final String[] FTYPE = {"bmp", "jpg", "jpeg"};
    private Item[] fileList;
    private File path = new File(Environment.getExternalStorageDirectory().toString());
    private String chosenFile;
    ListAdapter fileAdapter;

    public FilePicker(Context mContext) {
        this.mContext = mContext;
        loadFileList();
    }

    public static Item[] getFileList (File path, Boolean firstLvl) {
        Item[] fileList = null;

        if (path.exists()) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    return ((sel.isFile() && checkFileType(filename)) || sel.isDirectory()) && !sel.isHidden();

                }
            };

            String[] fList = path.list(filter);
            fileList = new Item[fList.length];
            for (int i = 0; i < fList.length; i++) {

                File sel = new File(path, fList[i]);

                if (sel.isDirectory()) {
                    fileList[i] = new Item(fList[i], R.drawable.directory_icon, false);
                } else {
                    fileList[i] = new Item(fList[i], R.drawable.file_icon1, true);
                }

            }

            if (!firstLvl) {
                Item temp[] = new Item[fileList.length + 1];
                for (int i = 0; i < fileList.length; i++) {
                    temp[i + 1] = fileList[i];
                }
                temp[0] = new Item("Up", R.drawable.directory_up, false);
                fileList = temp;
            }
        }

        return fileList;
    }

    private void loadFileList() {
        try {
            path.mkdirs();
        } catch (SecurityException e) {
        }

        fileList = getFileList(path, firstLvl);


        fileAdapter = new ArrayAdapter<Item>(mContext, R.layout.file_list_tv, R.id.myTV, fileList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(R.id.myTV);
                textView.setCompoundDrawablesWithIntrinsicBounds(fileList[position].icon, 0, 0, 0);
                int dp5 = (int) (5 * mContext.getResources().getDisplayMetrics().density + 0.5f);
                textView.setCompoundDrawablePadding(dp5);

                return view;
            }


        };
    }

    public static boolean checkFileType(String file) {
        boolean result = false;
        String fExt = file.substring(file.length() - 4, file.length());
        for (String ext : FTYPE) {
            if (fExt.contains(ext)) {
                result = true;
                break;
            }
        }
        return result;
    }

    public void showFilePickerDialog() {
        Builder builder = new Builder(mContext);

        if (fileList == null) {
            return;
        }

        builder.setTitle("Select catalog");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((MainActivity) mContext).setPath(dialog, path, fileList);
            }
        });

        builder.setAdapter(fileAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                chosenFile = fileList[which].file;
                File sel = new File(path + "/" + chosenFile);
                if (sel.isDirectory()) {
                    firstLvl = false;

                    str.add(chosenFile);
                    fileList = null;
                    path = new File(sel + "");

                    loadFileList();

                    dialog.dismiss();
                    showFilePickerDialog();

                } else if (chosenFile.equalsIgnoreCase("up") && !sel.exists()) {
                    String s = str.remove(str.size() - 1);

                    path = new File(path.toString().substring(0, path.toString().lastIndexOf(s)));
                    fileList = null;

                    if (str.isEmpty()) {
                        firstLvl = true;
                    }

                    loadFileList();

                    dialog.dismiss();
                    showFilePickerDialog();

                } else {
                    ((MainActivity) mContext).setPath(dialog, path, fileList);
                }
            }
        });
        builder.show();
    }
}
