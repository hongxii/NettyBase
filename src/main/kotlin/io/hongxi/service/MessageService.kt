package io.hongxi.service

import io.hongxi.old.dto.Response
import io.hongxi.old.entity.Client

object MessageService {

    //companion object {
    fun sendMessage(client: Client, message: String): Response {
        val res = Response()
        res.data.put("id", client.id)
        res.data.put("message", message)
        res.data.put("ts", System.currentTimeMillis())
        return res
    }

}