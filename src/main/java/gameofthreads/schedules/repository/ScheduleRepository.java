package gameofthreads.schedules.repository;

import gameofthreads.schedules.entity.MeetingEntity;
import gameofthreads.schedules.entity.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Integer> {
    @Query("SELECT s FROM ScheduleEntity s LEFT JOIN FETCH s.subscriptions WHERE s.id=:scheduleId")
    Optional<ScheduleEntity> fetchWithSubscriptions(Integer scheduleId);

    @Query("SELECT s FROM ScheduleEntity s LEFT JOIN FETCH s.conferenceEntities as c LEFT JOIN FETCH c.meetingEntities WHERE s.id=:scheduleId")
    Optional<ScheduleEntity> fetchWithConferencesAndMeetings(Integer scheduleId);

    @Query("SELECT s FROM ScheduleEntity s LEFT JOIN FETCH s.conferenceEntities as c LEFT JOIN FETCH c.meetingEntities WHERE s.publicLink=:uuid")
    Optional<ScheduleEntity> fetchWithConferencesAndMeetingsByUuid(String uuid);

    @Query("SELECT DISTINCT s FROM ScheduleEntity s LEFT JOIN FETCH s.conferenceEntities as c LEFT JOIN FETCH c.meetingEntities as m " +
            "WHERE m.lecturerName=:lecturerName and m.lecturerSurname=:lecturerSurname")
    List<ScheduleEntity> fetchWithConferencesAndMeetingsByLecturer(String lecturerName, String lecturerSurname);

    @Query("SELECT s FROM ScheduleEntity s LEFT JOIN FETCH s.conferenceEntities as c LEFT JOIN FETCH c.meetingEntities")
    Set<ScheduleEntity> fetchAllWithConferencesAndMeetings();

    Optional<ScheduleEntity> findByPublicLink(String uuid);
}
