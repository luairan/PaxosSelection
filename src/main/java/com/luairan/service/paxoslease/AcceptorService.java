package com.luairan.service.paxoslease;

import com.luairan.service.context.Request;
import com.luairan.service.context.Response;

/**
 * Created by luairan on 16/9/5.
 */
public interface AcceptorService {

    Response prepareRequest(Request request);

    Response proposeRequest(Request request);
}
