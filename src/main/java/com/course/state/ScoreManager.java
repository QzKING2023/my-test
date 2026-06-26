package com.course.state;

import com.course.pojo.PointObject;
import com.course.utils.FileUtils;
import com.course.utils.JsonUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Central scoring engine.
 * <p>
 * All methods are static because the AOP interceptor
 * ({@code ScoreMethodInterceptor}) creates a new service instance per
 * controller call via {@code Class.newInstance()}.
 * </p>
 *
 * <p>File layout (both at project root):</p>
 * <ul>
 *   <li>{@code score} — {@link PointObject} JSON</li>
 *   <li>{@code score_state} — {@link ScoreState} JSON</li>
 * </ul>
 */
public final class ScoreManager {

    private static final String SCORE_FILE = "score";
    private static final String STATE_FILE = "score_state";

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    /** Lock for atomic read-modify-write cycles. */
    private static final Object LOCK = new Object();

    private ScoreManager() { /* utility class */ }

    // ── Public API used by service classes ──────────────────────────

    /** Login — 1 growth point, first login of the day only. */
    public static void handleLogin() {
        synchronized (LOCK) {
            ScoreState state = loadOrCreateState();
            PointObject  pt    = loadOrCreatePoint();
            String today = LocalDate.now().format(DATE_FMT);
            if (!today.equals(state.getLastLoginDate())) {
                pt.setGrowScore(nvl(pt.getGrowScore()) + 1);
                pt.setScoreTotal(nvl(pt.getScoreTotal()) + 1);
                state.setLastLoginDate(today);
                saveBoth(state, pt);
            }
        }
    }

    /** FillInformation — 2 growth points, first time only. */
    public static void handleFillInformation() {
        synchronized (LOCK) {
            ScoreState state = loadOrCreateState();
            PointObject  pt    = loadOrCreatePoint();
            if (!state.isPersonalInfoFilled()) {
                pt.setGrowScore(nvl(pt.getGrowScore()) + 2);
                pt.setScoreTotal(nvl(pt.getScoreTotal()) + 2);
                state.setPersonalInfoFilled(true);
                saveBoth(state, pt);
            }
        }
    }

    /** BloodSugar — record one entry; award 1 growth point when count exceeds 3 (one-time). */
    public static void handleBloodSugar() {
        synchronized (LOCK) {
            ScoreState state = loadOrCreateState();
            PointObject  pt    = loadOrCreatePoint();
            state.setBloodSugarCount(state.getBloodSugarCount() + 1);
            if (state.getBloodSugarCount() > 3 && !state.isBloodSugarPointAwarded()) {
                pt.setGrowScore(nvl(pt.getGrowScore()) + 1);
                pt.setScoreTotal(nvl(pt.getScoreTotal()) + 1);
                state.setBloodSugarPointAwarded(true);
            }
            saveBoth(state, pt);
        }
    }

    /** BfzNote (complications record) — 3 growth points, once per calendar year. */
    public static void handleBfzNote() {
        synchronized (LOCK) {
            ScoreState state = loadOrCreateState();
            PointObject  pt    = loadOrCreatePoint();
            int thisYear = LocalDate.now().getYear();
            if (thisYear != state.getBfzNoteYear()) {
                pt.setGrowScore(nvl(pt.getGrowScore()) + 3);
                pt.setScoreTotal(nvl(pt.getScoreTotal()) + 3);
                state.setBfzNoteYear(thisYear);
                saveBoth(state, pt);
            }
        }
    }

    /**
     * EvaluateReport — 2 growth points.
     * Requires: personal info filled AND blood-sugar record count {@code >= 10}.
     */
    public static void handleEvaluateReport() {
        synchronized (LOCK) {
            ScoreState state = loadOrCreateState();
            PointObject  pt    = loadOrCreatePoint();
            if (state.isPersonalInfoFilled() && state.getBloodSugarCount() >= 10) {
                pt.setGrowScore(nvl(pt.getGrowScore()) + 2);
                pt.setScoreTotal(nvl(pt.getScoreTotal()) + 2);
                saveBoth(state, pt);
            }
        }
    }

    /** YdgnNote (insulin monitoring) — 2 growth points, once every 3 months. */
    public static void handleYdgnNote() {
        synchronized (LOCK) {
            ScoreState state = loadOrCreateState();
            PointObject  pt    = loadOrCreatePoint();
            YearMonth now = YearMonth.now();
            boolean award = false;
            if (state.getYdgnNoteLastDate() == null || state.getYdgnNoteLastDate().isEmpty()) {
                award = true;                         // never done before
            } else {
                YearMonth last = YearMonth.parse(state.getYdgnNoteLastDate(), MONTH_FMT);
                if (ChronoUnit.MONTHS.between(last, now) >= 3) {
                    award = true;
                }
            }
            if (award) {
                pt.setGrowScore(nvl(pt.getGrowScore()) + 2);
                pt.setScoreTotal(nvl(pt.getScoreTotal()) + 2);
                state.setYdgnNoteLastDate(now.format(MONTH_FMT));
                saveBoth(state, pt);
            }
        }
    }

    // ── Exchangeable (unconditional) ─────────────────────────────────

    /** FollowUp (clinic follow-up) — 3 exchangeable points. */
    public static void handleFollowUp() {
        synchronized (LOCK) {
            PointObject pt = loadOrCreatePoint();
            pt.setExchangeScore(nvl(pt.getExchangeScore()) + 3);
            pt.setScoreTotal(nvl(pt.getScoreTotal()) + 3);
            savePoint(pt);
        }
    }

    /** ExtendedActivity — 5 exchangeable points. */
    public static void handleExtendedActivity() {
        synchronized (LOCK) {
            PointObject pt = loadOrCreatePoint();
            pt.setExchangeScore(nvl(pt.getExchangeScore()) + 5);
            pt.setScoreTotal(nvl(pt.getScoreTotal()) + 5);
            savePoint(pt);
        }
    }

    /** ResearchRecruitment — 8 exchangeable points. */
    public static void handleResearchRecruitment() {
        synchronized (LOCK) {
            PointObject pt = loadOrCreatePoint();
            pt.setExchangeScore(nvl(pt.getExchangeScore()) + 8);
            pt.setScoreTotal(nvl(pt.getScoreTotal()) + 8);
            savePoint(pt);
        }
    }

    // ── File I/O helpers ─────────────────────────────────────────────

    private static ScoreState loadOrCreateState() {
        String json = FileUtils.readFile(STATE_FILE);
        if (json != null && !json.isEmpty()) {
            ScoreState s = JsonUtils.jsonToPojo(json, ScoreState.class);
            if (s != null) return s;
        }
        return new ScoreState();
    }

    private static PointObject loadOrCreatePoint() {
        String json = FileUtils.readFile(SCORE_FILE);
        if (json != null && !json.isEmpty()) {
            PointObject p = JsonUtils.jsonToPojo(json, PointObject.class);
            if (p != null) return p;
        }
        // initialise with zeros
        PointObject p = new PointObject();
        p.setId(1);
        p.setGrowScore(0);
        p.setExchangeScore(0);
        p.setScoreTotal(0);
        return p;
    }

    private static void savePoint(PointObject pt) {
        FileUtils.writeFile(SCORE_FILE, JsonUtils.objectToJson(pt));
    }

    private static void saveBoth(ScoreState state, PointObject pt) {
        FileUtils.writeFile(STATE_FILE, JsonUtils.objectToJson(state));
        FileUtils.writeFile(SCORE_FILE,  JsonUtils.objectToJson(pt));
    }

    /** Null-safe unwrap: returns 0 for null Integer. */
    private static int nvl(Integer v) {
        return v == null ? 0 : v;
    }
}
