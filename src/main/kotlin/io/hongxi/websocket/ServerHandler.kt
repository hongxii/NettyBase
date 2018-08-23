package io.hongxi.websocket

import io.hongxi.dto.Response
import io.hongxi.entity.Client
import io.hongxi.service.MessageService
import io.hongxi.service.RequestService
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.websocketx.*
import io.netty.util.CharsetUtil
import io.netty.util.concurrent.GlobalEventExecutor

import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

import io.netty.handler.codec.http.HttpHeaderNames.HOST
import io.netty.handler.codec.http.HttpMethod.GET
import io.netty.handler.codec.http.HttpResponseStatus.*
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import java.lang.System.err

class ServerHandler: SimpleChannelInboundHandler<Any>() {


    //uri of websocket serverice
    private val WEBSOCKET_PATH = "/websocket"

    //1 ChannelGroup is One LiveChannel
    private val channelGroupMap = ConcurrentHashMap<Long, ChannelGroup>()

    //the code requested
    private val HTTP_REQUEST_STRING = "request"

    private var client: Client = Client()

    private var handshaker: WebSocketServerHandshaker? = null

    public override fun messageReceived(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is FullHttpRequest)
            handleHttpRequest(ctx, msg)
        else if (msg is WebSocketFrame)
            handleWebSocketFrame(ctx, msg)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext){
        ctx.flush()
    }

    private fun handleHttpRequest(ctx: ChannelHandlerContext, req: FullHttpRequest) {
        // Handle a bad request.
        if (!req.decoderResult().isSuccess) {
            sendHttpResponse(ctx, req, DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST))
            return
        }

        // Allow only GET methods.
        if (req.method() !== GET) {
            sendHttpResponse(ctx, req, DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN))
            return
        }

        if ("/favicon.ico" == req.uri() || "/" == req.uri()) {
            sendHttpResponse(ctx, req, DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND))
            return
        }

        val queryStringDecoder = QueryStringDecoder(req.uri())
        val parameters = queryStringDecoder.parameters()

        if (parameters.size == 0 || !parameters.containsKey(HTTP_REQUEST_STRING)) {
            System.err.printf(HTTP_REQUEST_STRING + "参数不可缺省")
            sendHttpResponse(ctx, req, DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND))
            return
        }

        client = RequestService.clientRegister(parameters[HTTP_REQUEST_STRING]!!.get(0))
        if (client.roomId == 0L) {
            err.printf("房间号不可缺省")
            sendHttpResponse(ctx, req, DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND))
            return
        }

        // 房间列表中如果不存在则为该频道,则新增一个频道 ChannelGroup
        if (!channelGroupMap.containsKey(client.roomId)) {
            channelGroupMap[client.roomId] = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)
        }
        // 确定有房间号,才将客户端加入到频道中
        channelGroupMap[client.roomId]?.add(ctx.channel())

        // Handshake
        val wsFactory = WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, true)
        handshaker = wsFactory.newHandshaker(req)
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel())
        } else {
            val channelFuture = handshaker?.handshake(ctx.channel(), req)

            // 握手成功之后,业务逻辑
            if (channelFuture!!.isSuccess) {
                if (client.id == 0L) {
                    println(ctx.channel().toString() + " 游客")
                    return
                }

            }
        }
    }

    private fun broadcast(ctx: ChannelHandlerContext, frame: WebSocketFrame) {

        if (client.id == 0L) {
            val response = Response(1001, "没登录不能聊天哦")
            val msg = JSONObject(response).toString()
            ctx.channel().write(TextWebSocketFrame(msg))
            return
        }

        val request = (frame as TextWebSocketFrame).text()
        println(" 收到 " + ctx.channel() + request)

        val response = MessageService.sendMessage(client, request)
        val msg = JSONObject(response).toString()
        if (channelGroupMap.containsKey(client.roomId)) {
            channelGroupMap[client.roomId]?.writeAndFlush(TextWebSocketFrame(msg))
        }

    }

    //After HandShank
    private fun handleWebSocketFrame(ctx: ChannelHandlerContext, frame: WebSocketFrame) {

        if (frame is CloseWebSocketFrame) {
            handshaker?.close(ctx.channel(), frame.retain() as CloseWebSocketFrame)
            return
        }
        if (frame is PingWebSocketFrame) {
            ctx.channel().write(PongWebSocketFrame(frame.content().retain()))
            return
        }
        if (frame !is TextWebSocketFrame) {
            throw UnsupportedOperationException(String.format("%s frame types not supported", frame.javaClass.name))
        }

        broadcast(ctx, frame)
    }

    private fun sendHttpResponse(ctx: ChannelHandlerContext, req: FullHttpRequest, res: FullHttpResponse) {
        if (res.status().code() != 200) {
            val buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8)
            res.content().writeBytes(buf)
            buf.release()
            HttpHeaderUtil.setContentLength(res, res.content().readableBytes().toLong())
        }

        val f = ctx.channel().writeAndFlush(res)
        if (!HttpHeaderUtil.isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }

    @Throws(Exception::class)
    override fun handlerAdded(ctx: ChannelHandlerContext?) {
        val incoming = ctx!!.channel()
        println("收到" + incoming.remoteAddress() + " 握手请求")
    }

    @Throws(Exception::class)
    override fun handlerRemoved(ctx: ChannelHandlerContext?) {
        if (client.id != 0L && (channelGroupMap.containsKey(client.id))) {
            channelGroupMap[client.roomId]?.remove(ctx!!.channel())
        }
    }

    private fun getWebSocketLocation(req: FullHttpRequest): String {
        val location = req.headers().get(HOST).toString() + WEBSOCKET_PATH
        return "ws://$location"
    }
}