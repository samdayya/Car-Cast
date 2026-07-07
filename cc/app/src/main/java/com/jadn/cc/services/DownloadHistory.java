package com.jadn.cc.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.jadn.cc.core.Config;
import com.jadn.cc.core.Sayer;

/**
 * The history of all downloaded episodes. Backed by a human-readable file.
 */
public class DownloadHistory implements Sayer {
    private static final String UNKNOWN_SUBSCRIPTION = "unknown";
    private static final String HISTORY_TWO_HEADER = "history version 2";
    private static final String HISTORY_THREE_HEADER = "history version 3";

    private List<HistoryEntry> historyEntries = new ArrayList<HistoryEntry>();
    StringBuilder sb = new StringBuilder();

    Context context;

    @SuppressWarnings("unchecked")
    public DownloadHistory(Context context) {
        this.context = context;
        Config config = new Config(context);
        File historyFile = config.getPodcastRootPath("history.prop");

        if (!historyFile.exists()) {
            return;
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(historyFile));
            String firstLine = br.readLine();
            br.close();

            if (firstLine == null) {
                return;
            }

            if (firstLine.startsWith(HISTORY_THREE_HEADER)) {
                loadHumanReadable(historyFile);
                return;
            }

            if (firstLine.startsWith(HISTORY_TWO_HEADER)) {
                loadJavaSerialized(historyFile);
                save(); // migrate automatically to human-readable v3
                return;
            }

            loadOldPlainUrlFormat(historyFile);

        } catch (Throwable e) {
            Log.e(DownloadHelper.class.getName(), "error reading history file " + historyFile.toString(), e);
        }
    }

    private void loadHumanReadable(File historyFile) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(historyFile));
        String line = br.readLine(); // header

        while ((line = br.readLine()) != null) {
            if (line.trim().length() == 0) {
                continue;
            }

            String[] parts = line.split("\t", 2);
            if (parts.length != 2) {
                continue;
            }

            String subscription = URLDecoder.decode(parts[0], "UTF-8");
            String podcastURL = URLDecoder.decode(parts[1], "UTF-8");

            historyEntries.add(new HistoryEntry(subscription, podcastURL));
        }

        br.close();
    }

    @SuppressWarnings("unchecked")
    private void loadJavaSerialized(File historyFile) throws Exception {
        DataInputStream dis = new DataInputStream(new FileInputStream(historyFile));
        String line = dis.readLine();

        if (line != null && line.startsWith(HISTORY_TWO_HEADER)) {
            ObjectInputStream ois = new ObjectInputStream(dis);
            historyEntries = (List<HistoryEntry>) ois.readObject();
            ois.close();
        }

        dis.close();
    }

    private void loadOldPlainUrlFormat(File historyFile) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(historyFile));
        String line;

        while ((line = br.readLine()) != null) {
            historyEntries.add(new HistoryEntry(UNKNOWN_SUBSCRIPTION, line));
        }

        br.close();

        save(); // migrate old format to v3
    }

    public void add(MetaNet metaNet) {
        historyEntries.add(new HistoryEntry(metaNet.getSubscription(), metaNet.getUrl()));
        save();
    }

    public boolean contains(MetaNet metaNet) {
        for (HistoryEntry historyEntry : historyEntries) {
            if (!historyEntry.subscription.equals(UNKNOWN_SUBSCRIPTION) &&
                    !historyEntry.subscription.equals(metaNet.getSubscription())) {
                continue;
            }

            if (historyEntry.podcastURL.equals(metaNet.getUrl())) {
                return true;
            }
        }

        return false;
    }

    public int eraseHistory() {
        int size = historyEntries.size();
        historyEntries = new ArrayList<HistoryEntry>();
        save();
        return size;
    }

    public int eraseHistory(String subscription) {
        int size = historyEntries.size();
        List<HistoryEntry> nh = new ArrayList<HistoryEntry>();

        for (HistoryEntry he : historyEntries) {
            if (!he.subscription.equals(subscription)) {
                nh.add(he);
            }
        }

        historyEntries = nh;
        save();

        return size - nh.size();
    }

    private void save() {
        Config config = new Config(context);
        File historyFile = config.getPodcastRootPath("history.prop");

        try {
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(historyFile), "UTF-8")
            );

            writer.write(HISTORY_THREE_HEADER);
            writer.newLine();

            for (HistoryEntry entry : historyEntries) {
                writer.write(URLEncoder.encode(entry.subscription, "UTF-8"));
                writer.write("\t");
                writer.write(URLEncoder.encode(entry.podcastURL, "UTF-8"));
                writer.newLine();
            }

            writer.close();

        } catch (Throwable e) {
            say("problem writing history file: " + historyFile + " ex:" + e);
        }
    }

    @Override
    public void say(String text) {
        sb.append(text);
        sb.append('\n');
    }

    public int size() {
        return historyEntries.size();
    }
}