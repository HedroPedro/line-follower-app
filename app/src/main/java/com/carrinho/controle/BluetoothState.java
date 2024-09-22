package com.carrinho.controle;

public class BluetoothState {
    public static final BluetoothState NOT_ENABLED = new BluetoothState(0, "Habilitar");
    public static final BluetoothState ENABLED = new BluetoothState(1, "Parear");
    public static final BluetoothState CONNECTED = new BluetoothState(2, "Desconectar");

    private int id;
    private String msg;

    private BluetoothState(int id, String msg){
        this.id = id;
        this.msg = msg;
    }

    public int getId(){
        return id;
    }

    public String getMsg(){
        return msg;
    }
}
