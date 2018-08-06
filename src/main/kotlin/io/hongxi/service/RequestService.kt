package io.hongxi.service

import io.hongxi.old.entity.Client
import org.apache.commons.codec.binary.Base64
import org.json.JSONException
import org.json.JSONObject

object RequestService {

    //companion object {
    fun clientRegister(request: String): Client {
        val res = String(Base64.decodeBase64(request))
        val json = JSONObject(res)
        val client = Client()

        if (!json.has("rid"))
            return client

        try {
            client.roomId = json["rid"].toString().toLong()
        } catch (e: JSONException) {
            e.printStackTrace()
            return client
        }

        if (!json.has("id") || !json.has("token"))
            return client


        val id: Long
        val token: String
        try {
            id = json["id"].toString().toLong()
            token = json["token"] as String
        } catch (e: JSONException) {
            e.printStackTrace()
            return client
        }

        if (!checkToken(id, token)) return client
        client.id = id

        return client
    }


    private fun checkToken(id: Long, token: String): Boolean {
        return true
    }
}