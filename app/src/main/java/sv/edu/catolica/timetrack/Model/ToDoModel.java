package sv.edu.catolica.timetrack.Model;

import com.google.firebase.Timestamp;

public class ToDoModel extends TaskId {
    private String task, due, type;
    private Timestamp limitDate;
    private int status;

    public String getTask() {
        return task;
    }

    public String getDue() {
        return due;
    }

    public int getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public Timestamp getLimitDate() {
        return limitDate;
    }
}
