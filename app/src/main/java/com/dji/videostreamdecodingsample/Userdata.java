package com.dji.videostreamdecodingsample;

public class Userdata{
    private static Userdata instace = new Userdata();

    public String _id;
    public String _pw;
  //추가
//24d2bd1d-5617-46cd-a49c-e1ecbb69fc4d
    public int _code;
    public String _login_key1 = "6ce55a01-67e7-460b-afe1-648ebf9ea187";
    public String _login_key2 = "71ac032e-3780-49d1-8dd4-8e64e8e81ff4";
    public String _login_key;
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
    //public String _profile_photo;

    public String _server_url="https://101.55.28.64:444";
    //public String _operatingserver_url="https://rms.gg.go.kr";
    public int _selected_drone;

    private Userdata(){}

    public static Userdata getInstance(){
        return instace;
    }

}
