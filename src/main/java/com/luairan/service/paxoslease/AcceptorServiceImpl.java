package com.luairan.service.paxoslease;

import com.luairan.service.context.*;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;

@Service
public class AcceptorServiceImpl implements AcceptorService {
    @Resource private State state;
    public AcceptorServiceImpl() { }
    public AcceptorServiceImpl(State state) { this.state = state; }
    public Response prepareRequest(Request request) {
        synchronized (state) {
            Response response = reply(request, Type.Closed);
            if (!state.isReady() || request.getBallotNumber() <= 0 || request.getBallotNumber() < state.getHighestPromissed()) return response;
            state.setHighestPromissed(request.getBallotNumber());
            response.setType(Type.PrepareResponse);
            response.setAccpetedProposal(state.getAccpetedProposal());
            return response;
        }
    }
    public Response proposeRequest(Request request) {
        synchronized (state) {
            Response response = reply(request, Type.Closed);
            Proposal p = request.getProposal();
            if (!state.isReady() || request.getBallotNumber() <= 0 || request.getBallotNumber() < state.getHighestPromissed()
                    || p == null || p.getMachineId() == null || p.getMachineId().isEmpty()
                    || p.getTimeout() <= 0 || p.getTimeout() > State.MAX_LEASE_MILLIS) return response;
            Proposal previous = state.getAccpetedProposal();
            // A higher ballot never revokes an unexpired lease held by another node.
            if (previous != null && !previous.getMachineId().equals(p.getMachineId())) return response;
            state.setHighestPromissed(request.getBallotNumber());
            state.accept(p);
            response.setType(Type.ProposeResponse); response.setAccpetedProposal(state.getAccpetedProposal());
            return response;
        }
    }
    private Response reply(Request request, Type type) {
        Response r = new Response(); r.setType(type); r.setBallotNumber(request.getBallotNumber());
        r.setHighestPromised(state.getHighestPromissed()); return r;
    }
}
