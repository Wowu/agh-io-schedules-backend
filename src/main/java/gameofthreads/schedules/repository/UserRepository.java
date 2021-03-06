package gameofthreads.schedules.repository;

import gameofthreads.schedules.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    Optional<UserEntity> findByEmail_Email(String email);

    @Query("SELECT u FROM UserEntity u WHERE u.role = 'ADMIN'")
    UserEntity findAdmin();
}
