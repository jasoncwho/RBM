package com.robotmon.rbm.heart;

import android.util.Log;

import com.robotmon.rbm.RbmApp;
import com.robotmon.rbm.model.FriendRecord;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Persisted heart-sending/receiving history: per-friend receive counts (used
 * to recognize repeat senders) plus aggregate sent/received totals. Ported
 * from Tsum.prototype.readRecord()/saveRecord()/releaseRecord()/clear() and
 * the this.record['hearts_count'] bookkeeping in index.js.
 *
 * <p>Friend recognition itself is approximated -- see SenderRecognizer.
 */
public class RecordStore {
    private static final String TAG = "RecordStore";
    private static final String RECORD_DIR = "tsum_record";
    private static final String RECORD_FILE = "record.json";

    private final Map<String, FriendRecord> friends = new HashMap<>();
    private int receivedCount = 0;
    private int sentCount = 0;

    public int getReceivedCount() {
        return receivedCount;
    }

    public int getSentCount() {
        return sentCount;
    }

    public void incrementReceivedCount() {
        receivedCount++;
    }

    public void incrementSentCount() {
        sentCount++;
    }

    /**
     * Finds an existing friend whose stored name-image signature matches
     * {@code nameImg} closely enough, or registers {@code nameImg} as a new
     * friend. Ported from Tsum.prototype.recognizeSender(): matches the
     * original's quirk of returning "" (not the new record's id) the first
     * time a friend is seen, so countReceiveHeart() only starts counting
     * from their second gift onward.
     */
    public String recognizeSender(Mat nameImg) {
        double[] sig = SenderRecognizer.signature(nameImg);
        String bestId = null;
        double bestDistance = Double.MAX_VALUE;
        for (Map.Entry<String, FriendRecord> entry : friends.entrySet()) {
            double d = SenderRecognizer.distance(sig, entry.getValue().nameSignature);
            if (d < bestDistance) {
                bestDistance = d;
                bestId = entry.getKey();
            }
        }
        if (bestId != null && bestDistance <= SenderRecognizer.MATCH_THRESHOLD) {
            return bestId;
        }
        String id = "f_" + System.currentTimeMillis();
        FriendRecord record = new FriendRecord();
        record.nameSignature = sig;
        record.lastReceiveTime = System.currentTimeMillis();
        friends.put(id, record);
        return "";
    }

    /* Bumps today's receive count for a recognized friend. Ported from countReceiveHeart(). */
    public void countReceiveHeart(String friendId) {
        if (friendId == null || friendId.isEmpty()) {
            return;
        }
        FriendRecord record = friends.get(friendId);
        if (record == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long dayTime = now / (24 * 60 * 60 * 1000);
        record.receiveCounts.merge(dayTime, 1, Integer::sum);
        record.lastReceiveTime = now;
    }

    private File recordDir() {
        File dir = new File(RbmApp.getInstance().getFilesDir(), RECORD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /* Persists friends + aggregate counts to disk. Ported from saveRecord(). */
    public void save() {
        try {
            JSONObject root = new JSONObject();
            root.put("receivedCount", receivedCount);
            root.put("sentCount", sentCount);
            JSONObject friendsJson = new JSONObject();
            for (Map.Entry<String, FriendRecord> entry : friends.entrySet()) {
                FriendRecord r = entry.getValue();
                JSONObject fj = new JSONObject();
                fj.put("lastReceiveTime", r.lastReceiveTime);
                JSONObject counts = new JSONObject();
                for (Map.Entry<Long, Integer> c : r.receiveCounts.entrySet()) {
                    counts.put(String.valueOf(c.getKey()), c.getValue());
                }
                fj.put("receiveCounts", counts);
                JSONArray sig = new JSONArray();
                for (double v : r.nameSignature) {
                    sig.put(v);
                }
                fj.put("nameSignature", sig);
                friendsJson.put(entry.getKey(), fj);
            }
            root.put("friends", friendsJson);
            try (FileOutputStream out = new FileOutputStream(new File(recordDir(), RECORD_FILE))) {
                out.write(root.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (JSONException | IOException e) {
            Log.w(TAG, "Failed to save record", e);
        }
    }

    /* Loads friends + aggregate counts from disk, if present. Ported from readRecord(). */
    public void load() {
        File file = new File(recordDir(), RECORD_FILE);
        if (!file.exists()) {
            return;
        }
        try {
            String txt = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(txt);
            receivedCount = root.optInt("receivedCount", 0);
            sentCount = root.optInt("sentCount", 0);
            friends.clear();
            JSONObject friendsJson = root.optJSONObject("friends");
            if (friendsJson != null) {
                java.util.Iterator<String> keys = friendsJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject fj = friendsJson.getJSONObject(key);
                    FriendRecord r = new FriendRecord();
                    r.lastReceiveTime = fj.optLong("lastReceiveTime", 0);
                    JSONObject counts = fj.optJSONObject("receiveCounts");
                    if (counts != null) {
                        java.util.Iterator<String> dayKeys = counts.keys();
                        while (dayKeys.hasNext()) {
                            String dayKey = dayKeys.next();
                            r.receiveCounts.put(Long.valueOf(dayKey), counts.getInt(dayKey));
                        }
                    }
                    JSONArray sig = fj.optJSONArray("nameSignature");
                    if (sig != null) {
                        double[] arr = new double[sig.length()];
                        for (int i = 0; i < arr.length; i++) {
                            arr[i] = sig.getDouble(i);
                        }
                        r.nameSignature = arr;
                    }
                    friends.put(key, r);
                }
            }
        } catch (JSONException | IOException e) {
            Log.w(TAG, "Failed to load record", e);
        }
    }

    /** Drops in-memory friend records/counters, keeping the file on disk untouched. Ported from releaseRecord(). */
    public void release() {
        friends.clear();
        receivedCount = 0;
        sentCount = 0;
    }

    /* Deletes the on-disk record directory entirely. Ported from Tsum.prototype.clear(). */
    public void clear() {
        release();
        deleteRecursive(recordDir());
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
}