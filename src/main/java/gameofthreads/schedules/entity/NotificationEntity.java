package gameofthreads.schedules.entity;

import gameofthreads.schedules.dto.response.NotificationResponse;

import javax.persistence.*;

@Entity
@Table(name = "notification")
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "unit")
    @Enumerated(EnumType.STRING)
    private TimeUnit unit;

    @Column(name = "value")
    private Integer value;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    public NotificationEntity() {
    }

    public NotificationEntity(TimeUnit unit, Integer value, UserEntity user) {
        this.unit = unit;
        this.value = value;
        this.user = user;
    }

    public NotificationResponse buildResponse() {
        return new NotificationResponse(value, unit.name().toLowerCase());
    }

    public boolean isGlobal() {
        return Role.ADMIN.equals(user.getRole());
    }

    public boolean checkUser(Integer userId){
        return userId.equals(user.getId());
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public Integer getValue() {
        return value;
    }

}
