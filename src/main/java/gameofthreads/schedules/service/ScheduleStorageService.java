package gameofthreads.schedules.service;

import gameofthreads.schedules.domain.Conference;
import gameofthreads.schedules.domain.Meeting;
import gameofthreads.schedules.domain.Schedule;
import gameofthreads.schedules.entity.ConferenceEntity;
import gameofthreads.schedules.entity.MeetingEntity;
import gameofthreads.schedules.entity.ScheduleEntity;
import org.springframework.stereotype.Service;

@Service
public class ScheduleStorageService {

    public ScheduleEntity getScheduleEntity(Schedule schedule) {
        ScheduleEntity scheduleEntity = new ScheduleEntity(schedule.getFileName());
        for (Conference conference : schedule.getConferences()) {
            addConferenceToSchedule(conference, scheduleEntity);
        }
        return scheduleEntity;
    }

    public void addConferenceToSchedule(Conference conference, ScheduleEntity scheduleEntity) {
        ConferenceEntity conferenceEntity = new ConferenceEntity(scheduleEntity);
        for (Meeting meeting : conference.getMeetings()) {
            addMeetingToConference(meeting, conferenceEntity);
        }
        scheduleEntity.getConferences().add(conferenceEntity);
    }

    public void addMeetingToConference(Meeting meeting, ConferenceEntity conferenceEntity) {
        MeetingEntity meetingEntity = new MeetingEntity(conferenceEntity, meeting.getDateStart(),
                meeting.getDateEnd(), meeting.getSubject(), meeting.getGroup(), meeting.getLecturer(),
                meeting.getType(), meeting.getLengthInHours(), meeting.getFormat(), meeting.getRoom());

        conferenceEntity.getMeetingEntities().add(meetingEntity);
    }
}