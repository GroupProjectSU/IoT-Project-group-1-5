package com.example.smartlight;

import java.util.ArrayList;

public class OverlappingTimeintervallChecker {
    public static boolean checkForConflictingPreferences(ArrayList<TimeInterval> existingIntervals, TimeInterval newInterval) {
        for (TimeInterval existingInterval : existingIntervals) {
            if (isTimeOverlap(existingInterval, newInterval)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTimeOverlap(TimeInterval interval1, TimeInterval interval2) {
        //convert time-stamps to minutes since midnight
        int start1 = interval1.getStartHour() * 60 + interval1.getStartMinute();
        int end1 = interval1.getEndHour() * 60 + interval1.getEndMinute();
        int start2 = interval2.getStartHour() * 60 + interval2.getStartMinute();
        int end2 = interval2.getEndHour() * 60 + interval2.getEndMinute();

        //handle intervals that cross midnight so that interval with start times that is pre-midnight and end-times that are post midnight make sense.
        if (end1 <= start1) end1 += 24 * 60;
        if (end2 <= start2) end2 += 24 * 60;

        //check if one time interval (1) is within one day and the other interval (2) spans across two days
        if (end2 > 24 * 60 && start1 < end1) {
            if (start1 < (end2 - 24 * 60)) return true;
            if (end1 > start2 + 24 * 60) return true;
        }

        //check if time interval 2 is within one day and time interval 1 spans across two days
        if (end1 > 24 * 60 && start2 < end2) {
            if (start2 < (end1 - 24 * 60)) return true;
            if (end2 > start1 + 24 * 60) return true;
        }

        //standard time overlap (check for intervals within the same day)
        return start1 < end2 && start2 < end1;
    }
}
