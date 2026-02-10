package org.example.agendabd;

public class DireccionFila {
    private final String direccion;
    private final int id;
    public DireccionFila(int id,String direccion) {
        this.direccion = direccion;
        this.id = id;
    }
    public String getDireccion() {
        return direccion;
    }
    public int getId() {
        return id;
    }
}
