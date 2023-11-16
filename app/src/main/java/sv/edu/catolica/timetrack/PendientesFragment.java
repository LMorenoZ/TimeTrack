package sv.edu.catolica.timetrack;

import android.content.DialogInterface;
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
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
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
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new TouchHelper(adapter));
        itemTouchHelper.attachToRecyclerView(mRecyclerViewPendientes);

        // Llamando los datos desde Firestore y actualizando el adaptador del recyclerview
        showData();
        mRecyclerViewPendientes.setAdapter(adapter);
    }

    private void showData() {
        // obteniendo el id del usuario activo para identificar su db
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        usuarioId = currentUser.getUid();

        // llamada a firestore para traer las tareas
        query = firestore.collection(usuarioId).orderBy("time", Query.Direction.DESCENDING);
        listenerRegistration = query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                for(DocumentChange documentChange : value.getDocumentChanges())  {
                    if(documentChange.getType() == DocumentChange.Type.ADDED) {
                        String id = documentChange.getDocument().getId();
                        ToDoModel toDoModel = documentChange.getDocument().toObject(ToDoModel.class).withId(id);

                        mList.add(toDoModel);  // se aniade el elemento a la lista del adaptador
                        adapter.notifyDataSetChanged();  // el adaptador actualiza su respectivo recyclerview
                    }
                }
                listenerRegistration.remove();
            }
        });
    }

    @Override
    public void onDialogClose(DialogInterface dialogInterface) {
        actualizarUi();
    }

    private void actualizarUi() {
        mList.clear();
        showData();
        adapter.notifyDataSetChanged();
    }
}