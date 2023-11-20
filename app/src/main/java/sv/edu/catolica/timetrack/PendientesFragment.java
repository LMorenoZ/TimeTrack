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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
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

public class PendientesFragment extends Fragment implements OnDialogCloseListener, ToDoAdapter.OnItemClickListener {

    private RecyclerView mRecyclerViewPendientes;
    private TextView mTvPendientesVacio;
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

        // mensaje que aparece si en la lista del recyclerview no hay nada
        mTvPendientesVacio = view.findViewById(R.id.tv_pendientesVacia);

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
        adapter.setOnItemClickListener(this);

        adapter.setToDoAdapterListener(new ToDoAdapter.ToDoAdapterListener() {
            @Override
            public void onUltimoElementoEliminado(boolean listaVacia) {
                comprobarVisibilidad();
            }
        });
    }

    private void comprobarVisibilidad() {
        if (mList.isEmpty()) {
            mTvPendientesVacio.setVisibility(View.VISIBLE); // Mostrar el mensaje de lista vacía
            mRecyclerViewPendientes.setVisibility(View.GONE); // Ocultar el RecyclerView
        } else {
            mTvPendientesVacio.setVisibility(View.GONE); // Ocultar el mensaje de lista vacía
            mRecyclerViewPendientes.setVisibility(View.VISIBLE); // Mostrar el RecyclerView
        }
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

                            // TODO: Aqui tengo la posicion del elemento completado
                            // Para determinar que no se dupliquen elementos al actualizar
//                            Toast.makeText(getContext(), String.valueOf(toDoModel.getStatus()), Toast.LENGTH_SHORT).show();
//                            Toast.makeText(getContext(), String.valueOf(mList.size()), Toast.LENGTH_SHORT).show();


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
        tareasPendientes.orderBy("limitDate").get().addOnCompleteListener(tarea -> {
            if (tarea.isSuccessful()) {
                for (QueryDocumentSnapshot document : tarea.getResult()) {
                    String id = document.getId();
                    ToDoModel toDoModel = document.toObject(ToDoModel.class).withId(id);

                    // Para determinar que no se dupliquen elementos al actualizar datos que no sean el status
                    List<String> listaIds = new ArrayList<>();
                    for (ToDoModel toDo : mList) {
                        if(toDo.getStatus() == 0) {
                            listaIds.add(toDo.TaskId);
                        }
                    }

                    if (!listaIds.contains(id) && (toDoModel.getStatus() == 0) ) { // solo agrega tareas pendientes (status = 0)
//                        Toast.makeText(getContext(), "no deberia aparecer", Toast.LENGTH_SHORT).show();
                        mList.add(toDoModel);  // se aniade el elemento a la lista del adaptador
                        adapter.notifyDataSetChanged();  // el adaptador actualiza su respectivo recyclerview
                    }

                }
                comprobarVisibilidad();
            } else {
                Toast.makeText(getContext(), tarea.getException().toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(int position, boolean isChecked) {
        int nuevoStatus = 0;
        int elementoPosicion = position;

        nuevoStatus = isChecked == true ? 1 : 0;  // 0: not checked, 1: checked

        ToDoModel toDoModel;
        try {
            if (elementoPosicion >= 0 && elementoPosicion < mList.size()) {

                try {
                    toDoModel = mList.get(elementoPosicion);
                    actualizarStatusFirestore(nuevoStatus, elementoPosicion, toDoModel);
                } catch (Exception e) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                traerDB();

                toDoModel = mList.get(elementoPosicion);

                try {
                    actualizarStatusFirestore(nuevoStatus, elementoPosicion, toDoModel);
                } catch (Exception e) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            // TODO: Aqui da el error
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            traerDB();
        }
    }

    private void actualizarStatusFirestore(int nuevoStatus, int elementoPosicion, ToDoModel toDoModel) {
        firestore.collection(usuarioId).document(toDoModel.TaskId)
                .update("status", nuevoStatus, "reminder", "").addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            try {
//                                mList.remove(elementoPosicion);
//                                adapter.notifyItemRemoved(elementoPosicion);
                                if (elementoPosicion == 0 && mList.size() ==0) {
                                    adapter.notifyDataSetChanged();
                                }

                            } catch (Exception e) {
                                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}