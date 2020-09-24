package com.dji.videostreamdecodingsample;

public class Userdata{
    private static Userdata instace = new Userdata();

    public String _id;
    public String _pw;

    public int _code;
    public String _message;
    public String _room_key;
    public String _caller_id;
    public String _room_id;
    public String _title;
    public String _reservation_yn;
    public String _reservation_date;
    public String _reservation_time;
    public String _status;
    public String _reg_date;
    public String _start_date;
    public String _name;
    public String _profile_photo;

    private Userdata(){}

    public static Userdata getInstance(){
        return instace;
    }

}
