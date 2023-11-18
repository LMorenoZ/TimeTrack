package sv.edu.catolica.timetrack.Class;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import sv.edu.catolica.timetrack.MainActivity;
import sv.edu.catolica.timetrack.R;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        // obteniendo los datos de la notificacion
        Bundle bundle = intent.getBundleExtra("bundle_notification");
        String title = bundle.getString("Title");
        String description = bundle.getString("Description");
        int id = bundle.getInt("id");

        // preparando el intent
        Intent i = new Intent(context, MainActivity.class); // se pretende abrir la MainActivity al tocar la notificacion
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, id, i, PendingIntent.FLAG_UPDATE_CURRENT);


        // Construyendo la notificacion
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "timetrack")
                .setSmallIcon(R.drawable.logo_app)
                .setContentTitle(title)
                .setContentText(description)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);


        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        notificationManagerCompat.notify(id, builder.build());
    }
}
