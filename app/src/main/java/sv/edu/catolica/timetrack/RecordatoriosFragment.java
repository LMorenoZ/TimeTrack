package sv.edu.catolica.timetrack;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import sv.edu.catolica.timetrack.Model.ReminderModel;
import sv.edu.catolica.timetrack.Model.ToDoModel;

public class RecordatoriosFragment extends Fragment implements ReminderAdapter.OnItemClickListener {
    private RecyclerView mRecyclerViewReminder;
    private FirebaseFirestore firestore;
    private ListenerRegistration listenerRegistration;
    private String usuarioId;
    private FirebaseAuth mAuth;
    private ReminderAdapter adapter;
    private List<ReminderModel> mList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recordatorios, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = FirebaseFirestore.getInstance();

        // configurando el recyclerview
        mRecyclerViewReminder = view.findViewById(R.id.rvRecordatorios);
        mRecyclerViewReminder.setHasFixedSize(true);
        mRecyclerViewReminder.setLayoutManager(new LinearLayoutManager(view.getContext()));

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
                    for(DocumentChange documentChange : queryDocumentSnapshot.getDocumentChanges())  {

                        if(documentChange.getType() == DocumentChange.Type.MODIFIED) {
                            String id = documentChange.getDocument().getId();
//                            Toast.makeText(getContext(), id, Toast.LENGTH_SHORT).show();

                            ReminderModel reminderModel = documentChange.getDocument().toObject(ReminderModel.class).withId(id);

//                            Toast.makeText(getContext(), documentChange.getDocument().getString("task"), Toast.LENGTH_SHORT).show();

                            showData();
                        }
                    }
                });
//        showData();
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
        tareasAgendadas.get().addOnCompleteListener(tarea -> {
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

                            mList.add(reminderModel);  // se aniade el elemento a la lista del adaptador
                            adapter.notifyDataSetChanged();  // el adaptador actualiza su respectivo recyclerview
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }





                    // Para determinar que no se dupliquen elementos al actualizar
//                    List<String> listaIds = new ArrayList<>();
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                        mList.forEach(documento -> {
//                            listaIds.add(documento.TaskId);
//
//                        });
//                    }
//
//                    mList.add(reminderModel);  // se aniade el elemento a la lista del adaptador
//                    adapter.notifyDataSetChanged();  // el adaptador actualiza su respectivo recyclerview
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

                                // Ahora tienes el calendario con la fecha y hora seleccionadas
                                // Puedes usar este calendario para programar tu notificación
                                // por ejemplo, utilizando AlarmManager
                                // ...

                                // Formateando la fecha y hora de la notificacion
                                SimpleDateFormat simpleDateFormat =
                                        new SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy 'a las' h:mm a", Locale.getDefault());
                                String fechaHoraSeleccionada = simpleDateFormat.format(calendario.getTime());

                                // Actualizando el recordatorio
                                String idDocumento = mList.get(position).TaskId;
                                try {
                                    firestore.collection(usuarioId).document(idDocumento)
                                            .update("reminder", fechaHoraSeleccionada).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    Toast.makeText(getContext(), "Se actualizo el recordatorio", Toast.LENGTH_SHORT).show();
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
        datePickerDialog.show();
    }
}