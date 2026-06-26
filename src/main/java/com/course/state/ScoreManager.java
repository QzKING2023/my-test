package com.course.state;

import com.course.pojo.PointObject;
import com.course.utils.FileUtils;
import com.course.utils.JsonUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 积分管理核心类，负责所有积分规则的计算与持久化。
 * 每条规则对应一个 static handleXxx() 方法，供 com.course.service 层调用。
 *
 * @author lixuy
 * Created on 2019-04-11
 */
public class ScoreManager {

    private static final String SCORE_FILE = "score";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ==================== 私有辅助方法 ====================

    /**
     * 从 score 文件读取 PointObject
     */
    private static PointObject readPointObject() {
        String content = FileUtils.readFile(SCORE_FILE);
        return JsonUtils.jsonToPojo(content, PointObject.class);
    }

    /**
     * 将 PointObject 写回 score 文件，自动重算 scoreTotal
     */
    private static void writePointObject(PointObject po) {
        po.setScoreTotal(nullSafe(po.getGrowScore()) + nullSafe(po.getExchangeScore()));
        String content = JsonUtils.objectToJson(po);
        FileUtils.writeFile(SCORE_FILE, content);
    }

    /**
     * 安全地将 Integer 转为 int，null 视为 0
     */
    private static int nullSafe(Integer val) {
        return val == null ? 0 : val;
    }

    // ==================== 成长积分规则 ====================

    /**
     * 规则1：登陆平台 — 1分，每日首次登陆获得
     */
    public static void handleLogin() {
        PointObject po = readPointObject();
        String today = LocalDate.now().format(DATE_FMT);

        if (po.getLastLoginDate() == null || !po.getLastLoginDate().equals(today)) {
            po.setGrowScore(nullSafe(po.getGrowScore()) + 1);
            po.setLastLoginDate(today);
            writePointObject(po);
            System.out.println("[Login] 每日首次登陆，成长积分 +1");
        } else {
            System.out.println("[Login] 今日已登陆，不重复积分");
        }
    }

    /**
     * 规则2：填写个人资料 — 2分，首次填写获得
     */
    public static void handleFillInformation() {
        PointObject po = readPointObject();

        if (po.getInfoFilled() == null || po.getInfoFilled() == 0) {
            po.setGrowScore(nullSafe(po.getGrowScore()) + 2);
            po.setInfoFilled(1);
            writePointObject(po);
            System.out.println("[FillInformation] 首次填写个人资料，成长积分 +2");
        } else {
            System.out.println("[FillInformation] 已填写过个人资料，不重复积分");
        }
    }

    /**
     * 规则3：填写血糖 — 1分，血糖记录数大于3时每次积1分
     */
    public static void handleBloodSugar() {
        PointObject po = readPointObject();
        int count = nullSafe(po.getBloodSugarCount()) + 1;
        po.setBloodSugarCount(count);

        if (count > 3) {
            po.setGrowScore(nullSafe(po.getGrowScore()) + 1);
            writePointObject(po);
            System.out.println("[BloodSugar] 血糖记录数=" + count + " (>3)，成长积分 +1");
        } else {
            writePointObject(po);
            System.out.println("[BloodSugar] 血糖记录数=" + count + " (≤3)，不积分");
        }
    }

    /**
     * 规则4：填写并发症记录 — 3分，每年只计分1次
     */
    public static void handleBfzNote() {
        PointObject po = readPointObject();
        int currentYear = LocalDate.now().getYear();

        if (po.getLastBfzNoteYear() == null || po.getLastBfzNoteYear() != currentYear) {
            po.setGrowScore(nullSafe(po.getGrowScore()) + 3);
            po.setLastBfzNoteYear(currentYear);
            writePointObject(po);
            System.out.println("[BfzNote] 本年度首次并发症记录，成长积分 +3");
        } else {
            System.out.println("[BfzNote] 本年度已记录过并发症，不重复积分");
        }
    }

    /**
     * 规则5：生成评估报告 — 2分，需已填写个人资料且血糖记录数≥10
     */
    public static void handleEvaluateReport() {
        PointObject po = readPointObject();
        int infoFilled = nullSafe(po.getInfoFilled());
        int bloodSugarCount = nullSafe(po.getBloodSugarCount());

        if (infoFilled == 1 && bloodSugarCount >= 10) {
            po.setGrowScore(nullSafe(po.getGrowScore()) + 2);
            writePointObject(po);
            System.out.println("[EvaluateReport] 条件满足(已填资料,血糖记录=" + bloodSugarCount + ")，成长积分 +2");
        } else {
            System.out.println("[EvaluateReport] 条件不满足(资料=" + (infoFilled == 1 ? "已填" : "未填")
                    + ",血糖记录=" + bloodSugarCount + ")，不积分");
        }
    }

    /**
     * 规则6：监测胰岛功能 — 2分，3个月只积分1次
     * 使用 ChronoUnit.MONTHS.between 计算完整月份差：
     * 例如 1月15日 → 4月14日 = 2个月（不积分），1月15日 → 4月15日 = 3个月（积分）
     */
    public static void handleYdgnNote() {
        PointObject po = readPointObject();
        LocalDate now = LocalDate.now();
        String lastDateStr = po.getLastYdgnNoteDate();

        if (lastDateStr == null) {
            // 首次监测
            po.setGrowScore(nullSafe(po.getGrowScore()) + 2);
            po.setLastYdgnNoteDate(now.format(DATE_FMT));
            writePointObject(po);
            System.out.println("[YdgnNote] 首次胰岛功能监测，成长积分 +2");
        } else {
            LocalDate lastDate = LocalDate.parse(lastDateStr);
            long monthsBetween = ChronoUnit.MONTHS.between(lastDate, now);
            if (monthsBetween >= 3) {
                po.setGrowScore(nullSafe(po.getGrowScore()) + 2);
                po.setLastYdgnNoteDate(now.format(DATE_FMT));
                writePointObject(po);
                System.out.println("[YdgnNote] 距上次" + monthsBetween + "个月，成长积分 +2");
            } else {
                System.out.println("[YdgnNote] 距上次" + monthsBetween + "个月(<3)，不积分");
            }
        }
    }

    // ==================== 可兑换积分规则 ====================

    /**
     * 规则7：完成门诊随访 — 3分，无限制
     */
    public static void handleFollowUp() {
        PointObject po = readPointObject();
        po.setExchangeScore(nullSafe(po.getExchangeScore()) + 3);
        writePointObject(po);
        System.out.println("[FollowUp] 完成门诊随访，可兑换积分 +3");
    }

    /**
     * 规则8：参加拓展活动 — 5分，无限制
     */
    public static void handleExtendedActivity() {
        PointObject po = readPointObject();
        po.setExchangeScore(nullSafe(po.getExchangeScore()) + 5);
        writePointObject(po);
        System.out.println("[ExtendedActivity] 参加拓展活动，可兑换积分 +5");
    }

    /**
     * 规则9：参加科研招募 — 8分，无限制
     */
    public static void handleResearchRecruitment() {
        PointObject po = readPointObject();
        po.setExchangeScore(nullSafe(po.getExchangeScore()) + 8);
        writePointObject(po);
        System.out.println("[ResearchRecruitment] 参加科研招募，可兑换积分 +8");
    }
}
