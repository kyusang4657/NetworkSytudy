package gsj_project.filetransfer;

public class PacketUtil {
    public static String createRRQ(String filename) {
        return """
                {
                    "type": "RRQ",
                    "filename": "%s"
                }
                """.formatted(filename);
    }

    public static String createACK(int block) {
        return """
                {
                    "type": "ACK",
                    "block": "%d"
                }
                """.formatted(block);
    }

    public static String createError(String message) {
        return """
            {
                "type": "ERROR",
                "message": "%s"
        """.formatted(message);
    }

    public static boolean isRRQ(String json) {
        return json.contains("\"type\": \"RRQ\"");
    }

    public static String extractFilename(String json) {
        String key = "\"filename\":";
        int keyIndex = json.indexOf(key);

        if(keyIndex != -1){
            return null;
        }

        int firstQuote = json.indexOf("\"", keyIndex + key.length());
        int secondQuote = json.indexOf("\"", firstQuote + 1);

        if(firstQuote == -1 || secondQuote == -1){
            return null;
        }
        return json.substring(firstQuote + 1, secondQuote);
    }
}
