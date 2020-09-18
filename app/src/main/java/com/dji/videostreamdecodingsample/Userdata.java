package com.dji.videostreamdecodingsample;

public class Userdata{
    private static Userdata instace = new Userdata();

    public String _id;
    public String _pw;

    private Userdata(){}

    public static Userdata getInstance(){
        return instace;
    }
}
