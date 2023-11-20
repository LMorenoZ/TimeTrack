package sv.edu.catolica.timetrack.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import sv.edu.catolica.timetrack.Class.AddNewTask;
import sv.edu.catolica.timetrack.Model.ToDoModel;
import sv.edu.catolica.timetrack.R;

public class ToDoAdapter extends RecyclerView.Adapter<ToDoAdapter.MyViewHolder> {
    private List<ToDoModel> todoList;
    private FragmentActivity activity;
    private FirebaseFirestore firestore;
    private String usuarioId;
    private FirebaseAuth mAuth;
    private OnItemClickListener listener;
    private ToDoAdapterListener listenerRv;

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

        if (listenerRv != null) {
            listenerRv.onUltimoElementoEliminado(todoList.isEmpty());
        }


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
        if(toDoModel.getLimitDate() != null) {
            bundle.putLong("limitDateSec", toDoModel.getLimitDate().getSeconds());  // segundos del timestamp
            bundle.putInt("limitDateNano", toDoModel.getLimitDate().getNanoseconds()); // nanosegundos del timestamp
        }

        AddNewTask addNewTask = new AddNewTask();
        addNewTask.setArguments(bundle);

        addNewTask.show(fragment.getChildFragmentManager(), addNewTask.getTag());
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        int posicionElemento = position;
        // Colocar listener para escuchar cuando se toca el checkbox
        holder.mCheckBox.setOnCheckedChangeListener(null); // Para evitar el reciclado de la vista

        ToDoModel toDoModel = todoList.get(position);
        holder.mCheckBox.setText(toDoModel.getTask());
        holder.mCheckBox.setChecked(toBoolean(toDoModel.getStatus()));
        holder.mDueDateTv.setText("Programada en " + toDoModel.getDue());

        // Pintando el checkbox de un color diferente y quitando la fecha agendada si esta checkeado (= tarea completada)
        if(holder.mCheckBox.isChecked()) {
            holder.mDueDateTv.setText("Finalizada");
            holder.mConstraintLayout.setBackgroundColor(getContext().getResources().getColor(R.color.ligh_blue));
        }

        // Estableciendo el listener para el checkbox
        holder.mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (listener != null) {
                    listener.onItemClick(posicionElemento, isChecked); // Envía la posición y el estado del CheckBox
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

        ConstraintLayout mConstraintLayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            mConstraintLayout = itemView.findViewById(R.id.clTask);
            mDueDateTv = itemView.findViewById(R.id.tv_due_date_pendientes);
            mCheckBox = itemView.findViewById(R.id.cbPendiente);
        }
    }

    // Interfaces
    public interface OnItemClickListener {
        void onItemClick(int position, boolean isChecked);
    }

    public interface ToDoAdapterListener {
        void onUltimoElementoEliminado(boolean listaVacia);
    }

    public void setToDoAdapterListener(ToDoAdapterListener listener) {
        this.listenerRv = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

}
