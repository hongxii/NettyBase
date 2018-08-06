package io.hongxi.websocket

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler

class Server {

    companion object {
        private val PORT = 8083
        @JvmStatic
        @Throws(Exception::class)
        fun main(args: Array<String>) {

            /**
             * There has 2 LoopGroup Objects
             * 1."bossGroup"   is the host like nginx to Receive  ;
             * 2."workerGroup" is the loop            to MakeResponse;
             */
            val bossGroup = NioEventLoopGroup(1)
            val workerGroup = NioEventLoopGroup()
            try {
                val b = ServerBootstrap()
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel::class.java)
                        .handler(LoggingHandler(LogLevel.INFO))
                        .childHandler(ServerInitializer(null))

                val ch = b.bind(PORT).sync().channel()
                ch.closeFuture().sync()
            } finally {
                bossGroup.shutdownGracefully()
                workerGroup.shutdownGracefully()
            }
        }
    }

}