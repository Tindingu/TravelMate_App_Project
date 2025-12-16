package com.example.testproject1;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class AWSConfigLoader {

    public static HashMap<String, String> load(Context ctx) {
        HashMap<String, String> map = new HashMap<>();

        try {
            InputStream is = ctx.getResources().openRawResource(R.raw.awscredentials);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = br.readLine()) != null) {
                if (!line.contains("=")) continue;
                String[] parts = line.split("=");
                map.put(parts[0].trim(), parts[1].trim());
            }

            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }
}
