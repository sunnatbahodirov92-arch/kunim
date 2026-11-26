package uz.kunim.app;

import android.content.Intent;
import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AlarmPlugin.class);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        AlarmStore.foreground = true;
        // ilova ochilgach, native signal to'xtaydi (ilovaning o'zi boshqaradi)
        stopService(new Intent(this, AlarmService.class));
    }

    @Override
    public void onPause() {
        AlarmStore.foreground = false;
        super.onPause();
    }
}
