package com.example.chatbot;

import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class HwpReader {

    public static String getHwpText(String filename) throws IOException, DataFormatException {
        OleFile f = new OleFile(filename);
        List<String> dirs = f.listdir();

        // HWP 파일 검증
        if (!dirs.contains("FileHeader") || !dirs.contains("\u0005HwpSummaryInformation")) {
            throw new IOException("Not Valid HWP.");
        }

        // 문서 포맷 압축 여부 확인
        byte[] headerData = f.openstream("FileHeader");
        boolean isCompressed = (headerData[36] & 1) == 1;

        // Body Sections 불러오기
        List<String> sections = new ArrayList<>();
        for (String d : dirs) {
            if (d.startsWith("BodyText/Section")) {
                sections.add(d);
            }
        }

        // 전체 text 추출
        StringBuilder text = new StringBuilder();
        for (String section : sections) {
            byte[] data = f.openstream(section);
            byte[] unpackedData;
            if (isCompressed) {
                unpackedData = decompress(data);
            } else {
                unpackedData = data;
            }

            // 각 Section 내 text 추출
            int i = 0;
            int size = unpackedData.length;
            while (i < size) {
                int header = (unpackedData[i] & 0xFF) |
                        ((unpackedData[i + 1] & 0xFF) << 8) |
                        ((unpackedData[i + 2] & 0xFF) << 16) |
                        ((unpackedData[i + 3] & 0xFF) << 24);
                int recType = header & 0x3FF;
                int recLen = (header >> 20) & 0xFFF;

                if (recType == 67) {
                    String recData = new String(unpackedData, i + 4, recLen, "UTF-16LE");
                    text.append(recData).append("\n");
                }

                i += 4 + recLen;
            }
        }

        return text.toString();
    }

    private static byte[] decompress(byte[] data) throws DataFormatException {
        Inflater inflater = new Inflater(true);
        inflater.setInput(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];
        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    public static void main(String[] args) {
        try {
            String text = getHwpText("example.hwp");
            System.out.println(text);
        } catch (IOException | DataFormatException e) {
            e.printStackTrace();
        }
    }
}

class OleFile {
    private final FileInputStream fis;

    public OleFile(String filename) throws IOException {
        fis = new FileInputStream(filename);
    }

    public byte[] openstream(String streamName) throws IOException {
        fis.getChannel().position(0); // Ensure that we are at the beginning of the file
        return IOUtils.toByteArray(fis);
    }

    public List<String> listdir() throws IOException {
        List<String> dirs = new ArrayList<>();
        // Add logic here to list the directories in the HWP file
        return dirs;
    }
}

