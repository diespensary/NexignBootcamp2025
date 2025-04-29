package com.example.cdrservice.service;

import com.example.cdrservice.entity.CdrRecord;
import com.example.cdrservice.entity.Subscriber;
import com.example.cdrservice.repository.CdrRecordRepository;
import com.example.cdrservice.repository.SubscriberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CdrGenerationService {
    private final SubscriberRepository subRepo;
    private final CdrRecordRepository cdrRepo;

    private static final LocalDateTime START_GEN = LocalDateTime.of(2025,1,1,0,0);
    private static final LocalDateTime END_GEN = LocalDateTime.of(2026,1,1,0,0);
    private static final int MIN_INTERVAL_IN_SEC = 1;
    private static final int MAX_INTERVAL_IN_DAYS = 7;
    private static final int MIN_DURATION_IN_SEC = 1;
    private static final int MAX_DURATION_IN_MINUTES = 30;

    private Subscriber prevServedSubscriber;
    private Subscriber prevAnotherSubscriber;
    private LocalDateTime prevStartTime = START_GEN;
    private Duration prevDuration = Duration.ZERO;

    @Transactional
    public void generateNext() {
        List<Subscriber> allSubs = subRepo.findAll();
        List<Subscriber> romSubs = subRepo.findByOperator("Romashka");

        // интервал до следующего вызова (от секунды до 7 дней)
        Duration interval = randomDurationBetween(Duration.ofSeconds(MIN_INTERVAL_IN_SEC), Duration.ofDays(MAX_INTERVAL_IN_DAYS));
        LocalDateTime startTime = prevStartTime.plus(interval);

        // проверка границы периода генерации
        if (startTime.isAfter(END_GEN)) {
            throw new IllegalStateException("Generation period ended");
        }

        // выбор первого (только Romashka) и второго (любой, но != первому)
        Subscriber servedSub;
        Subscriber anotherSub;
        // если интервал меньше, чем предыдущая длительность разговора, то
        // оба номера в cdr-записи отличаются от номеров в предыдущей cdr-записи
        if (interval.compareTo(prevDuration) < 0) {
            // оба не равны предыдущим
            List<Subscriber> allowedServed = romSubs.stream()
                    .filter(s -> !s.equals(prevServedSubscriber) && !s.equals(prevAnotherSubscriber))
                    .toList();
            servedSub = randomSubscriber(allowedServed);

            List<Subscriber> allowedAnother = allSubs.stream()
                    .filter(s -> !s.equals(prevServedSubscriber)
                            && !s.equals(prevAnotherSubscriber)
                            && !s.equals(servedSub)).toList();
            anotherSub = randomSubscriber(allowedAnother);
        } else {
            // можно брать любые
            servedSub = randomSubscriber(romSubs);

            List<Subscriber> allowedAnother = allSubs.stream()
                    .filter(s -> !s.equals(servedSub))
                    .toList();
            anotherSub = randomSubscriber(allowedAnother);
        }

        // случайный тип вызова
        boolean is01 = ThreadLocalRandom.current().nextBoolean();
        String callType = is01 ? "01" : "02";

        // длительность (от секунды до 30 минут) и время конца
        Duration duration = randomDurationBetween(Duration.ofSeconds(MIN_DURATION_IN_SEC),
                Duration.ofMinutes(MAX_DURATION_IN_MINUTES));
        LocalDateTime endTime = startTime.plus(duration);

        // обновляем prev-значения
        prevStartTime = startTime;
        prevDuration = duration;
        prevServedSubscriber = servedSub;
        prevAnotherSubscriber = anotherSub;

        // проверка на то, нужно ли создавать зеркальную запись (если оба абонента Ромашки)
        boolean generateMirror = "Romashka".equals(servedSub.getOperator())
                && "Romashka".equals(anotherSub.getOperator());
        String mirrorCallType = is01 ? "02" : "01";

        // сохраняем записи, разбивая по полуночи (если это нужно)
        if (startTime.toLocalDate().equals(endTime.toLocalDate())) {
            saveRecord(callType, servedSub, anotherSub, startTime, endTime);
            if (generateMirror) {
                saveRecord(mirrorCallType, anotherSub, servedSub, startTime, endTime);
            }
        } else {
            // часть до полуночи
            LocalDateTime firstEnd = startTime.toLocalDate().atTime(23,59,59);
            saveRecord(callType, servedSub, anotherSub, startTime, firstEnd);
            if (generateMirror) {
                saveRecord(mirrorCallType, anotherSub, servedSub, startTime, firstEnd);
            }
            // часть после
            LocalDateTime secondStart = firstEnd.plusSeconds(1);
            saveRecord(callType, servedSub, anotherSub, secondStart, endTime);
            if (generateMirror) {
                saveRecord(mirrorCallType, anotherSub, servedSub, secondStart, endTime);
            }
        }
    }

    private void saveRecord(String callType,
                            Subscriber firstSubscriber,
                            Subscriber secondSubscriber,
                            LocalDateTime startTime,
                            LocalDateTime endTime) {
        CdrRecord rec = new CdrRecord();
        rec.setCallType(callType);
        rec.setServedSubscriber(firstSubscriber.getMsisdn());
        rec.setAnotherSubscriber(secondSubscriber.getMsisdn());
        rec.setStartTime(startTime);
        rec.setEndTime(endTime);
        cdrRepo.save(rec);
    }

    private Subscriber randomSubscriber(List<Subscriber> subs) {
        return subs.get(ThreadLocalRandom.current().nextInt(subs.size()));
    }

    private Duration randomDurationBetween(Duration min, Duration max) {
        long sec = ThreadLocalRandom.current()
                .nextLong(min.getSeconds(), max.getSeconds() + 1);
        return Duration.ofSeconds(sec);
    }
}
