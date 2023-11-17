package sv.edu.catolica.timetrack;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sv.edu.catolica.timetrack.Adapter.ToDoAdapter;
import sv.edu.catolica.timetrack.Class.AddNewTask;
import sv.edu.catolica.timetrack.Class.TouchHelper;
import sv.edu.catolica.timetrack.Interfaces.OnDialogCloseListener;
import sv.edu.catolica.timetrack.Model.ToDoModel;

public class PendientesFragment extends Fragment implements OnDialogCloseListener {

    private RecyclerView mRecyclerViewPendientes;
    private FloatingActionButton mFabPendiente;
    private FirebaseFirestore firestore;
    private String usuarioId;
    private FirebaseAuth mAuth;
    private ToDoAdapter adapter;
    private List<ToDoModel> mList;
    private Query query;
    private ListenerRegistration listenerRegistration;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pendientes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerViewPendientes = view.findViewById(R.id.rvPendientes);
        mFabPendiente = view.findViewById(R.id.fabPendientes);
        firestore = FirebaseFirestore.getInstance();

        // configurando el recyclerview
        mRecyclerViewPendientes.setHasFixedSize(true);
        mRecyclerViewPendientes.setLayoutManager(new LinearLayoutManager(view.getContext()));

        mFabPendiente.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AddNewTask.newInstance().show(getChildFragmentManager(), AddNewTask.TAG);
            }
        });

        mList = new ArrayList<>();
        adapter = new ToDoAdapter(getActivity(), mList);

        // Aniadiendo el efecto de editar y borrar al deslizar en el elemeto del recyclerview
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new TouchHelper(adapter, this));
        itemTouchHelper.attachToRecyclerView(mRecyclerViewPendientes);

        // Llamando los datos desde Firestore y actualizando el adaptador del recyclerview
        // obteniendo el id del usuario activo para identificar su db
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        usuarioId = currentUser.getUid();

        traerDB();
        mRecyclerViewPendientes.setAdapter(adapter);
    }

    @Override
    public void onDialogClose(DialogInterface dialogInterface) {
        traerDB();
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
                            ToDoModel toDoModel = documentChange.getDocument().toObject(ToDoModel.class).withId(id);

                            // Para determinar que no se dupliquen elementos al actualizar
                            traerDB();
                        }
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

    private void traerDB() {
        // Trae los datos desde la DB y los pinta en un recyclerview asociado
        mList.clear();

        CollectionReference tareasPendientes = firestore.collection(usuarioId);
        tareasPendientes.get().addOnCompleteListener(tarea -> {
            if (tarea.isSuccessful()) {
                for (QueryDocumentSnapshot document : tarea.getResult()) {
                    String id = document.getId();
                    ToDoModel toDoModel = document.toObject(ToDoModel.class).withId(id);

                    // Para determinar que no se dupliquen elementos al actualizar
                    List<String> listaIds = new ArrayList<>();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        mList.forEach(documento -> {
                            listaIds.add(documento.TaskId);

                        });
                    }

                    if (!listaIds.contains(id) && (toDoModel.getStatus() == 0) ) { // solo agrega tareas pendientes
                        mList.add(toDoModel);  // se aniade el elemento a la lista del adaptador
                        adapter.notifyDataSetChanged();  // el adaptador actualiza su respectivo recyclerview
                    }
                }

            } else {
                Toast.makeText(getContext(), tarea.getException().toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}