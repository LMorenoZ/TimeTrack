package sv.edu.catolica.timetrack.Class;

import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;
import sv.edu.catolica.timetrack.Adapter.ToDoAdapter;
import sv.edu.catolica.timetrack.Interfaces.OnDialogCloseListener;
import sv.edu.catolica.timetrack.PendientesFragment;
import sv.edu.catolica.timetrack.R;

public class TouchHelper extends ItemTouchHelper.SimpleCallback {
    private ToDoAdapter adapter;
    private PendientesFragment fragment;

    public TouchHelper(ToDoAdapter adapter, PendientesFragment fragment) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        this.fragment = fragment;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        final int position = viewHolder.getAdapterPosition();

        if (direction == ItemTouchHelper.RIGHT) {
            AlertDialog.Builder builder = new AlertDialog.Builder(adapter.getContext());
            builder.setMessage("¿Estás seguro?")
                    .setTitle("Borrar actividad")
                    .setPositiveButton("Borrar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            adapter.deleteTask(position);
                        }
                    }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {  }
                    }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            adapter.notifyItemChanged(position);
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            adapter.editTask(position, fragment);
            
//            adapter.notifyItemChanged(position);
        }
    }
    
    

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                .addSwipeRightActionIcon(R.drawable.ic_delete_24)
                .addSwipeRightBackgroundColor(Color.RED)
                .addSwipeLeftActionIcon(R.drawable.ic_edit_24)
                .addSwipeLeftBackgroundColor(ContextCompat.getColor(adapter.getContext(), R.color.lavender))
                .create()
                .decorate();

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
