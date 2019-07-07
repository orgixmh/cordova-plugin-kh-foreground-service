package com.nks.khforeground;

public class SharedObject {
    //eventually provides setters and getters
    public String deviceName;
    public String deviceMac;
    //------------

    private static SharedObject instance = null;
    private void Container(){

    }
    public static SharedObject getInstance(){
        if(instance==null){
            instance = new SharedObject();
        }
        return instance;
    }

}





