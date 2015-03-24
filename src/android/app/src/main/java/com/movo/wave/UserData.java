package com.movo.wave;

/**
 * Created by PhilG on 3/24/2015.
 */
public class UserData {
    private static UserData instance;

    private String currentUID;
    private String currentToken;


    public static UserData getUserData(){
        if(instance == null){
            instance = new UserData();
        }
        return instance;
    }



    private UserData(){
        currentUID = "";
        currentToken = "";
    }

    public String getCurUID(){
        return currentUID;
    }

    public String getCurToken(){
        return currentToken;
    }

    public String setCurUID(String input){
        currentUID = input;
        return currentUID;
    }

    public String setCurToken(String input){
        currentToken = input;
        return currentToken;
    }



}
