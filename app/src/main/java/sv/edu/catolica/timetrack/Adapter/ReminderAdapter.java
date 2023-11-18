package sv.edu.catolica.timetrack.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import sv.edu.catolica.timetrack.Model.ReminderModel;
import sv.edu.catolica.timetrack.Model.ToDoModel;
import sv.edu.catolica.timetrack.R;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.MyViewHolder> {
    private List<ReminderModel> reminderList;
    private FragmentActivity activity;
    private FirebaseFirestore firestore;
    private String usuarioId;
    private FirebaseAuth mAuth;
    private OnItemClickListener listener;

    public ReminderAdapter(FragmentActivity activity, List<ReminderModel> reminderList) {
        this.reminderList = reminderList;
        this.activity = activity;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.each_reminder, parent, false);

        // Para tener acceso a la db del usuario
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        usuarioId = currentUser.getUid();  // id del usuario para identificar su db

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        int posicionElemento = position;

        holder.mCvRecordatorio.setOnClickListener(null); //  // Para evitar el reciclado de la vista

        ReminderModel reminderModel = reminderList.get(posicionElemento);
        holder.mTvTitulo.setText(reminderModel.getTitulo());
        holder.mTvFecha.setText("Agendado para el " + reminderModel.getFecha());
        holder.mTvTipo.setText(reminderModel.getTipo());

        // Estableciendo el string del recordatorio
        String notificacionInfo = "";
        if (reminderModel.getHoraNoti().isEmpty()) {
            notificacionInfo = "No hay recordatorio programado";
        } else {
            notificacionInfo = "Recordarme el " + reminderModel.getHoraNoti();
        }
        holder.mTvNoti.setText(notificacionInfo);

        holder.mCvRecordatorio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onItemClick(posicionElemento);
                }
            }
        });
    }


    @Override
    public int getItemCount() {
        return reminderList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        CardView mCvRecordatorio;
        TextView mTvTitulo, mTvFecha, mTvTipo, mTvNoti;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            mCvRecordatorio = itemView.findViewById(R.id.cvRecordatorio);
            mTvTitulo = itemView.findViewById(R.id.tvTituloRecordatorio);
            mTvFecha = itemView.findViewById(R.id.tvFechaAgendada);
            mTvTipo = itemView.findViewById(R.id.tvTipoActividad);
            mTvNoti = itemView.findViewById(R.id.tvHoraNotificacion);
        }
    }

    // Interfaz
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}
