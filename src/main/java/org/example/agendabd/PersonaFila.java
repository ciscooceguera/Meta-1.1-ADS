package org.example.agendabd;

public class PersonaFila {
    private final int id;
    private final String nombre, direccion;

    public PersonaFila(int id, String nombre, String direccion) {
        this.id = id;
        this.nombre = nombre;
        this.direccion = direccion;
    }
    public int getId() {
        return id;
    }
    public String getNombre() {
        return nombre;
    }
    public String getDireccion() {
        return direccion;
    }
}
