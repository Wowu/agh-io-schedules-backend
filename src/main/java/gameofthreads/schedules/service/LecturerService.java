package gameofthreads.schedules.service;

import gameofthreads.schedules.dto.request.AddLecturerRequest;
import gameofthreads.schedules.dto.response.LecturerResponse;
import gameofthreads.schedules.entity.EmailEntity;
import gameofthreads.schedules.entity.LecturerEntity;
import gameofthreads.schedules.message.ErrorMessage;
import gameofthreads.schedules.repository.EmailRepository;
import gameofthreads.schedules.repository.LecturerRepository;
import gameofthreads.schedules.util.Validator;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LecturerService {
    private final LecturerRepository lecturerRepository;
    private final EmailRepository emailRepository;

    public LecturerService(LecturerRepository lecturerRepository, EmailRepository emailRepository) {
        this.lecturerRepository = lecturerRepository;
        this.emailRepository = emailRepository;
    }

    public List<LecturerEntity> getAll() {
        return lecturerRepository.findAll();
    }

    @Transactional
    public Either<Object, Boolean> delete(Integer id) {
        Optional<LecturerEntity> lecturer = lecturerRepository.findById(id);
        if (lecturer.isEmpty()) {
            return Either.left(ErrorMessage.WRONG_LECTURER_ID.asJson());
        }
        emailRepository.deleteById(lecturer.get().getEmailEntity().getId());
        return Either.right(true);
    }

    public Either<Object, LecturerResponse> add(AddLecturerRequest lecturerRequest) {
        if (!Validator.validateEmail(lecturerRequest.email)) {
            return Either.left(ErrorMessage.INCORRECT_EMAIL.asJson());
        }

        LecturerEntity lecturerEntity = new LecturerEntity(lecturerRequest);
        Try<LecturerEntity> trySave = Try.of(() -> lecturerRepository.save(lecturerEntity));

        return trySave
                .map(LecturerResponse::new)
                .map(Either::right)
                .getOrElse(() -> Either.left(ErrorMessage.NOT_AVAILABLE_EMAIL.asJson()));
    }

    @Transactional
    public Either<Object, LecturerResponse> update(Integer id, AddLecturerRequest lecturerRequest) {
        if (!Validator.validateEmail(lecturerRequest.email)) {
            return Either.left(ErrorMessage.INCORRECT_EMAIL.asJson());
        }

        Optional<LecturerEntity> entity = lecturerRepository.findById(id);

        if (entity.isEmpty()) {
            return Either.left(ErrorMessage.WRONG_LECTURER_ID.asJson());
        }

        Optional<EmailEntity> emailEntity = emailRepository.findByEmail(lecturerRequest.email);

        LecturerEntity lecturerEntity = entity.map(lecturer -> {
            lecturer.setName(lecturerRequest.name);
            lecturer.setSurname(lecturerRequest.surname);
            if (emailEntity.isPresent())
                lecturer.setEmail(emailEntity.get());
            else
                lecturer.setEmail(new EmailEntity(lecturerRequest.email));
            lecturer.setActiveSubscription(lecturerRequest.activeSubscription);
            return lecturer;
        }).get();

        lecturerRepository.save(lecturerEntity);
        return Either.right(new LecturerResponse(lecturerEntity));
    }

}
