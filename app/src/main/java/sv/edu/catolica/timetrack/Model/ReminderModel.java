package sv.edu.catolica.timetrack.Model;

public class ReminderModel extends TaskId{
    private String titulo, fecha, tipo, horaNoti;

    public String getTitulo() {
        return titulo;
    }

    public String getFecha() {
        return fecha;
    }

    public String getTipo() {
        return tipo;
    }

    public String getHoraNoti() {
        return horaNoti;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public void setHoraNoti(String horaNoti) {
        this.horaNoti = horaNoti;
    }
}
