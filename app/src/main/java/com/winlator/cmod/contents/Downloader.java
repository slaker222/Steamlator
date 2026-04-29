package com.winlator.cmod.contents;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class Downloader {

    public static boolean downloadFile(String address, File file) {
        try {
            URL url = new URL(address);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Steamlator/1.0");
            connection.setRequestProperty("Accept", "*/*");
            connection.connect();

            // download the file
            InputStream input = connection.getInputStream();

            // Output stream
            OutputStream output = new FileOutputStream(file.getAbsolutePath());

            byte[] data = new byte[1024];

            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }

            // flushing output
            output.flush();

            // closing streams
            output.close();
            input.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String downloadString(String address) {
        try {
            URL url = new URL(address);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Steamlator/1.0");
            connection.setRequestProperty("Accept", "application/json");
            connection.connect();

            InputStream input = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
