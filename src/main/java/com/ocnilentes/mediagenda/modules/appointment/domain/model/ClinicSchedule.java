package com.ocnilentes.mediagenda.modules.appointment.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ClinicSchedule {

    private static final int SLOT_MINUTES = 20;

    private ClinicSchedule() {
    }

    public static List<LocalTime> slotsFor(Consultorio consultorio) {
        return slotsFor(consultorio, null);
    }

    public static List<LocalTime> slotsFor(Consultorio consultorio, LocalDate date) {
        boolean saturdaySharedLunch = date != null && date.getDayOfWeek() == DayOfWeek.SATURDAY;
        return switch (consultorio) {
            case A -> merge(
                    generateSlots(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                    generateSlots(LocalTime.of(13, 0), LocalTime.of(17, 0))
            );
            case B -> {
                if (saturdaySharedLunch) {
                    yield merge(
                            generateSlots(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                            generateSlots(LocalTime.of(13, 0), LocalTime.of(17, 0))
                    );
                }
                yield merge(
                        generateSlots(LocalTime.of(9, 0), LocalTime.of(13, 0)),
                        generateSlots(LocalTime.of(14, 0), LocalTime.of(18, 0))
                );
            }
        };
    }

    public static Map<Consultorio, List<LocalTime>> allSlotsByConsultorio() {
        Map<Consultorio, List<LocalTime>> slots = new EnumMap<>(Consultorio.class);
        for (Consultorio consultorio : Consultorio.values()) {
            slots.put(consultorio, slotsFor(consultorio));
        }
        return slots;
    }

    public static boolean offers(Consultorio consultorio, LocalTime time) {
        return offers(consultorio, null, time);
    }

    public static boolean offers(Consultorio consultorio, LocalDate date, LocalTime time) {
        return slotsFor(consultorio, date).contains(time);
    }

    private static List<LocalTime> generateSlots(LocalTime startInclusive, LocalTime endExclusive) {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime current = startInclusive;
        while (current.isBefore(endExclusive)) {
            slots.add(current);
            current = current.plusMinutes(SLOT_MINUTES);
        }
        return slots;
    }

    private static List<LocalTime> merge(List<LocalTime> first, List<LocalTime> second) {
        List<LocalTime> merged = new ArrayList<>(first);
        merged.addAll(second);
        return List.copyOf(merged);
    }
}
