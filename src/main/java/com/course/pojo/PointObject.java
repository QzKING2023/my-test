package com.course.pojo;

import java.io.Serializable;

/**
 * @author lixuy
 * Created on 2019-04-10
 */
public class PointObject implements Serializable{

    private static final long serialVersionUID = 123456789L;

    private Integer id;
    //成长积分数
    private Integer growScore;
    //可兑换积分数
    private Integer exchangeScore;
    //总积分数
    private Integer scoreTotal;

    // ===== 积分规则追踪字段 =====
    // 最后登陆日期 "yyyy-MM-dd"，用于每日首次登陆判断
    private String lastLoginDate;
    // 是否已填写个人资料 (null或0=未填, 1=已填)
    private Integer infoFilled;
    // 血糖记录总数
    private Integer bloodSugarCount;
    // 最后并发症记录年份
    private Integer lastBfzNoteYear;
    // 最后胰岛功能监测日期 "yyyy-MM-dd"
    private String lastYdgnNoteDate;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getGrowScore() {
        return growScore;
    }

    public void setGrowScore(Integer growScore) {
        this.growScore = growScore;
    }

    public Integer getExchangeScore() {
        return exchangeScore;
    }

    public void setExchangeScore(Integer exchangeScore) {
        this.exchangeScore = exchangeScore;
    }

    public Integer getScoreTotal() {
        return scoreTotal;
    }

    public void setScoreTotal(Integer scoreTotal) {
        this.scoreTotal = scoreTotal;
    }

    public String getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(String lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public Integer getInfoFilled() {
        return infoFilled;
    }

    public void setInfoFilled(Integer infoFilled) {
        this.infoFilled = infoFilled;
    }

    public Integer getBloodSugarCount() {
        return bloodSugarCount;
    }

    public void setBloodSugarCount(Integer bloodSugarCount) {
        this.bloodSugarCount = bloodSugarCount;
    }

    public Integer getLastBfzNoteYear() {
        return lastBfzNoteYear;
    }

    public void setLastBfzNoteYear(Integer lastBfzNoteYear) {
        this.lastBfzNoteYear = lastBfzNoteYear;
    }

    public String getLastYdgnNoteDate() {
        return lastYdgnNoteDate;
    }

    public void setLastYdgnNoteDate(String lastYdgnNoteDate) {
        this.lastYdgnNoteDate = lastYdgnNoteDate;
    }
}
