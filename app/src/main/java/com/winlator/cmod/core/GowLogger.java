package com.winlator.cmod.core;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GowLogger {
    private static final String TAG = "GowLauncher";
    private static final String LOG_DIR = Environment.getExternalStorageDirectory() + "/Download/Winlator";
    private static final String LOG_FILE = "gowlauncher.log";
    private static File logFile;
    private static PrintWriter logWriter;

    static {
        try {
            File logDir = new File(LOG_DIR);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            logFile = new File(logDir, LOG_FILE);
            logWriter = new PrintWriter(new FileWriter(logFile, true), true);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao criar arquivo de log", e);
        }
    }

    private static String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        return sdf.format(new Date());
    }

    public static void log(String level, String tag, String message) {
        String logMessage = String.format("[%s] [%s] [%s] %s", getTimestamp(), level, tag, message);
        
        if (logWriter != null) {
            logWriter.println(logMessage);
            logWriter.flush();
        }
        
        switch (level.toUpperCase()) {
            case "E":
            case "ERROR":
                Log.e(tag, message);
                break;
            case "W":
            case "WARN":
                Log.w(tag, message);
                break;
            case "I":
            case "INFO":
                Log.i(tag, message);
                break;
            case "D":
            case "DEBUG":
                Log.d(tag, message);
                break;
            default:
                Log.v(tag, message);
        }
    }

    public static void i(String tag, String message) {
        log("INFO", tag, message);
    }

    public static void e(String tag, String message) {
        log("ERROR", tag, message);
    }

    public static void e(String tag, String message, Throwable throwable) {
        log("ERROR", tag, message + "\n" + Log.getStackTraceString(throwable));
    }

    public static void w(String tag, String message) {
        log("WARN", tag, message);
    }

    public static void d(String tag, String message) {
        log("DEBUG", tag, message);
    }

    public static String getLogFilePath() {
        return logFile != null ? logFile.getAbsolutePath() : "Log file not available";
    }

    public static void close() {
        if (logWriter != null) {
            logWriter.close();
        }
    }
}
