package cheng7.mediadesk.diagnostics;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Profile("local")
public class DatabaseProbeController {
    private final DatabaseProbeMapper mapper;

    public DatabaseProbeController(DatabaseProbeMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping("/api/dev/database")
    public Map<String, Integer> checkDatabase() {
        int selectOneResult = mapper.selectOne();
        return Map.of("result", selectOneResult);
    }
}
