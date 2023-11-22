package sv.edu.catolica.timetrack;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import sv.edu.catolica.timetrack.Adapter.ReminderAdapter;
import sv.edu.catolica.timetrack.Class.AlarmReceiver;
import sv.edu.catolica.timetrack.Model.ReminderModel;

public class RecordatoriosFragment extends Fragment implements ReminderAdapter.OnItemClickListener {
    private RecyclerView mRecyclerViewReminder;
    private LinearLayout mLLVacio;
    private FirebaseFirestore firestore;
    private ListenerRegistration listenerRegistration;
    private String usuarioId;
    private FirebaseAuth mAuth;
    private ReminderAdapter adapter;
    private List<ReminderModel> mList;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recordatorios, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Para poder establcer notificaciones programadas
        createNotificationChannel();

        firestore = FirebaseFirestore.getInstance();

        // configurando el recyclerview
        mRecyclerViewReminder = view.findViewById(R.id.rvRecordatorios);
        mRecyclerViewReminder.setHasFixedSize(true);
        mRecyclerViewReminder.setLayoutManager(new LinearLayoutManager(view.getContext()));

        // mensaje que aparece si en la lista del recyclerview no hay nada
        mLLVacio = view.findViewById(R.id.ll_agendadoVacio);

        // para popular el recyclerview con los recordatorios
        mList = new ArrayList<>();
        adapter = new ReminderAdapter(getActivity(), mList);

        // Llamando los datos desde Firestore y actualizando el adaptador del recyclerview
        // obteniendo el id del usuario activo para identificar su db
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        usuarioId = currentUser.getUid();

        showData();
        mRecyclerViewReminder.setAdapter(adapter);
        adapter.setOnItemClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();


        // Registra un listener para detectar modificaciones en la db y realizar acciones acordes
        listenerRegistration = firestore.collection(usuarioId)
                .addSnapshotListener((queryDocumentSnapshot, e) -> {
                    if (e != null) {
                        // Manejar errores
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // llama a la bd si ha ocurrido alguna modificacion en algun documento de la coleccion
                    boolean siTraerDB = false;
                    for(DocumentChange documentChange : queryDocumentSnapshot.getDocumentChanges())  {
                        if(documentChange.getType() == DocumentChange.Type.MODIFIED) {
                            siTraerDB = true;
                        }
                    }
                    if (siTraerDB) {
                        showData();
                    }
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        // Detener la escucha cuando el Fragment se detiene
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    // Metodos
    private void showData() {
        // Trae los datos desde la DB y los pinta en un recyclerview asociado
        mList.clear();

        CollectionReference tareasAgendadas = firestore.collection(usuarioId);
        tareasAgendadas.orderBy("limitDate").get().addOnCompleteListener(tarea -> {
            if (tarea.isSuccessful()) {
                for (QueryDocumentSnapshot document : tarea.getResult()) {
                    try {
                        long status = document.getLong("status");

                        if (status == 0) {  // indica que la actividad no ha sido completada
                            ReminderModel reminderModel = new ReminderModel();
                            reminderModel.withId(document.getId());
                            reminderModel.setTitulo(document.getString("task"));
                            reminderModel.setFecha(document.getString("due"));
                            reminderModel.setTipo(document.getString("type"));
                            reminderModel.setHoraNoti(document.getString("reminder"));
                            reminderModel.setIdInt(Math.toIntExact(document.getLong("idInt")));

                            mList.add(reminderModel);  // se aniade el elemento a la lista del adaptador
                            adapter.notifyDataSetChanged();  // el adaptador actualiza su respectivo recyclerview
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                if (mList.isEmpty()) {
                    mLLVacio.setVisibility(View.VISIBLE); // Mostrar el mensaje de lista vacía
                    mRecyclerViewReminder.setVisibility(View.GONE); // Ocultar el RecyclerView
                } else {
                    mLLVacio.setVisibility(View.GONE); // Ocultar el mensaje de lista vacía
                    mRecyclerViewReminder.setVisibility(View.VISIBLE); // Mostrar el RecyclerView
                }

            } else {
                Toast.makeText(getContext(), tarea.getException().toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Metodos de la interfaz ReminderAdapter.OnItemClickListener
    @Override
    public void onItemClick(int position) {
        // Obtener la fecha y hora actuales
        final Calendar calendario = Calendar.getInstance();
        ReminderModel reminderModel = mList.get(position);

        // Crear DatePickerDialog para seleccionar la fecha
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    // Acción cuando se elige la fecha
                    calendario.set(Calendar.YEAR, year);
                    calendario.set(Calendar.MONTH, month);
                    calendario.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // Crear TimePickerDialog para seleccionar la hora
                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            getContext(),
                            (view1, hourOfDay, minute) -> {
                                // Acción cuando se elige la hora
                                calendario.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendario.set(Calendar.MINUTE, minute);
                                calendario.set(Calendar.SECOND, 0);
                                calendario.set(Calendar.MILLISECOND, 0);


                                // Formateando la fecha y hora de la notificacion
                                SimpleDateFormat simpleDateFormat;
                                String idioma = Locale.getDefault().getLanguage();
                                switch (idioma) {
                                    case "es":
                                        simpleDateFormat = new SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy 'a las' h:mm a", Locale.getDefault()); // formato para español
                                        break;
                                    case "en":
                                        simpleDateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault()); // formato para ingles
                                        break;
                                    case "pt":
                                        simpleDateFormat = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy 'às' h:mm a", Locale.getDefault()); // formato para portugues
                                        break;
                                    default:
                                        simpleDateFormat = new SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy 'a las' h:mm a", Locale.getDefault()); // formato para otros idiomas
                                        break;
                                }

                                String fechaHoraSeleccionada = simpleDateFormat.format(calendario.getTime());

                                // Actualizando el recordatorio
                                String idDocumento = reminderModel.TaskId;
                                try {
                                    firestore.collection(usuarioId).document(idDocumento)
                                            .update("reminder", fechaHoraSeleccionada).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    Toast.makeText(getContext(), R.string.se_program_una_notificaci_n, Toast.LENGTH_SHORT).show();

                                                    cancelAlarm(reminderModel.getIdInt()); // si existe una notificacion previa, la borra para solo tener una

                                                    // estableciendo la notificacion
                                                    Bundle notificationBundle = new Bundle();
                                                    notificationBundle.putString("Title", reminderModel.getTitulo());
                                                    notificationBundle.putString("Description", getString(R.string.actividad_agendada_para_el) + reminderModel.getFecha());
                                                    notificationBundle.putInt("id", reminderModel.getIdInt());

                                                    setAlarm(calendario, notificationBundle, reminderModel.getIdInt());
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                } catch (Exception e) {
                                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }

                            },
                            calendario.get(Calendar.HOUR_OF_DAY), // Hora actual como predeterminada
                            calendario.get(Calendar.MINUTE),      // Minuto actual como predeterminado
                            false                                // Formato de hora de 24 horas
                    );

                    // Mostrar el TimePickerDialog para seleccionar la hora
                    timePickerDialog.show();
                },
                calendario.get(Calendar.YEAR),           // Año actual como predeterminado
                calendario.get(Calendar.MONTH),          // Mes actual como predeterminado
                calendario.get(Calendar.DAY_OF_MONTH)    // Día actual como predeterminado
        );

        // Mostrar el DatePickerDialog para seleccionar la fecha
        datePickerDialog.getDatePicker().setMinDate(calendario.getTimeInMillis());
        datePickerDialog.show();
    }

    // metodos
    private void cancelAlarm(int id) {
        try {
            Intent intent = new Intent(getContext(), AlarmReceiver.class);
            pendingIntent = PendingIntent.getBroadcast(getContext(), id, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            if (alarmManager == null) {
                alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            }

            alarmManager.cancel(pendingIntent);
        } catch (Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setAlarm(Calendar calendario, Bundle notificationBundle, int id) {
        alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(getContext(), AlarmReceiver.class);
        intent.setAction(Long.toString(System.currentTimeMillis()));
        intent.setData(Uri.parse("custom://notification/" + id)); // URI única
        intent.putExtra("bundle_notification", notificationBundle);

        // Especificando el contenido de la notificacion
        pendingIntent = PendingIntent.getBroadcast(
                getContext(),
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendario.getTimeInMillis(), pendingIntent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "TimeTrackChannel";
            String description = "Channel for Alarm Manager";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("timetrack", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}