package com.example.demo1.Patrones;

import java.util.ArrayList;
import java.util.List;

public abstract class Sujeto {

    private final List<Observador> observadores = new ArrayList<>();

    public void attach(Observador o) {
        if (o != null && !observadores.contains(o)) {
            observadores.add(o);
        }
    }

    public void detach(Observador o) {
        observadores.remove(o);
    }

    protected void notificarObservadores(String evento, Object datos) {
        for (Observador o : observadores) {
            o.actualizar(evento, datos);
        }
    }
}
