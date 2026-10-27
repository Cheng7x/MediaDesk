package cheng7.mediadesk.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Profile("local")
public class DevIdentityController {

    @GetMapping("/dev/me")
    public Map<String, String> me(Authentication authentication) {
        return Map.of("account", authentication.getName());
    }
}
