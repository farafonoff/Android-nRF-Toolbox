package no.nordicsemi.android.nrftoolbox.template;

import java.util.ArrayList;

public class BrtlUtils {
    static int prefix = (byte)0xab;
    static byte PushContent = 0x17;

    byte[][] pushContent(String data) {
        // "AB", 0x17, 0x01, data
        ArrayList<String> parts = new ArrayList<>();
        for(int i=0; i<data.length();i+=24) {
            if (i+24<data.length()) {
                parts.add(data.substring(i, i+24));
            } else {
                parts.add(data.substring(i));
            }
        }
        byte[][] result = new byte[parts.size()][];
        for(int i=0;i<parts.size();++i) {
            result[i] = framePacket(0xab, PushContent, 0x01, parts.size(), i, parts.get(i));
        }
        return result;
    }

    public static String transliterate(String message){
        char[] abcCyr =   {' ','а','б','в','г','д','е','ё', 'ж','з','и','й','к','л','м','н','о','п','р','с','т','у','ф','х', 'ц','ч', 'ш','щ','ъ','ы','ь','э', 'ю','я','А','Б','В','Г','Д','Е','Ё', 'Ж','З','И','Й','К','Л','М','Н','О','П','Р','С','Т','У','Ф','Х', 'Ц', 'Ч','Ш', 'Щ','Ъ','Ы','Ь','Э','Ю','Я','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};
        String[] abcLat = {" ","a","b","v","g","d","e","e","zh","z","i","y","k","l","m","n","o","p","r","s","t","u","f","h","ts","ch","sh","sch", "","i", "","e","ju","ja","A","B","V","G","D","E","E","Zh","Z","I","Y","K","L","M","N","O","P","R","S","T","U","F","H","Ts","Ch","Sh","Sch", "","I", "","E","Ju","Ja","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {
            boolean found = false;
            for (int x = 0; x < abcCyr.length; x++ ) {
                if (message.charAt(i) == abcCyr[x]) {
                    builder.append(abcLat[x]);
                    found = true;
                }
            }
            if (!found) {
                builder.append(message.charAt(i));
            }
        }
        return builder.toString();
    }

    byte[] framePacket(int prefix, int type, int suffix, int parts, int part, String payload) {
        byte[] result = new byte[100];
        int headerEnd = 6;
        result[0] = (byte)prefix;
        result[1] = (byte)(headerEnd+payload.length()+1);
        result[2] = (byte)type;
        result[3] = (byte)suffix;
        result[4] = (byte)parts;
        result[5] = (byte)(part + 1);
        byte[] bPayload = payload.getBytes();
        for(int i=0;i<bPayload.length;++i) {
            result[headerEnd + i] = bPayload[i];
        }
        int msgEnd = bPayload.length + headerEnd;
        result[msgEnd] = checkSum(result, msgEnd);
        byte[] output = new byte[msgEnd+1];
        System.arraycopy(result, 0, output, 0, msgEnd + 1);
        return output;
    }

    public static String print(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (byte b : bytes) {
            sb.append(String.format("0x%02X ", b));
        }
        sb.append("]");
        return sb.toString();
    }

    public static void main(String[] args) {
        BrtlUtils subject = new BrtlUtils();
        byte[][] test = subject.pushContent("+74957555923");
        System.out.println(print(test[0]));
    }

    public static byte checkSum(byte[] data, int len)
    {
        int[] tmp = new int[len];
        for (int i = 0; i < len; i++) {
            tmp[i] = data[i] & 0xFF;
        }
        int i = 0;
        for(int j=0;j<len;++j) {
            for(int k=0;k<8;++k) {
                if (((tmp[j]^i) & 0x1) == 1) {
                    i = (i^0x18) >> 1 | 0x80;
                } else {
                    i >>=1;
                }
                tmp[j] >>= 1;
            }
        }
        return (byte)i;
    }
}
