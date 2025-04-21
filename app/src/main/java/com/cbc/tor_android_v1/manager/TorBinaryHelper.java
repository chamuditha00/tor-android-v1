package com.cbc.tor_android_v1.manager;

import android.content.Context;
import android.util.Log;

import org.torproject.android.binary.TorResourceInstaller;
import org.torproject.android.binary.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class TorBinaryHelper {

    private File torDir;

    private Context context;

    private static final String TAG = "TorDebug";

    public TorBinaryHelper(Context context) {
        this.context = context;
    }




}
