package com.course.state;

/**
 * Tracks per-user state for enforcing point-award constraints.
 * Persisted as JSON alongside the score file.
 *
 * @author generated
 */
public class ScoreState {

    /** Last login date in "yyyy-MM-dd" format. Empty string means never logged in. */
    private String lastLoginDate = "";

    /** Cumulative count of blood sugar records filled. */
    private int bloodSugarCount;

    /** Whether the one-time blood sugar point (>3 records) has been awarded. */
    private boolean bloodSugarPointAwarded;

    /** Whether the user has filled in personal information. */
    private boolean personalInfoFilled;

    /** The calendar year (e.g. 2026) in which complications were last recorded. 0 = never. */
    private int bfzNoteYear;

    /** Last insulin monitoring date in "yyyy-MM" format. Empty string means never done. */
    private String ydgnNoteLastDate = "";

    // ── getters / setters ────────────────────────────────────────────

    public String getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(String lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public int getBloodSugarCount() {
        return bloodSugarCount;
    }

    public void setBloodSugarCount(int bloodSugarCount) {
        this.bloodSugarCount = bloodSugarCount;
    }

    public boolean isBloodSugarPointAwarded() {
        return bloodSugarPointAwarded;
    }

    public void setBloodSugarPointAwarded(boolean bloodSugarPointAwarded) {
        this.bloodSugarPointAwarded = bloodSugarPointAwarded;
    }

    public boolean isPersonalInfoFilled() {
        return personalInfoFilled;
    }

    public void setPersonalInfoFilled(boolean personalInfoFilled) {
        this.personalInfoFilled = personalInfoFilled;
    }

    public int getBfzNoteYear() {
        return bfzNoteYear;
    }

    public void setBfzNoteYear(int bfzNoteYear) {
        this.bfzNoteYear = bfzNoteYear;
    }

    public String getYdgnNoteLastDate() {
        return ydgnNoteLastDate;
    }

    public void setYdgnNoteLastDate(String ydgnNoteLastDate) {
        this.ydgnNoteLastDate = ydgnNoteLastDate;
    }
}
