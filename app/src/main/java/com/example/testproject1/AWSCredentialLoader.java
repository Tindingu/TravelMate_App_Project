package com.example.testproject1;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class AWSCredentialLoader {
    public static Map<String, String> load(Context context) {
        Map<String, String> map = new HashMap<>();

        try {
            InputStream is = context.getResources().openRawResource(R.raw.awscredentials);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = br.readLine()) != null) {
                if (!line.contains("=")) continue;
                String[] p = line.split("=");
                map.put(p[0].trim(), p[1].trim());
            }

            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }
}
