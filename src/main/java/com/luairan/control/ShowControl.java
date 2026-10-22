package com.luairan.control;

import com.luairan.service.context.State;
import com.luairan.service.paxoslease.ProposerService;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/show")
public class ShowControl {
    @Resource private State state;
    @Resource private ProposerService proposerService;
    @GetMapping("/showMater")
    public Map<String, Object> status() {
        return Map.of("nodeId", state.getNodeId(), "leaseOwner", state.isLeaseOwner(), "ballotNumber", state.getBallotNumber());
    }
    @PostMapping("/start") public Map<String, String> start() throws InterruptedException {
        proposerService.proposerStart(); return Map.of("status", "started");
    }
    @PostMapping("/stop") public Map<String, String> stop() {
        proposerService.cancle(); return Map.of("status", "stopped");
    }
}
