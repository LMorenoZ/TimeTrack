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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
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


    private int beforeDeleteElements;

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

    public void editTask (int position, Fragment fragment) {
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
        final int[] elementPosition = {position};
        beforeDeleteElements = getItemCount();

        ToDoModel toDoModel = todoList.get(position);
        holder.mCheckBox.setText(toDoModel.getTask());
        holder.mCheckBox.setChecked(toBoolean(toDoModel.getStatus()));
        holder.mDueDateTv.setText("Programada en " + toDoModel.getDue());

        holder.mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                // Modifica el checkbox cuando el usuario toca la cajita
                int nuevoStatus;  // 1: checked, 0: not checked


                nuevoStatus = isChecked == true ? 1 : 0;

                try {
                    firestore.collection(usuarioId).document(toDoModel.TaskId).update("status", nuevoStatus).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
//                      TODO: Toast.makeText(activity, "Tarea movida a completados", Toast.LENGTH_SHORT).show();
                                try {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(activity, "Se marco", Toast.LENGTH_SHORT).show();
                                        Toast.makeText(activity, "", Toast.LENGTH_SHORT).show();
                                        if (beforeDeleteElements > elementPosition[0]) {
                                            todoList.remove(elementPosition[0]);
                                            notifyItemRemoved(elementPosition[0]);
                                        }
                                        todoList.remove(elementPosition[0]);
                                        notifyItemRemoved(elementPosition[0]);
                                    } else {
                                        Toast.makeText(activity, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(activity, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(activity, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        Toast.makeText(activity, e.getMessage(), Toast.LENGTH_SHORT).show();
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
