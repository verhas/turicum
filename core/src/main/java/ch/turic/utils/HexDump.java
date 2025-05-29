package ch.turic.utils;

public class HexDump {

    public static void print(byte[] data) {
        print(data, 0, data.length);
    }

    public static void print(byte[] data, int offset, int length) {
        int end = offset + length;
        for (int i = offset; i < end; i += 16) {
            int lineEnd = Math.min(i + 16, end);
            // Print offset
            System.out.printf("%08x  ", i);

            // Print hex bytes
            for (int j = i; j < i + 16; j++) {
                if (j < lineEnd) {
                    System.out.printf("%02x ", data[j]);
                } else {
                    System.out.print("   "); // pad
                }
                if ((j - i) == 7) System.out.print(" "); // extra space after 8 bytes
            }

            System.out.print(" ");

            // Print ASCII characters
            for (int j = i; j < lineEnd; j++) {
                byte b = data[j];
                char c = (b >= 32 && b <= 126) ? (char) b : '.';
                System.out.print(c);
            }

            System.out.println();
        }
    }
}
