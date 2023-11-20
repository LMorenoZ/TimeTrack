package sv.edu.catolica.timetrack;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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
import java.util.List;

import sv.edu.catolica.timetrack.Adapter.ToDoAdapter;
import sv.edu.catolica.timetrack.Class.AddNewTask;
import sv.edu.catolica.timetrack.Class.TouchHelper;
import sv.edu.catolica.timetrack.Interfaces.OnDialogCloseListener;
import sv.edu.catolica.timetrack.Model.ToDoModel;

public class CompletadasFragment extends Fragment implements OnDialogCloseListener, ToDoAdapter.OnItemClickListener {
    private RecyclerView mRecyclerViewCompletas;

    private FirebaseFirestore firestore;
    private String usuarioId;
    private FirebaseAuth mAuth;
    private Query query;
    private ListenerRegistration listenerRegistration;

    private ToDoAdapter adapter;
    private List<ToDoModel> mList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_completadas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerViewCompletas = view.findViewById(R.id.rvCompletadas);
        firestore = FirebaseFirestore.getInstance();

        // configurando el recyclerview
        mRecyclerViewCompletas.setHasFixedSize(true);
        mRecyclerViewCompletas.setLayoutManager(new LinearLayoutManager(view.getContext()));

        mList = new ArrayList<>();
        adapter = new ToDoAdapter(getActivity(), mList);

        // Aniadiendo el efecto de editar y borrar al deslizar en el elemeto del recyclerview
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new TouchHelper(adapter, CompletadasFragment.this));
        itemTouchHelper.attachToRecyclerView(mRecyclerViewCompletas);

        // Llamando los datos desde Firestore y actualizando el adaptador del recyclerview
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        usuarioId = currentUser.getUid();
        traerDB();
        mRecyclerViewCompletas.setAdapter(adapter);
        adapter.setOnItemClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();

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

                            // Para determinar que no se dupliquen elementos
                            List<String> listaIds = new ArrayList<>();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                mList.forEach(documento -> {
                                  listaIds.add(documento.TaskId);
                                });
                            }
                            if ( !listaIds.contains(id) && (toDoModel.getStatus() == 1) ) {
                                mList.add(toDoModel);  // se aniade el elemento a la lista del adaptador
                                adapter.notifyDataSetChanged();  // el adaptador actualiza su respectivo recyclerview
                            } else if ( listaIds.contains(id) && (toDoModel.getStatus() == 1) ) {
                                traerDB();
                            }
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
        mList.clear();
        CollectionReference tareasCompletadas = firestore.collection(usuarioId);
        tareasCompletadas.orderBy("limitDate").get().addOnCompleteListener(tarea -> {
            if (tarea.isSuccessful()) {
                for (QueryDocumentSnapshot document : tarea.getResult()) {
                    String id = document.getId();
                    ToDoModel toDoModel = document.toObject(ToDoModel.class).withId(id);

                    if (toDoModel.getStatus() == 1) { // solo agrega tareas completadas
                        mList.add(toDoModel);  // se aniade el elemento a la lista del adaptador
                        adapter.notifyDataSetChanged();  // el adaptador actualiza su respectivo recyclerview
                    }
                }

            } else {
                Toast.makeText(getContext(), tarea.getException().toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(int position, boolean isChecked) {
        int nuevoStatus = 0;
        int elementoPosicion = position;

        nuevoStatus = isChecked == true ? 1 : 0;

        ToDoModel toDoModel;

        try {
            if (elementoPosicion >= 0 && elementoPosicion < mList.size()) {
                toDoModel = mList.get(elementoPosicion);

                try {
                    actualizarStatusFirestore(nuevoStatus, elementoPosicion, toDoModel);
                } catch (Exception e) {
                    Toast.makeText(getContext(),e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            } else {
                traerDB();

                toDoModel = mList.get(elementoPosicion);

                try {
                    actualizarStatusFirestore(nuevoStatus, elementoPosicion, toDoModel);
                } catch (Exception e) {
                    Toast.makeText(getContext(),e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizarStatusFirestore(int nuevoStatus, int elementoPosicion, ToDoModel toDoModel) {
        firestore.collection(usuarioId).document(toDoModel.TaskId).update("status", nuevoStatus).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    mList.remove(elementoPosicion);
                    adapter.notifyItemRemoved(elementoPosicion);

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

    @Override
    public void onDialogClose(DialogInterface dialogInterface) {
        traerDB();
    }
}