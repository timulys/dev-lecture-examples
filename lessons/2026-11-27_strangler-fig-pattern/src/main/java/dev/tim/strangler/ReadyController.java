package dev.tim.strangler;
import org.springframework.web.bind.annotation.*;
@RestController
public class ReadyController {
    @GetMapping("/ready")
    public String ready() { return "ready"; }
}
