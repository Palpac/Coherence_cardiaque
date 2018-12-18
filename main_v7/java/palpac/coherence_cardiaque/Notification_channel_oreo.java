package palpac.coherence_cardiaque;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class Notification_channel_oreo extends Application {

    public final static String OREO_CHANNEL_ID = "COHERENCE_CARDIAQUE";
    public final static String OREO_CHANNEL_NAME = "Coherence Cardiaque Channel";


    @Override
    public void onCreate() {
        super.onCreate();

        create_Oreo_notification_channel();
    }

    private void create_Oreo_notification_channel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel Oreo_serviceChannel = new NotificationChannel( OREO_CHANNEL_ID, OREO_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager Oreo_manager = getSystemService(NotificationManager.class);
            Oreo_manager.createNotificationChannel(Oreo_serviceChannel);
        }
    }
}
