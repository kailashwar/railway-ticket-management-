package com.railbook.service;

import com.railbook.model.Passenger;

import java.util.List;
import java.util.Random;

public class BerthAllocationService {
    private static final Random RANDOM = new Random();

    /**
     * Applies Indian Railways Senior Citizen Berth Preference Logic:
     * If Age > 60, automatically allocate Lower Berth / Down Seat.
     * Evaluates individually for every passenger in the booking.
     */
    public static void allocateBerths(List<Passenger> passengers, String travelClass) {
        String coachPrefix;
        if (travelClass.contains("AC 1") || travelClass.contains("Executive")) {
            coachPrefix = "H1";
        } else if (travelClass.contains("AC 2")) {
            coachPrefix = "A1";
        } else if (travelClass.contains("AC 3")) {
            coachPrefix = "B2";
        } else if (travelClass.contains("Chair Car") || travelClass.contains("CC")) {
            coachPrefix = "C1";
        } else {
            coachPrefix = "S3"; // Sleeper default
        }

        int seatCounter = 12 + RANDOM.nextInt(35);

        for (Passenger p : passengers) {
            p.setCoach(coachPrefix);

            // Age > 60: Senior Citizen check
            if (p.getAge() > 60) {
                p.setSeniorCitizen(true);
                p.setBerthPreference("Lower");
                // Assign a lower berth seat number (typically 1, 4, 9, 12, 17, 20, etc. in 8-berth bay)
                int lowerSeatNum = (seatCounter / 8) * 8 + 4;
                p.setSeat(lowerSeatNum + " (Lower Berth)");
            } else {
                p.setSeniorCitizen(false);
                String pref = p.getBerthPreference();
                if (pref == null || pref.isEmpty() || "No Preference".equalsIgnoreCase(pref)) {
                    pref = getRandomBerth();
                    p.setBerthPreference(pref);
                }
                p.setSeat(seatCounter + " (" + pref + " Berth)");
            }
            seatCounter++;
        }
    }

    private static String getRandomBerth() {
        String[] berths = {"Lower", "Middle", "Upper", "Side Lower", "Side Upper"};
        return berths[RANDOM.nextInt(berths.length)];
    }
}
