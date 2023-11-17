package sv.edu.catolica.timetrack.Adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import sv.edu.catolica.timetrack.Class.AddNewTask;
import sv.edu.catolica.timetrack.Model.ToDoModel;
import sv.edu.catolica.timetrack.PendientesFragment;
import sv.edu.catolica.timetrack.R;

public class ToDoAdapter extends RecyclerView.Adapter<ToDoAdapter.MyViewHolder> {
    private List<ToDoModel> todoList;
    private FragmentActivity activity;
    private FirebaseFirestore firestore;
    private String usuarioId;
    private FirebaseAuth mAuth;

    public ToDoAdapter(FragmentActivity fragmentActivity, List<ToDoModel> todoList) {
        this.todoList = todoList;
        activity = fragmentActivity;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.each_task, parent, false);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        usuarioId = currentUser.getUid();  // id del usuario para identificar su db

        return new MyViewHolder(view);
    }

    public void deleteTask (int position) {
        ToDoModel toDoModel = todoList.get(position);

        firestore.collection(usuarioId).document(toDoModel.TaskId).delete();
        todoList.remove(position);

        notifyItemRemoved(position);
        Toast.makeText(activity, "La tarea se borró correctamente", Toast.LENGTH_SHORT).show();
    }

    public Context getContext() {
        return activity;
    }

    public void editTask (int position, PendientesFragment fragment) {
        ToDoModel toDoModel = todoList.get(position);

        Bundle bundle = new Bundle();
        bundle.putString("task", toDoModel.getTask());
        bundle.putString("due", toDoModel.getDue());
        bundle.putString("type", toDoModel.getType());
        bundle.putString("id", toDoModel.TaskId);
        // Conviertiendo el timestamp "limitDate"
        bundle.putLong("limitDateSec", toDoModel.getLimitDate().getSeconds());  // segundos del timestamp
        bundle.putInt("limitDateNano", toDoModel.getLimitDate().getNanoseconds()); // nanosegundos del timestamp

        AddNewTask addNewTask = new AddNewTask();
        addNewTask.setArguments(bundle);

        addNewTask.show(fragment.getChildFragmentManager(), addNewTask.getTag());

    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ToDoModel toDoModel = todoList.get(position);
        holder.mCheckBox.setText(toDoModel.getTask());
        holder.mCheckBox.setChecked(toBoolean(toDoModel.getStatus()));
        holder.mDueDateTv.setText("Programada en " + toDoModel.getDue());
        int elementPosition = position;

        holder.mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                // Modifica el checkbox cuando el usuario toca la cajita
                if (isChecked) {
                    firestore.collection(usuarioId).document(toDoModel.TaskId).update("status", 1);
                    Toast.makeText(activity, "Tarea movida a completados", Toast.LENGTH_SHORT).show();

                    todoList.remove(elementPosition);
                    notifyItemRemoved(elementPosition);
                } else {
                    firestore.collection(usuarioId).document(toDoModel.TaskId).update("status", 0);
                }
            }
        });
    }

    private boolean toBoolean(int status) {
        return status != 0;
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView mDueDateTv;
        CheckBox mCheckBox;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            mDueDateTv = itemView.findViewById(R.id.tv_due_date_pendientes);
            mCheckBox = itemView.findViewById(R.id.cbPendiente);
        }
    }
}
