package gsj_project.filetransfer;

import com.google.gson.Gson;

public class PacketUtil {
    private static final Gson gson = new Gson();

    public static String createRRQ(String filename) {
        Packet packet = new Packet("RRQ", filename, 0, null, null, 0);
        return gson.toJson(packet);
    }

    public static String createACK(int block) {
        Packet packet = new Packet("ACK", null, block, null, null, 0);
        return gson.toJson(packet);
    }

    public static String createDATA(int block, String data, int dataSize) {
        Packet packet = new Packet("DATA", null, block, data, null, dataSize);
        return gson.toJson(packet);
    }

    public static String createError(String message) {
        Packet packet = new Packet("ERROR", null, 0, null, message, 0);
        return gson.toJson(packet);
    }

    public static Packet fromJson(String json) {
        return gson.fromJson(json, Packet.class);
    }
}
