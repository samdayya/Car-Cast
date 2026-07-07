package com.jadn.cc.services;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;

public class DeserializeHistory {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java com.jadn.cc.services.DeserializeHistory <history.prop>");
            System.exit(1);
        }
        File file = new File(args[0]);
        if (!file.exists()) {
            System.err.println("File not found: " + file.getAbsolutePath());
            System.exit(2);
        }
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            String header = dis.readLine();
            System.out.println("HEADER: " + header);
            if (!"history version 2".equals(header)) {
                System.out.println("Unexpected header");
            }
            try (ObjectInputStream ois = new ObjectInputStream(dis)) {
                Object obj = ois.readObject();
                if (obj instanceof List) {
                    List<?> list = (List<?>) obj;
                    System.out.println("Entries: " + list.size());
                    for (int i = 0; i < list.size(); i++) {
                        Object entry = list.get(i);
                        if (entry instanceof HistoryEntry) {
                            HistoryEntry he = (HistoryEntry) entry;
                            System.out.println((i + 1) + ": [" + he.subscription + "] " + he.podcastURL);
                        } else {
                            System.out.println((i + 1) + ": " + entry);
                        }
                    }
                } else {
                    System.out.println("Unexpected object type: " + obj.getClass().getName());
                }
            }
        }
    }
}

class HistoryEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    String subscription;
    String podcastURL;
}
