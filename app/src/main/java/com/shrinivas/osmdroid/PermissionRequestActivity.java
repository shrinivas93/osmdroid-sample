package com.shrinivas.osmdroid;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.ArrayList;

public class PermissionRequestActivity extends AppCompatActivity {

    private static final int CODE_WRITE_SETTINGS_PERMISSION = 332;
    private static String[] PERMISSIONS_ALL; //TODO You can Add multiple permissions here.
    private static final int PERMISSION_REQUEST_CODE = 223;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_request);
        context = this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            PackageInfo packageInfo = null;
            try {
                packageInfo = getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            } catch (PackageManager.NameNotFoundException e) {
                Toast.makeText(context, "Unable to fetch PackageInfo", Toast.LENGTH_SHORT).show();
            }

            PERMISSIONS_ALL = packageInfo.requestedPermissions;

            boolean allPermissionsGranted = true;
            ArrayList<String> toReqPermissions = new ArrayList<>();

            for (String permission : PERMISSIONS_ALL) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    toReqPermissions.add(permission);

                    allPermissionsGranted = false;
                }
            }
            if (allPermissionsGranted) {
                //TODO Now some permissions are very special and require Settings Activity to launch, as u might have seen in some apps. handleWriteSettingsPermission() is an example for WRITE_SETTINGS permission. If u don't need very special permission(s), replace handleWriteSettingsPermission() with initActivity().
//                handleWriteSettingsPermission();
                initActivity();
            } else {
                requestPermissions(toReqPermissions.toArray(new String[toReqPermissions.size()]), PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions not granted: " + permissions[i], Toast.LENGTH_LONG).show();
                    allPermGranted = false;
                    finish();
                    break;
                }
            }
            if (allPermGranted) {
//                handleWriteSettingsPermission();//TODO As mentioned above, use initActivity() here if u dont need very special permission WRITE_SETTINGS
                initActivity();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void handleWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(context)) {
                initActivity();
            } else {
                Toast.makeText(this, "Please Enable this permission for " +
                        getApplicationInfo().loadLabel(getPackageManager()).toString(), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                startActivityForResult(intent, CODE_WRITE_SETTINGS_PERMISSION);
            }
        }
    }

//TODO You don't need the following onActivityResult() function if u dont need very special permissions.

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requestCode == CODE_WRITE_SETTINGS_PERMISSION) {
            if (Settings.System.canWrite(this))
                initActivity();
            else {
                Toast.makeText(this, "Permissions not granted: " + Manifest.permission.WRITE_SETTINGS, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
