package de.tum.i13.server.echo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by chris on 19.10.15.
 */
public class ActiveConnection implements AutoCloseable {
    private final Socket socket;
    private final PrintWriter output;
    private final BufferedReader input;
    private boolean closed;

    public ActiveConnection(Socket socket, PrintWriter output, BufferedReader input) {
        this.socket = socket;

        this.output = output;
        this.input = input;
        this.closed = false;

    }

    public void write(String command) {
        output.write(command + "\r\n");
        output.flush();
    }

    public void writeWithoutCarriageReturn(String command) {
        output.write(command);
        output.flush();
    }

    public String readline()  {
        try {
            return input.readLine();
        } catch (SocketException ignored) {

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Error while reading!";
    }

    public void close() throws Exception {
        output.close();
        input.close();
        socket.close();
        this.closed = true;
    }

    public String getInfo() {
        return "/" + this.socket.getRemoteSocketAddress().toString();
    }

//    public void updateServer(String host, int port) throws IOException {
//        Socket s = new Socket(host, port);
//
//        PrintWriter output = new PrintWriter(s.getOutputStream());
//        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
//    }

    public String getServerInfo() {
        return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isConnected() {
        return socket.isConnected();
    }
}
