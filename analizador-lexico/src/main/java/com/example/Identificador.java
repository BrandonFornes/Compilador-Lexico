package com.example;

public class Identificador {
    public String id;
    public String tipo;
    public String clase;
    public int ambito;
    
    public Integer  tArr;
    public Integer  dimensionArr;
    public Integer  numeroPar;
    public String  tamañoPar;

    public Identificador(String id, String tipo, String clase, int ambito) {
        this.id = id;
        this.tipo = tipo;
        this.clase = clase;
        this.ambito = ambito;
    }

    @Override
    public String toString() {
        return "Identificador {" +
                "id='" + id + '\'' +
                ", tipo='" + tipo + '\'' +
                ", clase='" + clase + '\'' +
                ", ambito=" + ambito +
                ", tArr=" + tArr +
                ", dimensionArr=" + dimensionArr +
                ", numeroPar=" + numeroPar +
                ", tamañoPar=" + tamañoPar +
                '}';
    }
}
