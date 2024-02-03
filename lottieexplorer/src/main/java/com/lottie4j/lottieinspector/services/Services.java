package com.lottie4j.lottieinspector.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Logger;

public class Services {
    Logger logger = Logger.getLogger("info");

    private TreeView<String> treeView;

    public Services(TreeView<String> treeView) {
        this.treeView = treeView;
    }

    public void handleJsonFile(String path) throws FileNotFoundException {
        File file = new File(path);
        FileReader fileReader = new FileReader(file);
        JsonObject jsonObject = JsonParser.parseReader(fileReader).getAsJsonObject();
        treeView.getRoot().setValue(file.getName());
        initialize(jsonObject, file.getName(), treeView.getRoot());
    }

    public void handleJsonArray(JsonArray jsonArray, String name, TreeItem<String> parent) {
        logger.info("Array size: " + jsonArray.size() + "\n");
        for (JsonElement jsonElement : jsonArray) {
            if (jsonElement.isJsonPrimitive()) {
                logger.info(jsonElement.getAsString() + ": is a primitive" + "\n");
                parent.getChildren().add(new TreeItem<>(name + ": " + jsonElement));
            } else if (jsonElement.isJsonObject()) {
                logger.info(String.valueOf(jsonElement.getAsJsonObject()) + "\n");
                TreeItem<String> obj = new TreeItem<>(name + " " + parent.getChildren().size());
                parent.getChildren().add(obj);
                handleJsonObject(jsonElement.getAsJsonObject(), obj.getValue(), obj);
            } else if (jsonElement.isJsonArray()) {
                logger.info("Nested Array:" + jsonElement + "\n");
                TreeItem<String> array = new TreeItem<>(String.valueOf(parent.getChildren().size()));
                parent.getChildren().add(array);
                handleJsonArray(jsonElement.getAsJsonArray(), array.getValue(), array);
            }
        }
    }

    public void handleJsonObject(JsonObject jsonObject, String name, TreeItem<String> parent) {
        logger.info(String.valueOf(jsonObject));
        for (String key : jsonObject.keySet()) {
            JsonElement jsonElement = jsonObject.get(key);
            if (jsonElement.isJsonArray()) {
                logger.info(key + ": is an array" + "\n");
                TreeItem<String> array = new TreeItem<>(key);
                parent.getChildren().add(array);
                handleJsonArray(jsonElement.getAsJsonArray(), key, array);
            } else if (jsonElement.isJsonObject()) {
                logger.info(key + ": is an object" + "\n");
                TreeItem<String> obj = new TreeItem<>(key);
                parent.getChildren().add(obj);
                handleJsonObject(jsonElement.getAsJsonObject(), key, obj);
            } else if (jsonElement.isJsonPrimitive()) {
                logger.info(key + ": is a primitive" + "\n");
                parent.getChildren().add(new TreeItem<>(key + ": " + jsonElement));
            }
        }
    }

    public void initialize(JsonObject jsonObject, String name, TreeItem<String> parent) {
        for (String key : jsonObject.keySet()) {
            JsonElement jsonElement = jsonObject.get(key);
            if (jsonElement.isJsonArray()) {
                logger.info(key + ": is an array" + "\n");
                TreeItem<String> array = new TreeItem<>(key + " size(" + jsonElement.getAsJsonArray().size() + ")");
                parent.getChildren().add(array);
                handleJsonArray(jsonElement.getAsJsonArray(), key, array);
            } else if (jsonElement.isJsonObject()) {
                logger.info(key + ": is an object" + "\n");
                TreeItem<String> object = new TreeItem<>(key);
                parent.getChildren().add(object);
            } else if (jsonElement.isJsonPrimitive()) {
                logger.info(key + ": is a primitive" + "\n");
                parent.getChildren().add(new TreeItem<>(key + ": " + jsonElement));
            }
        }
    }

}
