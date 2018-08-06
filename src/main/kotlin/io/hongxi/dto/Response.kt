package io.hongxi.dto

data class Response(

    // 成功时 0 ,如果大于 0 则表示则显示error_msg
    private var error_code: Int = 0 ,
    private var error_msg: String = "",
    var data: MutableMap<String, Any> = HashMap()
)