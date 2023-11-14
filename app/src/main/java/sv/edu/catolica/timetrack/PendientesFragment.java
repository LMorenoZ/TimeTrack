package sv.edu.catolica.timetrack;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sv.edu.catolica.timetrack.Adapter.ToDoAdapter;
import sv.edu.catolica.timetrack.Class.AddNewTask;
import sv.edu.catolica.timetrack.Model.ToDoModel;

public class PendientesFragment extends Fragment {

    private RecyclerView mRecyclerViewPendientes;
    private FloatingActionButton mFabPendiente;
    private FirebaseFirestore firestore;
    private String usuarioId;
    private FirebaseAuth mAuth;
    private ToDoAdapter adapter;
    private List<ToDoModel> mList;

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

        mRecyclerViewPendientes.setAdapter(adapter);
        showData();
    }

    private void showData() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        usuarioId = currentUser.getUid();  // id del usuario para identificar su db

        firestore.collection(usuarioId).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                for(DocumentChange documentChange : value.getDocumentChanges()) {
                    if(documentChange.getType() == DocumentChange.Type.ADDED) {
                        String id = documentChange.getDocument().getId();
                        ToDoModel toDoModel = documentChange.getDocument().toObject(ToDoModel.class).withId(id);

                        mList.add(toDoModel);
                        adapter.notifyDataSetChanged();
                    }
                }
                Collections.reverse(mList);
            }
        });
    }
}