package com.course.service;

import com.course.state.ScoreManager;

/**
 * @author lixuy
 * Created on 2019-04-11
 */
//类名与方法名须与controller层拦截的方法一致
public class YdgnNote {

    public void ydgnNote(){
        ScoreManager.handleYdgnNote();
        System.out.println("+++++ydgnNote积分计算方法执行+++++");
    }

}
