package wang.umoo.myworker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

public class VolumeBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Objects.equals(intent.getAction(), "android.media.VOLUME_CHANGED_ACTION")) {
            CollectService.Companion.end(context);
        }
    }
}
