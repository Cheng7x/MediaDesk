package cheng7.mediadesk.auth;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {
    @GetMapping("/api/me")
    public ResponseEntity<MeResponse> me(
        @AuthenticationPrincipal MediaDeskPrincipal principal
    ) {
        var body = new MeResponse(
            principal.getId().toString(),
            principal.getUsername(),
            principal.getDisplayName()
        );

        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(body);
    }
}
