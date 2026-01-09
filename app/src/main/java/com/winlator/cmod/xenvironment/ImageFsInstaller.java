package com.winlator.cmod.xenvironment;

import android.content.Context;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.winlator.cmod.MainActivity;
import com.winlator.cmod.R;
import com.winlator.cmod.SettingsFragment;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.contents.AdrenotoolsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.DownloadProgressDialog;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.core.WineInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ImageFsInstaller {
    public static final byte LATEST_VERSION = 21;

    private static void resetContainerImgVersions(Context context) {
        ContainerManager manager = new ContainerManager(context);
        for (Container container : manager.getContainers()) {
            String imgVersion = container.getExtra("imgVersion");
            String wineVersion = container.getWineVersion();
            if (!imgVersion.isEmpty() && WineInfo.isMainWineVersion(wineVersion) && Short.parseShort(imgVersion) <= 5) {
                container.putExtra("wineprefixNeedsUpdate", "t");
            }

            container.putExtra("imgVersion", null);
            container.saveData();
        }
    }

    public static void installWineFromAssets(final Context context) {
        String[] versions = context.getResources().getStringArray(R.array.wine_entries);
        File rootDir = ImageFs.find(context).getRootDir();
        for (String version : versions) {
            File outFile = new File(rootDir, "/opt/" + version);
            outFile.mkdirs();
            TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, context, version + ".txz", outFile);
        }
    }

    private static void installDriversFromAssets(final Context context) {
        AdrenotoolsManager adrenotoolsManager = new AdrenotoolsManager(context);
        String[] adrenotoolsAssetDrivers = context.getResources().getStringArray(R.array.wrapper_graphics_driver_version_entries);

        for (String driver : adrenotoolsAssetDrivers)
            adrenotoolsManager.extractDriverFromResources(driver);
    }

    public static void installFromAssets(final MainActivity activity) {
        AppUtils.keepScreenOn(activity);
        ImageFs imageFs = ImageFs.find(activity);
        File rootDir = imageFs.getRootDir();

        SettingsFragment.resetEmulatorsVersion(activity);

        final DownloadProgressDialog dialog = new DownloadProgressDialog(activity);
        dialog.show(R.string.installing_system_files);
        Executors.newSingleThreadExecutor().execute(() -> {
            clearRootDir(rootDir);
            final byte compressionRatio = 22;
            final long contentLength = (long)(FileUtils.getSize(activity, "imagefs.txz") * (100.0f / compressionRatio));
            AtomicLong totalSizeRef = new AtomicLong();

            boolean success = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, activity, "imagefs.txz", rootDir, (file, size) -> {
                if (size > 0) {
                    long totalSize = totalSizeRef.addAndGet(size);
                    final int progress = (int)(((float)totalSize / contentLength) * 100);
                    activity.runOnUiThread(() -> dialog.setProgress(progress));
                }
                return file;
            });

            if (success) {
                installWineFromAssets(activity);
                installDriversFromAssets(activity);
                imageFs.createImgVersionFile(LATEST_VERSION);
                resetContainerImgVersions(activity);
            }
            else AppUtils.showToast(activity, R.string.unable_to_install_system_files);

            dialog.closeOnUiThread();
        });
    }

    public static void installIfNeeded(final MainActivity activity) {
        ImageFs imageFs = ImageFs.find(activity);
        if (!imageFs.isValid() || imageFs.getVersion() < LATEST_VERSION) installFromAssets(activity);
    }

    public static void installIfNeeded(final AppCompatActivity activity, final Runnable onComplete) {
        ImageFs imageFs = ImageFs.find(activity);
        if (!imageFs.isValid() || imageFs.getVersion() < LATEST_VERSION) {
            installFromAssets(activity, onComplete);
        } else {
            if (onComplete != null) onComplete.run();
        }
    }

    public static void installFromAssets(final AppCompatActivity activity, final Runnable onComplete) {
        AppUtils.keepScreenOn(activity);
        ImageFs imageFs = ImageFs.find(activity);
        File rootDir = imageFs.getRootDir();

        SettingsFragment.resetEmulatorsVersion(activity);

        final DownloadProgressDialog dialog = new DownloadProgressDialog(activity);
        dialog.show(R.string.installing_system_files);
        Executors.newSingleThreadExecutor().execute(() -> {
            clearRootDir(rootDir);
            final byte compressionRatio = 22;
            final long contentLength = (long)(FileUtils.getSize(activity, "imagefs.txz") * (100.0f / compressionRatio));
            AtomicLong totalSizeRef = new AtomicLong();

            boolean success = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, activity, "imagefs.txz", rootDir, (file, size) -> {
                if (size > 0) {
                    long totalSize = totalSizeRef.addAndGet(size);
                    final int progress = (int)(((float)totalSize / contentLength) * 100);
                    activity.runOnUiThread(() -> dialog.setProgress(progress));
                }
                return file;
            });

            if (success) {
                installWineFromAssets(activity);
                installDriversFromAssets(activity);
                imageFs.createImgVersionFile(LATEST_VERSION);
                resetContainerImgVersions(activity);
            }
            else AppUtils.showToast(activity, R.string.unable_to_install_system_files);

            dialog.closeOnUiThread();
            if (onComplete != null) activity.runOnUiThread(onComplete);
        });
    }

    private static void clearOptDir(File optDir) {
        File[] files = optDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals("installed-wine")) continue;
                FileUtils.delete(file);
            }
        }
    }

    private static void clearRootDir(File rootDir) {
        if (rootDir.isDirectory()) {
            File[] files = rootDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        String name = file.getName();
                        if (name.equals("home")) {
                            continue;
                        }
                    }
                    FileUtils.delete(file);
                }
            }
        }
        else rootDir.mkdirs();
    }

    public static void installFromRootFs(final AppCompatActivity activity, final File orfsFile, 
                                        final Runnable onComplete, final Callback<String> onError) {
        if (orfsFile == null || !orfsFile.exists()) {
            if (onError != null) onError.call("Arquivo RootFS não encontrado");
            return;
        }

        AppUtils.keepScreenOn(activity);
        ImageFs imageFs = ImageFs.find(activity);
        File rootDir = imageFs.getRootDir();
        File tempDir = new File(activity.getCacheDir(), "rootfs_temp");

        SettingsFragment.resetEmulatorsVersion(activity);

        final DownloadProgressDialog dialog = new DownloadProgressDialog(activity);
        dialog.show(R.string.installing_system_files);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                FileUtils.delete(tempDir);
                tempDir.mkdirs();

                activity.runOnUiThread(() -> dialog.setProgress(5));
                Log.d("ImageFsInstaller", "Extracting RootFS package...");

                boolean extractSuccess = TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    orfsFile,
                    tempDir,
                    null
                );

                if (!extractSuccess) {
                    throw new Exception("Failed to extract RootFS package");
                }

                activity.runOnUiThread(() -> dialog.setProgress(15));

                File rootfsDir = new File(tempDir, "rootfs");
                if (!rootfsDir.exists()) {
                    throw new Exception("Invalid RootFS package structure");
                }

                File metadataFile = new File(rootfsDir, "metadata.json");
                if (!metadataFile.exists()) {
                    Log.w("ImageFsInstaller", "No metadata.json found, proceeding anyway");
                }

                activity.runOnUiThread(() -> dialog.setProgress(20));

                clearRootDir(rootDir);

                File imagefsArchive = new File(rootfsDir, "imagefs/imagefs.txz");
                if (!imagefsArchive.exists()) {
                    throw new Exception("ImageFS archive not found in package");
                }

                Log.d("ImageFsInstaller", "Extracting ImageFS...");
                final byte compressionRatio = 22;
                final long contentLength = imagefsArchive.length() * compressionRatio;
                AtomicLong totalSizeRef = new AtomicLong();

                boolean imageFsSuccess = TarCompressorUtils.extract(
                    TarCompressorUtils.Type.XZ,
                    imagefsArchive,
                    rootDir,
                    (file, size) -> {
                        if (size > 0) {
                            long totalSize = totalSizeRef.addAndGet(size);
                            final int progress = 20 + (int)((((float)totalSize / contentLength) * 100) * 0.5f);
                            activity.runOnUiThread(() -> dialog.setProgress(Math.min(progress, 70)));
                        }
                        return file;
                    }
                );

                if (!imageFsSuccess) {
                    throw new Exception("Failed to extract ImageFS");
                }

                activity.runOnUiThread(() -> dialog.setProgress(75));
                Log.d("ImageFsInstaller", "Installing Wine...");
                installWineFromRootFs(activity, rootfsDir, rootDir);

                activity.runOnUiThread(() -> dialog.setProgress(85));
                Log.d("ImageFsInstaller", "Installing graphics drivers...");
                installDriversFromRootFs(activity, rootfsDir);

                activity.runOnUiThread(() -> dialog.setProgress(95));
                Log.d("ImageFsInstaller", "Installing additional components...");
                installAdditionalComponentsFromRootFs(activity, rootfsDir, rootDir);

                imageFs.createImgVersionFile(LATEST_VERSION);
                resetContainerImgVersions(activity);

                activity.runOnUiThread(() -> dialog.setProgress(100));

                FileUtils.delete(tempDir);

                dialog.closeOnUiThread();
                if (onComplete != null) activity.runOnUiThread(onComplete);

                Log.d("ImageFsInstaller", "RootFS installation completed successfully");

            } catch (Exception e) {
                Log.e("ImageFsInstaller", "Error installing from RootFS", e);
                FileUtils.delete(tempDir);
                dialog.closeOnUiThread();
                if (onError != null) {
                    activity.runOnUiThread(() -> onError.call("Erro ao instalar RootFS: " + e.getMessage()));
                }
            }
        });
    }

    private static void installWineFromRootFs(Context context, File rootfsDir, File destRootDir) {
        File protonDir = new File(rootfsDir, "proton");
        File protonArchive = new File(protonDir, "proton-9.0-arm64ec.txz");
        
        Log.d("ImageFsInstaller", "Looking for Proton at: " + protonArchive.getAbsolutePath());
        
        if (!protonArchive.exists()) {
            Log.w("ImageFsInstaller", "Proton archive not found in RootFS package");
            Log.w("ImageFsInstaller", "This is expected if using a lightweight RootFS");
            return;
        }
        
        File outFile = new File(destRootDir, "/opt/proton-9.0-arm64ec");
        outFile.mkdirs();
        
        try {
            Log.d("ImageFsInstaller", "Extracting Proton to: " + outFile.getAbsolutePath());
            boolean success = TarCompressorUtils.extract(
                TarCompressorUtils.Type.XZ,
                protonArchive,
                outFile,
                null
            );
            
            if (success) {
                File prefixPack = new File(outFile, "prefixPack.txz");
                Log.d("ImageFsInstaller", "Proton installed successfully");
                Log.d("ImageFsInstaller", "Checking prefixPack.txz: " + prefixPack.exists());
            } else {
                Log.e("ImageFsInstaller", "Failed to extract Proton archive");
            }
        } catch (Exception e) {
            Log.e("ImageFsInstaller", "Failed to install Proton", e);
        }
    }

    private static void installDriversFromRootFs(Context context, File rootfsDir) {
        AdrenotoolsManager adrenotoolsManager = new AdrenotoolsManager(context);
        File driversDir = new File(rootfsDir, "graphics_driver");
        File adrenotoolsDir = new File(context.getFilesDir(), "contents/adrenotools");
        
        if (!driversDir.exists()) {
            Log.w("ImageFsInstaller", "No graphics_driver directory in RootFS");
            return;
        }

        String[] drivers = {"System", "v819", "turnip25.1.0"};
        for (String driver : drivers) {
            File driverArchive = new File(driversDir, "adrenotools-" + driver + ".tzst");
            if (driverArchive.exists()) {
                File dst = new File(adrenotoolsDir, driver);
                dst.mkdirs();
                
                try {
                    TarCompressorUtils.extract(
                        TarCompressorUtils.Type.ZSTD,
                        driverArchive,
                        dst,
                        null
                    );
                    Log.d("ImageFsInstaller", "Driver " + driver + " installed");
                } catch (Exception e) {
                    Log.e("ImageFsInstaller", "Failed to install driver " + driver, e);
                }
            }
        }
    }

    private static void installAdditionalComponentsFromRootFs(Context context, File rootfsDir, File destRootDir) {
        File othersDir = new File(rootfsDir, "others");
        if (!othersDir.exists()) {
            Log.w("ImageFsInstaller", "No others directory in RootFS");
        }

        Log.d("ImageFsInstaller", "Installing additional components...");
        
        copyRootFsDirectory(new File(rootfsDir, "dxwrapper"), new File(context.getFilesDir(), "contents/dxwrapper"));
        copyRootFsDirectory(new File(rootfsDir, "wincomponents"), new File(context.getFilesDir(), "contents/wincomponents"));
        copyRootFsDirectory(new File(rootfsDir, "box64"), new File(context.getFilesDir(), "contents/box64"));
        copyRootFsDirectory(new File(rootfsDir, "fexcore"), new File(context.getFilesDir(), "contents/fexcore"));
        
        if (othersDir.exists()) {
            File contentsOthers = new File(context.getFilesDir(), "contents/others");
            copyRootFsDirectory(othersDir, contentsOthers);
            
            File containerPattern = new File(contentsOthers, "proton-9.0-arm64ec_container_pattern.tzst");
            Log.d("ImageFsInstaller", "Container pattern in contents/others: " + containerPattern.exists());
        }

        Log.d("ImageFsInstaller", "Additional components installed");
    }

    private static void copyRootFsDirectory(File sourceDir, File destDir) {
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            return;
        }

        destDir.mkdirs();
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                File destFile = new File(destDir, file.getName());
                if (file.isDirectory()) {
                    copyRootFsDirectory(file, destFile);
                } else {
                    FileUtils.copy(file, destFile);
                }
            }
        }
    }

    public interface Callback<T> {
        void call(T value);
    }
}