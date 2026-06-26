package com.course.service;

import com.course.state.ScoreManager;

/**
 * @author lixuy
 * Created on 2019-04-11
 */
//类名与方法名须与controller层拦截的方法一致
public class BloodSugar {

    public void bloodSugar(){
        ScoreManager.handleBloodSugar();
        System.out.println("+++++bloodSugar积分计算方法执行+++++");
    }

}
