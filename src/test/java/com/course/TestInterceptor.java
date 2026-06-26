package com.course;

import com.course.controller.*;
import com.course.pojo.PointObject;
import com.course.utils.FileUtils;
import com.course.utils.JsonUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * AOP 拦截器 + 积分规则集成测试
 *
 * @author lixuy
 * Created on 2019-04-10
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring/*.xml"})
public class TestInterceptor {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ==================== 注入全部 Controller ====================

    @Autowired
    TestDesign testDesign;

    @Autowired
    Login login;

    @Autowired
    FillInformation fillInformation;

    @Autowired
    BloodSugar bloodSugar;

    @Autowired
    BfzNote bfzNote;

    @Autowired
    EvaluateReport evaluateReport;

    @Autowired
    YdgnNote ydgnNote;

    @Autowired
    FollowUp followUp;

    @Autowired
    ExtendedActivity extendedActivity;

    @Autowired
    ResearchRecruitment researchRecruitment;

    // ==================== 辅助方法 ====================

    /**
     * 重置 score 文件为干净初始状态
     */
    private void resetScoreFile() {
        PointObject po = new PointObject();
        po.setId(1);
        po.setGrowScore(0);
        po.setExchangeScore(0);
        po.setScoreTotal(0);
        String json = JsonUtils.objectToJson(po);
        FileUtils.writeFile("score", json);
    }

    /**
     * 从 score 文件读取当前 PointObject
     */
    private PointObject readCurrentState() {
        String content = FileUtils.readFile("score");
        return JsonUtils.jsonToPojo(content, PointObject.class);
    }

    /**
     * 将自定义 PointObject 写入 score 文件（用于构造特定前置状态）
     */
    private void writeState(PointObject po) {
        po.setScoreTotal(
                (po.getGrowScore() == null ? 0 : po.getGrowScore())
                        + (po.getExchangeScore() == null ? 0 : po.getExchangeScore()));
        String json = JsonUtils.objectToJson(po);
        FileUtils.writeFile("score", json);
    }

    // 兼容旧测试的辅助方法
    private int assertScore() {
        try {
            String file = FileUtils.readFile("score");
            PointObject pointObject = JsonUtils.jsonToPojo(file, PointObject.class);
            System.out.println("成长积分：" + pointObject.getGrowScore());
            System.out.println("可交换积分：" + pointObject.getExchangeScore());
            System.out.println("总积分：" + pointObject.getScoreTotal());
            return pointObject.getScoreTotal();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ==================== 生命周期 ====================

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        System.out.println("this is setUpBeforeClass...");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        System.out.println("this is tearDownAfterClass...");
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("this is setUp...");
        resetScoreFile();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("this is tearDown...");
    }

    // ==================== 原有测试：保持兼容 ====================

    @Test
    public void testDesign() {
        try {
            int score1 = assertScore();
            testDesign.testDesign();
            int score2 = assertScore();
            assertEquals(1, score2 - score1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 成长积分规则测试 ====================

    /**
     * 规则1：登陆平台 — 1分，每日首次登陆获得
     */
    @Test
    public void testLogin() {
        // 首次登陆，应获得积分
        login.login();
        PointObject po = readCurrentState();
        assertEquals(Integer.valueOf(1), po.getGrowScore());
        assertEquals(LocalDate.now().format(DATE_FMT), po.getLastLoginDate());
        assertEquals(Integer.valueOf(1), po.getScoreTotal());

        // 同日再次登陆，不应再获得积分
        login.login();
        po = readCurrentState();
        assertEquals(Integer.valueOf(1), po.getGrowScore());

        // 模拟昨天登陆过，今天再次登陆应获得积分
        PointObject yesterdayState = new PointObject();
        yesterdayState.setId(1);
        yesterdayState.setGrowScore(1);
        yesterdayState.setExchangeScore(0);
        yesterdayState.setLastLoginDate(LocalDate.now().minusDays(1).format(DATE_FMT));
        writeState(yesterdayState);

        login.login();
        po = readCurrentState();
        assertEquals(Integer.valueOf(2), po.getGrowScore());
    }

    /**
     * 规则2：填写个人资料 — 2分，首次填写获得
     */
    @Test
    public void testFillInformation() {
        // 首次填写，应获得2分
        fillInformation.fillInformation();
        PointObject po = readCurrentState();
        assertEquals(Integer.valueOf(2), po.getGrowScore());
        assertEquals(Integer.valueOf(1), po.getInfoFilled());

        // 再次填写，不应再获得积分
        fillInformation.fillInformation();
        po = readCurrentState();
        assertEquals(Integer.valueOf(2), po.getGrowScore());
    }

    /**
     * 规则3：填写血糖 — 1分，血糖记录数大于3时每次积1分
     */
    @Test
    public void testBloodSugar() {
        // 前3次记录，不积分
        for (int i = 1; i <= 3; i++) {
            bloodSugar.bloodSugar();
            PointObject po = readCurrentState();
            assertEquals("第" + i + "次血糖记录不应积分", Integer.valueOf(0), po.getGrowScore());
            assertEquals("血糖记录数应为" + i, Integer.valueOf(i), po.getBloodSugarCount());
        }

        // 第4次记录，开始积分
        bloodSugar.bloodSugar();
        PointObject po = readCurrentState();
        assertEquals(Integer.valueOf(1), po.getGrowScore());
        assertEquals(Integer.valueOf(4), po.getBloodSugarCount());

        // 第5次记录，继续积分
        bloodSugar.bloodSugar();
        po = readCurrentState();
        assertEquals(Integer.valueOf(2), po.getGrowScore());
        assertEquals(Integer.valueOf(5), po.getBloodSugarCount());
    }

    /**
     * 规则4：填写并发症记录 — 3分，每年只计分1次
     */
    @Test
    public void testBfzNote() {
        int currentYear = LocalDate.now().getYear();

        // 首次记录，应获得3分
        bfzNote.bfzNote();
        PointObject po = readCurrentState();
        assertEquals(Integer.valueOf(3), po.getGrowScore());
        assertEquals(Integer.valueOf(currentYear), po.getLastBfzNoteYear());

        // 同年再次记录，不应再获得积分
        bfzNote.bfzNote();
        po = readCurrentState();
        assertEquals(Integer.valueOf(3), po.getGrowScore());

        // 模拟去年记录过，今年再次记录应获得积分
        PointObject lastYearState = new PointObject();
        lastYearState.setId(1);
        lastYearState.setGrowScore(3);
        lastYearState.setExchangeScore(0);
        lastYearState.setLastBfzNoteYear(currentYear - 1);
        writeState(lastYearState);

        bfzNote.bfzNote();
        po = readCurrentState();
        assertEquals(Integer.valueOf(6), po.getGrowScore());
        assertEquals(Integer.valueOf(currentYear), po.getLastBfzNoteYear());
    }

    /**
     * 规则5：生成评估报告 — 2分，需已填写个人资料且血糖记录数≥10
     */
    @Test
    public void testEvaluateReport() {
        // 条件不满足（资料未填，血糖不足），不积分
        evaluateReport.evaluateReport();
        PointObject po = readCurrentState();
        assertEquals(Integer.valueOf(0), po.getGrowScore());

        // 仅填写资料，血糖不足，不积分
        PointObject state1 = new PointObject();
        state1.setId(1);
        state1.setGrowScore(0);
        state1.setExchangeScore(0);
        state1.setInfoFilled(1);
        state1.setBloodSugarCount(5);
        writeState(state1);

        evaluateReport.evaluateReport();
        po = readCurrentState();
        assertEquals(Integer.valueOf(0), po.getGrowScore());

        // 资料已填，血糖记录≥10，应积分
        PointObject state2 = new PointObject();
        state2.setId(1);
        state2.setGrowScore(0);
        state2.setExchangeScore(0);
        state2.setInfoFilled(1);
        state2.setBloodSugarCount(10);
        writeState(state2);

        evaluateReport.evaluateReport();
        po = readCurrentState();
        assertEquals(Integer.valueOf(2), po.getGrowScore());

        // 再次评估（条件仍满足），仍可积分
        evaluateReport.evaluateReport();
        po = readCurrentState();
        assertEquals(Integer.valueOf(4), po.getGrowScore());
    }

    /**
     * 规则6：监测胰岛功能 — 2分，3个月只积分1次
     */
    @Test
    public void testYdgnNote() {
        // 首次监测，应获得2分
        ydgnNote.ydgnNote();
        PointObject po = readCurrentState();
        assertEquals(Integer.valueOf(2), po.getGrowScore());
        assertNotNull(po.getLastYdgnNoteDate());
        assertEquals(Integer.valueOf(2), po.getScoreTotal());

        // 短期内再次监测，不应再获得积分
        ydgnNote.ydgnNote();
        po = readCurrentState();
        assertEquals(Integer.valueOf(2), po.getGrowScore());

        // 模拟4个月前监测过，再次监测应获得积分
        String fourMonthsAgo = LocalDate.now().minusMonths(4).format(DATE_FMT);
        PointObject oldState = new PointObject();
        oldState.setId(1);
        oldState.setGrowScore(2);
        oldState.setExchangeScore(0);
        oldState.setLastYdgnNoteDate(fourMonthsAgo);
        writeState(oldState);

        ydgnNote.ydgnNote();
        po = readCurrentState();
        assertEquals(Integer.valueOf(4), po.getGrowScore());
        assertEquals(LocalDate.now().format(DATE_FMT), po.getLastYdgnNoteDate());
    }

    // ==================== 可兑换积分规则测试 ====================

    /**
     * 规则7：完成门诊随访 — 3分，无限制
     */
    @Test
    public void testFollowUp() {
        // 第一次随访
        followUp.followUp();
        PointObject po = readCurrentState();
        assertEquals(Integer.valueOf(3), po.getExchangeScore());
        assertEquals(Integer.valueOf(3), po.getScoreTotal());

        // 第二次随访，继续积分（无限制）
        followUp.followUp();
        po = readCurrentState();
        assertEquals(Integer.valueOf(6), po.getExchangeScore());
        assertEquals(Integer.valueOf(6), po.getScoreTotal());
    }

    /**
     * 规则8：参加拓展活动 — 5分，无限制
     */
    @Test
    public void testExtendedActivity() {
        extendedActivity.extendedActivity();
        PointObject po = readCurrentState();
        assertEquals(Integer.valueOf(5), po.getExchangeScore());
        assertEquals(Integer.valueOf(5), po.getScoreTotal());

        // 再次参加，继续积分
        extendedActivity.extendedActivity();
        po = readCurrentState();
        assertEquals(Integer.valueOf(10), po.getExchangeScore());
    }

    /**
     * 规则9：参加科研招募 — 8分，无限制
     */
    @Test
    public void testResearchRecruitment() {
        researchRecruitment.researchRecruitment();
        PointObject po = readCurrentState();
        assertEquals(Integer.valueOf(8), po.getExchangeScore());
        assertEquals(Integer.valueOf(8), po.getScoreTotal());

        // 再次参加，继续积分
        researchRecruitment.researchRecruitment();
        po = readCurrentState();
        assertEquals(Integer.valueOf(16), po.getExchangeScore());
    }

    // ==================== 综合测试 ====================

    /**
     * 总分一致性：scoreTotal 始终等于 growScore + exchangeScore
     */
    @Test
    public void testTotalScoreConsistency() {
        // 执行一系列操作
        login.login();               // growScore +1
        fillInformation.fillInformation(); // growScore +2
        followUp.followUp();         // exchangeScore +3
        extendedActivity.extendedActivity(); // exchangeScore +5

        PointObject po = readCurrentState();
        int expectedTotal = nullSafe(po.getGrowScore()) + nullSafe(po.getExchangeScore());
        assertEquals("总分应等于成长积分+可兑换积分",
                Integer.valueOf(expectedTotal), po.getScoreTotal());
    }

    /**
     * 混合场景：成长积分和可兑换积分互不影响
     */
    @Test
    public void testMixedOperations() {
        // 执行成长积分操作
        login.login();
        fillInformation.fillInformation();

        PointObject po = readCurrentState();
        assertEquals(Integer.valueOf(3), po.getGrowScore());   // 1+2
        assertEquals(Integer.valueOf(0), po.getExchangeScore());
        assertEquals(Integer.valueOf(3), po.getScoreTotal());

        // 执行可兑换积分操作
        followUp.followUp();
        researchRecruitment.researchRecruitment();

        po = readCurrentState();
        assertEquals(Integer.valueOf(3), po.getGrowScore());   // 不变
        assertEquals(Integer.valueOf(11), po.getExchangeScore()); // 3+8
        assertEquals(Integer.valueOf(14), po.getScoreTotal());
    }

    // ==================== 辅助 ====================

    private int nullSafe(Integer val) {
        return val == null ? 0 : val;
    }
}
