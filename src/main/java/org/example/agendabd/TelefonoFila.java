package org.example.agendabd;

public class TelefonoFila {
    private final int id;
    private final int personaID;
    private final String telefono;

    public TelefonoFila(int id, int personaID, String telefono) {
        this.id = id;
        this.personaID = personaID;
        this.telefono = telefono;
    }
    public int getId() {
        return id;
    }
    public int getPersonaId() {
        return personaID;
    }
    public String getTelefono() {
        return telefono;
    }
}
