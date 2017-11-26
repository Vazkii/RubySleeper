package vazkii.rubysleeper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

// Taken from https://stackoverflow.com/questions/3873659/android-how-can-i-get-the-current-foreground-activity-from-a-service/27642535#27642535
public class RubySleeperService extends AccessibilityService {

    public static boolean connected = false;
    private static DataOutputStream su;

    private static boolean notificationsEnabled = true;

    private static final List<Pattern> DISABLED_APPS = Arrays.asList(
            Pattern.compile("klb\\.android\\.lovelive"), // School Idol Festival
            Pattern.compile("com\\.rayark"), // Cytus, Deemo, VOEZ
            Pattern.compile("jp\\.co\\.bandainamcoent\\.BNEI0242"), // Starlight Stage
            Pattern.compile("jp\\.co\\.craftegg\\.band") // Girls Band Party
    );

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;

        if (Build.VERSION.SDK_INT >= 16)
            config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

        setServiceInfo(config);

        boolean root = getRoot();
        if(!root)
            Toast.makeText(this, "RubySleeper can not run without Root.", Toast.LENGTH_LONG).show();
        else {
            Toast.makeText(this, "RubySleeper is now running!", Toast.LENGTH_LONG).show();
            connected = true;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String pkg = event.getPackageName().toString();
            ComponentName componentName = new ComponentName(pkg, event.getClassName().toString());
            try {
                ActivityInfo activityInfo = getPackageManager().getActivityInfo(componentName, 0);

                if(activityInfo != null) {
                    boolean enable = true;
                    for(Pattern p : DISABLED_APPS)
                        if(p.matcher(pkg).find()) {
                            enable = false;
                            break;
                        }

                    if(notificationsEnabled != enable) {
                        setNotifications(enable ? 1 : 0);
                        notificationsEnabled = enable;
                    }
                }
            } catch(PackageManager.NameNotFoundException e) {}
        }
    }

    private boolean getRoot() {
        if(su != null)
            return true;

        boolean hasRoot = false;
        try {
            Process suProcess = Runtime.getRuntime().exec("su");
            DataOutputStream out = new DataOutputStream(suProcess.getOutputStream());
            out.writeBytes("exit\n");
            out.flush();
            suProcess.waitFor();
            if(suProcess.exitValue() == 0)  {
                hasRoot = true;
                suProcess = Runtime.getRuntime().exec("su");
                su = new DataOutputStream(suProcess.getOutputStream());
            }
        } catch(IOException | InterruptedException e) { }

        return hasRoot;
    }

    private void setNotifications(int setting) {
        String command = "settings put global heads_up_notifications_enabled " + setting;
        boolean executed = false;
        try {
            if(getRoot()) {
                su.writeBytes(command);
                su.writeBytes("\n");
                su.flush();
                executed = true;
            }
        } catch(IOException e) {
            Log.e("RubySleeper", "Problem!", e);
        }

        if(!executed)
            su = null;
    }

    @Override
    public void onInterrupt() {
        // NO-OP
    }

}
