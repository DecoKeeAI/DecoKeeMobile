package com.decokee.decokeemobile.utils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import com.decokee.decokeemobile.BuildConfig;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateCheckUtil {

    private static final String TAG = UpdateCheckUtil.class.getSimpleName();
    private static long sDownloadId = -1;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void checkForUpdates(Context context, Consumer<Integer> updateCheckCallback) {

        if (sDownloadId != -1) {
            updateCheckCallback.accept(2);
            return;
        }

        // 这里应该是检查更新的API，通常是一个返回版本信息的HTTP请求
        String updateCheckUrl = "https://raw.github.com/DecoKeeAI/DecoKeeMobile/main/ota/latest.json";

        // 使用例如OkHttp等网络库发起请求
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(updateCheckUrl)
                .build();


        sDownloadId = -1;

        client.newCall(request).enqueue(new Callback() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onFailure(Call call, IOException e) {
                updateCheckCallback.accept(0);
                // 处理请求失败
                Log.w(TAG, "onFailure: ", e);
            }

            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    updateCheckCallback.accept(0);
                    throw new IOException("Unexpected code " + response);
                }

                String responseString = response.body().string();
                // 解析返回的JSON，比如检查version字段
                JSONObject result = null;
                try {
                    result = new JSONObject(responseString);
                    String newVersion = result.getString("version");
                    // 获取当前应用版本
                    String currentVersion = context.getPackageManager()
                            .getPackageInfo(context.getPackageName(), 0).versionName;

                    Log.d(TAG, "checkForUpdates: onResponse: responseString: " + responseString + " NewVersion: " + newVersion + " CurrentVersion: " + currentVersion);

                    String[] currentVersionInfo = currentVersion.trim().split("\\.");
                    String[] newVersionInfo = newVersion.trim().split("\\.");

                    if (newVersionInfo.length != 3) {
                        updateCheckCallback.accept(0);
                        return;
                    }

                    for (int i = 0; i < 3; i++) {
                        int cmpResult = Integer.parseInt(newVersionInfo[i]) - Integer.parseInt(currentVersionInfo[i]);
                        if (cmpResult > 0) {
                            Log.d(TAG, "checkForUpdates: onResponse: found update info");
                            downladAndInstallUpdate(context, newVersion);
                            updateCheckCallback.accept(1);
                            break;
                        } else if (cmpResult < 0) {
                            updateCheckCallback.accept(0);
                            break;
                        }
                    }
                    updateCheckCallback.accept(0);
                } catch (Exception e) {
                    updateCheckCallback.accept(0);
                    e.printStackTrace();
                }
            }
        });
    }

    public static void downladAndInstallUpdate(Context context, String newVersion) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        String url = "https://github.com/DecoKeeAI/DecoKeeAI/releases/download/V" + newVersion + "/DecoKeeMobile.apk"; // replace with your APK URL
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        String apkPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/DecoKeeMobile.apk";

        File apkFile = new File(apkPath);
        apkFile.deleteOnExit();

        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "DecoKeeMobile.apk");
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setAllowedOverRoaming(true);
        request.setTitle("DecoKeeMobile");
        request.setDescription("Downloading...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        sDownloadId = downloadManager.enqueue(request);
    }

    public static class DownloadCompleteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            Log.d(TAG, "onReceive: DOWNLOAD_COMPLETE: downloadId: " + downloadId + " LocalID: " + sDownloadId);
            // Verify the download ID matches the one we enqueued
            if (downloadId == sDownloadId) {
                DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                Uri destinationUri = downloadManager.getUriForDownloadedFile(sDownloadId);
                Log.d(TAG, "onReceive: DOWNLOAD_COMPLETE: destinationUri: " + destinationUri);
                // Install the APK file
                installApk(context);
            }

            sDownloadId = -1;
        }
    }

    private static void installApk(Context context) {
        String apkPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/DecoKeeMobile.apk";
        Log.d(TAG, "onReceive: installApk: apkPath: " + apkPath);

        File apkFile = new File(apkPath);

        if (apkFile.exists()) {
            Uri uriForFile = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", apkFile);
            Log.d(TAG, "onReceive: installApk: apkPath: exist");
            Intent installIntent = new Intent(Intent.ACTION_VIEW);
//            installIntent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            installIntent.setDataAndType(uriForFile, "application/vnd.android.package-archive");
            installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.N) {
                installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            context.startActivity(installIntent);
            return;
        }

        Log.d(TAG, "onReceive: installApk: apkPath: not exist");
    }
}
