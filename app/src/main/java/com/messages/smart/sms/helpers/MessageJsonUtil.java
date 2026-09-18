package com.messages.smart.sms.helpers;

import com.messages.smart.sms.models.MessagesModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class MessageJsonUtil {
    private MessageJsonUtil() {
    }

    public static String toJsonString(List<MessagesModel> messages) {
        JSONArray array = new JSONArray();
        for (MessagesModel model : messages) {
            array.put(toJson(model));
        }
        return array.toString();
    }

    public static List<MessagesModel> fromJsonString(String json) {
        List<MessagesModel> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                MessagesModel model = fromJson(array.getJSONObject(i));
                if (model != null) {
                    list.add(model);
                }
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    public static void removeByThreadId(List<MessagesModel> list, String threadId) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).getThreadId().equals(threadId)) {
                list.remove(i);
            }
        }
    }

    private static JSONObject toJson(MessagesModel model) {
        JSONObject object = new JSONObject();
        try {
            object.put("threadId", model.getThreadId());
            object.put("name", model.getName());
            object.put("address", model.getAddress());
            object.put("body", model.getBody());
            object.put("date", model.getDate());
            object.put("type", model.getType());
            object.put("read", model.getRead());
            object.put("contactPhotoUri", model.getContactPhotoUri());
            object.put("category", model.category);
            object.put("unreadCount", model.getUnreadCount());
        } catch (JSONException ignored) {
        }
        return object;
    }

    private static MessagesModel fromJson(JSONObject object) {
        try {
            MessagesModel model = new MessagesModel(object.getString("threadId"), object.getString("name"), object.getString("address"), object.getString("body"), object.getLong("date"), object.getInt("type"), object.getInt("read"), object.optString("contactPhotoUri", null), object.optInt("unreadCount", 0));
            model.category = object.optString("category", "");
            return model;
        } catch (JSONException e) {
            return null;
        }
    }
}