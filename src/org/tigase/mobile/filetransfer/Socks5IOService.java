package org.tigase.mobile.filetransfer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tigase.mobile.net.IOService;
import org.tigase.mobile.net.SocketThread;

/**
 *
 * @author andrzej
 */
public class Socks5IOService extends IOService<Object> {

        private static final Logger log = Logger.getLogger(Socks5IOService.class.getCanonicalName());

        public static enum State {
                Welcome,
                WelcomeResp,
                Auth,
                AuthResp,
                Active,
                Closed
        }
        private State state = State.Welcome;
        private ByteBuffer inputBuffer;
        private ByteBuffer outputBuffer;
        private final FileTransfer ft;
        private long startTime;
        private long stopTime;

        public Socks5IOService(SocketChannel channel, FileTransfer ft) throws IOException {
                super(channel);

                this.ft = ft;

                channel.configureBlocking(false);
                channel.socket().setSendBufferSize(64*1024);
                channel.socket().setReceiveBufferSize(64*1024); 
                channel.socket().setSoLinger(false, 0);
                channel.socket().setReuseAddress(true);

                inputBuffer = ByteBuffer.allocate((ft.outgoing ? 1 : 32) * 1024);
        }

        @Override
        public boolean isConnected() {
                return super.isConnected() && state != State.Closed;
        }
        
        @Override
        public boolean waitingToSend() {
                return state == State.Welcome || outputBuffer != null && outputBuffer.hasRemaining();
        }
        
        @Override
        public synchronized void processSocketData() throws IOException {
                if (!isConnected()) {
                        forceStop();
                        return;
                }
                
                ByteBuffer buf = read(this.inputBuffer);

                if (buf != null && buf.hasRemaining()) {
                        if (log.isLoggable(Level.FINEST)) {
                                log.log(Level.FINEST, "processing received data");
                        }
                        switch (state) {
                                case WelcomeResp:
                                        if (log.isLoggable(Level.FINEST)) {
                                                log.finest("for WELCOME response read = "+buf.remaining());
                                                forceStop();
                                        }                                        
                                        int ver = (int) buf.get();
                                        if (ver != 0x05) {
                                                log.warning("bad protocol version!");
                                                forceStop();
                                        }
                                        int status = (int) buf.get();
                                        buf.clear();
                                        if (status == 0) {
                                                state = State.Auth;
                                        }
                                        break;

                                case AuthResp:
                                        if (log.isLoggable(Level.FINEST)) {
                                                log.finest("for AUTH response read = "+buf.remaining());
                                        }
                                        if (buf.get() != 0x05) {
                                                log.warning("bad protocol version!");
                                        }
                                        // let's ignore response for now                                      
                                        buf.clear();
                                        state = State.Active;
                                        ft.connectedToProxy();
                                        startTime = System.currentTimeMillis();
                                        break;

                                case Active:
                                        ft.receivedData(buf);
                                        buf.clear();
                                        break;
                                        
                                default:
                                        buf.clear();
                        }
                        if (log.isLoggable(Level.FINEST)) {
                                log.log(Level.FINEST, "after processing received data set in state = " + state);
                        }
                }

                if (state == State.Welcome) {
                        if (log.isLoggable(Level.FINEST)) {
                                log.log(Level.FINEST, "sending WELCOME request");
                        }
                        ByteBuffer out = ByteBuffer.allocate(128);
                        // version
                        out.put((byte) 0x05);
                        // count
                        out.put((byte) 0x01);
                        // method
                        out.put((byte) 0x00);
                        out.flip();
                        state = State.WelcomeResp;
                        write(out);
                } else if (state == State.Auth) {
                        if (log.isLoggable(Level.FINEST)) {
                                log.log(Level.FINEST, "sending AUTH request");
                        }
                        state = State.AuthResp;
                        ByteBuffer out = ByteBuffer.allocate(256);
                        // version
                        out.put((byte) 0x05);
                        // cmd id (auth)
                        out.put((byte) 0x01);
                        // reserved 0x00
                        out.put((byte) 0x00);
                        // auth type
                        out.put((byte) 0x03);

                        byte[] hexHash = ft.getAuthString().getBytes();//sid+initiator.toString()+recipient.toString();

                        out.put((byte) hexHash.length);
                        out.put(hexHash);

                        // port
                        out.put((byte) 0x00);
                        out.put((byte) 0x00);

                        out.flip();
                        int wrote = write(out);
                        if (out.hasRemaining()) {
                            log.warning("we wrote to stream = "+wrote+" but we have remaining = "+out.remaining());
                        }
                }

                if (outputBuffer != null) {                
                        if (outputBuffer.hasRemaining()) {
                                if (log.isLoggable(Level.FINEST)) {
                                        log.log(Level.FINEST, "sending data...");
                                }
                                //ByteBuffer out = ByteBuffer.allocate(64*1024);
                                //outputBuffer.get(out.array());
                                //out.flip();
                                int wrote = write(outputBuffer);
                                if (log.isLoggable(Level.FINEST)) {
                                        log.log(Level.FINEST, "sent data, remaining = "+outputBuffer.remaining());
                                }
                                if (wrote != -1) {
                                        ft.sentData(wrote);
                                } else {
                                        forceStop();
                                }
                                if (!outputBuffer.hasRemaining()) {
                                		ft.transferFinished();
                                        forceStop();
                                }
                        } else {
                        		ft.transferFinished();
                                forceStop();
                        }
                }
        }
        
        public void sendData(ByteBuffer buf) {
            //    buf.flip();
                outputBuffer = buf;
                SocketThread.addSocketService(this);
        }
        
        public long getTime() {
                if (stopTime == 0) 
                        return 0;
                
                return stopTime - startTime;
        }
        
        @Override
        public void forceStop() throws IOException {
                stopTime = System.currentTimeMillis();
                state = State.Closed;
                ft.transferError("connection error");
                super.forceStop();
        }

        @Override
        public int hashCode() {
                return Math.abs(getUniqueId().hashCode());
        }
        
}
