package gameofthreads.schedules.controller;

import gameofthreads.schedules.dto.request.MyNotificationsDto;
import gameofthreads.schedules.service.LecturerService;
import gameofthreads.schedules.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/me")
public class MeController {
    private final static Logger LOGGER = LoggerFactory.getLogger(LoggerFactory.class);
    private final LecturerService lecturerService;
    private final NotificationService notificationService;

    public MeController(LecturerService lecturerService, NotificationService notificationService) {

        this.lecturerService = lecturerService;
        this.notificationService = notificationService;
    }

    @GetMapping("/schedules")
    public ResponseEntity<?> getMySchedules(Authentication token) {
        return lecturerService.getMySchedules((JwtAuthenticationToken) token)
                .fold(error -> {
                    LOGGER.info(error.toString());
                    return ResponseEntity.badRequest().body(error);
                }, success -> ResponseEntity.status(HttpStatus.OK).body(success));
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getMyNotifications(Authentication token) {
        return notificationService.getMyNotifications((JwtAuthenticationToken) token)
                .fold(error -> {
                    LOGGER.info(error.toString());
                    return ResponseEntity.badRequest().body(error);
                }, ResponseEntity::ok);
    }

    @PutMapping(value = "/notifications")
    public ResponseEntity<MyNotificationsDto> addGlobalNotifications(
            Authentication token, @RequestBody MyNotificationsDto notifications) {

        return ResponseEntity.ok(notificationService.addMyNotifications((JwtAuthenticationToken) token, notifications));
    }

}
