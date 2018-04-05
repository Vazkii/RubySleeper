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

    private static final String MESSENGER_ID = "com.facebook.orca";

    public static boolean connected = false;
    private static DataOutputStream su;

    private static boolean notificationsEnabled = true;
    private static boolean facebookMessenger = false;

    private static final List<Pattern> DISABLED_APPS = Arrays.asList(
            Pattern.compile("klb\\.android\\.lovelive"), // School Idol Festival
            Pattern.compile("com\\.rayark"), // Cytus, Deemo, VOEZ
            Pattern.compile("co\\.bandainamcoent\\.BNEI0242"), // Starlight Stage
            Pattern.compile("co\\.craftegg\\.band"), // Girls Band Party
            Pattern.compile("com\\.bushiroad\\.en\\.bangdreamgbp"), // Girls Band Party (EN)
            Pattern.compile("com\\.bandainamcoent\\.imas_millionlive_theaterdays"), // Theater Days
            Pattern.compile("donuts(.*)\\.t7s"), // Tokyo 7th Sisters
            Pattern.compile("co\\.bandainamcoonline\\.idolish7"), // IDOLiSH 7
            Pattern.compile("co\\.cyberagent\\.gfonpu"), // Girl Friend Beta
            Pattern.compile("com\\.bandainamcoent\\.aktpos"), // Aikatsu: Photo on Stage
            Pattern.compile("co\\.happyelements\\.boys"), // Ensenble Stars
            Pattern.compile("com\\.klab\\.utapri\\.shininglive"), // Shining Live
            Pattern.compile("com\\.dena\\.a12024374"), // Uta Macross
            Pattern.compile("com\\.ponycanyon\\.game\\.prismstep"), // Prism Step
            Pattern.compile("com\\.bandainamcoent\\.imas_SideM_LIVEONSTAGE"), // SideM
            Pattern.compile("com\\.dmm\\.games\\.legenne"), // Legenne
            Pattern.compile("app\\.ebs$"), // 8 beat Story
            Pattern.compile("dalcomsoft\\.ss\\.jyp"), // SuperStar JYPNATION
            Pattern.compile("dalcomsoft\\.superstar\\.a") // SuperStar SMTOWN
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

        Log.i("RubySleeper", "Accessibility Service Enabled");
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
            Log.i("RubySleeper", "Access");
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

                    Log.i("RubySleeper", "Opened " + pkg);
                    if(pkg.equals(MESSENGER_ID)) {
                        facebookMessenger = true;
                        Log.i("RubySleeper", "Facebook messenger has been loaded!");
                    }

                    if(notificationsEnabled != enable) {
                        setNotifications(enable);
                        notificationsEnabled = enable;
                    }
                }
            } catch(PackageManager.NameNotFoundException e) { }
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

    private void setNotifications(boolean setting) {
        boolean executed = false;
        try {
            if(getRoot()) {
                String command = "settings put global heads_up_notifications_enabled " + (setting ? 1 : 0);
                su.writeBytes(command);
                su.writeBytes("\n");

                if(facebookMessenger) {
                    String option = setting ? "grant" : "revoke";
                    command = String.format("pm %s %s android.permission.SYSTEM_ALERT_WINDOW", option, MESSENGER_ID);
                    su.writeBytes(command);
                    su.writeBytes("\n");
                }

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
