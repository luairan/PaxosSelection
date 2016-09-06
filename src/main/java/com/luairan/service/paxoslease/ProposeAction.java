package com.luairan.service.paxoslease;

import com.luairan.service.context.Response;
import com.luairan.service.http.HTTPClient;

import java.io.*;

class ProposeAction implements HTTPClient.Action<Response> {

    @Override
    public void sendInfo(OutputStream os, Object... args) throws Exception {
        ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(os));
        oos.writeObject(args[0]);
        oos.flush();
        oos.close();
    }

    @Override
    public Response reciveInfo(InputStream is) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(is));
        return (Response) ois.readObject();
    }
}