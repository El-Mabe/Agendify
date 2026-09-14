package com.ocnilentes.mediagenda.modules.whatsapp.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ConversationSession {

    private final String phone;
    private ConversationStep step;
    private String cedula;
    private String patientName;
    private LocalDate selectedDate;
    private List<LocalDate> dateOptions;
    private List<LocalTime> availableTimes;
    private List<LocalTime> timeOptions;
    private List<String> cancelCandidateIds;

    public ConversationSession(String phone) {
        this.phone = phone;
        reset();
    }

    public String phone() {
        return phone;
    }

    public ConversationStep step() {
        return step;
    }

    public String cedula() {
        return cedula;
    }

    public String patientName() {
        return patientName;
    }

    public LocalDate selectedDate() {
        return selectedDate;
    }

    public List<LocalDate> dateOptions() {
        return dateOptions;
    }

    public List<LocalTime> availableTimes() {
        return availableTimes;
    }

    public List<LocalTime> timeOptions() {
        return timeOptions;
    }

    public List<String> cancelCandidateIds() {
        return cancelCandidateIds;
    }

    public void reset() {
        this.step = ConversationStep.IDLE;
        this.cedula = null;
        this.patientName = null;
        this.selectedDate = null;
        this.dateOptions = new ArrayList<>();
        this.availableTimes = new ArrayList<>();
        this.timeOptions = new ArrayList<>();
        this.cancelCandidateIds = new ArrayList<>();
    }

    public void startScheduling() {
        reset();
        this.step = ConversationStep.AWAITING_SCHEDULE_CEDULA;
    }

    public void startCancellingAskCedula() {
        reset();
        this.step = ConversationStep.AWAITING_CANCEL_CEDULA;
    }

    public void cedulaReceivedForNewPatient(String cedula) {
        this.cedula = cedula;
        this.step = ConversationStep.AWAITING_NAME;
    }

    public void knownPatientReady(String cedula, String patientName, List<LocalDate> dateOptions) {
        this.cedula = cedula;
        this.patientName = patientName;
        this.dateOptions = new ArrayList<>(dateOptions);
        this.step = ConversationStep.AWAITING_DATE_SELECTION;
    }

    public void nameReceived(String patientName, List<LocalDate> dateOptions) {
        this.patientName = patientName;
        this.dateOptions = new ArrayList<>(dateOptions);
        this.step = ConversationStep.AWAITING_DATE_SELECTION;
    }

    public void dateSelected(LocalDate date, List<LocalTime> availableTimes) {
        this.selectedDate = date;
        this.availableTimes = new ArrayList<>(availableTimes);
        this.timeOptions = new ArrayList<>();
        this.step = ConversationStep.AWAITING_TIME_PERIOD_SELECTION;
    }

    public void timePeriodSelected(List<LocalTime> timeOptions) {
        this.timeOptions = new ArrayList<>(timeOptions);
        this.step = ConversationStep.AWAITING_TIME_SELECTION;
    }

    public void backToTimePeriodSelection() {
        this.timeOptions = new ArrayList<>();
        this.step = ConversationStep.AWAITING_TIME_PERIOD_SELECTION;
    }

    public void startCancelling(String cedula, List<String> candidateIds) {
        this.cedula = cedula;
        this.patientName = null;
        this.selectedDate = null;
        this.dateOptions = new ArrayList<>();
        this.availableTimes = new ArrayList<>();
        this.timeOptions = new ArrayList<>();
        this.cancelCandidateIds = new ArrayList<>(candidateIds);
        this.step = ConversationStep.AWAITING_CANCEL_SELECTION;
    }

    public void awaitRescheduleDecision(String patientName, String cedula) {
        this.patientName = patientName;
        this.cedula = cedula;
        this.selectedDate = null;
        this.dateOptions = new ArrayList<>();
        this.availableTimes = new ArrayList<>();
        this.timeOptions = new ArrayList<>();
        this.cancelCandidateIds = new ArrayList<>();
        this.step = ConversationStep.AWAITING_RESCHEDULE_DECISION;
    }

    public void clearAfterCompletion() {
        reset();
    }
}
