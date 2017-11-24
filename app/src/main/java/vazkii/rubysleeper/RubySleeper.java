package vazkii.rubysleeper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class RubySleeper extends Activity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        if(RubySleeperService.connected)
            Toast.makeText(this, "RubySleeper is already running", Toast.LENGTH_SHORT).show();
        else {
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivityForResult(intent, 0);

            Toast.makeText(this, "Please find RubySleeper and enable Accessibility", Toast.LENGTH_LONG).show();
        }

        finish();
    }
}
