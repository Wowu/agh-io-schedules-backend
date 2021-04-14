package gameofthreads.schedules.repository;

import gameofthreads.schedules.entity.MeetingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetingRepository extends JpaRepository<MeetingEntity, Integer> {
}
