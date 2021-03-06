package gameofthreads.schedules.service;

import gameofthreads.schedules.dto.request.AddLecturerRequest;
import gameofthreads.schedules.dto.response.LecturerDetailedResponse;
import gameofthreads.schedules.dto.response.LecturerMediumResponse;
import gameofthreads.schedules.dto.response.LecturerResponseList;
import gameofthreads.schedules.dto.response.LecturerShortResponse;
import gameofthreads.schedules.entity.EmailEntity;
import gameofthreads.schedules.entity.LecturerEntity;
import gameofthreads.schedules.message.ErrorMessage;
import gameofthreads.schedules.repository.EmailRepository;
import gameofthreads.schedules.repository.LecturerRepository;
import gameofthreads.schedules.repository.MeetingRepository;
import gameofthreads.schedules.repository.ScheduleRepository;
import gameofthreads.schedules.util.Validator;
import io.vavr.control.Either;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LecturerService {
    private final LecturerRepository lecturerRepository;
    private final EmailRepository emailRepository;
    private final ScheduleRepository scheduleRepository;
    private final MeetingRepository meetingRepository;

    public LecturerService(LecturerRepository lecturerRepository, EmailRepository emailRepository,
                           ScheduleRepository scheduleRepository, MeetingRepository meetingRepository) {
        this.lecturerRepository = lecturerRepository;
        this.emailRepository = emailRepository;
        this.scheduleRepository = scheduleRepository;
        this.meetingRepository = meetingRepository;
    }

    // TODO: one query instead of N
    public LecturerResponseList getAll() {
        List<LecturerEntity> lecturers = lecturerRepository.findAll();

        List<LecturerMediumResponse> lecturerResponses = lecturers
                .stream()
                .map(lecturerEntity ->
                        new LecturerMediumResponse(
                                lecturerEntity,
                                meetingRepository.countMeetingEntityByLecturerSurnameAndAndLecturerName(
                                        lecturerEntity.getSurname(),
                                        lecturerEntity.getName()
                                ))
                )
                .filter(lecturerMediumResponse -> !lecturerMediumResponse.name.equals("ADMIN"))
                .collect(Collectors.toList());
        return new LecturerResponseList(lecturerResponses);
    }

    public Either<Object, LecturerDetailedResponse> get(Integer id) {
        Optional<LecturerEntity> lecturerEntity = lecturerRepository.findById(id);
        if (lecturerEntity.isEmpty()) {
            return Either.left(ErrorMessage.WRONG_LECTURER_ID.asJson());
        }

        return Either.right(new LecturerDetailedResponse(lecturerEntity.get(),
                scheduleRepository.fetchWithConferencesAndMeetingsByLecturer(lecturerEntity.get().getName(), lecturerEntity.get().getSurname())));
    }

    public Either<Object, LecturerDetailedResponse> getMySchedules(JwtAuthenticationToken token) {
        if (!token.getTokenAttributes().get("scope").equals("LECTURER"))
            return Either.left(ErrorMessage.LECTURER_ONLY.asJson());

        String lecturerEmail = (String) token.getTokenAttributes().get("sub");

        Optional<LecturerEntity> lecturerEntity = lecturerRepository.findByEmail_Email(lecturerEmail);
        if (lecturerEntity.isEmpty()) {
            return Either.left(ErrorMessage.WRONG_LECTURER_ID.asJson());
        }

        return Either.right(new LecturerDetailedResponse(lecturerEntity.get(),
                scheduleRepository.fetchWithConferencesAndMeetingsByLecturer(lecturerEntity.get().getName(), lecturerEntity.get().getSurname())));
    }

    @Transactional
    public Either<Object, Boolean> delete(Integer id) {
        Optional<LecturerEntity> lecturer = lecturerRepository.findById(id);
        if (lecturer.isEmpty()) {
            return Either.left(ErrorMessage.WRONG_LECTURER_ID.asJson());
        }

        if (lecturer.get().getEmailEntity() != null)
            emailRepository.deleteById(lecturer.get().getEmailEntity().getId());
        else
            lecturerRepository.delete(lecturer.get());

        return Either.right(true);
    }

    @Transactional
    public Either<Object, LecturerShortResponse> add(AddLecturerRequest lecturerRequest) {
        if (!Validator.validateEmail(lecturerRequest.email)) {
            return Either.left(ErrorMessage.INCORRECT_EMAIL.asJson());
        }

        Optional<EmailEntity> emailEntity = emailRepository.findByEmail(lecturerRequest.email);
        boolean isEmailUnavailable = emailEntity.map(EmailEntity::getLecturer).isPresent();

        if (isEmailUnavailable) {
            return Either.left(ErrorMessage.NOT_AVAILABLE_EMAIL.asJson());
        }

        EmailEntity email = emailEntity.orElseGet(() -> new EmailEntity(lecturerRequest.email));
        LecturerEntity lecturerEntity = new LecturerEntity(lecturerRequest, email);
        lecturerRepository.save(lecturerEntity);

        return Either.right(new LecturerShortResponse(lecturerEntity));
    }

    @Transactional
    public Either<Object, LecturerMediumResponse> update(Integer id, AddLecturerRequest lecturerRequest) {
        if (!Validator.validateEmail(lecturerRequest.email)) {
            return Either.left(ErrorMessage.INCORRECT_EMAIL.asJson());
        }

        final Optional<Integer> emailOwner = emailRepository.findByEmail(lecturerRequest.email)
                .map(EmailEntity::getLecturer)
                .map(LecturerEntity::getId);

        if (emailOwner.isPresent() && !emailOwner.get().equals(id)) {
            return Either.left(ErrorMessage.NOT_AVAILABLE_EMAIL.asJson());
        }

        Optional<LecturerEntity> entity = lecturerRepository.findById(id);

        if (entity.isEmpty()) {
            return Either.left(ErrorMessage.WRONG_LECTURER_ID.asJson());
        }

        LecturerEntity lecturerEntity = entity.map(lecturer -> {
            lecturer.setName(lecturerRequest.name);
            lecturer.setSurname(lecturerRequest.surname);
            lecturer.updateEmail(lecturerRequest.email);
            return lecturer;
        }).get();

        lecturerRepository.save(lecturerEntity);

        long eventsCount = meetingRepository.countMeetingEntityByLecturerSurnameAndAndLecturerName(
                lecturerRequest.surname, lecturerRequest.name);

        return Either.right(new LecturerMediumResponse(lecturerEntity, eventsCount));
    }

}
