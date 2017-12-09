package com.lin.zipfile;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static int BUFFER = 512;
    private static long TOOBIG = 0x6400000; // 100 MB
    private static int TOOMANY = 1024;
    private String dataDir;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dataDir = getDataDir() + "/Target";
        new File(dataDir).mkdir();

        try {
            InputStream in = getAssets().open("hello.hpk");
            Log.d(TAG, "onCreate: " + isZipFile(in));

            if (isZipFile(in)) {
                in.reset();
                Log.d(TAG, "onCreate: A zip file" + stream2string(in));
                in.reset();
                unzip(in);
            } else {
                in.reset();
                Log.d(TAG, "onCreate: Not a zip file " + stream2string(in));
            }
            //ZipInputStream zin = new ZipInputStream(in);
            //String str = stream2string(zin);
            //Log.d(TAG, "onCreate: " + str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void unzip(InputStream in) {
        ZipInputStream zin = new ZipInputStream(new BufferedInputStream(in));
        ZipEntry entry;
        int entries = 0;
        int total = 0;
        try {
            while ((entry = zin.getNextEntry()) != null) {
                Log.d(TAG, "unzip: extracting " + entry);
                int count;
                byte data[] = new byte[BUFFER];
                String name  = validateFilename(entry.getName(), ".");
                if (entry.isDirectory()) {
                    Log.d(TAG, "unzip: creating dir " + name);
                    String destName = dataDir + "/" + name;
                    new File(destName).mkdir();
                    continue;
                }

                FileOutputStream fos = new FileOutputStream(dataDir + "/" +name);
                BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
                while (total + BUFFER <= TOOBIG && (count = zin.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                    total += count;
                }

                dest.flush();
                dest.close();
                zin.closeEntry();
                entries ++;
                if (entries > TOOMANY) {
                    throw new IllegalStateException("Too many files to unzip.");
                }

                if (total + BUFFER > TOOBIG) {
                    throw new IllegalStateException("File being unzipped is too big.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                zin.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String validateFilename(String name, String targetDir) throws java.io.IOException {
        File f = new File(name);
        String canonicalPath = f.getCanonicalPath();
        File td = new File (targetDir);
        String canonicalTD = td.getCanonicalPath();
        if (canonicalPath.startsWith(canonicalTD)) {
            return canonicalPath;
        } else {
            throw new IllegalStateException("File is outside extraction target directory.");
        }
    }

    private String stream2string(InputStream in) {
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line+"\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        return sb.toString();
    }

    private boolean isZipFile(InputStream in) {
        try {
            boolean isZipped = new ZipInputStream(in).getNextEntry() != null;
            return isZipped;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
