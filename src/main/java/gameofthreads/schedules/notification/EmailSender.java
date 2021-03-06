package gameofthreads.schedules.notification;

import gameofthreads.schedules.entity.*;
import gameofthreads.schedules.notification.model.*;
import gameofthreads.schedules.repository.ConferenceRepository;
import gameofthreads.schedules.repository.LecturerRepository;
import gameofthreads.schedules.repository.NotificationRepository;
import gameofthreads.schedules.repository.SubscriptionRepository;
import gameofthreads.schedules.util.HtmlCreator;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.mail.internet.MimeMessage;
import java.util.*;

import static java.util.stream.Collectors.*;

@Component
public class EmailSender {
    private final static Logger LOGGER = LoggerFactory.getLogger(EmailSender.class);

    private final EmailQueue emailQueue;
    private final JavaMailSender javaMailSender;
    private final ConferenceRepository conferenceRepository;
    private final NotificationRepository notificationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final LecturerRepository lecturerRepository;

    public EmailSender(EmailQueue notificationQueue, NotificationRepository notificationRepository,
                       SubscriptionRepository subscriptionRepository, ConferenceRepository conferenceRepository,
                       JavaMailSender javaMailSender, LecturerRepository lecturerRepository) {

        this.emailQueue = notificationQueue;
        this.notificationRepository = notificationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.conferenceRepository = conferenceRepository;
        this.javaMailSender = javaMailSender;
        this.lecturerRepository = lecturerRepository;
    }

    public void initEmailQueue() {
        List<NotificationEntity> notificationEntities = notificationRepository.findAll();

        Map<Integer, List<ConferenceEntity>> conferenceGroupedBySchedule = conferenceRepository
                .fetchWithScheduleAndMeetings()
                .stream()
                .collect(groupingBy(conference -> conference.getScheduleEntity().getId()));

        List<NotificationEntity> globalNotifications = notificationEntities
                .stream()
                .filter(NotificationEntity::isGlobal)
                .collect(toList());

        List<SubscriptionEntity> subscriptionEntities = subscriptionRepository
                .findAll()
                .stream()
                .filter(SubscriptionEntity::isActive)
                .collect(toList());

        for (SubscriptionEntity subscription : subscriptionEntities) {
            Boolean isNotificationsEnabled = subscription.getSchedule().getNotifications();

            if (isNotificationsEnabled != null && !isNotificationsEnabled) {
                continue;
            }

            Integer scheduleId = subscription.getSchedule().getId();
            List<ConferenceEntity> conferencePerSchedule = conferenceGroupedBySchedule.get(scheduleId);

            if (emailQueue.update(scheduleId) == null) {
                emailQueue.add(scheduleId, new Notification(prepareConference(conferencePerSchedule)));
            }

            String fullName;

            if (subscription.getLecturer() == null) {
                fullName = "";
            } else {
                fullName = subscription.getLecturer().getFullName();
            }

            if (subscription.getUser() != null && subscription.isGlobal()) {
                addNotifications(globalNotifications, scheduleId, subscription.getEmail(), false, fullName);
            } else if (subscription.getLecturer() != null && subscription.getUser() == null) {
                addNotifications(globalNotifications, scheduleId, subscription.getEmail(), false, fullName);
            } else if (subscription.getUser() == null) {
                addNotifications(globalNotifications, scheduleId, subscription.getEmail(), true, fullName);
            } else {
                List<NotificationEntity> userNotifications = notificationEntities
                        .stream()
                        .filter(notification -> notification.checkUser(subscription.getUser().getId()))
                        .collect(toList());

                if (userNotifications.size() == 0) {
                    addNotifications(globalNotifications, scheduleId, subscription.getEmail(), false, fullName);
                } else {
                    addNotifications(userNotifications, scheduleId, subscription.getEmail(), false, fullName);
                }
            }
        }
    }

    private List<Conference> prepareConference(List<ConferenceEntity> conferencePerSchedule) {
        List<Conference> conferences = new ArrayList<>();

        for (ConferenceEntity conferenceEntity : conferencePerSchedule) {
            List<Meeting> meetingsPerConference = conferenceEntity.getMeetingEntities()
                    .stream()
                    .map(MeetingEntity::buildMeetingNotification)
                    .collect(toList());

            conferences.add(new Conference(meetingsPerConference));
        }

        return conferences;
    }

    private void addNotifications(List<NotificationEntity> notifications, Integer scheduleId, String email, boolean full, String fullName) {
        for (NotificationEntity notification : notifications) {
            var details = new ScheduleDetails(notification.getUnit(), notification.getValue(), full, fullName);
            emailQueue.updateDetails(scheduleId, email, details);
        }
    }

    @PostConstruct
    public void activeJob() {
        initEmailQueue();
    }

    @Scheduled(initialDelay = 1000 * 30, fixedDelay = 100)
    public void run() {
        var optionalPair = emailQueue.pop();
        if (optionalPair.isPresent()) {
            var pair = optionalPair.get();

            LOGGER.info("SEND EMAIL TO : " + pair.getSecond().getEmail());
            String fullName = "";
            String html = "";

            if (!pair.getSecond().isFullNotification()) {
                fullName = lecturerRepository.findByEmail_Email(pair.getSecond().getEmail())
                        .map(LecturerEntity::getFullName)
                        .orElseGet(() -> {
                            LOGGER.info("SEND EMAIL : fullName was not found.");
                            return "";
                        });

                if (!fullName.equals("")) {
                    final TreeSet<Conference> conferences = new TreeSet<>();

                    for (Conference conference : pair.getFirst()) {
                        List<Meeting> meetings = new ArrayList<>();
                        for (Meeting meeting : conference.getMeetings()) {
                            if (meeting.getFullName().equals(fullName)) {
                                meetings.add(meeting);
                            }
                        }
                        if (meetings.size() > 0) {
                            conferences.add(new Conference(meetings));
                        }
                    }

                    html = HtmlCreator.createConferencesEmail(conferences, fullName);
                }
            }

            if (html.equals("")) {
                html = HtmlCreator.createConferencesEmail(pair.getFirst(), fullName);
            }
            sendEmail(pair.getSecond().getEmail(), html);
        }
    }

    private void sendEmail(String email, String htmlMessage) {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        Try<Void> setEmail = Try.run(() -> helper.setTo(email));
        Try<Void> setSubject = Try.run(() -> helper.setSubject("Przypomnienie o Konferencji"));
        Try<Void> setMessage = Try.run(() -> helper.setText(htmlMessage, true));

        if (setMessage.isSuccess() && setSubject.isSuccess() && setMessage.isSuccess()) {
            javaMailSender.send(message);
        } else {
            LOGGER.error(
                    "Error during send email to : " + email,
                    setEmail.getCause(),
                    setMessage.getCause(),
                    setSubject.getCause()
            );
        }
    }

}
